package com.atom.lib.frontend.checker;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class CompilerIsolationTest {
    @TempDir Path temp;
    @Test void sourceAttributionDoesNotExecuteAutoStartCompilerPluginsFromConsumerClasspath()throws Exception {
        Path src=temp.resolve("AutoPlugin.java"),classes=temp.resolve("classes");Files.createDirectories(classes);
        Files.writeString(src,"public class AutoPlugin implements com.sun.source.util.Plugin {public String getName(){return \"fcl-probe\";} public boolean autoStart(){return true;} public void init(com.sun.source.util.JavacTask t,String... a){System.setProperty(\"fcl.compiler.plugin\",\"executed\");}}");
        assertEquals(0,javax.tools.ToolProvider.getSystemJavaCompiler().run(null,null,null,"-d",classes.toString(),src.toString()));
        Path jar=temp.resolve("plugin.jar");try(var out=new java.util.zip.ZipOutputStream(Files.newOutputStream(jar))){for(String name:List.of("AutoPlugin.class","META-INF/services/com.sun.source.util.Plugin")){out.putNextEntry(new java.util.zip.ZipEntry(name));out.write(name.endsWith(".class")?Files.readAllBytes(classes.resolve(name)):"AutoPlugin\n".getBytes(java.nio.charset.StandardCharsets.UTF_8));out.closeEntry();}}
        Path page=temp.resolve("Page.java");Files.writeString(page,"class Page {void build(){int n=1;}}");
        try{var result=BoundaryCheck.inspect(List.of(page),List.of(jar),17,new BoundaryPolicy(Set.of("Page#build()"),Set.of(),Set.of(),Set.of(),Set.of(),Map.of()));assertTrue(result.valid(),result.toString());assertNull(System.getProperty("fcl.compiler.plugin"),"analysis must never execute a consumer compiler plugin");}
        finally{System.clearProperty("fcl.compiler.plugin");}
    }
}
