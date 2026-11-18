package com.atom.lib.frontend.contract;

import java.util.*;

/** Finite named state expressions. There is no user callback or environment access. */
public sealed interface NamedCondition permits NamedCondition.State,NamedCondition.All,NamedCondition.Any,NamedCondition.Not {
    record State(String name) implements NamedCondition {}
    record All(List<NamedCondition> operands) implements NamedCondition {public All{operands=List.copyOf(operands);}}
    record Any(List<NamedCondition> operands) implements NamedCondition {public Any{operands=List.copyOf(operands);}}
    record Not(NamedCondition operand) implements NamedCondition {}
}
