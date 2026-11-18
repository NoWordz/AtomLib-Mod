package com.atom.lib.frontend.checker;

import java.nio.file.*;
import java.util.*;

/** Read-only observation command. Its output is explicitly not an approval input. */
public final class InventoryMain {
    private InventoryMain() {}
    public static void main(String[] args) throws Exception {
        if(args.length!=3) throw new IllegalArgumentException("sources.txt classpath.txt output-directory");
        List<Path> sources=Files.readAllLines(Path.of(args[0])).stream().filter(s->!s.isBlank()).map(Path::of).toList();
        List<Path> cp=Files.readAllLines(Path.of(args[1])).stream().filter(s->!s.isBlank()).map(Path::of).toList();
        BoundaryReport report=BoundaryCheck.inspect(sources,cp,Runtime.version().feature(),
            new BoundaryPolicy(Set.of(),Set.of(),Set.of("io.github.humbleui.skija.Canvas","net.minecraft.client.gui.GuiGraphics","net.minecraft.client.gui.DrawContext"),
                Set.of("net.minecraft.client.gui.screen.Screen","net.minecraft.client.gui.screens.Screen","com.atom.chat.nav.PageTransitionComposite.Host"),
                Set.of("render","renderBackground","mouseClicked","mouseReleased","mouseDragged","mouseScrolled","keyPressed","charTyped","removed","drawArrivingBody","drawArrivingHeader","bodyVariant"),Map.of()));
        Path output=Path.of(args[2]);Files.createDirectories(output);
        Files.writeString(output.resolve("status.txt"),"OBSERVED_ONLY_NOT_APPROVED\nsources="+sources.size()+"\nuiMethods="+report.uiStructures().size()+"\n");
        Properties observed=new Properties();
        report.uiStructures().forEach((key,value)->observed.setProperty(key,Base64.getEncoder().encodeToString(value.getBytes(java.nio.charset.StandardCharsets.UTF_8))));
        try(var writer=Files.newBufferedWriter(output.resolve("observed-methods.properties"))) {observed.store(writer,"Observed only; never use as automatic legacy/approval enrollment");}
        Properties classes=new Properties();report.implementationStructures().forEach((key,value)->classes.setProperty(key,Base64.getEncoder().encodeToString(value.getBytes(java.nio.charset.StandardCharsets.UTF_8))));
        try(var writer=Files.newBufferedWriter(output.resolve("observed-implementations.properties"))){classes.store(writer,"Observed source only, not approval; reviewer chooses the closure and scope");}
        Files.write(output.resolve("diagnostics.txt"),report.diagnostics());
        List<String> classification=new ArrayList<>(),dependencies=new ArrayList<>();
        new TreeMap<>(report.categories()).forEach((key,value)->classification.add(key+"\t"+String.join(";",new TreeSet<>(value))));
        new TreeMap<>(report.dependencies()).forEach((key,value)->dependencies.add(key+"\t"+String.join(";",new TreeSet<>(value))));
        Files.write(output.resolve("classification.tsv"),classification);Files.write(output.resolve("dependencies.tsv"),dependencies);
        System.out.println("OBSERVED_ONLY_NOT_APPROVED sources="+sources.size()+" uiMethods="+report.uiStructures().size()+" diagnostics="+report.diagnostics().size());
        if(report.diagnostics().stream().anyMatch(s->s.startsWith("SOURCE_ANALYSIS_FAILED"))) System.exit(2);
    }
}
