package com.atom.lib.frontend.checker;

import java.util.*;

/** Prototype evidence comparison. Expectations must come from reviewed references. */
public final class ObservationCheck {
    private ObservationCheck() {}
    public record Expected(double value,double tolerance) {
        public Expected {
            if(!Double.isFinite(value) || !Double.isFinite(tolerance) || tolerance<0) throw new IllegalArgumentException("invalid reference");
        }
    }
    public static List<String> compare(Map<String,Expected> reviewed,Map<String,Double> actual,
                                        Set<String> requiredConditions,Map<String,Set<Boolean>> witnesses) {
        List<String> errors=new ArrayList<>();
        if(reviewed.isEmpty()) errors.add("MISSING_EVIDENCE no reviewed observations");
        for(var entry:reviewed.entrySet()) {
            Double value=actual.get(entry.getKey());
            if(value==null) errors.add("MISSING_OBSERVATION "+entry.getKey());
            else if(!Double.isFinite(value) || Math.abs(value-entry.getValue().value())>entry.getValue().tolerance()) errors.add("SEMANTIC_DRIFT "+entry.getKey()+" actual="+value+" expected="+entry.getValue().value());
        }
        for(String extra:actual.keySet()) if(!reviewed.containsKey(extra)) errors.add("UNCOVERED_OBSERVATION "+extra);
        for(String condition:requiredConditions) if(!Set.of(false,true).equals(witnesses.get(condition))) errors.add("CONDITION_WITNESS_MISSING "+condition+" both reachable outcomes required");
        return List.copyOf(errors);
    }
}
