package com.atom.lib.frontend.checker;

import java.util.*;

/** Compares output events with independently reviewed expectations. */
public final class SoundCheck {
    private SoundCheck() {}
    public static List<String> compare(boolean explicitlySilent,List<SoundObservation> expected,List<SoundObservation> actual,Set<String> resolvedResources) {
        List<String> errors=new ArrayList<>();
        if(explicitlySilent && !expected.isEmpty() || !explicitlySilent && expected.isEmpty()) errors.add("MISSING_SOUND_COVERAGE explicit audible/silent expectation required");
        if(actual.size()!=expected.size()) errors.add("SOUND_EVENT_COUNT expected="+expected.size()+" actual="+actual.size());
        for(var item:expected) if(!resolvedResources.contains(item.resource())) errors.add("SOUND_RESOURCE_MISSING "+item.resource());
        for(var item:actual) if(!resolvedResources.contains(item.resource())) errors.add("SOUND_RESOURCE_MISSING "+item.resource());
        for(int i=0;i<Math.min(expected.size(),actual.size());i++) if(!expected.get(i).equals(actual.get(i))) errors.add("SOUND_PARAMETER_DRIFT event="+i+" expected="+expected.get(i)+" actual="+actual.get(i));
        return List.copyOf(errors);
    }
}
