package com.gregtechceu.gtceu.api.multiblock.pattern.predicate;

import com.gregtechceu.gtceu.api.machine.MachineDefinition;

public class PredicateController extends PatternPredicate {

    public PredicateController(MachineDefinition definition) {
        super(new PredicateBlocks(definition.get()));
    }

    public PredicateController(PatternPredicate predicate) {
        super(predicate);
    }
}
