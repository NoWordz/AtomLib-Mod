package com.atom.lib.frontend.checker;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class SceneBuildGateTest {
    @TempDir Path temp;
    @Test void declaredScenariosWithoutAProductionRunnerAndIndependentBaselineCannotPass()throws Exception{
        Path c=temp.resolve("current"),r=temp.resolve("reviewed");Files.createDirectories(c);Files.createDirectories(r);
        for(Path p:List.of(c,r)){Files.writeString(p.resolve("Design.java"),ContractAuthorCompilerTest.valid());Files.writeString(p.resolve("contract.properties"),"protocol=2\nprofiles=sample/1\ndesign=design.bin\nauthor=Design.java\n");Files.write(p.resolve("design.bin"),ProjectCheck.proposed(p.resolve("contract.properties")));}
        var result=SceneCheckerMain.check(c.resolve("contract.properties"),r.resolve("contract.properties"),"unused","",temp.resolve("capture.fcs"));
        assertEquals("MISSING_EVIDENCE",result.status());assertTrue(result.diagnostics().stream().anyMatch(x->x.startsWith("MISSING_SCENE_ENTRY")),result.toString());
    }

    @Test void testRunnerMustUseTheReviewedInputsFromTheActualTestSources()throws Exception{
        Path roots = Files.createDirectories(temp.resolve("test-sources"));
        Path runner = roots.resolve("app/SettingsScene.java");
        Files.createDirectories(runner.getParent());
        String source = "package app;\nclass SettingsScene {}\n";
        Files.writeString(runner, source.replace("\n", "\r\n"));
        Properties reviewed = new Properties();
        reviewed.setProperty("scene.main","app.SettingsScene");
        reviewed.setProperty("scene.sourceSet","test");
        assertFalse(SceneCheckerMain.hasRunnerBinding(reviewed));
        reviewed.setProperty("scene.input.app/SettingsScene.java", ProjectCheck.digest(source.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        assertTrue(SceneCheckerMain.hasRunnerBinding(reviewed));
        assertEquals(List.of(), SceneCheckerMain.checkTestInputs(reviewed,List.of(roots),null));
        Files.writeString(runner, source.replace("SettingsScene {}", "SettingsScene { int drift; }"));
        assertEquals(List.of("SCENE_INPUT_CHANGED app/SettingsScene.java"), SceneCheckerMain.checkTestInputs(reviewed,List.of(roots),null));
        assertEquals(List.of("MISSING_SCENE_TEST_CLASSPATH"),SceneCheckerMain.checkTestInputs(reviewed,List.of(),null));
    }
}
