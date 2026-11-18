package com.atom.lib.frontend.checker;

import java.util.Map;
import java.util.Set;

/** Exact symbols, not file or package exemptions. Prototype protocol. */
public record BoundaryPolicy(Set<String> strictEntries, Set<String> trustedMethods,
                             Set<String> drawingTypes, Set<String> hostTypes,
                             Set<String> hostCallbacks, Map<String, String> legacy,
                             Map<String,String> implementations,boolean enforceImplementations,Set<String> businessMethods) {
    public BoundaryPolicy(Set<String> strictEntries,Set<String> trustedMethods,Set<String> drawingTypes,Set<String> hostTypes,Set<String> hostCallbacks,Map<String,String> legacy,Map<String,String> implementations,boolean enforceImplementations){this(strictEntries,trustedMethods,drawingTypes,hostTypes,hostCallbacks,legacy,implementations,enforceImplementations,Set.of());}
    public BoundaryPolicy(Set<String> strictEntries,Set<String> trustedMethods,Set<String> drawingTypes,Set<String> hostTypes,Set<String> hostCallbacks,Map<String,String> legacy){this(strictEntries,trustedMethods,drawingTypes,hostTypes,hostCallbacks,legacy,Map.of(),false);}
    public BoundaryPolicy {
        strictEntries = Set.copyOf(strictEntries);
        trustedMethods = Set.copyOf(trustedMethods);
        drawingTypes = Set.copyOf(drawingTypes);
        hostTypes = Set.copyOf(hostTypes);
        hostCallbacks = Set.copyOf(hostCallbacks);
        legacy = Map.copyOf(legacy);
        implementations=Map.copyOf(implementations);
        businessMethods=Set.copyOf(businessMethods);
        if(!java.util.Collections.disjoint(trustedMethods,businessMethods))throw new IllegalArgumentException("INVALID_POLICY business ports and privileged component entries must be distinct");
    }
}
