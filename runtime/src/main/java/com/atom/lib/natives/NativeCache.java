// SPDX-License-Identifier: Apache-2.0
package com.atom.lib.natives;

import com.atom.lib.NativePreparation;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;
import java.util.zip.ZipFile;

import static com.atom.lib.natives.NativeManifest.*;

final class NativeCache {
    private static final ReentrantLock LOCAL_LOCK = new ReentrantLock();
    private final Path root;

    NativeCache(Path cache, String platform) {
        root = cache.toAbsolutePath().normalize().resolve(platform);
    }

    Map<Entry, Path> prepare(List<Archive> artifacts, Archive bundle, Path offlineZip, NativePreparation.Source mode,
                             Duration timeout, Consumer<String> progress) throws IOException {
        long deadline = System.nanoTime() + timeout.toNanos();
        try {
            return prepareLocked(artifacts, bundle, offlineZip, mode, progress, deadline);
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw new IOException("Native preparation interrupted", interrupted);
        }
    }

    private Map<Entry, Path> prepareLocked(List<Archive> artifacts, Archive bundle, Path offlineZip,
                                            NativePreparation.Source mode, Consumer<String> progress, long deadline)
            throws IOException, InterruptedException {
        if (!LOCAL_LOCK.tryLock(Math.max(0, deadline - System.nanoTime()), TimeUnit.NANOSECONDS)) {
            throw new IOException("Timed out waiting for AtomLib native cache");
        }
        try {
            Files.createDirectories(root);
            try (FileChannel channel = FileChannel.open(root.resolve(".lock"),
                    StandardOpenOption.CREATE, StandardOpenOption.WRITE);
                 FileLock ignored = lockCache(channel, deadline)) {
                if (offlineZip != null) importBundle(bundle, offlineZip, deadline, progress);
                List<Archive> missing = new ArrayList<>();
                Map<Entry, Path> result = new LinkedHashMap<>();
                for (Archive artifact : artifacts) {
                    boolean complete = true;
                    for (Entry entry : artifact.entries()) {
                        Path target = entry.cachePath(root);
                        result.put(entry, target);
                        complete &= verified(target, entry.blob(), deadline);
                    }
                    if (!complete) missing.add(artifact);
                }
                if (missing.isEmpty()) return result;
                if (mode == NativePreparation.Source.OFFLINE) {
                    throw new IOException("Native cache missing or damaged. Import the matching offline ZIP.");
                }
                var download = new NativeDownload();
                if (bundle != null && !bundle.sources().isEmpty()) {
                    download(bundle, true, mode, deadline, download, progress);
                } else {
                    for (int i = 0; i < missing.size(); i++) {
                        remaining(deadline);
                        long remaining = deadline - System.nanoTime();
                        long reserve = Math.min(TimeUnit.SECONDS.toNanos(8), remaining / missing.size());
                        long archiveDeadline = deadline - (missing.size() - i - 1) * reserve;
                        download(missing.get(i), false, mode, archiveDeadline, download, progress);
                    }
                }
                return result;
            }
        } finally {
            LOCAL_LOCK.unlock();
        }
    }

    private static FileLock lockCache(FileChannel channel, long deadline) throws IOException, InterruptedException {
        while (true) {
            remaining(deadline);
            FileLock lock = channel.tryLock();
            if (lock != null) return lock;
            Thread.sleep(50);
        }
    }

    private void importBundle(Archive bundle, Path zip, long deadline, Consumer<String> progress) throws IOException {
        if (bundle == null) throw new IOException("No matching offline native bundle");
        if (!Files.isRegularFile(zip)) throw new IOException("Offline ZIP does not exist: " + zip);
        progress.accept("Verifying offline ZIP");
        Path copy = Files.createTempFile(root, ".import-", ".part");
        try {
            try (InputStream input = Files.newInputStream(zip)) {
                verifyStream(input, copy, bundle.blob(), deadline);
            }
            extract(copy, bundle, true, deadline);
        } finally {
            Files.deleteIfExists(copy);
        }
    }

    private void download(Archive archive, boolean bundle, NativePreparation.Source mode, long deadline,
                          NativeDownload download, Consumer<String> progress) throws IOException, InterruptedException {
        var urls = archive.sources();
        List<Integer> order = sourceOrder(mode, urls.size(), bundle);
        if (order.isEmpty()) throw new IOException("No configured native download for " + mode
                + ". Import the matching offline ZIP.");
        List<String> failures = new ArrayList<>();
        for (int source = 0; source < order.size(); source++) {
            int index = order.get(source);
            URI uri = urls.get(index);
            progress.accept((failures.isEmpty() ? "" : String.join("; ", failures) + " | ")
                    + "Fetching " + (bundle ? "native bundle" : archive.entries().get(0).library())
                    + " from " + uri.getHost());
            Path staged = Files.createTempFile(root, ".download-", ".part");
            try {
                // Keep time for the fallbacks even when a slow transfer still reports progress.
                long now = System.nanoTime();
                long attemptDeadline = now + (deadline - now) / (order.size() - source);
                download.fetch(uri, staged, archive.blob().size(), attemptDeadline);
                if (!verified(staged, archive.blob(), attemptDeadline)) throw new IOException("Archive checksum mismatch");
                extract(staged, archive, bundle, attemptDeadline);
                rememberSource(index);
                return;
            } catch (IOException failure) {
                failures.add(uri.getHost() + ": " + failure.getMessage());
            } finally {
                Files.deleteIfExists(staged);
            }
        }
        throw new IOException("Native download failed. " + String.join("; ", failures));
    }

    private void rememberSource(int index) {
        try {
            Files.writeString(root.resolve(".source"), Integer.toString(index));
        } catch (IOException ignored) {
            // Failure to remember the source does not invalidate the installed library.
        }
    }

    private List<Integer> sourceOrder(NativePreparation.Source mode, int count, boolean bundle) {
        if (mode == NativePreparation.Source.MIRROR) return !bundle && count > 0 ? List.of(0) : List.of();
        if (mode == NativePreparation.Source.CENTRAL) return !bundle && count > 1 ? List.of(1) : List.of();
        int preferred = 0;
        try {
            if (count > 1) preferred = Integer.parseInt(Files.readString(root.resolve(".source")).strip());
        } catch (IOException | NumberFormatException ignored) {
            // The source preference can be rebuilt on the next successful install.
        }
        var order = new ArrayList<Integer>();
        if (preferred >= 0 && preferred < count) order.add(preferred);
        for (int index = 0; index < count; index++) if (index != preferred) order.add(index);
        return order;
    }

    private void extract(Path archive, Archive manifest, boolean bundle, long deadline) throws IOException {
        Map<Path, Path> stagedFiles = new LinkedHashMap<>();
        try (ZipFile zip = new ZipFile(archive.toFile())) {
            Set<String> allowed = new HashSet<>();
            var names = new HashSet<String>();
            for (Entry entry : manifest.entries()) allowed.add(bundle ? entry.packPath() : entry.jarPath());
            var entries = zip.entries();
            while (entries.hasMoreElements()) {
                var entry = entries.nextElement();
                String name = entry.getName();
                String checked = entry.isDirectory() ? name.substring(0, name.length() - 1) : name;
                boolean notice = name.equals("SOURCES.txt") || name.startsWith("licenses/") && name.endsWith(".txt");
                if (!safeName(checked) || !names.add(checked) || bundle && !notice && !allowed.contains(name)) {
                    throw new IOException("Unexpected or duplicate native archive entry: " + name);
                }
            }
            for (Entry entry : manifest.entries()) {
                Path target = entry.cachePath(root);
                String path = bundle ? entry.packPath() : entry.jarPath();
                var member = zip.getEntry(path);
                if (member == null || member.isDirectory() || member.getSize() != entry.blob().size()) {
                    throw new IOException("Missing or invalid native archive entry: " + path);
                }
                if (verified(target, entry.blob(), deadline)) continue;
                Files.createDirectories(target.getParent());
                Path staged = Files.createTempFile(target.getParent(), ".native-", ".part");
                stagedFiles.put(target, staged);
                try (InputStream input = zip.getInputStream(member)) {
                    verifyStream(input, staged, entry.blob(), deadline);
                }
            }
            for (var staged : stagedFiles.entrySet()) {
                Files.move(staged.getValue(), staged.getKey(), StandardCopyOption.ATOMIC_MOVE,
                        StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            for (Path staged : stagedFiles.values()) Files.deleteIfExists(staged);
        }
    }

    private static boolean verified(Path file, Blob blob, long deadline) throws IOException {
        if (!Files.isRegularFile(file, LinkOption.NOFOLLOW_LINKS) || Files.size(file) != blob.size()) return false;
        try (InputStream input = Files.newInputStream(file)) {
            return matches(input, null, blob, deadline);
        }
    }

    private static void verifyStream(InputStream input, Path target, Blob blob, long deadline) throws IOException {
        if (!matches(input, target, blob, deadline)) throw new IOException("Native file length or checksum mismatch");
    }

    private static boolean matches(InputStream input, Path target, Blob blob, long deadline) throws IOException {
        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException impossible) {
            throw new AssertionError(impossible);
        }
        long count = 0;
        byte[] buffer = new byte[65536];
        try (var output = target == null ? java.io.OutputStream.nullOutputStream() : Files.newOutputStream(target)) {
            for (int read; (read = input.read(buffer)) != -1;) {
                remaining(deadline);
                count += read;
                if (count > blob.size()) return false;
                digest.update(buffer, 0, read);
                output.write(buffer, 0, read);
            }
        }
        return count == blob.size() && HexFormat.of().formatHex(digest.digest()).equals(blob.sha256());
    }

    private static void remaining(long deadline) throws IOException {
        if (deadline - System.nanoTime() <= 0) throw new IOException("Native preparation timed out");
    }
}
