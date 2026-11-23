package com.atom.lib.layout.internal;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import static org.junit.jupiter.api.Assertions.*;

/** The native must reach LWJGL even when it only exists inside a mod jar. */
class YogaNativesTest {
    private static String libraryName() {
        String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        return os.startsWith("windows") ? "lwjgl_yoga.dll"
                : os.contains("mac") || os.contains("darwin") ? "liblwjgl_yoga.dylib" : "liblwjgl_yoga.so";
    }

    @Test void extractsThePlatformLibraryFromTheNativesJar() throws Exception {
        Path directory = YogaNatives.extractNative();
        assertNotNull(directory, "the Yoga natives jar must be on the test classpath");
        Path library = directory.resolve(libraryName());
        assertTrue(Files.isRegularFile(library), library.toString());
        assertTrue(Files.size(library) > 20_000, "extracted library looks truncated: " + Files.size(library));
    }

    @Test void readsJarJarWindowsPayloadFromANonFileCodeSource(@TempDir Path bundle) throws Exception {
        String classEntry = YogaNatives.class.getName().replace('.', '/') + ".class";
        Path copy = bundle.resolve(classEntry);
        Files.createDirectories(copy.getParent());
        try (var input = YogaNatives.class.getResourceAsStream("/" + classEntry)) {
            Files.copy(input, copy);
        }

        Path index = bundle.resolve("META-INF/atomlib/native-artifacts.properties");
        Files.createDirectories(index.getParent());
        Files.writeString(index, "yogaVersion=3.3.9\n");
        byte[] x64 = {1, 4, 7};
        byte[] arm64 = {2, 5, 8};
        writeJarJar(bundle, "natives-windows", "windows/x64", x64);
        writeJarJar(bundle, "natives-windows-arm64", "windows/arm64", arm64);

        try (var loader = new URLClassLoader(new URL[]{bundle.toUri().toURL()}, null)) {
            Class<?> isolated = loader.loadClass(YogaNatives.class.getName());
            assertSame(loader, isolated.getClassLoader());
            assertFalse(Files.isRegularFile(Path.of(isolated.getProtectionDomain().getCodeSource().getLocation().toURI())));
            var read = isolated.getDeclaredMethod("readFromEmbeddedNatives", String.class, String.class);
            read.setAccessible(true);
            assertArrayEquals(x64, (byte[]) read.invoke(null, "windows/x64", "lwjgl_yoga.dll"));
            assertArrayEquals(arm64, (byte[]) read.invoke(null, "windows/arm64", "lwjgl_yoga.dll"));
        }
    }

    private static void writeJarJar(Path bundle, String classifier, String platform, byte[] payload) throws Exception {
        Path jar = bundle.resolve("META-INF/jarjar/lwjgl-yoga-3.3.9-" + classifier + ".jar");
        Files.createDirectories(jar.getParent());
        try (var output = new ZipOutputStream(Files.newOutputStream(jar))) {
            output.putNextEntry(new ZipEntry(platform + "/org/lwjgl/yoga/lwjgl_yoga.dll"));
            output.write(payload);
        }
    }

    @Test void extendsLwjglLibraryPathExactlyOnce() throws Exception {
        Path directory = YogaNatives.extractNative();
        assertNotNull(directory);
        YogaNatives.extendLibraryPath(directory);
        String first = org.lwjgl.system.Configuration.LIBRARY_PATH.get();
        assertNotNull(first);
        assertTrue(first.contains(directory.toAbsolutePath().toString()), first);
        YogaNatives.extendLibraryPath(directory);
        assertEquals(first, org.lwjgl.system.Configuration.LIBRARY_PATH.get(), "the path must not be added twice");
    }
}
