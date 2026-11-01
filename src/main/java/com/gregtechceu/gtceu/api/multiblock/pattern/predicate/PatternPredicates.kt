package com.gregtechceu.gtceu.api.multiblock.pattern.predicate

import com.gregtechceu.gtceu.api.GTCEuAPI
import com.gregtechceu.gtceu.api.block.MetaMachineBlock
import com.gregtechceu.gtceu.api.block.property.GTBlockStateProperties.ACTIVE
import com.gregtechceu.gtceu.api.capability.recipe.EURecipeCapability
import com.gregtechceu.gtceu.api.capability.recipe.FluidRecipeCapability
import com.gregtechceu.gtceu.api.capability.recipe.ItemRecipeCapability
import com.gregtechceu.gtceu.api.data.chemical.material.Material
import com.gregtechceu.gtceu.api.machine.MachineDefinition
import com.gregtechceu.gtceu.api.machine.multiblock.PartAbility
import com.gregtechceu.gtceu.api.multiblock.MultiblockBlockInfo
import com.gregtechceu.gtceu.api.multiblock.MultiblockState
import com.gregtechceu.gtceu.api.multiblock.pattern.model.PatternPredicateDefinition
import com.gregtechceu.gtceu.api.recipe.GTRecipeType
import com.gregtechceu.gtceu.common.block.LampBlock
import com.gregtechceu.gtceu.common.data.GTBlocks.BORDERLESS_LAMPS
import com.gregtechceu.gtceu.common.data.GTBlocks.LAMPS
import com.gregtechceu.gtceu.common.machine.multiblock.electric.PowerSubstationMachine
import com.gregtechceu.gtceu.common.machine.multiblock.electric.PowerSubstationMachine.PMC_BATTERY_HEADER
import com.gregtechceu.gtceu.config.ConfigHolder

import net.minecraft.network.chat.Component
import net.minecraft.tags.TagKey
import net.minecraft.world.item.DyeColor
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.material.Fluid

import com.tterrag.registrate.util.entry.BlockEntry
import it.unimi.dsi.fastutil.objects.Object2ObjectMaps
import it.unimi.dsi.fastutil.objects.ObjectArrayList
import it.unimi.dsi.fastutil.objects.ObjectLists

import java.util.EnumMap
import java.util.function.Predicate
import java.util.function.Supplier

/** Factory methods for the standard multiblock predicates. */
object PatternPredicates {
	@JvmStatic
	fun controller(definition: MachineDefinition): PatternPredicate = PredicateController(definition)

	@JvmStatic
	fun controller(predicate: PatternPredicate): PatternPredicate = PredicateController(predicate)

	@JvmStatic
	fun states(vararg allowedStates: BlockState): PatternPredicate {
		val candidates = ObjectArrayList<BlockState>()
		allowedStates.forEach { state ->
			candidates.add(state)
			if (state.hasProperty(ACTIVE)) {
				candidates.add(state.setValue(ACTIVE, !state.getValue(ACTIVE)))
			}
		}
		return PatternPredicate(PredicateStates(*candidates.toTypedArray()))
	}

	@JvmStatic
	fun blocks(vararg blocks: Block): PatternPredicate = PatternPredicate(PredicateBlocks(*blocks))

	@JvmStatic
	fun blocks(vararg blocks: MetaMachineBlock): PatternPredicate = PatternPredicate(PredicateBlocks(*blocks.map { it as Block }.toTypedArray()))

	@JvmStatic
	fun machines(vararg definitions: MachineDefinition): PatternPredicate {
		val machineBlocks = ObjectArrayList<MetaMachineBlock>(definitions.size)
		definitions.forEach { machineBlocks.add(it.get()) }
		return blocks(*machineBlocks.toTypedArray())
	}

	@JvmStatic
	fun blockTag(tag: TagKey<Block>): PatternPredicate = PatternPredicate(PredicateBlockTag(tag))

	@JvmStatic
	fun fluids(vararg fluids: Fluid): PatternPredicate = PatternPredicate(PredicateFluids(*fluids))

	@JvmStatic
	fun fluidTag(tag: TagKey<Fluid>): PatternPredicate = PatternPredicate(PredicateFluidTag(tag))

	@JvmStatic
	fun custom(predicate: Predicate<MultiblockState>, candidates: Supplier<Array<MultiblockBlockInfo>>): PatternPredicate = fromBlockInfos(predicate, candidates)

	@JvmStatic
	fun any(): PatternPredicate = PatternPredicate(PredicateRule.any)

	@JvmStatic
	fun air(): PatternPredicate = PatternPredicate(PredicateRule.air)

	@JvmStatic
	fun lamps(vararg lampEntries: BlockEntry<LampBlock>): PatternPredicate = fromBlockInfos(
		Predicate { state -> lampEntries.any { state.blockState.`is`(it.get()) } },
		Supplier {
			lampEntries.map { MultiblockBlockInfo(it.get().defaultBlockState(), null) }.toTypedArray()
		},
	)

	@JvmStatic
	fun anyLamp(): PatternPredicate {
		val all = ObjectArrayList<BlockEntry<LampBlock>>()
		all.addAll(LAMPS.values)
		all.addAll(BORDERLESS_LAMPS.values)
		return lamps(*all.toTypedArray())
	}

	private val lampsByColor = EnumMap<DyeColor, PatternPredicate>(DyeColor::class.java)

	@JvmStatic
	fun lampsByColor(color: DyeColor): PatternPredicate = lampsByColor.computeIfAbsent(color) { lamps(LAMPS[it]!!, BORDERLESS_LAMPS[it]!!) }

	@JvmStatic
	fun abilities(vararg abilities: PartAbility): PatternPredicate {
		val names = ObjectArrayList<String>(abilities.size)
		abilities.forEach { names.add(it.name) }
		return PatternPredicateCompiler.compile(PatternPredicateDefinition("part", "", mapOf("abilities" to names), emptyList()))
	}

	@JvmStatic
	fun ability(ability: PartAbility, vararg tiers: Int): PatternPredicate {
		if (tiers.isEmpty()) return abilities(ability)
		return PatternPredicateCompiler.compile(PatternPredicateDefinition("part", "", mapOf("abilities" to ObjectArrayList<String>().also { it.add(ability.name) }, "tiers" to mapOf("min" to tiers.min(), "max" to tiers.max()), "allowedTiers" to tiers), emptyList()))
	}

	@JvmStatic
	fun autoAbilities(vararg recipeType: GTRecipeType): PatternPredicate = autoAbilities(recipeType, true, true, true, true, true, true)

	@JvmStatic
	fun autoAbilities(recipeType: Array<out GTRecipeType>, checkEnergyIn: Boolean, checkEnergyOut: Boolean, checkItemIn: Boolean, checkItemOut: Boolean, checkFluidIn: Boolean, checkFluidOut: Boolean): PatternPredicate {
		var predicate = PatternPredicate()
		if (checkEnergyIn && recipeType.any { it.getMaxInputs(EURecipeCapability.CAP) > 0 }) {
			predicate = predicate.or(
				abilities(PartAbility.INPUT_ENERGY).setMinGlobalLimited(1)
					.setMaxGlobalLimited(2).setPreviewCount(1),
			)
		}
		if (checkEnergyOut && recipeType.any { it.getMaxOutputs(EURecipeCapability.CAP) > 0 }) {
			predicate = predicate.or(
				abilities(PartAbility.OUTPUT_ENERGY).setMinGlobalLimited(1)
					.setMaxGlobalLimited(2).setPreviewCount(1),
			)
		}
		if (checkItemIn && recipeType.any { it.getMaxInputs(ItemRecipeCapability.CAP) > 0 }) {
			predicate = predicate.or(abilities(PartAbility.IMPORT_ITEMS).setPreviewCount(1))
		}
		if (checkItemOut && recipeType.any { it.getMaxOutputs(ItemRecipeCapability.CAP) > 0 }) {
			predicate = predicate.or(abilities(PartAbility.EXPORT_ITEMS).setPreviewCount(1))
		}
		if (checkFluidIn && recipeType.any { it.getMaxInputs(FluidRecipeCapability.CAP) > 0 }) {
			predicate = predicate.or(abilities(PartAbility.IMPORT_FLUIDS).setPreviewCount(1))
		}
		if (checkFluidOut && recipeType.any { it.getMaxOutputs(FluidRecipeCapability.CAP) > 0 }) {
			predicate = predicate.or(abilities(PartAbility.EXPORT_FLUIDS).setPreviewCount(1))
		}
		return predicate
	}

	@JvmStatic
	fun autoAbilities(checkMaintenance: Boolean, checkMuffler: Boolean, checkParallel: Boolean): PatternPredicate {
		var predicate = PatternPredicate()
		if (checkMaintenance) {
			predicate = predicate.or(
				abilities(PartAbility.MAINTENANCE)
					.setMinGlobalLimited(if (ConfigHolder.INSTANCE.machines.enableMaintenance) 1 else 0)
					.setMaxGlobalLimited(1),
			)
		}
		if (checkMuffler) {
			predicate = predicate.or(abilities(PartAbility.MUFFLER).setMinGlobalLimited(1).setMaxGlobalLimited(1))
		}
		if (checkParallel) {
			predicate = predicate.or(abilities(PartAbility.PARALLEL_HATCH).setMaxGlobalLimited(1).setPreviewCount(1))
		}
		return predicate
	}

	@JvmStatic
	fun heatingCoils(): PatternPredicate = PatternPredicateCompiler.compile(PatternPredicateDefinition("coil", "", Object2ObjectMaps.emptyMap(), ObjectLists.emptyList())).addTooltips(Component.translatable("gtpm.multiblock.pattern.error.coils"))

	@JvmStatic
	fun cleanroomFilters(): PatternPredicate = PatternPredicateCompiler.compile(PatternPredicateDefinition("filter", "", Object2ObjectMaps.emptyMap(), ObjectLists.emptyList())).addTooltips(Component.translatable("gtpm.multiblock.pattern.error.filters"))

	@JvmStatic
	fun powerSubstationBatteries(): PatternPredicate = fromBlockInfos(
		Predicate { state ->
			GTCEuAPI.PSS_BATTERIES.entries.any { entry ->
				if (!state.blockState.`is`(entry.value.get())) return@any false
				val battery = entry.key
				if (battery.tier != -1 && battery.capacity > 0) {
					val key = PMC_BATTERY_HEADER + battery.batteryName
					var wrapper: PowerSubstationMachine.BatteryMatchWrapper? = state.facts.get(key)
					if (wrapper == null) wrapper = PowerSubstationMachine.BatteryMatchWrapper(battery)
					state.facts.set(key, wrapper.increment())
				}
				true
			}
		},
		Supplier {
			GTCEuAPI.PSS_BATTERIES.entries.sortedBy { it.key.tier }
				.map { MultiblockBlockInfo(it.value.get().defaultBlockState(), null) }
				.toTypedArray()
		},
	).addTooltips(Component.translatable("gtpm.multiblock.pattern.error.batteries"))

	@JvmStatic
	fun dataHatchPredicate(definition: PatternPredicate): PatternPredicate = if (ConfigHolder.INSTANCE.machines.enableResearch) {
		abilities(PartAbility.DATA_ACCESS, PartAbility.OPTICAL_DATA_RECEPTION).setExactLimit(1).or(definition)
	} else {
		definition
	}

	@JvmStatic
	fun frames(vararg frameMaterials: Material): PatternPredicate {
		val materials = ObjectArrayList<String>(frameMaterials.size)
		frameMaterials.forEach { materials.add(it.resourceLocation.toString()) }
		return PatternPredicateCompiler.compile(PatternPredicateDefinition("frame", "", Object2ObjectMaps.singleton("materials", materials), ObjectLists.emptyList()))
	}

	private fun fromBlockInfos(predicate: Predicate<MultiblockState>, candidates: Supplier<Array<MultiblockBlockInfo>>): PatternPredicate = PatternPredicate(
		predicate,
		Supplier {
			val infos = candidates.get()
			if (infos.isEmpty()) MultiblockBlockInfo.EMPTY else infos[0]
		},
		Supplier { candidates.get().map { it.blockState.block }.toTypedArray() },
	)
}
