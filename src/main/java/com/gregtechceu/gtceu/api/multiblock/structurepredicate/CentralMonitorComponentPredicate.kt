package com.gregtechceu.gtceu.api.multiblock.structurepredicate

import com.gregtechceu.gtceu.GTCEu
import com.gregtechceu.gtceu.api.machine.MachineDefinition
import com.gregtechceu.gtceu.api.machine.multiblock.PartAbility
import com.gregtechceu.gtceu.api.multiblock.MultiblockState
import com.gregtechceu.gtceu.common.data.GTBlocks
import com.gregtechceu.gtceu.common.data.GTMachines

import net.minecraft.resources.ResourceLocation
import net.neoforged.neoforge.common.util.Lazy

import com.mojang.serialization.MapCodec
import it.unimi.dsi.fastutil.objects.ObjectArrayList
import it.unimi.dsi.fastutil.objects.ObjectList

enum class CentralMonitorComponentPredicate : AlternativePredicate {
	INSTANCE,
	;

	override fun type(): StructurePredicateType<*> = TYPE
	override fun alternatives(state: MultiblockState?): ObjectList<StructurePredicate> = PREDICATES.get()

	companion object {
		val TYPE: StructurePredicateType<CentralMonitorComponentPredicate> by lazy { StructurePredicateType.register(GTCEu.id("central_monitor_components"), CODEC) }
		private val PREDICATES = Lazy.of {
			ObjectList.of(
				RestrictedPredicate.builder().base(AbilityPredicate(ObjectList.of(PartAbility.INPUT_ENERGY))).minCount(1).maxCount(2).previewCount(1).build(),
				RestrictedPredicate.builder().base(
					ConcatenatedPredicate(
						ObjectList.of(
							AbilityPredicate(ObjectList.of(PartAbility.DATA_ACCESS)),
							BlockPredicate { machineBlocks(GTMachines.BATTERY_BUFFER_4) },
							BlockPredicate { machineBlocks(GTMachines.BATTERY_BUFFER_16) },
						),
					),
				).maxCount(4).previewCount(1).build(),
				BlockPredicate { machineBlocks(GTMachines.HULL) },
				BlockPredicate {
					ObjectList.of(
						GTMachines.MONITOR.id,
						GTMachines.ADVANCED_MONITOR.id,
						GTBlocks.CASING_ALUMINIUM_FROSTPROOF.id,
					)
				},
			)
		}
		private fun machineBlocks(definitions: Array<out MachineDefinition?>): ObjectList<ResourceLocation> = ObjectArrayList<ResourceLocation>().also { result -> definitions.forEach { if (it != null) result.add(it.id) } }

		@JvmField
		val CODEC: MapCodec<CentralMonitorComponentPredicate> = MapCodec.unit(INSTANCE)
	}
}
