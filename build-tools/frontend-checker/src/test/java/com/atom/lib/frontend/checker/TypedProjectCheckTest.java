package com.atom.lib.frontend.checker;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class TypedProjectCheckTest {
    @TempDir Path root;
    Path config(String dir)throws Exception{Path p=root.resolve(dir);Files.createDirectories(p);Files.writeString(p.resolve("Design.java"),ContractAuthorCompilerTest.valid());Files.writeString(p.resolve("contract.properties"),"protocol=2\nprofiles=sample/1\ndesign=design.bin\nauthor=Design.java\nstrict=consumer.Page#build()\n");Files.write(p.resolve("design.bin"),ProjectCheck.proposed(p.resolve("contract.properties")));return p.resolve("contract.properties");}
    @Test void authorIsRecompiledSoStalePackageCannotApproveChangedDesign()throws Exception{
        Path current=config("current"),reviewed=config("reviewed"),source=root.resolve("Page.java");Files.writeString(source,"package consumer; class Page {void build(){int count=42;}} ");
        assertTrue(ProjectCheck.run(current,reviewed,List.of(source),List.of(),17).ordinarySuccess());
        Path author=current.resolveSibling("Design.java");Files.writeString(author,Files.readString(author).replace("new NumberValue(8,","new NumberValue(12,"));
        assertEquals("INVALID_REUSED_VERSION",ProjectCheck.run(current,reviewed,List.of(source),List.of(),17).status());
        Files.writeString(author,Files.readString(author).replace("\"sample\",\"1\"","\"sample\",\"2\""));
        assertEquals("CANDIDATE_VALID",ProjectCheck.run(current,reviewed,List.of(source),List.of(),17).status());
    }
    @Test void firstEnrollmentCanCheckSourceButCannotAdmitUnreviewedInputs() throws Exception {
        Path current = config("first-enrollment");
        Path source = root.resolve("Page.java");
        Files.writeString(source, "package consumer; class Page { void build() {} }");
        var candidate = ProjectCheck.firstEnrollment(current, List.of(source), List.of(), 17, List.of(), List.of());
        assertEquals("CANDIDATE_VALID", candidate.status());
        assertFalse(candidate.ordinarySuccess());
        assertEquals("MISSING_EVIDENCE", ProjectCheck.run(current, null, List.of(source), List.of(), 17).status());

        Files.writeString(current, Files.readString(current) + "drawingTypes=consumer.Canvas\n");
        Files.writeString(source, """
                package consumer;
                class Canvas { static void draw() {} }
                class Page { void build() { Canvas.draw(); } }
                """);
        var invalid = ProjectCheck.firstEnrollment(current, List.of(source), List.of(), 17, List.of(), List.of());
        assertEquals("INVALID", invalid.status());
        assertTrue(invalid.diagnostics().stream().anyMatch(line -> line.startsWith("PAGE_DRAWING")));
    }
}
