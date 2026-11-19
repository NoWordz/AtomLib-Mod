package com.atom.lib.frontend.checker;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DependencyCheckTest {
    private static final String ZIP_PIN = "zip-content-sha256:8e23be75b52683c173a433e4ae882e59f3bb82d1514a977559479ddda19b8967";
    private static final String API = "fixture/Api.class";
    private static final String RESOURCE = "assets/feedback.json";
    @TempDir Path directory;

    @Test void systemTypesAndJavacArrayMembersDoNotRequireDependencyJars() throws Exception {
        String method = "fixture.Host#render()";
        var policy = new BoundaryPolicy(Set.of(), Set.of(), Set.of(), Set.of(), Set.of(), Map.of(),
                Map.of(), true, Set.of(), Map.of(method, "snapshot"));
        var dependencies = Set.of("Array#<init>(int)", "Array#<initializer:length>", "Array#clone()",
                "jdk.jfr.Event#begin()", "jdk.jfr.EventType#getName()", "fixture.Missing#paint()");
        var report = new BoundaryReport(List.of(), Map.of(), Map.of(), Map.of(method, dependencies));
        assertEquals(List.of("MISSING_IMPLEMENTATION fixture.Missing external trusted type not found in resolved dependencies"),
                DependencyCheck.inspect(report, policy, List.of(), new Properties()));
    }

    @Test void remapIdentityIgnoresPackagingButRawPinStillRequiresExactBytes() throws Exception {
        Path original = jar("original", List.of(API, RESOURCE), List.of("api", "cue"), 1, 2001);
        Path repacked = jar("repacked", List.of("assets/", RESOURCE, API), List.of("", "cue", "api"), 0, 2026);
        assertEquals(List.of(), inspectJar(original, ZIP_PIN));
        assertEquals(List.of(), inspectJar(repacked, ZIP_PIN));
        String raw = DependencyCheck.digest(original);
        assertEquals(List.of(), inspectJar(original, raw));
        assertChanged(repacked, raw);
    }

    @Test void remapIdentityRejectsChangedAddedAndRemovedEntries() throws Exception {
        assertChanged(jar("changed", List.of(API, RESOURCE), List.of("api", "new"), 1, 2001), ZIP_PIN);
        assertChanged(jar("added", List.of(API, RESOURCE, "assets/more.json"), List.of("api", "cue", "new"), 1, 2001), ZIP_PIN);
        assertChanged(jar("removed", List.of(API), List.of("api"), 1, 2001), ZIP_PIN);
    }

    @Test void remapIdentityRejectsDuplicateEntryNames() throws Exception {
        Path jar = jar("duplicate", List.of(API, "assets/a", "assets/b"), List.of("api", "a", "b"), 1, 2001);
        var binary = StandardCharsets.ISO_8859_1;
        String bytes = new String(Files.readAllBytes(jar), binary);
        assertEquals(2, bytes.split("assets/b", -1).length - 1);
        Files.write(jar, bytes.replace("assets/b", "assets/a").getBytes(binary));
        var error = assertThrows(IOException.class, () -> inspectJar(jar, ZIP_PIN));
        assertTrue(error.getMessage().contains("DEPENDENCY_DUPLICATE_ZIP_ENTRY"));
    }

    private void assertChanged(Path jar, String pin) throws Exception {
        var diagnostics = inspectJar(jar, pin);
        assertEquals(1, diagnostics.size(), diagnostics.toString());
        assertTrue(diagnostics.get(0).startsWith("DEPENDENCY_CHANGED sample.jar "), diagnostics.toString());
    }

    private static List<String> inspectJar(Path jar, String expected) throws Exception {
        var method = "fixture.Host#render()";
        var policy = new BoundaryPolicy(Set.of(), Set.of(), Set.of(), Set.of(), Set.of(), Map.of(),
                Map.of(), true, Set.of(), Map.of(method, "snapshot"));
        var report = new BoundaryReport(List.of(), Map.of(), Map.of(), Map.of(method, Set.of("fixture.Api#paint()")));
        var pins = new Properties();
        pins.setProperty("dependency.sample.jar", expected);
        return DependencyCheck.inspect(report, policy, List.of(jar), pins);
    }

    private Path jar(String folder, List<String> names, List<String> content, int compression, int year) throws IOException {
        Path jar = Files.createDirectory(directory.resolve(folder)).resolve("sample.jar");
        try (var zip = new ZipOutputStream(Files.newOutputStream(jar))) {
            zip.setLevel(compression);
            for (int i = 0; i < names.size(); i++) {
                var entry = new ZipEntry(names.get(i));
                entry.setTimeLocal(LocalDateTime.of(year, 1, 1, 12, 0));
                byte[] data = content.get(i).getBytes(StandardCharsets.UTF_8);
                if (compression == 0) {
                    var crc = new CRC32();
                    crc.update(data);
                    entry.setMethod(ZipEntry.STORED);
                    entry.setSize(data.length);
                    entry.setCrc(crc.getValue());
                }
                zip.putNextEntry(entry);
                zip.write(data);
                zip.closeEntry();
            }
        }
        return jar;
    }
}
