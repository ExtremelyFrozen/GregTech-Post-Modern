package com.gregtechceu.gtceu.api.multiblock.structurepredicate

import com.gregtechceu.gtceu.api.GTCEuAPI
import com.gregtechceu.gtceu.api.machine.MultiblockMachineDefinition
import com.gregtechceu.gtceu.api.machine.multiblock.IBatteryData
import com.gregtechceu.gtceu.api.multiblock.MultiblockBlockInfo
import com.gregtechceu.gtceu.api.multiblock.MultiblockState
import com.gregtechceu.gtceu.api.multiblock.autobuild.BlockCategories
import com.gregtechceu.gtceu.common.machine.multiblock.electric.PowerSubstationMachine
import com.gregtechceu.gtceu.common.machine.multiblock.electric.PowerSubstationMachine.PMC_BATTERY_HEADER
import com.gregtechceu.gtceu.common.registry.GTRegistration.REGISTRATE

import net.minecraft.world.level.block.state.BlockState

import com.mojang.serialization.MapCodec
import it.unimi.dsi.fastutil.objects.ObjectImmutableList
import it.unimi.dsi.fastutil.objects.ObjectList

/** Battery aggregation is performed once by the actual rule for Java and serialized patterns. */
enum class PowerSubstationBatteryPredicate : CategoryPredicate {
	INSTANCE,
	;

	override fun hasPositionDependencies(): Boolean = true

	override val category: String = BlockCategories.POWER_SUBSTATION_BATTERIES
	override fun type(): StructurePredicateType<*> = TYPE
	override fun observeCandidate(candidateState: BlockState): PatternObservation = PatternObservation.of(
		blockCandidates().contains(candidateState.block),
		ObjectList.of("battery"),
	)
	private data class Position(val battery: IBatteryData) : PatternObservation.PositionMatch
	override fun observePosition(state: MultiblockState): PatternObservation {
		val battery = GTCEuAPI.PSS_BATTERIES.entries.firstOrNull { state.blockState.`is`(it.value.get()) }?.key
			?: return PatternObservation.of(false)
		return PatternObservation.of(true, ObjectList.of("battery")).withPositionMatch(Position(battery))
	}
	override fun acceptPosition(state: MultiblockState, observation: PatternObservation): Boolean {
		if (!observation.matched) return false
		val battery = (observation.positionMatch as Position).battery
		if (battery.tier != -1 && battery.capacity > 0) {
			val key = PMC_BATTERY_HEADER + battery.batteryName
			val wrapper = state.facts.get<PowerSubstationMachine.BatteryMatchWrapper>(key)
				?: PowerSubstationMachine.BatteryMatchWrapper(battery)
			state.facts.set(key, wrapper.increment())
		}
		return true
	}

	override fun test(multiblockState: MultiblockState, mutateCount: Boolean): Boolean {
		for ((battery, block) in GTCEuAPI.PSS_BATTERIES.entries) {
			if (!multiblockState.blockState.`is`(block.get())) continue
			if (mutateCount && battery.tier != -1 && battery.capacity > 0) {
				val key = PMC_BATTERY_HEADER + battery.batteryName
				val wrapper = multiblockState.facts.get<PowerSubstationMachine.BatteryMatchWrapper>(key)
					?: PowerSubstationMachine.BatteryMatchWrapper(battery)
				multiblockState.facts.set(key, wrapper.increment())
			}
			return true
		}
		return false
	}
	override fun previewChoices(definition: MultiblockMachineDefinition): ObjectList<StructurePreviewChoice> = ObjectList.of(
		StructurePreviewChoice.unrestricted(
			sortedBatteries().stream().filter { it.tier >= 0 && it.capacity > 0 }
				.map { MultiblockBlockInfo(GTCEuAPI.PSS_BATTERIES[it]!!.get().defaultBlockState(), null) }.collect(ObjectImmutableList.toList()),
		),
	)
	private fun sortedBatteries(): ObjectList<IBatteryData> = GTCEuAPI.PSS_BATTERIES.keys.stream().sorted(compareBy { it.tier }).collect(ObjectImmutableList.toList())

	companion object {
		val TYPE: StructurePredicateType<PowerSubstationBatteryPredicate> by lazy { REGISTRATE.structurePredicate("power_substation_batteries", CODEC) }

		@JvmField
		val CODEC: MapCodec<PowerSubstationBatteryPredicate> = MapCodec.unit(INSTANCE)
	}
}
