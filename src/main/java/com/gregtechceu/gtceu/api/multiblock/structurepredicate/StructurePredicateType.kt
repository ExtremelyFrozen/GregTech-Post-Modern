package com.gregtechceu.gtceu.api.multiblock.structurepredicate

import com.gregtechceu.gtceu.api.addon.AddonFinder
import com.gregtechceu.gtceu.api.addon.IGTAddon
import com.gregtechceu.gtceu.api.registry.GTRegistries

import net.minecraft.resources.ResourceLocation

import com.mojang.serialization.MapCodec

/** Registry identity and the actual leaf codec are shared with the runtime compiler registry. */
@JvmRecord
data class StructurePredicateType<T : StructurePredicate>(val id: ResourceLocation, val codec: MapCodec<T>) {
	companion object {
		@JvmStatic
		fun <T : StructurePredicate> register(id: ResourceLocation, codec: MapCodec<T>): StructurePredicateType<T> {
			val type = StructurePredicateType(id, codec)
			PredicateRegistry.register(id, PredicateType(codec) { PatternPredicate(it) })
			return GTRegistries.register(GTRegistries.STRUCTURE_PREDICATE_TYPES, id, type)
		}

		@JvmStatic
		fun byId(id: ResourceLocation): StructurePredicateType<*>? = GTRegistries.lookup(GTRegistries.STRUCTURE_PREDICATE_TYPES, id)

		/** Definitions own their registration; addons still register before machine definitions use their codecs. */
		@JvmStatic
		fun init() {
			BlockPredicate.TYPE
			BlockStatePredicate.TYPE
			BlockTagPredicate.TYPE
			FluidPredicate.TYPE
			FluidTagPredicate.TYPE
			AbilityPredicate.TYPE
			AutoRecipeAbilityPredicate.TYPE
			AutoMaintenancePredicate.TYPE
			ActiveTransformerHatchPredicate.TYPE
			CentralMonitorComponentPredicate.TYPE
			CharcoalLogOrAirPredicate.TYPE
			CleanroomDoorPredicate.TYPE
			CleanroomInnerPredicate.TYPE
			CleanroomBasePredicate.TYPE
			HeatingCoilPredicate.TYPE
			CleanroomFilterPredicate.TYPE
			FramePredicate.TYPE
			PowerSubstationBatteryPredicate.TYPE
			SnowPredicate.TYPE
			DataHatchOrPredicate.TYPE
			RotorHolderPredicate.TYPE
			RestrictedPredicate.TYPE
			ConcatenatedPredicate.TYPE
			AnyPredicate.TYPE
			AirPredicate.TYPE
			PredicateController.TYPE
			RuntimePredicateCodec.TYPE
			CombinedPredicate.ALL_OF_TYPE
			CombinedPredicate.ANY_OF_TYPE
			CombinedPredicate.ONE_OF_TYPE
			CombinedPredicate.NOT_TYPE
			AddonFinder.getAddonList().forEach(IGTAddon::registerStructurePredicateTypes)
		}
	}
}
