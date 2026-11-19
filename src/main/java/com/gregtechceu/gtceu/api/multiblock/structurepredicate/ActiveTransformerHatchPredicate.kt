package com.gregtechceu.gtceu.api.multiblock.structurepredicate

import com.gregtechceu.gtceu.api.machine.MultiblockMachineDefinition
import com.gregtechceu.gtceu.api.machine.multiblock.PartAbility
import com.gregtechceu.gtceu.api.multiblock.MultiblockState
import com.gregtechceu.gtceu.api.multiblock.pattern.layout.PatternLayout
import com.gregtechceu.gtceu.common.registry.GTRegistration.REGISTRATE

import com.mojang.serialization.MapCodec
import it.unimi.dsi.fastutil.objects.ObjectArrayList
import it.unimi.dsi.fastutil.objects.ObjectList

/** Transformer IO and its two representative hatch families share the actual ability definitions. */
enum class ActiveTransformerHatchPredicate : AlternativePredicate {
	INSTANCE,
	;

	override fun type(): StructurePredicateType<*> = TYPE
	override fun alternatives(state: MultiblockState?): ObjectList<StructurePredicate> = MATCH_PREDICATES
	override fun previewAlternatives(definition: MultiblockMachineDefinition, layout: PatternLayout?): ObjectList<StructurePredicate> = PREVIEW_PREDICATES

	companion object {
		val TYPE: StructurePredicateType<ActiveTransformerHatchPredicate> by lazy { REGISTRATE.structurePredicate("active_transformer_hatches", CODEC) }

		@JvmField
		val CODEC: MapCodec<ActiveTransformerHatchPredicate> = MapCodec.unit(INSTANCE)
		private val INPUT_ABILITIES = ObjectList.of(PartAbility.INPUT_ENERGY, PartAbility.SUBSTATION_INPUT_ENERGY, PartAbility.INPUT_LASER)
		private val OUTPUT_ABILITIES = ObjectList.of(PartAbility.OUTPUT_ENERGY, PartAbility.SUBSTATION_OUTPUT_ENERGY, PartAbility.OUTPUT_LASER)
		private val ABILITIES = ObjectArrayList(INPUT_ABILITIES).also { it.addAll(OUTPUT_ABILITIES) }
		private val MATCH_PREDICATES: ObjectList<StructurePredicate> = ObjectList.of(AbilityPredicate(ABILITIES))
		private val PREVIEW_PREDICATES: ObjectList<StructurePredicate> = ObjectList.of(
			RestrictedPredicate.builder().base(AbilityPredicate(INPUT_ABILITIES)).previewCount(1).build(),
			RestrictedPredicate.builder().base(AbilityPredicate(OUTPUT_ABILITIES)).previewCount(1).build(),
		)
	}
}
