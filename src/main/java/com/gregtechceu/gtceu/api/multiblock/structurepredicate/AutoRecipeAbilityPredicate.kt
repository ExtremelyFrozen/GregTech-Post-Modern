package com.gregtechceu.gtceu.api.multiblock.structurepredicate

import com.gregtechceu.gtceu.GTCEu
import com.gregtechceu.gtceu.api.capability.recipe.EURecipeCapability
import com.gregtechceu.gtceu.api.capability.recipe.FluidRecipeCapability
import com.gregtechceu.gtceu.api.capability.recipe.ItemRecipeCapability
import com.gregtechceu.gtceu.api.machine.MultiblockMachineDefinition
import com.gregtechceu.gtceu.api.machine.multiblock.PartAbility
import com.gregtechceu.gtceu.api.multiblock.MultiblockState
import com.gregtechceu.gtceu.api.multiblock.pattern.layout.PatternLayout
import com.gregtechceu.gtceu.api.recipe.GTRecipeType

import com.mojang.serialization.Codec
import com.mojang.serialization.MapCodec
import com.mojang.serialization.codecs.RecordCodecBuilder
import it.unimi.dsi.fastutil.objects.Object2ObjectMap
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap
import it.unimi.dsi.fastutil.objects.ObjectArrayList
import it.unimi.dsi.fastutil.objects.ObjectList

/** Recipe capabilities select the actual counted hatch rules for the owning machine definition. */
@JvmRecord
data class AutoRecipeAbilityPredicate(val checkEnergyIn: Boolean, val checkEnergyOut: Boolean, val checkItemIn: Boolean, val checkItemOut: Boolean, val checkFluidIn: Boolean, val checkFluidOut: Boolean) : AlternativePredicate {
	override fun type(): StructurePredicateType<*> = TYPE
	override fun alternatives(state: MultiblockState?): ObjectList<StructurePredicate> {
		if (state == null) return ObjectList.of()
		val controller = state.controller ?: error("Auto recipe ability predicates require a multiblock controller")
		return collect(controller.definition.recipeTypes)
	}
	override fun previewAlternatives(definition: MultiblockMachineDefinition, layout: PatternLayout?): ObjectList<StructurePredicate> = collect(definition.recipeTypes)

	private fun collect(types: Array<GTRecipeType>): ObjectList<StructurePredicate> {
		val result = ObjectArrayList<StructurePredicate>()
		if (checkEnergyIn && types.any { it.getMaxInputs(EURecipeCapability.CAP) > 0 }) result.add(ABILITY_PREDICATES.getValue(PartAbility.INPUT_ENERGY))
		if (checkEnergyOut && types.any { it.getMaxOutputs(EURecipeCapability.CAP) > 0 }) result.add(ABILITY_PREDICATES.getValue(PartAbility.OUTPUT_ENERGY))
		if (checkItemIn && types.any { it.getMaxInputs(ItemRecipeCapability.CAP) > 0 }) result.add(ABILITY_PREDICATES.getValue(PartAbility.IMPORT_ITEMS))
		if (checkItemOut && types.any { it.getMaxOutputs(ItemRecipeCapability.CAP) > 0 }) result.add(ABILITY_PREDICATES.getValue(PartAbility.EXPORT_ITEMS))
		if (checkFluidIn && types.any { it.getMaxInputs(FluidRecipeCapability.CAP) > 0 }) result.add(ABILITY_PREDICATES.getValue(PartAbility.IMPORT_FLUIDS))
		if (checkFluidOut && types.any { it.getMaxOutputs(FluidRecipeCapability.CAP) > 0 }) result.add(ABILITY_PREDICATES.getValue(PartAbility.EXPORT_FLUIDS))
		return result
	}

	companion object {
		val TYPE: StructurePredicateType<AutoRecipeAbilityPredicate> by lazy { StructurePredicateType.register(GTCEu.id("auto_recipe_abilities"), CODEC) }
		private val ABILITY_PREDICATES: Object2ObjectMap<PartAbility, StructurePredicate> = Object2ObjectOpenHashMap<PartAbility, StructurePredicate>().also { result ->
			result[PartAbility.INPUT_ENERGY] = energy(PartAbility.INPUT_ENERGY)
			result[PartAbility.OUTPUT_ENERGY] = energy(PartAbility.OUTPUT_ENERGY)
			result[PartAbility.IMPORT_ITEMS] = preview(PartAbility.IMPORT_ITEMS)
			result[PartAbility.EXPORT_ITEMS] = preview(PartAbility.EXPORT_ITEMS)
			result[PartAbility.IMPORT_FLUIDS] = preview(PartAbility.IMPORT_FLUIDS)
			result[PartAbility.EXPORT_FLUIDS] = preview(PartAbility.EXPORT_FLUIDS)
		}
		private fun energy(ability: PartAbility): RestrictedPredicate = RestrictedPredicate.builder()
			.base(AbilityPredicate(ObjectList.of(ability))).minCount(1).maxCount(2).previewCount(1).build()
		private fun preview(ability: PartAbility): RestrictedPredicate = RestrictedPredicate.builder()
			.base(AbilityPredicate(ObjectList.of(ability))).previewCount(1).build()

		@JvmField
		val CODEC: MapCodec<AutoRecipeAbilityPredicate> = RecordCodecBuilder.mapCodec { instance ->
			instance.group(
				Codec.BOOL.optionalFieldOf("checkEnergyIn", true).forGetter(AutoRecipeAbilityPredicate::checkEnergyIn),
				Codec.BOOL.optionalFieldOf("checkEnergyOut", true).forGetter(AutoRecipeAbilityPredicate::checkEnergyOut),
				Codec.BOOL.optionalFieldOf("checkItemIn", true).forGetter(AutoRecipeAbilityPredicate::checkItemIn),
				Codec.BOOL.optionalFieldOf("checkItemOut", true).forGetter(AutoRecipeAbilityPredicate::checkItemOut),
				Codec.BOOL.optionalFieldOf("checkFluidIn", true).forGetter(AutoRecipeAbilityPredicate::checkFluidIn),
				Codec.BOOL.optionalFieldOf("checkFluidOut", true).forGetter(AutoRecipeAbilityPredicate::checkFluidOut),
			).apply(instance, ::AutoRecipeAbilityPredicate)
		}
	}
}
