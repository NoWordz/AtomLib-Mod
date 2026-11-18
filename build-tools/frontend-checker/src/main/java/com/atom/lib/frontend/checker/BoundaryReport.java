package com.atom.lib.frontend.checker;

import java.util.List;
import java.util.Map;
import java.util.Set;

public record BoundaryReport(List<String> diagnostics, Map<String,String> uiStructures,
                             Map<String,Set<String>> categories,Map<String,Set<String>> dependencies,
                             Map<String,String> implementationStructures,Map<String,Set<String>> implementationDependencies) {
    public BoundaryReport(List<String> diagnostics,Map<String,String> structures,Map<String,Set<String>> categories,Map<String,Set<String>> dependencies,Map<String,String> implementations){this(diagnostics,structures,categories,dependencies,implementations,Map.of());}
    public BoundaryReport(List<String> diagnostics,Map<String,String> structures,Map<String,Set<String>> categories,Map<String,Set<String>> dependencies){this(diagnostics,structures,categories,dependencies,Map.of());}
    public BoundaryReport(List<String> diagnostics,Map<String,String> structures) {this(diagnostics,structures,Map.of(),Map.of());}
    public BoundaryReport {
        diagnostics = List.copyOf(diagnostics);
        uiStructures = Map.copyOf(uiStructures);
        implementationStructures=Map.copyOf(implementationStructures);
        implementationDependencies=implementationDependencies.entrySet().stream().collect(java.util.stream.Collectors.toUnmodifiableMap(Map.Entry::getKey,e->Set.copyOf(e.getValue())));
        categories=categories.entrySet().stream().collect(java.util.stream.Collectors.toUnmodifiableMap(Map.Entry::getKey,e->Set.copyOf(e.getValue())));
        dependencies=dependencies.entrySet().stream().collect(java.util.stream.Collectors.toUnmodifiableMap(Map.Entry::getKey,e->Set.copyOf(e.getValue())));
    }
    public boolean valid() { return diagnostics.isEmpty(); }
}
