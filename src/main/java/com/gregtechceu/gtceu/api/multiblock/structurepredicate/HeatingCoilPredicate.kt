package com.gregtechceu.gtceu.api.multiblock.structurepredicate

import com.gregtechceu.gtceu.api.GTCEuAPI
import com.gregtechceu.gtceu.api.multiblock.MultiblockState
import com.gregtechceu.gtceu.api.multiblock.autobuild.BlockCategories
import com.gregtechceu.gtceu.api.multiblock.error.PatternStringError
import com.gregtechceu.gtceu.common.registry.GTRegistration.REGISTRATE

import net.minecraft.world.level.block.state.BlockState

import com.mojang.serialization.MapCodec
import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap
import it.unimi.dsi.fastutil.objects.ObjectList

enum class HeatingCoilPredicate : CategoryPredicate {
	INSTANCE,
	;

	override fun hasPositionDependencies(): Boolean = true

	override val category: String = BlockCategories.HEATING_COILS
	override fun type(): StructurePredicateType<*> = TYPE

	override fun observeCandidate(candidateState: BlockState): PatternObservation {
		val stats = GTCEuAPI.HEATING_COILS.entries.firstOrNull { it.value.get() == candidateState.block }?.key
			?: return PatternObservation.of(false)
		val properties = Object2ObjectLinkedOpenHashMap<String, Any>()
		properties["CoilType"] = stats
		properties["coil.temperature"] = stats.coilTemperature
		properties["coil.tier"] = stats.tier
		return PatternObservation.of(true, ObjectList.of("coil"), properties)
	}

	override fun observePosition(state: MultiblockState): PatternObservation {
		val observed = observeCandidate(state.blockState)
		if (!observed.matched) return observed
		val current = state.facts.get<Any>("CoilType")
		return if (current == null || current == observed.properties["CoilType"]) observed else PatternObservation.of(false)
	}
	override fun acceptPosition(state: MultiblockState, observation: PatternObservation): Boolean {
		if (!observation.matched) return false
		state.facts.getOrPut("CoilType", observation.properties["CoilType"]!!)
		return true
	}

	override fun test(multiblockState: MultiblockState, mutateCount: Boolean): Boolean {
		for ((stats, block) in GTCEuAPI.HEATING_COILS.entries) {
			if (!multiblockState.blockState.`is`(block.get())) continue
			val currentCoil: Any = multiblockState.facts.getOrPut("CoilType", stats)
			if (currentCoil != stats) {
				multiblockState.setError(PatternStringError("gtpm.multiblock.pattern.error.coils"))
				return false
			}
			return true
		}
		return false
	}

	companion object {
		val TYPE: StructurePredicateType<HeatingCoilPredicate> by lazy { REGISTRATE.structurePredicate("coil", CODEC) }

		@JvmField
		val CODEC: MapCodec<HeatingCoilPredicate> = MapCodec.unit(INSTANCE)
	}
}
