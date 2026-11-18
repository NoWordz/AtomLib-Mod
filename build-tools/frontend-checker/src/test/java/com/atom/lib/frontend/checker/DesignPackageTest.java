package com.atom.lib.frontend.checker;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class DesignPackageTest {
    @TempDir Path root;
    private Path author(String fields) throws Exception {
        Path p = root.resolve("Theme.java");
        Files.writeString(p,"public final class Theme {" + fields + "}"); return p;
    }
    private String header() { return "public static final String namespace=\"sample\", version=\"1\", profile=\"probe-v1\";"; }
    @Test void rejectsEnvironmentCallsWithoutExecutingAuthorCode() throws Exception {
        assertThrows(IllegalArgumentException.class, () -> DesignPackage.compile(author(header()+"public static final String color=System.getProperty(\"theme.color\");")));
    }
    @Test void canonicalOrderRoundTripsAndCannotReuseVersionForDifferentData() throws Exception {
        byte[] a = DesignPackage.compile(author(header()+"public static final int gap=8; public static final String material=\"frosted\";"));
        byte[] b = DesignPackage.compile(author(header()+"public static final String material=\"frosted\"; public static final int gap=8;"));
        assertArrayEquals(a,b);
        assertEquals("8", DesignPackage.read(a).get("gap"));
        byte[] changed = DesignPackage.compile(author(header()+"public static final int gap=9; public static final String material=\"frosted\";"));
        assertEquals("INVALID_REUSED_VERSION", ApprovalCheck.assess(a, changed, Set.of("probe-v1")));
    }
    @Test void approvalDoesNotPromoteCandidateAndUnsupportedProfileNeverFallsBack() throws Exception {
        byte[] a = DesignPackage.compile(author(header()+"public static final int gap=8;"));
        assertEquals("APPROVED_VALID",ApprovalCheck.assess(a,a,Set.of("probe-v1")));
        assertEquals("MISSING_EVIDENCE",ApprovalCheck.assess(null,a,Set.of("probe-v1")));
        byte[] b = DesignPackage.compile(author(header().replace("version=\"1\"", "version=\"2\"")+"public static final int gap=9;"));
        assertEquals("CANDIDATE_VALID",ApprovalCheck.assess(a,b,Set.of("probe-v1")));
        assertEquals("CANDIDATE_VALID",ApprovalCheck.assess(a,b,Set.of("probe-v1")),"candidate validation cannot create approval");
        assertEquals("INCOMPATIBLE_PROFILE",ApprovalCheck.assess(a,a,Set.of("probe-v2")));
    }
    @Test void rejectsInitializersMethodsAndNoncanonicalOrTruncatedRuntimeData() throws Exception {
        assertThrows(IllegalArgumentException.class, () -> DesignPackage.compile(author(header()+"static { System.setProperty(\"bad\",\"true\"); }")));
        assertThrows(IllegalArgumentException.class, () -> DesignPackage.compile(author(header()+"public static int gap(){return 8;}")));
        byte[] good=DesignPackage.compile(author(header()));
        assertThrows(IllegalArgumentException.class, () -> DesignPackage.read(Arrays.copyOf(good,good.length-1)));
        assertThrows(IllegalArgumentException.class, () -> DesignPackage.read(Arrays.copyOf(good,good.length+1)));
    }
}
