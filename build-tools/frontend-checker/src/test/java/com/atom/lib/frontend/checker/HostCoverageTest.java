package com.atom.lib.frontend.checker;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class HostCoverageTest {
    @TempDir Path root;
    @Test void discoversRegisteredHudEventEvenWithoutAnyDrawing() throws Exception {
        Path sdk=root.resolve("RenderGuiEvent.java"),page=root.resolve("Page.java");
        Files.writeString(sdk,"package net.minecraftforge.client.event; public class RenderGuiEvent {public static class Post {}} ");
        Files.writeString(page,"package demo; class Page {void frame(net.minecraftforge.client.event.RenderGuiEvent.Post e){int diagnosticsOnly=1;}} ");
        var result=BoundaryCheck.inspect(List.of(sdk,page),List.of(),17,new BoundaryPolicy(Set.of(),Set.of(),Set.of(),Set.of(),Set.of(),Map.of()));
        assertFalse(result.diagnostics().stream().anyMatch(s->s.startsWith("SOURCE_ANALYSIS_FAILED")),result.toString());
        String symbol="demo.Page#frame(net.minecraftforge.client.event.RenderGuiEvent.Post)";
        assertTrue(result.uiStructures().containsKey(symbol),result.toString());
        assertTrue(result.categories().getOrDefault(symbol,Set.of()).contains("HUD_EVENT"),result.toString());
        assertTrue(result.diagnostics().stream().anyMatch(s->s.startsWith("UNCOVERED_ENTRY ")),result.toString());
    }
}
