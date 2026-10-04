package com.gregtechceu.gtceu.api.multiblock.pattern.constraint

import com.gregtechceu.gtceu.api.multiblock.pattern.model.PatternConstraint
import com.gregtechceu.gtceu.api.multiblock.pattern.model.PatternDefinition
import com.gregtechceu.gtceu.api.multiblock.pattern.model.PatternFacts
import org.jspecify.annotations.NullMarked

@NullMarked
object PatternConstraintEvaluator {
    @JvmStatic
    fun satisfied(definition: PatternDefinition, facts: PatternFacts): Boolean =
        definition.constraints.all { constraint ->
            constraint !is PatternConstraint.Count || facts.count(constraint.fact) in constraint.minimum..constraint.maximum
        }
}
