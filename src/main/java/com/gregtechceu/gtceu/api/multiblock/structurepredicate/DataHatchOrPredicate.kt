package com.gregtechceu.gtceu.api.multiblock.structurepredicate

import com.gregtechceu.gtceu.api.machine.multiblock.PartAbility
import com.gregtechceu.gtceu.api.multiblock.MultiblockState
import com.gregtechceu.gtceu.common.registry.GTRegistration.REGISTRATE
import com.gregtechceu.gtceu.config.ConfigHolder

import net.neoforged.neoforge.common.util.Lazy

import com.mojang.serialization.MapCodec
import com.mojang.serialization.codecs.RecordCodecBuilder
import it.unimi.dsi.fastutil.objects.ObjectList

/** Research enables one stable exact-count data rule; its alternative alone is used when research is disabled. */
class DataHatchOrPredicate(val alternative: StructurePredicate) : AlternativePredicate {
	private val dataHatches = Lazy.of {
		RestrictedPredicate.builder().base(AbilityPredicate(ObjectList.of(PartAbility.DATA_ACCESS, PartAbility.OPTICAL_DATA_RECEPTION))).exactCount(1).build()
	}
	override fun type(): StructurePredicateType<*> = TYPE
	override fun alternatives(state: MultiblockState?): ObjectList<StructurePredicate> = if (ConfigHolder.INSTANCE.machines.enableResearch) ObjectList.of(dataHatches.get(), alternative) else ObjectList.of(alternative)

	private data class Position(val selected: StructurePredicate, val child: PatternObservation, val requiredData: RestrictedPredicate?) : PatternObservation.PositionMatch
	internal fun restorePreparedPosition(observation: PatternObservation, selected: StructurePredicate, child: PatternObservation, requiredData: RestrictedPredicate?): PatternObservation = observation.withPositionMatch(Position(selected, child, requiredData))

	override fun observePosition(state: MultiblockState): PatternObservation {
		val data = if (ConfigHolder.INSTANCE.machines.enableResearch) dataHatches.get() else null
		if (data != null) {
			val observed = data.observePosition(state)
			if (observed.matched) return observed.withPositionMatch(Position(data, observed, null))
		}
		val observed = alternative.observePosition(state)
		return observed.withPositionMatch(Position(alternative, observed, data))
	}
	override fun acceptPosition(state: MultiblockState, observation: PatternObservation): Boolean {
		val position = observation.positionMatch as? Position ?: return false
		position.requiredData?.reserveMinimumCounters(state)
		return position.selected.acceptPosition(state, position.child)
	}

	override fun test(multiblockState: MultiblockState, mutateCount: Boolean): Boolean {
		if (!ConfigHolder.INSTANCE.machines.enableResearch) return alternative.test(multiblockState, mutateCount)
		val data = dataHatches.get()
		if (data.test(multiblockState, mutateCount)) return true
		val matched = alternative.test(multiblockState, mutateCount)
		if (matched && mutateCount && !multiblockState.structureGlobalCount.containsKey(data)) multiblockState.structureGlobalCount.put(data, 0)
		return matched
	}
	override fun hasAir(): Boolean = alternative.hasAir()

	companion object {
		val TYPE: StructurePredicateType<DataHatchOrPredicate> by lazy { REGISTRATE.structurePredicate("data_hatch_or", CODEC) }

		@JvmField
		val CODEC: MapCodec<DataHatchOrPredicate> = RecordCodecBuilder.mapCodec { instance ->
			instance.group(StructurePredicate.CODEC.fieldOf("alternative").forGetter(DataHatchOrPredicate::alternative)).apply(instance, ::DataHatchOrPredicate)
		}
	}
}
