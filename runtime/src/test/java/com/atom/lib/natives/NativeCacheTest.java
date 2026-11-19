// SPDX-License-Identifier: Apache-2.0
package com.atom.lib.natives;

import com.atom.lib.NativePreparation;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.HexFormat;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.*;

class NativeCacheTest {
    private static final byte[] LIBRARY = {10, 20, 30};
    private static final String ENTRY = "linux/x64/libskija.so";
    private static final String PACK_ENTRY = "skija/0.143.18/libskija.so";
    @TempDir Path temp;

    @Test void fallsBackRepairsCorruptionAndReusesTheVerifiedFileOffline() throws Exception {
        byte[] jar = archive(ENTRY, LIBRARY);
        var mirrorCalls = new AtomicInteger();
        var centralCalls = new AtomicInteger();
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/mirror", exchange -> {
            mirrorCalls.incrementAndGet();
            exchange.sendResponseHeaders(429, -1);
            exchange.close();
        });
        server.createContext("/central", exchange -> {
            centralCalls.incrementAndGet();
            exchange.sendResponseHeaders(200, jar.length);
            try (var response = exchange.getResponseBody()) { response.write(jar); }
        });
        server.start();
        try {
            var entry = new NativeManifest.Entry(ENTRY, PACK_ENTRY, blob(LIBRARY));
            var artifact = new NativeManifest.Archive(blob(jar),
                    List.of(url(server, "/mirror"), url(server, "/central")), List.of(entry));
            NativeCache cache = new NativeCache(temp, "linux-x64");
            Path file = cache.prepare(List.of(artifact), null, null, NativePreparation.Source.AUTO,
                    Duration.ofSeconds(8), ignored -> {}).get(entry);
            assertArrayEquals(LIBRARY, Files.readAllBytes(file));
            assertEquals(1, mirrorCalls.get());
            assertEquals(1, centralCalls.get());
            assertEquals(file, cache.prepare(List.of(artifact), null, null, NativePreparation.Source.OFFLINE,
                    Duration.ofSeconds(8), ignored -> {}).get(entry));
            Files.write(file, new byte[]{30, 20, 10});
            assertThrows(IOException.class, () -> cache.prepare(List.of(artifact), null, null,
                    NativePreparation.Source.OFFLINE, Duration.ofSeconds(8), ignored -> {}));
            cache.prepare(List.of(artifact), null, null, NativePreparation.Source.AUTO,
                    Duration.ofSeconds(8), ignored -> {});
            assertArrayEquals(LIBRARY, Files.readAllBytes(file));
            assertEquals(1, mirrorCalls.get());
            assertEquals(2, centralCalls.get());
        } finally {
            server.stop(0);
        }
    }

    @Test void importsTheSameCacheEntryAndRejectsArchiveTraversal() throws Exception {
        var entry = new NativeManifest.Entry(ENTRY, PACK_ENTRY, blob(LIBRARY));
        byte[] zip = archive(PACK_ENTRY, LIBRARY);
        var requested = new NativeManifest.Archive(blob(archive(ENTRY, LIBRARY)),
                List.of(), List.of(entry));
        var bundle = new NativeManifest.Archive(blob(zip), List.of(), List.of(entry));
        NativeCache cache = new NativeCache(temp.resolve("cache"), "linux-x64");
        Path imported = temp.resolve("offline.zip");
        Files.write(imported, archive("../libskija.so", LIBRARY));
        var badBundle = new NativeManifest.Archive(blob(Files.readAllBytes(imported)), List.of(), List.of(entry));
        assertThrows(IOException.class, () -> cache.prepare(List.of(requested), badBundle, imported,
                NativePreparation.Source.OFFLINE, Duration.ofSeconds(8), ignored -> {}));
        Files.write(imported, zip);
        var installed = cache.prepare(List.of(requested), bundle, imported, NativePreparation.Source.OFFLINE,
                Duration.ofSeconds(8), ignored -> {});
        assertArrayEquals(LIBRARY, Files.readAllBytes(installed.get(entry)));
        assertEquals(installed, cache.prepare(List.of(requested), bundle, null, NativePreparation.Source.OFFLINE,
                Duration.ofSeconds(8), ignored -> {}));
    }

    @Test void concurrentPreparationsShareOneDownloadAndRedirectsAreNotFollowed() throws Exception {
        byte[] jar = archive(ENTRY, LIBRARY);
        var redirectedCalls = new AtomicInteger();
        var downloads = new AtomicInteger();
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/redirect", exchange -> {
            exchange.getResponseHeaders().add("Location", "/redirected");
            exchange.sendResponseHeaders(302, -1);
            exchange.close();
        });
        server.createContext("/redirected", exchange -> {
            redirectedCalls.incrementAndGet();
            exchange.sendResponseHeaders(404, -1);
            exchange.close();
        });
        server.createContext("/good", exchange -> {
            downloads.incrementAndGet();
            exchange.sendResponseHeaders(200, jar.length);
            try (var response = exchange.getResponseBody()) { response.write(jar); }
        });
        server.start();
        try {
            var entry = new NativeManifest.Entry(ENTRY, PACK_ENTRY, blob(LIBRARY));
            var artifact = new NativeManifest.Archive(blob(jar),
                    List.of(url(server, "/redirect"), url(server, "/good")), List.of(entry));
            var first = prepareInBackground(artifact);
            var second = prepareInBackground(artifact);
            assertEquals(first.get(), second.get());
            assertEquals(0, redirectedCalls.get());
            assertEquals(1, downloads.get());
        } finally {
            server.stop(0);
        }
    }

    @Test void stalledTruncatedAndCorruptResponsesLeaveNoPartialNative() throws Exception {
        byte[] jar = archive(ENTRY, LIBRARY);
        var unblock = new CountDownLatch(1);
        var executor = Executors.newCachedThreadPool();
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.setExecutor(executor);
        server.createContext("/stalled", exchange -> {
            exchange.sendResponseHeaders(200, jar.length);
            try (var response = exchange.getResponseBody()) {
                response.write(jar, 0, 1);
                response.flush();
                unblock.await(12, TimeUnit.SECONDS);
            } catch (IOException ignored) {
                // The stalled response must be abandoned before the next source.
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
            }
        });
        server.createContext("/short", exchange -> {
            exchange.sendResponseHeaders(200, 0);
            try (var response = exchange.getResponseBody()) { response.write(jar, 0, jar.length / 2); }
        });
        byte[] corrupt = jar.clone();
        corrupt[20] ^= 1;
        server.createContext("/wrong", exchange -> {
            exchange.sendResponseHeaders(200, corrupt.length);
            try (var response = exchange.getResponseBody()) { response.write(corrupt); }
        });
        server.createContext("/missing", exchange -> {
            exchange.sendResponseHeaders(404, -1);
            exchange.close();
        });
        server.start();
        try {
            var entry = new NativeManifest.Entry(ENTRY, PACK_ENTRY, blob(LIBRARY));
            var artifact = new NativeManifest.Archive(blob(jar),
                    List.of(url(server, "/stalled"), url(server, "/short"), url(server, "/wrong"), url(server, "/missing")),
                    List.of(entry));
            var error = assertThrows(IOException.class, () -> new NativeCache(temp, "linux-x64")
                    .prepare(List.of(artifact), null, null, NativePreparation.Source.AUTO, Duration.ofSeconds(24), ignored -> {}));
            assertTrue(error.getMessage().contains("stalled"), error.getMessage());
            assertTrue(error.getMessage().contains("ended early"), error.getMessage());
            assertTrue(error.getMessage().contains("checksum mismatch"), error.getMessage());
            assertTrue(error.getMessage().contains("404"), error.getMessage());
            assertFalse(Files.exists(entry.cachePath(temp.resolve("linux-x64"))));
            try (var paths = Files.walk(temp)) {
                assertTrue(paths.noneMatch(path -> path.getFileName().toString().endsWith(".part")));
            }
        } finally {
            unblock.countDown();
            server.stop(0);
            executor.shutdownNow();
        }
    }

    @Test void detectsAndroidBeforeLinuxAndSelectsTheJvmArchitecture() throws Exception {
        assertEquals("android-arm64", NativePlatform.detect("Linux", "aarch64", "OpenJDK", "", true).id());
        assertEquals("android-x64", NativePlatform.detect("Linux", "amd64", "OpenJDK", "android", false).id());
        assertEquals("macos-x64", NativePlatform.detect("Mac OS X", "x86_64", "OpenJDK", "", false).id());
        assertThrows(IOException.class, () -> NativePlatform.detect("Linux", "armv7", "OpenJDK", "", false));
        assertThrows(IOException.class, () -> NativePlatform.detect("FreeBSD", "amd64", "OpenJDK", "", false));
    }

    private CompletableFuture<Path> prepareInBackground(NativeManifest.Archive artifact) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return new NativeCache(temp, "linux-x64").prepare(List.of(artifact), null, null,
                        NativePreparation.Source.AUTO, Duration.ofSeconds(8), ignored -> {}).values().iterator().next();
            } catch (IOException failure) {
                throw new java.io.UncheckedIOException(failure);
            }
        });
    }

    private static URI url(HttpServer server, String path) {
        return URI.create("http://127.0.0.1:" + server.getAddress().getPort() + path);
    }

    private static NativeManifest.Blob blob(byte[] contents) throws Exception {
        return new NativeManifest.Blob(contents.length,
                HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(contents)));
    }

    private static byte[] archive(String entry, byte[] contents) throws IOException {
        var bytes = new ByteArrayOutputStream();
        try (var zip = new ZipOutputStream(bytes)) {
            zip.putNextEntry(new ZipEntry(entry));
            zip.write(contents);
        }
        return bytes.toByteArray();
    }
}
