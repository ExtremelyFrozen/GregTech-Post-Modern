package com.gregtechceu.gtceu.api.multiblock.pattern.predicate

import com.gregtechceu.gtceu.api.machine.MachineDefinition

/** Predicate that marks a machine controller block. */
open class PredicateController : PatternPredicate {
    constructor(definition: MachineDefinition) : super(PredicateBlocks(definition.get()))

    constructor(predicate: PatternPredicate) : super(predicate)
}
