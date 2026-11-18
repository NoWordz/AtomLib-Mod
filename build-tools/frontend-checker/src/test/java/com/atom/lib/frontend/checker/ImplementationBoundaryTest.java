package com.atom.lib.frontend.checker;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import java.util.*;
import com.atom.lib.frontend.contract.ContractDesign;
import static org.junit.jupiter.api.Assertions.*;

class ImplementationBoundaryTest {
    @TempDir Path temp;
    @Test void anonymousImplementationCannotHideAStyleDependency()throws Exception{
        Path source=temp.resolve("Page.java");Files.writeString(source,"package consumer; class Ink{static void rect(int n){}} class Palette{static final int RADIUS=6;} class Component{static void paint(){java.util.function.IntSupplier deferred=new java.util.function.IntSupplier(){public int getAsInt(){return Palette.RADIUS;}};Ink.rect(deferred.getAsInt());}} class Page{void build(){Component.paint();}}");
        var capture=inspect(source,Map.of());var bindings=new HashMap<String,String>();
        for(String id:List.of("consumer.Component","consumer.Ink"))bindings.put(id,capture.implementationStructures().get(id));
        var missing=inspect(source,bindings);
        assertTrue(missing.diagnostics().stream().anyMatch(x->x.startsWith("MISSING_IMPLEMENTATION_BINDING consumer.Palette")),missing.toString());
        assertFalse(missing.diagnostics().stream().anyMatch(x->x.startsWith("SOURCE_ANALYSIS_FAILED")),missing.toString());
        bindings.putAll(capture.implementationStructures());assertTrue(inspect(source,bindings).valid(),inspect(source,bindings).toString());
        Files.writeString(source,Files.readString(source).replace("RADIUS=6","RADIUS=12"));
        assertTrue(inspect(source,bindings).diagnostics().stream().anyMatch(x->x.startsWith("IMPLEMENTATION_CHANGED consumer.Palette")));
    }
    @Test void trustedImplementationConstantCannotChangeUnderSameBinding()throws Exception{
        Path source=temp.resolve("Page.java");String text="package consumer; class Ink{static void rect(int n){}} class Component{static final int RADIUS=6; static void paint(){Ink.rect(RADIUS);}} class Page{void build(){Component.paint();}}";Files.writeString(source,text);
        var initial=inspect(source,Map.of("consumer.Component",""));String ast=initial.implementationStructures().get("consumer.Component");assertNotNull(ast,"capture complete implementation, including fields");
        var approved=Map.of("consumer.Component",ast,"consumer.Ink",initial.implementationStructures().get("consumer.Ink"));assertTrue(inspect(source,approved).valid());Files.writeString(source,text.replace("RADIUS=6","RADIUS=12"));
        var bad=inspect(source,approved);assertTrue(bad.diagnostics().stream().anyMatch(x->x.startsWith("IMPLEMENTATION_CHANGED ")),bad.toString());assertFalse(bad.diagnostics().stream().anyMatch(x->x.startsWith("SOURCE_ANALYSIS_FAILED")));
    }
    BoundaryReport inspect(Path source,Map<String,String> implementations)throws Exception{return BoundaryCheck.inspect(List.of(source),List.of(),17,new BoundaryPolicy(Set.of("consumer.Page#build()"),Set.of("consumer.Component#paint()","consumer.Ink#rect(int)"),Set.of("consumer.Ink"),Set.of(),Set.of(),Map.of(),implementations,true));}
    @Test void rawGeometryAndForgingAdmissionAreCapabilitiesNotBusinessValues()throws Exception{
        Path source=temp.resolve("Page.java");Files.writeString(source,"package consumer; class Page{void build(){new com.atom.lib.geometry.Rect(1,2,3,4);com.atom.lib.frontend.contract.ContractRegistry.manifest(new byte[0],new byte[0]);}} ");
        Path model=Path.of(ContractDesign.class.getProtectionDomain().getCodeSource().getLocation().toURI());var report=BoundaryCheck.inspect(List.of(source),List.of(model),17,new BoundaryPolicy(Set.of("consumer.Page#build()"),Set.of(),Set.of(),Set.of(),Set.of(),Map.of()));
        assertTrue(report.diagnostics().stream().filter(x->x.startsWith("PAGE_DRAWING ")).count()>=2,report.toString());assertFalse(report.diagnostics().stream().anyMatch(x->x.startsWith("SOURCE_ANALYSIS_FAILED")));
    }
    @Test void trustedComponentCannotHideChangesInHelperOrFieldOnlyDependencies()throws Exception{
        Path source=temp.resolve("Page.java");Files.writeString(source,"package consumer; class Ink{static void rect(int n){}} class Palette{static final int RADIUS=6;} class Helper{static void paint(){Ink.rect(Palette.RADIUS);}} class Component{static void paint(){Helper.paint();}} class Page{void build(){Component.paint();}}");
        var capture=inspect(source,Map.of());var bindings=new HashMap<String,String>();
        for(String id:List.of("consumer.Component","consumer.Ink"))bindings.put(id,capture.implementationStructures().get(id));
        var missing=inspect(source,bindings);
        assertTrue(missing.diagnostics().stream().anyMatch(x->x.startsWith("MISSING_IMPLEMENTATION_BINDING consumer.Helper")),missing.toString());
        assertTrue(missing.diagnostics().stream().anyMatch(x->x.startsWith("MISSING_IMPLEMENTATION_BINDING consumer.Palette")),missing.toString());
        for(String id:List.of("consumer.Helper","consumer.Palette"))bindings.put(id,capture.implementationStructures().get(id));
        assertTrue(inspect(source,bindings).valid(),inspect(source,bindings).toString());
        Files.writeString(source,Files.readString(source).replace("RADIUS=6","RADIUS=12"));
        assertTrue(inspect(source,bindings).diagnostics().stream().anyMatch(x->x.startsWith("IMPLEMENTATION_CHANGED consumer.Palette")));
    }
    @Test void trustedPaintMethodCannotExposeRawCoordinatesToAStrictPage()throws Exception{
        Path source=temp.resolve("Page.java");Files.writeString(source,"package consumer; class Ink{static void rect(float x,float y){}} class Component{static void paint(float x,float y){Ink.rect(x,y);}} class Page{void build(){float x=480/2f-24;Component.paint(x,48);}}");
        Set<String> trusted=Set.of("consumer.Component#paint(float,float)","consumer.Ink#rect(float,float)");
        var capture=BoundaryCheck.inspect(List.of(source),List.of(),17,new BoundaryPolicy(Set.of("consumer.Page#build()"),trusted,Set.of("consumer.Ink"),Set.of(),Set.of(),Map.of()));
        var bindings=Map.of("consumer.Component",capture.implementationStructures().get("consumer.Component"),"consumer.Ink",capture.implementationStructures().get("consumer.Ink"));
        var bad=BoundaryCheck.inspect(List.of(source),List.of(),17,new BoundaryPolicy(Set.of("consumer.Page#build()"),trusted,Set.of("consumer.Ink"),Set.of(),Set.of(),Map.of(),bindings,true));
        assertTrue(bad.diagnostics().stream().anyMatch(x->x.startsWith("UNSAFE_COMPONENT_INPUT ")),bad.toString());assertFalse(bad.diagnostics().stream().anyMatch(x->x.startsWith("SOURCE_ANALYSIS_FAILED")));
    }
}
