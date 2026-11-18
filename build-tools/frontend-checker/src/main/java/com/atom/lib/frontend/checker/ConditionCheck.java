package com.atom.lib.frontend.checker;

import java.util.*;

/** Finite named boolean conditions only; no Java callbacks or environment reads. */
public final class ConditionCheck {
    private ConditionCheck() {}
    public static Set<Boolean> witnesses(List<String> expression,Set<String> names,List<Set<String>> observedStates) {
        if(expression.isEmpty() || observedStates.isEmpty()) throw new IllegalArgumentException("MISSING_EVIDENCE condition/sample input empty");
        if(names.stream().anyMatch(Set.of("AND","OR","NOT")::contains)) throw new IllegalArgumentException("condition state uses reserved operator name");
        Set<Boolean> results=new HashSet<>();
        for(Set<String> state:observedStates) {
            if(!names.containsAll(state)) throw new IllegalArgumentException("unknown observed state");
            Deque<Boolean> values=new ArrayDeque<>();
            for(String token:expression) {
                if(names.contains(token)) values.push(state.contains(token));
                else if(token.equals("NOT")) {
                    if(values.isEmpty()) throw new IllegalArgumentException("invalid condition expression");
                    values.push(!values.pop());
                } else if(token.equals("AND") || token.equals("OR")) {
                    if(values.size()<2) throw new IllegalArgumentException("invalid condition expression");
                    boolean right=values.pop(),left=values.pop();values.push(token.equals("AND")?left&&right:left||right);
                } else throw new IllegalArgumentException("unknown condition token "+token);
            }
            if(values.size()!=1) throw new IllegalArgumentException("invalid condition expression");
            results.add(values.pop());
        }
        return Set.copyOf(results);
    }
}
