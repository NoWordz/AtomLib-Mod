package com.atom.lib.layout.internal;

import com.atom.lib.natives.ClientNatives;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Properties;
import java.util.regex.Pattern;
import java.util.zip.ZipInputStream;

/**
 * LWJGL resolves a native from {@code java.library.path} or
 * {@code org.lwjgl.librarypath}, and caches both when its Configuration class
 * initializes — long before any mod runs. A native nested inside a mod jar is
 * therefore invisible to it, which is exactly how lwjgl_yoga.dll went missing
 * inside Minecraft while every Gradle test passed.
 *
 * <p>This extracts the platform library next to the game and extends LWJGL's
 * already-initialized library path before the first Yoga call. When the
 * library is already reachable nothing is extracted.</p>
 */
public final class YogaNatives {
    private static boolean prepared;

    private YogaNatives() { }

    static synchronized void ensure() {
        ClientNatives.requireAvailable();
        if (prepared) return;
        try {
            if (!hasConfiguredNative()) {
                Path directory = extractNative();
                if (directory != null) extendLibraryPath(directory);
            }
        } catch (Exception ignored) {
            // Leave the diagnostics to LWJGL: it reports the missing library with
            // the platform it searched, which is more useful than a wrapped error.
        }
        prepared = true;
    }

    /** Preserves the host's explicitly supplied native libraries. */
    public static boolean hasConfiguredNative() {
        return containsLibrary(org.lwjgl.system.Configuration.LIBRARY_PATH.get())
                || containsLibrary(System.getProperty("java.library.path"));
    }

    private static boolean containsLibrary(String paths) {
        if (paths == null) return false;
        String library = System.mapLibraryName("lwjgl_yoga");
        return Arrays.stream(paths.split(Pattern.quote(File.pathSeparator)))
                .filter(path -> !path.isEmpty()).anyMatch(path -> Files.isRegularFile(Path.of(path).resolve(library)));
    }

    /** Configures LWJGL before any caller initializes the Yoga JNI binding. */
    public static void usePreparedNative(Path directory) {
        if (!hasConfiguredNative()) extendLibraryPath(directory);
    }

    /** Confirms that the prepared binding can initialize and run a native call. */
    public static void initializePreparedNative() {
        org.lwjgl.util.yoga.Yoga.YGConfigFree(newConfig());
    }

    static long newConfig() {
        try {
            long config = org.lwjgl.util.yoga.Yoga.YGConfigNew();
            if (config == 0) throw new IllegalStateException("Yoga could not allocate a configuration");
            return config;
        } catch (LinkageError failure) {
            ClientNatives.reportLoadFailure(failure);
            throw failure;
        }
    }

    /** Extracts the platform library into a content-addressed temp directory. */
    static Path extractNative() throws Exception {
        String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        String arch = System.getProperty("os.arch", "").toLowerCase(Locale.ROOT);
        boolean arm = arch.contains("aarch64") || arch.contains("arm64") || arch.startsWith("arm");
        String platform = os.startsWith("windows")
                ? "windows/" + (arm ? "arm64" : "x64")
                : os.contains("mac") || os.contains("darwin")
                ? "macos/" + (arm ? "arm64" : "x64")
                : "linux/" + (arm ? "arm64" : "x64");
        String fileName = os.startsWith("windows") ? "lwjgl_yoga.dll"
                : os.contains("mac") || os.contains("darwin") ? "liblwjgl_yoga.dylib" : "liblwjgl_yoga.so";
        String resource = platform + "/org/lwjgl/yoga/" + fileName;
        byte[] library = read("/" + resource);
        if (library == null) library = readFromEmbeddedNatives(platform, fileName);
        if (library == null) return null;
        Path directory = Path.of(System.getProperty("java.io.tmpdir"), "atomlib-natives", digest(library));
        Files.createDirectories(directory);
        Path target = directory.resolve(fileName);
        if (!Files.isRegularFile(target) || Files.size(target) != library.length
                || !MessageDigest.isEqual(Files.readAllBytes(target), library)) {
            Path temporary = Files.createTempFile(directory, fileName, ".tmp");
            try {
                Files.write(temporary, library);
                Files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } finally {
                Files.deleteIfExists(temporary);
            }
        }
        return directory;
    }

    private static byte[] read(String resource) throws Exception {
        try (InputStream in = YogaNatives.class.getResourceAsStream(resource)) {
            return in == null ? null : in.readAllBytes();
        }
    }

    /** Reads the bundled Windows DLL even when JarJar does not expose the native module. */
    private static byte[] readFromEmbeddedNatives(String platform, String fileName) throws IOException {
        if (!platform.startsWith("windows/")) return null;
        var index = new Properties();
        try (InputStream input = YogaNatives.class.getResourceAsStream("/META-INF/atomlib/native-artifacts.properties")) {
            if (input == null) return null;
            index.load(input);
        }
        String version = index.getProperty("yogaVersion");
        if (version == null) return null;
        String classifier = platform.equals("windows/arm64") ? "natives-windows-arm64" : "natives-windows";
        String jar = "/META-INF/jarjar/lwjgl-yoga-" + version + "-" + classifier + ".jar";
        String entry = platform + "/org/lwjgl/yoga/" + fileName;

        // Forge exposes a union filesystem root as the code source. Open the JarJar entry
        // from this class's module so neither a physical jar path nor a sibling module is required.
        try (InputStream input = YogaNatives.class.getResourceAsStream(jar)) {
            if (input == null) return null;
            try (var nested = new ZipInputStream(input)) {
                for (var candidate = nested.getNextEntry(); candidate != null; candidate = nested.getNextEntry()) {
                    if (entry.equals(candidate.getName())) return nested.readAllBytes();
                }
            }
        }
        return null;
    }

    static void extendLibraryPath(Path directory) {
        String current = org.lwjgl.system.Configuration.LIBRARY_PATH.get();
        String path = directory.toAbsolutePath().toString();
        if (current != null && Arrays.asList(current.split(Pattern.quote(File.pathSeparator))).contains(path)) return;
        org.lwjgl.system.Configuration.LIBRARY_PATH.set(current == null || current.isEmpty() ? path : current + File.pathSeparator + path);
    }

    private static String digest(byte[] library) throws Exception {
        byte[] hash = MessageDigest.getInstance("SHA-256").digest(library);
        return HexFormat.of().formatHex(hash);
    }
}
