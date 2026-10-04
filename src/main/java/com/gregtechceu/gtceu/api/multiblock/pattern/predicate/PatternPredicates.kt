package com.gregtechceu.gtceu.api.multiblock.pattern.predicate

import com.gregtechceu.gtceu.api.GTCEuAPI
import com.gregtechceu.gtceu.api.block.ICoilType
import com.gregtechceu.gtceu.api.block.MetaMachineBlock
import com.gregtechceu.gtceu.api.capability.recipe.EURecipeCapability
import com.gregtechceu.gtceu.api.capability.recipe.FluidRecipeCapability
import com.gregtechceu.gtceu.api.capability.recipe.ItemRecipeCapability
import com.gregtechceu.gtceu.api.data.chemical.material.Material
import com.gregtechceu.gtceu.api.data.tag.TagPrefix
import com.gregtechceu.gtceu.api.machine.MachineDefinition
import com.gregtechceu.gtceu.api.machine.multiblock.IBatteryData
import com.gregtechceu.gtceu.api.machine.multiblock.PartAbility
import com.gregtechceu.gtceu.api.multiblock.MultiblockBlockInfo
import com.gregtechceu.gtceu.api.multiblock.MultiblockState
import com.gregtechceu.gtceu.api.multiblock.error.PatternStringError
import com.gregtechceu.gtceu.api.pipenet.IPipeNode
import com.gregtechceu.gtceu.api.recipe.GTRecipeType
import com.gregtechceu.gtceu.common.block.BatteryBlock
import com.gregtechceu.gtceu.common.block.CoilBlock
import com.gregtechceu.gtceu.common.block.LampBlock
import com.gregtechceu.gtceu.common.data.GTMaterialBlocks
import com.gregtechceu.gtceu.common.machine.multiblock.electric.PowerSubstationMachine
import com.gregtechceu.gtceu.config.ConfigHolder
import com.tterrag.registrate.util.entry.BlockEntry
import com.tterrag.registrate.util.entry.RegistryEntry
import it.unimi.dsi.fastutil.objects.ObjectArrayList
import net.minecraft.network.chat.Component
import net.minecraft.tags.TagKey
import net.minecraft.world.item.DyeColor
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.material.Fluid
import java.util.EnumMap
import java.util.function.Predicate
import java.util.function.Supplier
import com.gregtechceu.gtceu.api.block.property.GTBlockStateProperties.ACTIVE
import com.gregtechceu.gtceu.common.data.GTBlocks.BORDERLESS_LAMPS
import com.gregtechceu.gtceu.common.data.GTBlocks.LAMPS
import com.gregtechceu.gtceu.common.machine.multiblock.electric.PowerSubstationMachine.PMC_BATTERY_HEADER

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
    fun blocks(vararg blocks: MetaMachineBlock): PatternPredicate =
        PatternPredicate(PredicateBlocks(*blocks.map { it as Block }.toTypedArray()))

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
    fun custom(
        predicate: Predicate<MultiblockState>,
        candidates: Supplier<Array<MultiblockBlockInfo>>,
    ): PatternPredicate = fromBlockInfos(predicate, candidates)

    @JvmStatic
    fun any(): PatternPredicate = PatternPredicate(PredicateRule.ANY)

    @JvmStatic
    fun air(): PatternPredicate = PatternPredicate(PredicateRule.AIR)

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
    fun lampsByColor(color: DyeColor): PatternPredicate =
        lampsByColor.computeIfAbsent(color) { lamps(LAMPS[it]!!, BORDERLESS_LAMPS[it]!!) }

    @JvmStatic
    fun abilities(vararg abilities: PartAbility): PatternPredicate {
        val blocks = ObjectArrayList<Block>()
        abilities.forEach { blocks.addAll(it.getAllBlocks()) }
        return blocks(*blocks.toTypedArray())
    }

    @JvmStatic
    fun ability(ability: PartAbility, vararg tiers: Int): PatternPredicate {
        val blocks = if (tiers.isEmpty()) ability.getAllBlocks() else ability.getBlocks(*tiers)
        return blocks(*blocks.toTypedArray())
    }

    @JvmStatic
    fun autoAbilities(vararg recipeType: GTRecipeType): PatternPredicate =
        autoAbilities(recipeType, true, true, true, true, true, true)

    @JvmStatic
    fun autoAbilities(
        recipeType: Array<out GTRecipeType>,
        checkEnergyIn: Boolean,
        checkEnergyOut: Boolean,
        checkItemIn: Boolean,
        checkItemOut: Boolean,
        checkFluidIn: Boolean,
        checkFluidOut: Boolean,
    ): PatternPredicate {
        var predicate = PatternPredicate()
        if (checkEnergyIn && recipeType.any { it.getMaxInputs(EURecipeCapability.CAP) > 0 }) {
            predicate = predicate.or(abilities(PartAbility.INPUT_ENERGY).setMinGlobalLimited(1)
                .setMaxGlobalLimited(2).setPreviewCount(1))
        }
        if (checkEnergyOut && recipeType.any { it.getMaxOutputs(EURecipeCapability.CAP) > 0 }) {
            predicate = predicate.or(abilities(PartAbility.OUTPUT_ENERGY).setMinGlobalLimited(1)
                .setMaxGlobalLimited(2).setPreviewCount(1))
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
    fun autoAbilities(
        checkMaintenance: Boolean,
        checkMuffler: Boolean,
        checkParallel: Boolean,
    ): PatternPredicate {
        var predicate = PatternPredicate()
        if (checkMaintenance) {
            predicate = predicate.or(abilities(PartAbility.MAINTENANCE)
                .setMinGlobalLimited(if (ConfigHolder.INSTANCE.machines.enableMaintenance) 1 else 0)
                .setMaxGlobalLimited(1))
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
    fun heatingCoils(): PatternPredicate = fromBlockInfos(
        Predicate { state ->
            GTCEuAPI.HEATING_COILS.entries.any { entry ->
                if (!state.blockState.`is`(entry.value.get())) return@any false
                val stats = entry.key
                val current = state.facts.getOrPut("CoilType", stats)
                if (current != stats) {
                    state.setError(PatternStringError("gtpm.multiblock.pattern.error.coils"))
                    return@any false
                }
                true
            }
        },
        Supplier {
            GTCEuAPI.HEATING_COILS.entries.sortedBy { it.key.tier }
                .map { MultiblockBlockInfo.fromBlockState(it.value.get().defaultBlockState()) }
                .toTypedArray()
        },
    ).addTooltips(Component.translatable("gtpm.multiblock.pattern.error.coils"))

    @JvmStatic
    fun cleanroomFilters(): PatternPredicate = fromBlockInfos(
        Predicate { state ->
            GTCEuAPI.CLEANROOM_FILTERS.entries.any { entry ->
                if (!state.blockState.`is`(entry.value.get())) return@any false
                val stats = entry.key
                val current = state.facts.getOrPut("FilterType", stats)
                if (current != stats) {
                    state.setError(PatternStringError("gtpm.multiblock.pattern.error.filters"))
                    return@any false
                }
                true
            }
        },
        Supplier {
            GTCEuAPI.CLEANROOM_FILTERS.values.map { MultiblockBlockInfo.fromBlockState(it.get().defaultBlockState()) }
                .toTypedArray()
        },
    ).addTooltips(Component.translatable("gtpm.multiblock.pattern.error.filters"))

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
    fun dataHatchPredicate(definition: PatternPredicate): PatternPredicate =
        if (ConfigHolder.INSTANCE.machines.enableResearch) {
            abilities(PartAbility.DATA_ACCESS, PartAbility.OPTICAL_DATA_RECEPTION).setExactLimit(1).or(definition)
        } else {
            definition
        }

    @JvmStatic
    fun frames(vararg frameMaterials: Material): PatternPredicate {
        val frameBlocks = ObjectArrayList<Block>()
        frameMaterials.forEach { material ->
            val entry = GTMaterialBlocks.MATERIAL_BLOCKS.get(TagPrefix.frameGt, material)
            if (entry != null && entry.isBound) frameBlocks.add(entry.get())
        }
        return blocks(*frameBlocks.toTypedArray()).or(fromBlockInfos(
            Predicate { state ->
                val blockEntity = state.blockEntity
                if (blockEntity !is IPipeNode<*, *>) return@Predicate false
                frameMaterials.contains(blockEntity.frameMaterial)
            },
            Supplier {
                frameMaterials.mapNotNull { material ->
                    val entry = GTMaterialBlocks.MATERIAL_BLOCKS.get(TagPrefix.frameGt, material)
                    if (entry != null && entry.isBound) MultiblockBlockInfo.fromBlock(entry.get()) else null
                }.toTypedArray()
            },
        ))
    }

    private fun fromBlockInfos(
        predicate: Predicate<MultiblockState>,
        candidates: Supplier<Array<MultiblockBlockInfo>>,
    ): PatternPredicate = PatternPredicate(
        predicate,
        Supplier {
            val infos = candidates.get()
            if (infos.isEmpty()) MultiblockBlockInfo.EMPTY else infos[0]
        },
        Supplier { candidates.get().map { it.blockState.block }.toTypedArray() },
    )
}
