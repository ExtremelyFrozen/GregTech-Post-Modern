package com.gregtechceu.gtceu.api.multiblock.pattern.constraint

import com.gregtechceu.gtceu.api.multiblock.pattern.model.PatternConstraint
import com.gregtechceu.gtceu.api.multiblock.pattern.model.PatternDefinition
import com.gregtechceu.gtceu.api.multiblock.pattern.model.PatternFacts

import it.unimi.dsi.fastutil.ints.IntList
import it.unimi.dsi.fastutil.ints.IntLists
import it.unimi.dsi.fastutil.objects.Object2IntMap
import it.unimi.dsi.fastutil.objects.Object2IntMaps
import org.jspecify.annotations.NullMarked

@NullMarked
object PatternConstraintEvaluator {
	@JvmStatic
	fun entries(constraint: PatternConstraint.Count, facts: PatternFacts): Object2IntMap<String> = when (val scope = constraint.scope) {
		is PatternConstraint.Scope.All -> Object2IntMaps.singleton("all", facts.count(constraint.fact))
		is PatternConstraint.Scope.Node -> facts.scopeEntries("node:${scope.id}", constraint.fact)
		is PatternConstraint.Scope.Repeat -> facts.scopeEntries("repeat:${scope.id}", constraint.fact)
		is PatternConstraint.Scope.Fragment -> facts.scopeEntries("fragment:${scope.id}", constraint.fact)
	}

	@JvmStatic
	fun satisfied(definition: PatternDefinition, facts: PatternFacts): Boolean = definition.constraints.all { constraint ->
		constraint !is PatternConstraint.Count || counts(constraint, facts).all { it in constraint.minimum..constraint.maximum }
	}

	@JvmStatic
	fun counts(constraint: PatternConstraint.Count, facts: PatternFacts): IntList = when (val scope = constraint.scope) {
		is PatternConstraint.Scope.All -> IntLists.singleton(facts.count(constraint.fact))
		is PatternConstraint.Scope.Node -> facts.scopedCounts("node:${scope.id}", constraint.fact).ifEmpty { IntLists.singleton(0) }
		is PatternConstraint.Scope.Repeat -> facts.scopedCounts("repeat:${scope.id}", constraint.fact).ifEmpty { IntLists.singleton(0) }
		is PatternConstraint.Scope.Fragment -> facts.scopedCounts("fragment:${scope.id}", constraint.fact).ifEmpty { IntLists.singleton(0) }
	}
}
