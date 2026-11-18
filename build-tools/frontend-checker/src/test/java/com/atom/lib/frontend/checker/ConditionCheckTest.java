package com.atom.lib.frontend.checker;

import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class ConditionCheckTest {
    private final Set<String> names=Set.of("enabled","hovered");
    @Test void derivesBothWitnessesFromExecutedStateInputs() {
        assertEquals(Set.of(false,true),ConditionCheck.witnesses(List.of("enabled","hovered","AND"),names,List.of(Set.of("enabled"),Set.of("enabled","hovered"))));
        assertEquals(Set.of(false),ConditionCheck.witnesses(List.of("enabled","hovered","AND"),names,List.of(Set.of(),Set.of("hovered"))));
    }
    @Test void impossibleConditionCannotManufacturePositiveWitness() {
        var outcomes=ConditionCheck.witnesses(List.of("enabled","enabled","NOT","AND"),names,List.of(Set.of(),Set.of("enabled"),Set.of("enabled","hovered")));
        assertEquals(Set.of(false),outcomes);
        var errors=ObservationCheck.compare(Map.of("hover",new ObservationCheck.Expected(0,0)),Map.of("hover",0d),Set.of("interactive"),Map.of("interactive",outcomes));
        assertTrue(errors.stream().anyMatch(s->s.startsWith("CONDITION_WITNESS_MISSING ")));
    }
    @Test void unknownInputsEmptySamplesAndMalformedExpressionsFailInsteadOfSkip() {
        assertThrows(IllegalArgumentException.class,()->ConditionCheck.witnesses(List.of("environment"),names,List.of(Set.of())));
        assertThrows(IllegalArgumentException.class,()->ConditionCheck.witnesses(List.of("enabled"),names,List.of(Set.of("environment"))));
        assertThrows(IllegalArgumentException.class,()->ConditionCheck.witnesses(List.of("enabled","AND"),names,List.of(Set.of())));
        assertThrows(IllegalArgumentException.class,()->ConditionCheck.witnesses(List.of("enabled","hovered"),names,List.of(Set.of())));
        assertThrows(IllegalArgumentException.class,()->ConditionCheck.witnesses(List.of("enabled"),names,List.of()));
    }
}
