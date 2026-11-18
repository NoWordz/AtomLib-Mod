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
}
