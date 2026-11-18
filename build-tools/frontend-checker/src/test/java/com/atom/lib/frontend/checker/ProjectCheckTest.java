package com.atom.lib.frontend.checker;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class ProjectCheckTest {
    @TempDir Path root;
    private Path config(Path dir,String version,String extra) throws Exception {
        Files.createDirectories(dir);
        Path author=dir.resolve("Theme.java");
        Files.writeString(author,"class Theme {static final String namespace=\"test\",version=\""+version+"\",profile=\"probe-v1\";}");
        Files.write(dir.resolve("design.bin"),DesignPackage.compile(author));
        Path config=dir.resolve("contract.properties");
        Files.writeString(config,"protocol=1\nprofiles=probe-v1\ndesign=design.bin\nstrict=consumer.Page#build()\ndrawingTypes=consumer.Ink\n"+extra);
        return config;
    }
    private List<Path> sources(String body) throws Exception {
        Path source=root.resolve("Page.java");
        Files.writeString(source,"package consumer; class Ink{static void rect(int n){}} class Page{void build(){"+body+"}}");
        return List.of(source);
    }
    @Test void ordinaryModeRequiresIndependentReviewedInput() throws Exception {
        Path c=config(root.resolve("candidate"),"1","");
        assertEquals("MISSING_EVIDENCE",ProjectCheck.run(c,null,sources("int volume=100;"),List.of(),17).status());
        assertEquals("MISSING_EVIDENCE",ProjectCheck.run(c,c,sources(""),List.of(),17).status());
    }
    @Test void protectsCheckerConfigurationAndNeverMakesCandidateOrdinarySuccess() throws Exception {
        Path r=config(root.resolve("reviewed"),"1","");
        Path c=config(root.resolve("candidate"),"1","");
        assertTrue(ProjectCheck.run(c,r,sources("int volume=100;"),List.of(),17).ordinarySuccess());
        c=config(root.resolve("candidate"),"2","");
        assertEquals("CANDIDATE_VALID",ProjectCheck.run(c,r,sources(""),List.of(),17).status());
        assertFalse(ProjectCheck.run(c,r,sources(""),List.of(),17).ordinarySuccess());
        c=config(root.resolve("candidate"),"1","trusted=consumer.Ink#rect(int)\n");
        assertFalse(ProjectCheck.run(c,r,sources("Ink.rect(99);"),List.of(),17).ordinarySuccess());
    }
    @Test void boundaryFailuresCannotBecomeCandidatesOrCompilerErrorSuccesses() throws Exception {
        Path r=config(root.resolve("reviewed"),"1","");
        Path c=config(root.resolve("candidate"),"1","");
        var bad=ProjectCheck.run(c,r,sources("Ink.rect(99);"),List.of(),17);
        assertEquals("INVALID",bad.status());
        assertTrue(bad.diagnostics().stream().anyMatch(s->s.startsWith("PAGE_DRAWING ")));
        var syntax=ProjectCheck.run(c,r,sources("missing();"),List.of(),17);
        assertEquals("INVALID",syntax.status());
        assertTrue(syntax.diagnostics().stream().anyMatch(s->s.startsWith("SOURCE_ANALYSIS_FAILED ")));
    }
    @Test void independentPreflightDetectsRemovedBuildIntegration() throws Exception {
        Path r=config(root.resolve("reviewed"),"1","protectedFiles=build.gradle\n");
        Path c=config(root.resolve("candidate"),"1","protectedFiles=build.gradle\n");
        Files.writeString(r.getParent().resolve("build.gradle"),"apply plugin: 'checked'\n");
        Files.writeString(c.getParent().resolve("build.gradle"),"apply plugin: 'java'\n");
        var result=ProjectCheck.run(c,r,sources(""),List.of(),17);
        assertFalse(result.ordinarySuccess());
        assertTrue(result.diagnostics().stream().anyMatch(s->s.startsWith("INTEGRATION_CHANGED ")));
    }
}
