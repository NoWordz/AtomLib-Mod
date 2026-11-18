package com.atom.lib.frontend.checker;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class SceneProcessCheckTest {
    @TempDir Path temp;
    @Test void currentInvocationAndDesignIdentityAreRequiredAndPayloadRoundTrips()throws Exception{
        Path file=temp.resolve("capture.fcs");var sample=ScenarioCheckTest.sample("start",0,ScenarioCheck.Phase.INITIAL,0xff334455,true,Map.of());var capture=new SceneCaptureIO.Capture("current","0123456789",List.of(sample));SceneCaptureIO.write(file,capture);var decoded=SceneCaptureIO.read(file);assertEquals(capture.invocation(),decoded.invocation());assertEquals(sample.image().getRGB(0,0),decoded.samples().get(0).image().getRGB(0,0));
        assertThrows(IllegalArgumentException.class,()->SceneProcessCheck.requireCurrent(decoded,"other","0123456789"));
        assertThrows(IllegalArgumentException.class,()->SceneProcessCheck.requireCurrent(decoded,"current","changed-design"));
        assertDoesNotThrow(()->SceneProcessCheck.requireCurrent(decoded,"current","0123456789"));
        Files.write(file,new byte[]{0},StandardOpenOption.APPEND);assertThrows(java.io.IOException.class,()->SceneCaptureIO.read(file));
    }
    @Test void productionCheckExecutesANewProcessAndRejectsStaleOrMissingSamples()throws Exception{
        Path author=temp.resolve("Design.java");Files.writeString(author,ContractAuthorCompilerTest.valid());byte[] design=ContractAuthorCompiler.compile(author);
        var baseline=ScenarioCheckTest.baseline(ScenarioCheckTest.sample("start",0,ScenarioCheck.Phase.INITIAL,0xff334455,true,Map.of()));
        var javaExecutable=Path.of(System.getProperty("java.home"),"bin",System.getProperty("os.name").startsWith("Windows")?"java.exe":"java");
        String cp=String.join(java.io.File.pathSeparator,List.of(Path.of(getClass().getProtectionDomain().getCodeSource().getLocation().toURI()).toString(),Path.of(CheckerMain.class.getProtectionDomain().getCodeSource().getLocation().toURI()).toString(),Path.of(com.atom.lib.frontend.contract.ContractDesign.class.getProtectionDomain().getCodeSource().getLocation().toURI()).toString()));
        for(String mode:List.of("good","stale","empty")){
            var errors=SceneProcessCheck.run(List.of(javaExecutable.toString(),"-cp",cp,CaptureMain.class.getName(),mode),temp.resolve("current.fcs"),design,List.of(baseline),java.time.Duration.ofSeconds(30));
            if(mode.equals("good"))assertTrue(errors.isEmpty(),errors.toString());
            if(mode.equals("stale"))assertTrue(errors.stream().anyMatch(x->x.startsWith("STALE_SCENE_CAPTURE")),errors.toString());
            if(mode.equals("empty"))assertTrue(errors.stream().anyMatch(x->x.startsWith("MISSING_SAMPLE")),errors.toString());
        }
    }
    public static class CaptureMain {
        public static void main(String[] args)throws Exception{
            var samples=args[0].equals("empty")?List.<ScenarioCheck.Sample>of():List.of(ScenarioCheckTest.sample("start",0,ScenarioCheck.Phase.INITIAL,0xff334455,true,Map.of()));
            SceneCaptureIO.write(Path.of(args[1]),new SceneCaptureIO.Capture(args[0].equals("stale")?"previous":args[2],args[3],samples));
        }
    }
}
