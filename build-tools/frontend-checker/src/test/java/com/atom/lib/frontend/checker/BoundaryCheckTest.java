package com.atom.lib.frontend.checker;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;

class BoundaryCheckTest {
    @TempDir Path root;
    private BoundaryReport check(String code, Set<String> strict, Map<String,String> legacy) throws Exception {
        Path file = root.resolve("Fixture.java");
        Files.writeString(file, """
            package fixture;
            class Canvas { static void rect(int x) {} }
            class Screen { void render() {} }
            class Components { static void card(int businessValue) {} }
            """ + code);
        return BoundaryCheck.inspect(List.of(file), List.of(), 17,
            new BoundaryPolicy(strict, Set.of("fixture.Components#card(int)"),
                Set.of("fixture.Canvas"), Set.of("fixture.Screen"), Set.of("render"), legacy));
    }
    private void rejects(BoundaryReport report, String code) {
        assertFalse(report.diagnostics().stream().anyMatch(s -> s.contains("SOURCE_ANALYSIS_FAILED")), report.toString());
        assertTrue(report.diagnostics().stream().anyMatch(s -> s.startsWith(code + " ")), report.toString());
    }
    @Test void permitsBusinessNumbersInApprovedComponents() throws Exception {
        assertTrue(check("class Page { void build() { Components.card(125); } }",
            Set.of("fixture.Page#build()"), Map.of()).valid());
    }
    @Test void rejectsDirectDrawing() throws Exception {
        rejects(check("class Page { void build() { Canvas.rect(12); } }",
            Set.of("fixture.Page#build()"), Map.of()), "PAGE_DRAWING");
    }
    @Test void followsIndirectHelperAndMethodReference() throws Exception {
        rejects(check("class Page { void build() { Helper.paint(); } } class Helper { static void paint() { java.util.function.IntConsumer c = Canvas::rect; c.accept(12); } }",
            Set.of("fixture.Page#build()"), Map.of()), "PAGE_DRAWING");
    }
    @Test void rejectsUnregisteredHostAndInitializer() throws Exception {
        rejects(check("class Page extends Screen { void render() { Components.card(1); } }", Set.of(), Map.of()), "UNCOVERED_ENTRY");
        rejects(check("class Page { static { Canvas.rect(1); } }", Set.of(), Map.of()), "UNCOVERED_ENTRY");
    }
    @Test void rejectsMissingAndEmptyScan() throws Exception {
        rejects(check("class Page {}", Set.of("fixture.Page#build()"), Map.of()), "MISSING_ENTRY");
        rejects(BoundaryCheck.inspect(List.of(), List.of(), 17,
            new BoundaryPolicy(Set.of(),Set.of(),Set.of(),Set.of(),Set.of(),Map.of())), "MISSING_EVIDENCE");
    }
    @Test void freezesExactLegacyRouteAndOuterShellInsteadOfWholeFile() throws Exception {
        String old = "class Page extends Screen { int route; void render() { if(route == 1) Canvas.rect(12); } int count() { return 1; } }";
        BoundaryReport snapshot = check(old, Set.of(), Map.of());
        assertTrue(snapshot.uiStructures().containsKey("fixture.Page#render()"), snapshot.toString());
        Map<String,String> reviewed = snapshot.uiStructures();
        assertTrue(check(old, Set.of(), reviewed).valid());
        assertTrue(check(old.replace("return 1", "return 2"), Set.of(), reviewed).valid());
        rejects(check(old.replace("if(route == 1)", "if(route == 1 || route == 2)"), Set.of(), reviewed), "LEGACY_UI_CHANGED");
        rejects(check(old.replace("Canvas.rect(12);", "Canvas.rect(12); Canvas.rect(99);"), Set.of(), reviewed), "LEGACY_UI_CHANGED");
        rejects(check(old + "class Other extends Screen { void render() { Components.card(1); } }", Set.of(), reviewed), "UNCOVERED_ENTRY");
    }
    @Test void recognizesStaticImportAndFullyQualifiedSymbols() throws Exception {
        Path file = root.resolve("Imported.java");
        Files.writeString(file, "package imported; import static imported.Ink.rect; class Ink {static void rect(int n){}} class Page {void build(){rect(1); imported.Ink.rect(2);}} ");
        rejects(BoundaryCheck.inspect(List.of(file), List.of(), 17,
            new BoundaryPolicy(Set.of("imported.Page#build()"),Set.of(),Set.of("imported.Ink"),Set.of(),Set.of(),Map.of())), "PAGE_DRAWING");
    }
    @Test void secondLegacyInitializerCannotHideBehindFirstInitializer() throws Exception {
        String old="class Page { static { Canvas.rect(1); } static { Canvas.rect(2); } }";
        var reviewed=check(old,Set.of(),Map.of()).uiStructures();
        assertTrue(check(old,Set.of(),reviewed).valid());
        rejects(check(old.replace("rect(2)","rect(99)"),Set.of(),reviewed),"LEGACY_UI_CHANGED");
    }
    @Test void abstractDispatchCannotReachReviewedLegacyFromStrictPage() throws Exception {
        String code="interface Painter {void paint();} class Old implements Painter { public void paint(){Canvas.rect(2);} } class Page {void build(Painter p){p.paint();}}";
        var old=check(code,Set.of(),Map.of()).uiStructures();
        rejects(check(code,Set.of("fixture.Page#build(fixture.Painter)"),old),"UNSUPPORTED_DYNAMIC_UI");
    }
    @Test void differentAnonymousCallbacksCannotOverwriteEachOthersCoverage() throws Exception {
        var observed=check("class Page {void make(){ Runnable a=new Runnable(){public void run(){Canvas.rect(1);}}; Runnable b=new Runnable(){public void run(){Canvas.rect(2);}}; }}",Set.of(),Map.of());
        assertEquals(2,observed.uiStructures().keySet().stream().filter(k->k.endsWith("#run()")).count(),observed.toString());
    }
    @Test void creatingNativeScreenWithoutDrawingStillRequiresRegistration() throws Exception {
        rejects(check("class Factory {Screen make(){return new Screen();}}",Set.of(),Map.of()),"UNCOVERED_ENTRY");
    }
}
