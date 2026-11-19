package com.atom.lib.frontend.checker;

import com.atom.lib.frontend.contract.*;
import java.nio.file.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class CheckerRuntimeOutputTest {
    @TempDir Path root;
    @Test void ordinaryCliEmitsMatchingRuntimeResourcesAndFailureRemovesPriorAdmission()throws Exception{
        Path current=root.resolve("current"),reviewed=root.resolve("reviewed"),output=root.resolve("generated"),source=root.resolve("Page.java");Files.createDirectories(current);Files.createDirectories(reviewed);
        String config="protocol=2\nprofiles=sample/1\ndesign=design.bin\nauthor=Design.java\nstrict=consumer.Page#build()\n";
        for(Path p:List.of(current,reviewed)){Files.writeString(p.resolve("contract.properties"),config);Files.writeString(p.resolve("Design.java"),ContractAuthorCompilerTest.valid());}
        Files.write(reviewed.resolve("design.bin"),ProjectCheck.proposed(reviewed.resolve("contract.properties")));Files.writeString(source,"package consumer; class Page{void build(){int volume=125;}}");
        var first=run(current,reviewed,source,output);assertEquals(0,first.getKey(),first.getValue());
        assertTrue(first.getValue().contains("ENTRY STRICT consumer.Page#build()"),"successful admission must expose the precise migration/entry classification, not just a green status");
        Path design=output.resolve("sample/1/design.fcl"),manifest=output.resolve("sample/1/manifest.fcl");assertTrue(Files.isRegularFile(manifest),"ordinary CLI must emit runtime admission");
        assertEquals("sample",ContractRegistry.load(Files.readAllBytes(design),Files.readAllBytes(manifest),Set.of("sample/1")).namespace());
        Files.writeString(current.resolve("Design.java"),ContractAuthorCompilerTest.valid().replace("\"sample\",\"1\"","\"sample\",\"2\""));
        var pending=run(current,reviewed,source,output);assertNotEquals(0,pending.getKey());assertTrue(pending.getValue().contains("CANDIDATE_VALID"),pending.getValue());assertFalse(Files.exists(manifest),"failed candidate cannot retain a previous runtime admission");
    }
    private Map.Entry<Integer,String> run(Path current,Path reviewed,Path source,Path output)throws Exception{
        String tool=Path.of(CheckerMain.class.getProtectionDomain().getCodeSource().getLocation().toURI()).toString();String core=Path.of(ContractDesign.class.getProtectionDomain().getCodeSource().getLocation().toURI()).toString();
        var executable=Path.of(System.getProperty("java.home"),"bin",System.getProperty("os.name").startsWith("Windows")?"java.exe":"java");
        var invocation = new java.util.ArrayList<>(List.of(executable.toString(), "-cp",
                tool + java.io.File.pathSeparator + core, CheckerMain.class.getName()));
        // A real consumer catalog is larger than a Windows process command line.
        var arguments = new java.util.ArrayList<>(List.of("--config", current.resolve("contract.properties").toString(),
                "--reviewed", reviewed.resolve("contract.properties").toString(), "--source", source.toString(),
                "--report", root.resolve("report.txt").toString(), "--runtime-output", output.toString()));
        for (int i = 0; i < 150; i++) {
            arguments.add("--dependency");
            arguments.add(root.resolve("unused path with spaces").resolve(("part-" + i + "-").repeat(30)).toString());
        }
        invocation.addAll(CheckerArguments.write(root.resolve("build arguments.txt"), arguments));
        var p = new ProcessBuilder(invocation).redirectErrorStream(true).start();
        String report = new String(p.getInputStream().readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
        return Map.entry(p.waitFor(), report);
    }
}
