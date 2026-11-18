package com.atom.lib.frontend.checker;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class ResourceAndDiffTest {
    @TempDir Path root;
    Path config(String dir,String asset)throws Exception {
        Path folder=root.resolve(dir);Files.createDirectories(folder);
        String author=ContractAuthorCompilerTest.valid();
        if(!asset.isEmpty())author=author.replace("Map.of(),Map.of(\"page\"",asset+",Map.of(\"page\"");
        Files.writeString(folder.resolve("Design.java"),author);
        Files.write(folder.resolve("design.bin"),ContractAuthorCompiler.compile(folder.resolve("Design.java")));
        Files.writeString(folder.resolve("contract.properties"),"protocol=2\nprofiles=sample/1\ndesign=design.bin\nauthor=Design.java\nstrict=consumer.Page#build()\n");
        Files.write(folder.resolve("design.bin"),ProjectCheck.proposed(folder.resolve("contract.properties")));
        return folder.resolve("contract.properties");
    }
    ProjectCheck.Result run(Path current,Path reviewed)throws Exception {
        Path source=root.resolve("Page.java");Files.writeString(source,"package consumer; class Page{void build(){int n=1;}}");
        return ProjectCheck.run(current,reviewed,List.of(source),List.of(),17);
    }
    void pin(Path config,String entry,byte[] bytes)throws Exception {
        Path file=config.resolveSibling("src/main/resources").resolve(entry);Files.createDirectories(file.getParent());Files.write(file,bytes);
        String hash=HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        Files.writeString(config,"resource."+entry+"="+hash+"\n",StandardOpenOption.APPEND);
        Files.write(config.resolveSibling("design.bin"),ProjectCheck.proposed(config));
    }
    @Test void declaredAssetMustResolveToTheApprovedBytesNotJustAnId()throws Exception {
        String asset="Map.of(\"paper\",new Asset(AssetKind.TEXTURE,\"sample:textures/paper.png\",\"approved archive\"))";
        Path c=config("current",asset),r=config("reviewed",asset);
        assertTrue(run(c,r).diagnostics().stream().anyMatch(x->x.startsWith("RESOURCE_MISSING ")),run(c,r).toString());
        for(Path p:List.of(c,r))pin(p,"assets/sample/textures/paper.png",new byte[]{1,2,3});
        assertTrue(run(c,r).ordinarySuccess());
        Files.write(c.resolveSibling("src/main/resources/assets/sample/textures/paper.png"),new byte[]{1,2,4});
        assertTrue(run(c,r).diagnostics().stream().anyMatch(x->x.startsWith("RESOURCE_CHANGED ")));
    }
    @Test void soundEventMustIncludeItsRealTransitiveFilesAndCannotHideInADeclaration()throws Exception {
        String asset="Map.of(\"click\",new Asset(AssetKind.SOUND,\"sample:click\",\"approved archive\"))";
        Path c=config("current",asset),r=config("reviewed",asset);
        byte[] json="{\"click\":{\"sounds\":[{\"name\":\"sample:nested\",\"type\":\"event\"}]},\"nested\":{\"sounds\":[\"sample:tap\"]}}".getBytes(java.nio.charset.StandardCharsets.UTF_8);
        for(Path p:List.of(c,r))pin(p,"assets/sample/sounds.json",json);
        var missing=run(c,r);assertTrue(missing.diagnostics().stream().anyMatch(x->x.startsWith("RESOURCE_MISSING assets/sample/sounds/tap.ogg")),missing.toString());
        for(Path p:List.of(c,r))pin(p,"assets/sample/sounds/tap.ogg",new byte[]{1,2,3});
        assertTrue(run(c,r).ordinarySuccess());
        Files.write(c.resolveSibling("src/main/resources/assets/sample/sounds/tap.ogg"),new byte[]{9});
        assertTrue(run(c,r).diagnostics().stream().anyMatch(x->x.startsWith("RESOURCE_CHANGED assets/sample/sounds/tap.ogg")));
    }
    @Test void bundledClasspathFontIsResolvedAndBoundWithoutPretendingItIsAMinecraftAsset()throws Exception {
        String asset="Map.of(\"body\",new Asset(AssetKind.FONT,\"classpath:com/atom/lib/fonts/regular.otf\",\"approved bundled font\"))";
        Path c=config("current",asset),r=config("reviewed",asset);
        var missing=run(c,r);
        assertTrue(missing.diagnostics().stream().anyMatch(x->x.startsWith("RESOURCE_MISSING com/atom/lib/fonts/regular.otf")),missing.toString());
        for(Path p:List.of(c,r))pin(p,"com/atom/lib/fonts/regular.otf",new byte[]{1,2,3});
        assertTrue(run(c,r).ordinarySuccess(),run(c,r).toString());
        Files.write(c.resolveSibling("src/main/resources/com/atom/lib/fonts/regular.otf"),new byte[]{9});
        assertTrue(run(c,r).diagnostics().stream().anyMatch(x->x.startsWith("RESOURCE_CHANGED com/atom/lib/fonts/regular.otf")));
        assertThrows(IllegalArgumentException.class,()->config("escape","Map.of(\"font\",new Asset(AssetKind.FONT,\"classpath:../private.otf\",\"invalid\"))"));
    }
    @Test void candidateReportsNormalizedChangesAndAffectedConsumers()throws Exception {
        Path c=config("current",""),r=config("reviewed","");
        Path a=c.resolveSibling("Design.java");Files.writeString(a,Files.readString(a).replace("\"sample\",\"1\"","\"sample\",\"2\"").replace("new NumberValue(8,","new NumberValue(12,"));
        var result=run(c,r);assertEquals("CANDIDATE_VALID",result.status());
        assertTrue(result.diagnostics().stream().anyMatch(x->x.contains("families/gap/members/normal/value")&&x.contains("8.0")&&x.contains("12.0")),result.toString());
        assertTrue(result.diagnostics().contains("AFFECTED_PAGE page"),result.toString());
    }
    @Test void externalTrustedImplementationPinsItsResolvedDependencyBytes()throws Exception {
        Path classes=root.resolve("external-classes");Files.createDirectories(classes);Path java=root.resolve("Skin.java");
        Files.writeString(java,"package external; public class Skin{public static void paint(){int radius=6;}}");
        assertEquals(0,javax.tools.ToolProvider.getSystemJavaCompiler().run(null,null,null,"-d",classes.toString(),java.toString()));
        Path jar=root.resolve("skin.jar");writeJar(jar,classes.resolve("external/Skin.class"));
        Path c=config("current",""),r=config("reviewed","");
        for(Path p:List.of(c,r)){Files.writeString(p,"trusted=external.Skin#paint()\n",StandardOpenOption.APPEND);Files.write(p.resolveSibling("design.bin"),ProjectCheck.proposed(p));}
        Path page=root.resolve("Page.java");Files.writeString(page,"package consumer; class Page{void build(){external.Skin.paint();}}");
        var missing=ProjectCheck.run(c,r,List.of(page),List.of(jar),17);
        assertTrue(missing.diagnostics().stream().anyMatch(x->x.startsWith("DEPENDENCY_UNBOUND skin.jar")),missing.toString());
        String hash=ProjectCheck.digest(Files.readAllBytes(jar));
        for(Path p:List.of(c,r)){Files.writeString(p,"dependency.skin.jar="+hash+"\n",StandardOpenOption.APPEND);Files.write(p.resolveSibling("design.bin"),ProjectCheck.proposed(p));}
        assertTrue(ProjectCheck.run(c,r,List.of(page),List.of(jar),17).ordinarySuccess());
        Path runtimeOnly=root.resolve("runtime-only.jar");try(var out=new java.util.zip.ZipOutputStream(Files.newOutputStream(runtimeOnly))){out.putNextEntry(new java.util.zip.ZipEntry("payload"));out.write(1);out.closeEntry();}
        var runtimeMissing=ProjectCheck.run(c,r,List.of(page),List.of(jar),17,List.of(),List.of(jar,runtimeOnly));
        assertTrue(runtimeMissing.diagnostics().stream().anyMatch(x->x.startsWith("DEPENDENCY_UNBOUND runtime-only.jar")),runtimeMissing.toString());
        Files.writeString(java,Files.readString(java).replace("radius=6","radius=12"));assertEquals(0,javax.tools.ToolProvider.getSystemJavaCompiler().run(null,null,null,"-d",classes.toString(),java.toString()));writeJar(jar,classes.resolve("external/Skin.class"));
        var changed=ProjectCheck.run(c,r,List.of(page),List.of(jar),17);
        assertTrue(changed.diagnostics().stream().anyMatch(x->x.startsWith("DEPENDENCY_CHANGED skin.jar")),changed.toString());
        Files.writeString(c,Files.readString(c).replace(hash,ProjectCheck.digest(Files.readAllBytes(jar))));
        assertEquals("INVALID_REUSED_VERSION",ProjectCheck.run(c,r,List.of(page),List.of(jar),17).status(),"updating a jar pin cannot reuse the old design identity");
    }
    private void writeJar(Path jar,Path bytes)throws Exception{try(var out=new java.util.zip.ZipOutputStream(Files.newOutputStream(jar))){var entry=new java.util.zip.ZipEntry("external/Skin.class");entry.setTime(0);out.putNextEntry(entry);out.write(Files.readAllBytes(bytes));out.closeEntry();}}
    @Test void reviewedBusinessPortAcceptsBusinessNumbersButCannotHideDrawing()throws Exception {
        Path c=config("current",""),r=config("reviewed","");
        for(Path p:List.of(c,r)){
            String text=Files.readString(p).replace("strict=consumer.Page#build()","strict=consumer.Page#build(consumer.Business)");
            Files.writeString(p,text+"businessMethods=consumer.Business#save(int)\ndrawingTypes=consumer.Ink\n");Files.write(p.resolveSibling("design.bin"),ProjectCheck.proposed(p));
        }
        Path page=root.resolve("Page.java");Files.writeString(page,"package consumer; interface Business{void save(int count);} class Ink{static void rect(int x){}} class Page{void build(Business port){port.save(42);}}");
        var legal=ProjectCheck.run(c,r,List.of(page),List.of(),17);assertTrue(legal.ordinarySuccess(),legal.toString());
        Files.writeString(page,"package consumer; interface Business{default void save(int count){Ink.rect(count);}} class Ink{static void rect(int x){}} class Page{void build(Business port){port.save(42);}}");
        var illegal=ProjectCheck.run(c,r,List.of(page),List.of(),17);assertTrue(illegal.diagnostics().stream().anyMatch(x->x.startsWith("PAGE_DRAWING ")),illegal.toString());assertFalse(illegal.diagnostics().stream().anyMatch(x->x.startsWith("SOURCE_ANALYSIS_FAILED")));
    }
}
