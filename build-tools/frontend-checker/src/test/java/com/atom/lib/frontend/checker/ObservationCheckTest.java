package com.atom.lib.frontend.checker;

import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class ObservationCheckTest {
    private final Map<String,ObservationCheck.Expected> reference=Map.of(
        "hover.start",new ObservationCheck.Expected(0,0),
        "hover.middle",new ObservationCheck.Expected(.5,.001),
        "hover.end",new ObservationCheck.Expected(1,0));
    @Test void catchesSameProfileWithChangedCurveAndMissingIntermediateFrame() {
        assertTrue(ObservationCheck.compare(reference,Map.of("hover.start",0d,"hover.middle",.5,"hover.end",1d),Set.of(),Map.of()).isEmpty());
        assertTrue(ObservationCheck.compare(reference,Map.of("hover.start",0d,"hover.middle",.25,"hover.end",1d),Set.of(),Map.of()).stream().anyMatch(s->s.startsWith("SEMANTIC_DRIFT ")));
        assertTrue(ObservationCheck.compare(reference,Map.of("hover.start",0d,"hover.end",1d),Set.of(),Map.of()).stream().anyMatch(s->s.startsWith("MISSING_OBSERVATION ")));
    }
    @Test void rejectsAlwaysFalseDisabledOnlyAndAbsentEvidenceInsteadOfCountingSkips() {
        assertFalse(ObservationCheck.compare(reference,Map.of(),Set.of("interactive"),Map.of("interactive",Set.of(false))).isEmpty());
        assertTrue(ObservationCheck.compare(reference,Map.of("hover.start",0d,"hover.middle",.5,"hover.end",1d),Set.of("interactive"),Map.of("interactive",Set.of(false))).stream().anyMatch(s->s.startsWith("CONDITION_WITNESS_MISSING ")));
        assertTrue(ObservationCheck.compare(reference,Map.of("hover.start",0d,"hover.middle",.5,"hover.end",1d),Set.of("interactive"),Map.of("interactive",Set.of(false,true))).isEmpty());
        assertFalse(ObservationCheck.compare(Map.of(),Map.of(),Set.of(),Map.of()).isEmpty());
    }
    @Test void rejectsNonfiniteAndUnreviewedObservations() {
        assertFalse(ObservationCheck.compare(reference,Map.of("hover.start",0d,"hover.middle",Double.NaN,"hover.end",1d),Set.of(),Map.of()).isEmpty());
        assertFalse(ObservationCheck.compare(reference,Map.of("hover.start",0d,"hover.middle",.5,"hover.end",1d,"other",1d),Set.of(),Map.of()).isEmpty());
    }
}
