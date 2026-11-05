package com.gregtechceu.gtceu.api.multiblock.structurepredicate

import com.gregtechceu.gtceu.GTCEu
import com.gregtechceu.gtceu.api.machine.MultiblockMachineDefinition
import com.gregtechceu.gtceu.api.machine.multiblock.PartAbility
import com.gregtechceu.gtceu.api.multiblock.MultiblockState
import com.gregtechceu.gtceu.api.multiblock.pattern.layout.PatternAxis
import com.gregtechceu.gtceu.api.multiblock.pattern.layout.PatternLayout
import com.gregtechceu.gtceu.common.data.GTBlocks
import com.gregtechceu.gtceu.common.data.GTMachines
import com.gregtechceu.gtceu.config.ConfigHolder
import com.gregtechceu.gtceu.data.recipe.CustomTags

import net.minecraft.resources.ResourceLocation
import net.neoforged.neoforge.common.util.Lazy

import com.mojang.serialization.Codec
import com.mojang.serialization.MapCodec
import com.mojang.serialization.codecs.RecordCodecBuilder
import it.unimi.dsi.fastutil.objects.ObjectArrayList
import it.unimi.dsi.fastutil.objects.ObjectList

import java.util.function.Supplier

/** Boundary rules retain floor-area-dependent pass-through limits without duplicating serialized structure data. */
@JvmRecord
data class CleanroomBasePredicate(val allowFloorBlocks: Boolean, val allowDoors: Boolean) : AlternativePredicate {
	override fun type(): StructurePredicateType<*> = TYPE
	override fun alternatives(state: MultiblockState?): ObjectList<StructurePredicate> = boundaryAlternatives(
		if (state == null) PASSTHROUGH.get() else passthrough(state.facts.getInt("pattern.width") * state.facts.getInt("pattern.depth") / 4),
	)
	override fun previewAlternatives(definition: MultiblockMachineDefinition, layout: PatternLayout?): ObjectList<StructurePredicate> = boundaryAlternatives(
		if (layout == null) PASSTHROUGH.get() else passthrough(layout.size(PatternAxis.WIDTH) * layout.size(PatternAxis.DEPTH) / 4),
	)
	private fun boundaryAlternatives(passthrough: StructurePredicate): ObjectList<StructurePredicate> = ObjectArrayList<StructurePredicate>().also { result ->
		val walls = WALL_BLOCKS.get()
		result.add(if (allowFloorBlocks) walls.or(FLOOR_BLOCKS.get()) else walls)
		result.add(INPUT_ENERGY.get())
		result.add(if (ConfigHolder.INSTANCE.machines.enableMaintenance) MAINTENANCE_ENABLED.get() else MAINTENANCE_DISABLED.get())
		result.add(passthrough)
		if (allowDoors) result.add(DOORS.get())
	}

	companion object {
		val TYPE: StructurePredicateType<CleanroomBasePredicate> by lazy { StructurePredicateType.register(GTCEu.id("cleanroom_base"), CODEC) }
		private val WALL_BLOCKS = Lazy.of<StructurePredicate> { BlockPredicate(Supplier(::wallBlocks)) }
		private val INPUT_ENERGY = Lazy.of<StructurePredicate> { RestrictedPredicate.builder().base(AbilityPredicate(ObjectList.of(PartAbility.INPUT_ENERGY))).minCount(1).maxCount(2).build() }
		private val MAINTENANCE_ENABLED = Lazy.of<StructurePredicate> { RestrictedPredicate.builder().base(BlockPredicate(Supplier(::maintenanceBlocks))).minCount(1).maxCount(1).build() }
		private val MAINTENANCE_DISABLED = Lazy.of<StructurePredicate> { RestrictedPredicate.builder().base(BlockPredicate(Supplier(::maintenanceBlocks))).minCount(0).maxCount(1).build() }
		private val FLOOR_BLOCKS = Lazy.of<StructurePredicate> { BlockTagPredicate(CustomTags.CLEANROOM_FLOORS) }
		private val DOORS = Lazy.of<StructurePredicate> { RestrictedPredicate.builder().base(CleanroomDoorPredicate.INSTANCE).maxCount(8).build() }
		private val PASSTHROUGH = Lazy.of<StructurePredicate> { AbilityPredicate(ObjectList.of(PartAbility.PASSTHROUGH_HATCH)) }

		private fun passthrough(maximum: Int): StructurePredicate = RestrictedPredicate.builder().base(PASSTHROUGH.get()).maxCount(maximum).build()
		private fun wallBlocks(): ObjectList<ResourceLocation> = ObjectList.of(GTBlocks.PLASTCRETE.id, GTBlocks.CLEANROOM_GLASS.id)
		private fun maintenanceBlocks(): ObjectList<ResourceLocation> = ObjectList.of(GTMachines.MAINTENANCE_HATCH.id, GTMachines.AUTO_MAINTENANCE_HATCH.id)

		@JvmField
		val CODEC: MapCodec<CleanroomBasePredicate> = RecordCodecBuilder.mapCodec { instance ->
			instance.group(
				Codec.BOOL.optionalFieldOf("allowFloorBlocks", false).forGetter(CleanroomBasePredicate::allowFloorBlocks),
				Codec.BOOL.optionalFieldOf("allowDoors", false).forGetter(CleanroomBasePredicate::allowDoors),
			).apply(instance, ::CleanroomBasePredicate)
		}
	}
}
