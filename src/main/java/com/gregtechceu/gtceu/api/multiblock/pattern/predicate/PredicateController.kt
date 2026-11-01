package com.gregtechceu.gtceu.api.multiblock.pattern.predicate

import com.gregtechceu.gtceu.api.machine.MachineDefinition

/** Predicate that marks a machine controller block. */
open class PredicateController : PatternPredicate {
	var owner: MachineDefinition? = null
		private set

	constructor(definition: MachineDefinition) : super(PredicateBlocks(definition.get())) {
		owner = definition
	}

	constructor(predicate: PatternPredicate) : super(predicate) {
		owner = (predicate as? PredicateController)?.owner
	}
}
