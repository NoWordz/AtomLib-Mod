package com.atom.lib.frontend.checker;

import java.util.*;

/** Exact entry classifications from the current attributed production catalog, not an enrollment command. */
final class MigrationInventory {
    private MigrationInventory(){}
    static List<String> describe(BoundaryPolicy policy,BoundaryReport report){
        Set<String> roots=new HashSet<>(policy.strictEntries());report.categories().forEach((id,categories)->{if(categories.contains("CONTRACT_PAGE"))roots.add(id);});
        Set<String> strict=new HashSet<>(roots);Deque<String> pending=new ArrayDeque<>(strict);
        while(!pending.isEmpty())for(String next:report.dependencies().getOrDefault(pending.remove(),Set.of()))if(strict.add(next))pending.add(next);
        Set<String> lines=new TreeSet<>();for(String entry:roots)lines.add("ENTRY STRICT "+entry);
        for(String entry:report.uiStructures().keySet()){
            String owner=entry.substring(0,entry.indexOf('#'));String category=roots.contains(entry)?"STRICT":strict.contains(entry)?"STRICT_DEPENDENCY":policy.compositions().containsKey(entry)?"HOST_COMPOSITION":policy.dispatch().containsKey(entry)?"HOST_DISPATCH":policy.implementations().containsKey(owner)?"IMPLEMENTATION":policy.legacy().containsKey(entry)?"LEGACY":"UNCOVERED";
            lines.add("ENTRY "+category+" "+entry);
        }
        for(String owner:policy.implementations().keySet())lines.add("IMPLEMENTATION_BOUND "+owner);
        for(String port:policy.businessMethods())lines.add("BUSINESS_PORT "+port);
        return List.copyOf(lines);
    }
}
