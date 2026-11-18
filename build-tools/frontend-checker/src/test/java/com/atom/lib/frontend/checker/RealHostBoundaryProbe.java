package com.atom.lib.frontend.checker;

import java.nio.file.*;
import java.util.*;

/** Opt-in integration probe against an isolated copy of actual consumer sources. */
public final class RealHostBoundaryProbe {
    private RealHostBoundaryProbe() {}
    public static void main(String[] args) throws Exception {
        Path inputs=Path.of(args[0]),output=Path.of(args[1]);Files.createDirectories(output);
        List<Path> original=Files.readAllLines(inputs.resolve("sources.txt")).stream().filter(s->!s.isBlank()).map(Path::of).toList();
        List<Path> cp=Files.readAllLines(inputs.resolve("classpath.txt")).stream().filter(s->!s.isBlank()).map(Path::of).toList();
        List<Path> sources=new ArrayList<>();
        Path host=null;
        for(int i=0;i<original.size();i++) {
            Path source=original.get(i),copy=output.resolve("source-snapshot/"+i+"/"+source.getFileName());
            Files.createDirectories(copy.getParent());Files.copy(source,copy);
            sources.add(copy);
            if(source.getFileName().toString().equals("AtomChatScreen.java")) host=copy;
        }
        if(host==null) throw new AssertionError("production host missing");
        BoundaryPolicy unreviewed=new BoundaryPolicy(Set.of(),Set.of(),Set.of("io.github.humbleui.skija.Canvas"),
            Set.of("net.minecraft.client.gui.screen.Screen"),Set.of("render","renderBackground","mouseClicked","mouseReleased","mouseDragged","mouseScrolled","keyPressed","charTyped","removed"),Map.of());
        var baseline=BoundaryCheck.inspect(sources,cp,21,unreviewed);
        requireAnalyzed(baseline);
        BoundaryPolicy fixtureOnly=new BoundaryPolicy(Set.of(),Set.of(),unreviewed.drawingTypes(),unreviewed.hostTypes(),unreviewed.hostCallbacks(),baseline.uiStructures());
        // This is a fixture enrollment solely to exercise legacy change detection.
        // Nothing here creates or writes a consumer approval index.
        var unchanged=BoundaryCheck.inspect(sources,cp,21,fixtureOnly);
        if(!unchanged.valid()) throw new AssertionError("unchanged fixture did not pass: "+unchanged.diagnostics());
        String before=Files.readString(host);
        String marker="public void drawArrivingBody(Canvas canvas, UiLayout layout, int mouseX, int mouseY) {";
        if(!before.contains(marker)) throw new AssertionError("actual host callback changed; adapt probe explicitly");
        List<String> evidence=new ArrayList<>(List.of("OBSERVED_ONLY_NOT_APPROVED", "sources="+sources.size(), "uiMethods="+baseline.uiStructures().size(),"unchanged=PASS"));
        for(var mutation:Map.of("same-file-new-route","if (mouseX == -12345) { canvas.clear(0xFF112233); return; }", "outer-shell-paint","canvas.clear(0xFF112233);").entrySet()) {
            try {
                String target=marker;
                if(mutation.getKey().equals("outer-shell-paint")) {
                    int header=before.indexOf("public void drawArrivingHeader(Canvas canvas,");
                    if(header<0) throw new AssertionError("actual outer header callback missing");
                    int opening=before.indexOf('{',header);
                    target=before.substring(header,opening+1);
                }
                Files.writeString(host,before.replace(target,target+mutation.getValue()));
                var report=BoundaryCheck.inspect(sources,cp,21,fixtureOnly);requireAnalyzed(report);
                List<String> caught=report.diagnostics().stream().filter(s->s.startsWith("LEGACY_UI_CHANGED ")).toList();
                if(caught.isEmpty()) throw new AssertionError("real host mutation escaped: "+mutation.getKey());
                Files.write(output.resolve(mutation.getKey()+".txt"),report.diagnostics());
                evidence.add(mutation.getKey()+"=PASS "+caught.size()+" changed methods");
            } finally {Files.writeString(host,before);}
        }
        var restored=BoundaryCheck.inspect(sources,cp,21,fixtureOnly);requireAnalyzed(restored);
        if(!restored.valid()) throw new AssertionError("fixture restore failed");
        evidence.add("restored=PASS");Files.write(output.resolve("results.txt"),evidence);evidence.forEach(System.out::println);
    }
    private static void requireAnalyzed(BoundaryReport report) {
        if(report.uiStructures().isEmpty() || report.diagnostics().stream().anyMatch(s->s.startsWith("SOURCE_ANALYSIS_FAILED"))) throw new AssertionError("invalid negative, attribution failed: "+report.diagnostics());
    }
}
