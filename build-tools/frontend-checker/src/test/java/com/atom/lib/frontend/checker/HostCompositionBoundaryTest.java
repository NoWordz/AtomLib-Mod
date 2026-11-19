package com.atom.lib.frontend.checker;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class HostCompositionBoundaryTest {
    private static final String HEADER = "consumer.Shell#header(consumer.Canvas)";
    private static final String BODY = "consumer.Shell#compose(consumer.Canvas)";
    private static final String SOURCE = """
            package consumer;
            class Canvas { void draw(int x) {} }
            class Shell {
                private static final int INSET = 12;
                void header(Canvas canvas) { canvas.draw(INSET); }
                void compose(Canvas canvas) { header(canvas); }
            }
            """;

    @TempDir Path directory;

    @Test void reviewedMethodsNeedTheirOwnDependencyBindingsAndRejectAChangedConstant() throws Exception {
        var inventory = inspect(SOURCE, Map.of(), Map.of());
        var canvas = Map.of("consumer.Canvas", inventory.implementationStructures().get("consumer.Canvas"));
        var bodyOnly = Map.of(BODY, inventory.compositionSnapshot(BODY));
        var unresolved = inspect(SOURCE, canvas, bodyOnly);
        assertTrue(unresolved.diagnostics().stream().anyMatch(line -> line.equals("COMPOSITION_DEPENDENCY_UNBOUND " + BODY + " -> " + HEADER)));

        var reviewed = Map.of(BODY, inventory.compositionSnapshot(BODY), HEADER, inventory.compositionSnapshot(HEADER));
        var approved = inspect(SOURCE, canvas, reviewed);
        assertTrue(approved.valid(), approved.diagnostics().toString());
        assertTrue(approved.categories().get(BODY).contains("HOST_COMPOSITION"));

        var changed = inspect(SOURCE.replace("INSET = 12", "INSET = 20"), canvas, reviewed);
        assertTrue(changed.diagnostics().stream().anyMatch(line -> line.startsWith("COMPOSITION_CHANGED " + HEADER)));
        assertFalse(changed.diagnostics().stream().anyMatch(line -> line.startsWith("SOURCE_ANALYSIS_FAILED")));
        var removed = inspect(SOURCE.replace("void compose", "void renamed"), canvas, reviewed);
        assertTrue(removed.diagnostics().contains("MISSING_COMPOSITION " + BODY));
    }

    @Test void aBoundInitializerDetectsSwitchingTheActualHostObject() throws Exception {
        String key = "consumer.Shell#<initializer:frame>";
        String source = """
                package consumer;
                interface Host {}
                class SelectedHost implements Host {}
                class OtherHost implements Host {}
                class Composite { Composite(Host host) {} }
                class Shell { final Composite frame = new Composite(new SelectedHost()); }
                """;
        var observed = inspect(source, Map.of(), Map.of(key, "observation"));
        assertNotNull(observed.compositionSnapshot(key));
        var components = Map.of("consumer.Composite", observed.implementationStructures().get("consumer.Composite"),
                "consumer.Host", observed.implementationStructures().get("consumer.Host"),
                "consumer.SelectedHost", observed.implementationStructures().get("consumer.SelectedHost"));
        var reviewed = Map.of(key, observed.compositionSnapshot(key));
        assertTrue(inspect(source, components, reviewed).valid());
        var changed = inspect(source.replace("new SelectedHost()", "new OtherHost()"), components, reviewed);
        assertTrue(changed.diagnostics().stream().anyMatch(line -> line.startsWith("COMPOSITION_CHANGED " + key)));
    }

    @Test void aReferencedFieldWithNoInitializerCallsStillRequiresAScopedBinding() throws Exception {
        String field = "consumer.Shell#<initializer:insets>";
        String source = SOURCE.replace("private static final int INSET = 12;",
                        "private static final int[] insets = {12}; private final int[] unrelated = {2};")
                .replace("draw(INSET)", "draw(insets[0])");
        var observed = inspect(source, Map.of(), Map.of());
        var canvas = Map.of("consumer.Canvas", observed.implementationStructures().get("consumer.Canvas"));
        var methods = Map.of(BODY, observed.compositionSnapshot(BODY), HEADER, observed.compositionSnapshot(HEADER));
        var unbound = inspect(source, canvas, methods);
        assertTrue(unbound.diagnostics().contains("COMPOSITION_DEPENDENCY_UNBOUND " + HEADER + " -> " + field));

        var reviewed = new java.util.HashMap<>(methods);
        reviewed.put(field, observed.compositionSnapshot(field));
        assertTrue(inspect(source, canvas, reviewed).valid());
        assertTrue(inspect(source.replace("unrelated = {2}", "unrelated = {3}"), canvas, reviewed).valid());
        var changed = inspect(source.replace("insets = {12}", "insets = {20}"), canvas, reviewed);
        assertTrue(changed.diagnostics().stream().anyMatch(line -> line.startsWith("COMPOSITION_CHANGED " + field)));
    }

    @Test void mixedHostDispatchIsExactButCannotBeCalledFromTheSelectedBody() throws Exception {
        String dispatch = "consumer.Shell#paint(boolean,consumer.Canvas)";
        String selected = "consumer.Shell#history(consumer.Canvas)";
        String wallpaper = "consumer.Shell#wallpaper(consumer.Canvas)";
        String source = """
                package consumer;
                class Canvas { void draw(int x) {} }
                class Shell {
                    void paint(boolean showHistory, Canvas canvas) {
                        if (showHistory) history(canvas); else wallpaper(canvas);
                    }
                    void history(Canvas canvas) { canvas.draw(3); }
                    void wallpaper(Canvas canvas) { canvas.draw(6); }
                }
                """;
        var observed = inspect(source, Map.of(), Map.of());
        var implementation = Map.of("consumer.Canvas", observed.implementationStructures().get("consumer.Canvas"));
        var route = Map.of(dispatch, observed.compositionSnapshot(dispatch));
        var body = Map.of(selected, observed.compositionSnapshot(selected));
        var legacy = Map.of(wallpaper, observed.uiStructures().get(wallpaper));

        var exact = inspect(source, implementation, body, route, legacy);
        assertTrue(exact.valid(), exact.diagnostics().toString());
        assertTrue(exact.categories().get(dispatch).contains("HOST_DISPATCH"));
        var moved = inspect(source.replace("if (showHistory)", "if (!showHistory)"), implementation, body, route, legacy);
        assertTrue(moved.diagnostics().stream().anyMatch(x -> x.startsWith("HOST_DISPATCH_CHANGED " + dispatch)));
        var legacyChanged = inspect(source.replace("canvas.draw(6)", "canvas.draw(7)"), implementation, body, route, legacy);
        assertTrue(legacyChanged.diagnostics().stream().anyMatch(x -> x.startsWith("LEGACY_UI_CHANGED " + wallpaper)));

        var bypass = inspect(source.replace("canvas.draw(3)", "paint(false, canvas)"), implementation, body, route, legacy);
        assertTrue(bypass.diagnostics().contains("COMPOSITION_DISPATCH_BYPASS " + selected + " -> " + dispatch));
        assertTrue(bypass.diagnostics().stream().anyMatch(x -> x.startsWith("COMPOSITION_CHANGED " + selected)));
    }

    @Test void attributedStructuresAcceptForeignLineEndingsAndStillRejectBehaviorChanges() throws Exception {
        String dispatch = "consumer.Shell#paint(boolean,consumer.Canvas)";
        String data = "consumer.Shell#config()";
        String legacy = "consumer.Shell#wallpaper(consumer.Canvas)";
        String source = """
                package consumer;
                class Canvas { void draw(int x) {} }
                class Shell {
                    void paint(boolean showHistory, Canvas canvas) {
                        if (showHistory) compose(canvas); else wallpaper(canvas);
                    }
                    void compose(Canvas canvas) { canvas.draw(3); }
                    void wallpaper(Canvas canvas) { canvas.draw(6); }
                    String config() { return "first\\r\\nsecond"; }
                }
                """;
        var observed = inspect(source, Map.of(), Map.of());
        var policy = new BoundaryPolicy(Set.of(), Set.of(), Set.of("consumer.Canvas"), Set.of(), Set.of(),
                Map.of(legacy, foreignLineEndings(observed.uiStructures().get(legacy)),
                        data, foreignLineEndings(observed.compositionSnapshot(data).split("\nresolved calls:\n", 2)[0])),
                Map.of("consumer.Canvas", foreignLineEndings(observed.implementationStructures().get("consumer.Canvas"))),
                true, Set.of(data), Map.of(BODY, foreignComposition(observed.compositionSnapshot(BODY))),
                Map.of(dispatch, foreignComposition(observed.compositionSnapshot(dispatch))));
        var file = directory.resolve("Shell.java");
        Files.writeString(file, source);
        var exact = BoundaryCheck.inspect(List.of(file), List.of(), 17, policy);
        assertTrue(exact.valid(), exact.diagnostics().toString());

        Files.writeString(file, source.replace("if (showHistory)", "if (!showHistory)")
                .replace("canvas.draw(3)", "canvas.draw(4)")
                .replace("canvas.draw(6)", "canvas.draw(7)")
                .replace("first\\r\\nsecond", "first\\nsecond")
                .replace("void draw(int x) {}", "void draw(int x) { System.out.print(x); }"));
        var changed = BoundaryCheck.inspect(List.of(file), List.of(), 17, policy);
        for (String diagnostic : List.of("HOST_DISPATCH_CHANGED " + dispatch, "COMPOSITION_CHANGED " + BODY,
                "LEGACY_UI_CHANGED " + legacy, "LEGACY_DATA_CHANGED " + data, "IMPLEMENTATION_CHANGED consumer.Canvas")) {
            assertTrue(changed.diagnostics().stream().anyMatch(line -> line.startsWith(diagnostic)),
                    changed.diagnostics().toString());
        }
    }

    private static String foreignComposition(String snapshot) {
        int metadata = snapshot.indexOf("\nresolved calls:\n");
        return foreignLineEndings(snapshot.substring(0, metadata)) + snapshot.substring(metadata);
    }

    private static String foreignLineEndings(String value) {
        return System.lineSeparator().equals("\r\n") ? value.replace("\r\n", "\n") : value.replace("\n", "\r\n");
    }

    private BoundaryReport inspect(String source, Map<String, String> implementations,
                                   Map<String, String> compositions) throws Exception {
        return inspect(source, implementations, compositions, Map.of(), Map.of());
    }

    private BoundaryReport inspect(String source, Map<String, String> implementations,
                                   Map<String, String> compositions, Map<String, String> dispatch,
                                   Map<String, String> legacy) throws Exception {
        var file = directory.resolve("Shell.java");
        Files.writeString(file, source);
        var policy = new BoundaryPolicy(Set.of(), Set.of(), Set.of("consumer.Canvas"), Set.of(), Set.of(),
                legacy, implementations, true, Set.of(), compositions, dispatch);
        return BoundaryCheck.inspect(List.of(file), List.of(), 17, policy);
    }
}
