package com.gregtechceu.gtceu.api.multiblock.pattern.model

import net.minecraft.resources.ResourceLocation
import org.jspecify.annotations.NullMarked

/** Canonical source model shared by Java DSL, JSON and compressed binary definitions. */
@NullMarked
@JvmRecord
data class PatternDefinition(
    val machine: ResourceLocation,
    val structure: ResourceLocation,
    val axes: PatternAxes,
    val orientation: PatternOrientation,
    val origin: PatternOrigin,
    val parameters: Map<String, PatternParameter>,
    val fragments: Map<String, PatternFragment>,
    val predicates: Map<Char, PatternPredicateDefinition>,
    val body: PatternNode,
    val constraints: List<PatternConstraint>,
)
