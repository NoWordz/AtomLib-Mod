package com.atom.lib.frontend.checker;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import javax.tools.ToolProvider;
import java.nio.file.*;
import java.net.URLClassLoader;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

/** Mutates an isolated copy of the real implementation, never the shared source. */
class SemanticCompatibilityProbeTest {
    @TempDir Path root;
    private Map<String,Double> sample(String source,Path directory) throws Exception {
        Path input=directory.resolve("com/atom/lib/motion/MotionSpec.java");
        Files.createDirectories(input.getParent());Files.writeString(input,source);
        assertEquals(0,ToolProvider.getSystemJavaCompiler().run(null,null,null,"--release","17","-d",directory.toString(),input.toString()),"compilation failure is not a semantic negative");
        try(var loader=new URLClassLoader(new java.net.URL[]{directory.toUri().toURL()},null)) {
            Class<?> type=loader.loadClass("com.atom.lib.motion.MotionSpec");
            Object spec=type.getField("STANDARD").get(null);
            var scale=type.getMethod("scale",float.class,float.class,float.class,float.class);
            return Map.of("hover",((Float)scale.invoke(spec,1f,0f,100f,2f)).doubleValue(),
                "press",((Float)scale.invoke(spec,0f,1f,100f,2f)).doubleValue());
        }
    }
    @Test void unchangedIdentityAndParametersCannotHideChangedInterpretation() throws Exception {
        Path source=Path.of("../../core/src/main/java/com/atom/lib/motion/MotionSpec.java");
        String original=Files.readString(source);
        String altered=original.replace("return 1+Math.max(","return 1+.5f*Math.max(");
        // Current implementation returns a clamped displacement using a local bound.
        if(altered.equals(original)) altered=original.replace("return 1+", "return 1+.5f*");
        assertNotEquals(original,altered,"mutation must hit the real interpreter");
        var reference=Map.of("hover",new ObservationCheck.Expected(1.015,.000001),"press",new ObservationCheck.Expected(.975,.000001));
        assertTrue(ObservationCheck.compare(reference,sample(original,root.resolve("old")),Set.of(),Map.of()).isEmpty());
        var errors=ObservationCheck.compare(reference,sample(altered,root.resolve("new")),Set.of(),Map.of());
        assertTrue(errors.stream().anyMatch(s->s.startsWith("SEMANTIC_DRIFT ")),errors.toString());
    }
}
