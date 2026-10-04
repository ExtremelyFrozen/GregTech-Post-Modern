package com.gregtechceu.gtceu.api.multiblock.pattern.predicate;

import com.gregtechceu.gtceu.api.GTCEuAPI;
import com.gregtechceu.gtceu.api.block.ICoilType;
import com.gregtechceu.gtceu.api.block.MetaMachineBlock;
import com.gregtechceu.gtceu.api.capability.recipe.EURecipeCapability;
import com.gregtechceu.gtceu.api.capability.recipe.FluidRecipeCapability;
import com.gregtechceu.gtceu.api.capability.recipe.ItemRecipeCapability;
import com.gregtechceu.gtceu.api.data.chemical.material.Material;
import com.gregtechceu.gtceu.api.data.tag.TagPrefix;
import com.gregtechceu.gtceu.api.machine.MachineDefinition;
import com.gregtechceu.gtceu.api.machine.multiblock.IBatteryData;
import com.gregtechceu.gtceu.api.machine.multiblock.PartAbility;
import com.gregtechceu.gtceu.api.multiblock.MultiblockBlockInfo;
import com.gregtechceu.gtceu.api.multiblock.MultiblockState;
import com.gregtechceu.gtceu.api.multiblock.error.PatternStringError;
import com.gregtechceu.gtceu.api.pipenet.IPipeNode;
import com.gregtechceu.gtceu.api.recipe.GTRecipeType;
import com.gregtechceu.gtceu.common.block.BatteryBlock;
import com.gregtechceu.gtceu.common.block.CoilBlock;
import com.gregtechceu.gtceu.common.block.LampBlock;
import com.gregtechceu.gtceu.common.data.GTMaterialBlocks;
import com.gregtechceu.gtceu.common.machine.multiblock.electric.PowerSubstationMachine;
import com.gregtechceu.gtceu.config.ConfigHolder;

import net.minecraft.network.chat.Component;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;

import com.tterrag.registrate.util.entry.BlockEntry;
import com.tterrag.registrate.util.entry.RegistryEntry;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.apache.commons.lang3.ArrayUtils;

import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.function.Supplier;

import static com.gregtechceu.gtceu.api.block.property.GTBlockStateProperties.ACTIVE;
import static com.gregtechceu.gtceu.common.data.GTBlocks.BORDERLESS_LAMPS;
import static com.gregtechceu.gtceu.common.data.GTBlocks.LAMPS;
import static com.gregtechceu.gtceu.common.machine.multiblock.electric.PowerSubstationMachine.PMC_BATTERY_HEADER;

public class PatternPredicates {

    public static PatternPredicate controller(MachineDefinition definition) {
        return new PredicateController(definition);
    }

    public static PatternPredicate controller(PatternPredicate predicate) {
        return new PredicateController(predicate);
    }

    public static PatternPredicate states(BlockState... allowedStates) {
        var candidates = new ObjectArrayList<BlockState>();
        for (BlockState state : allowedStates) {
            candidates.add(state);
            if (state.hasProperty(ACTIVE)) {
                candidates.add(state.setValue(ACTIVE, !state.getValue(ACTIVE)));
            }
        }
        return new PatternPredicate(new PredicateStates(candidates.toArray(BlockState[]::new)));
    }

    public static PatternPredicate blocks(Block... blocks) {
        return new PatternPredicate(new PredicateBlocks(blocks));
    }

    public static PatternPredicate blocks(MetaMachineBlock... blocks) {
        return new PatternPredicate(
                new PredicateBlocks(Arrays.stream(blocks).toArray(Block[]::new)));
    }

    public static PatternPredicate machines(MachineDefinition... definitions) {
        var machineBlocks = new ObjectArrayList<MetaMachineBlock>(definitions.length);
        for (var definition : definitions) {
            if (definition != null) {
                machineBlocks.add(definition.get());
            }
        }
        return blocks(machineBlocks.toArray(MetaMachineBlock[]::new));
    }

    public static PatternPredicate blockTag(TagKey<Block> tag) {
        return new PatternPredicate(new PredicateBlockTag(tag));
    }

    public static PatternPredicate fluids(Fluid... fluids) {
        return new PatternPredicate(new PredicateFluids(fluids));
    }

    public static PatternPredicate fluidTag(TagKey<Fluid> tag) {
        return new PatternPredicate(new PredicateFluidTag(tag));
    }

    public static PatternPredicate custom(Predicate<MultiblockState> predicate,
                                          Supplier<MultiblockBlockInfo[]> candidates) {
        return fromBlockInfos(predicate, candidates);
    }

    public static PatternPredicate any() {
        return new PatternPredicate(PredicateRule.ANY);
    }

    public static PatternPredicate air() {
        return new PatternPredicate(PredicateRule.AIR);
    }

    @SafeVarargs
    public static PatternPredicate lamps(BlockEntry<LampBlock>... lampEntries) {
        return fromBlockInfos(blockWorldState -> {
            BlockState state = blockWorldState.getBlockState();
            for (BlockEntry<LampBlock> entry : lampEntries) {
                if (state.is(entry.get())) return true;
            }
            return false;
        }, () -> Arrays.stream(lampEntries)
                .map(entry -> new MultiblockBlockInfo(entry.get().defaultBlockState(), null))
                .toArray(MultiblockBlockInfo[]::new));
    }

    public static PatternPredicate anyLamp() {
        List<BlockEntry<LampBlock>> all = new ObjectArrayList<>();
        all.addAll(LAMPS.values());
        all.addAll(BORDERLESS_LAMPS.values());
        return lamps(all.toArray(BlockEntry[]::new));
    }

    private static final Map<DyeColor, PatternPredicate> LAMPS_BY_COLOR = new EnumMap<>(DyeColor.class);

    public static PatternPredicate lampsByColor(DyeColor color) {
        return LAMPS_BY_COLOR.computeIfAbsent(color, c -> lamps(LAMPS.get(c), BORDERLESS_LAMPS.get(c)));
    }

    public static PatternPredicate abilities(PartAbility... abilities) {
        return blocks(Arrays.stream(abilities).map(PartAbility::getAllBlocks).flatMap(Collection::stream)
                .toArray(Block[]::new));
    }

    public static PatternPredicate ability(PartAbility ability, int... tiers) {
        return blocks((tiers.length == 0 ? ability.getAllBlocks() : ability.getBlocks(tiers)).toArray(Block[]::new));
    }

    public static PatternPredicate autoAbilities(GTRecipeType... recipeType) {
        return autoAbilities(recipeType, true, true, true, true, true, true);
    }

    public static PatternPredicate autoAbilities(GTRecipeType[] recipeType,
                                                 boolean checkEnergyIn,
                                                 boolean checkEnergyOut,
                                                 boolean checkItemIn,
                                                 boolean checkItemOut,
                                                 boolean checkFluidIn,
                                                 boolean checkFluidOut) {
        PatternPredicate predicate = new PatternPredicate();

        if (checkEnergyIn) {
            for (var type : recipeType) {
                if (type.getMaxInputs(EURecipeCapability.CAP) > 0) {
                    predicate = predicate.or(abilities(PartAbility.INPUT_ENERGY).setMinGlobalLimited(1)
                            .setMaxGlobalLimited(2).setPreviewCount(1));
                    break;
                }
            }
        }
        if (checkEnergyOut) {
            for (var type : recipeType) {
                if (type.getMaxOutputs(EURecipeCapability.CAP) > 0) {
                    predicate = predicate.or(abilities(PartAbility.OUTPUT_ENERGY).setMinGlobalLimited(1)
                            .setMaxGlobalLimited(2).setPreviewCount(1));
                    break;
                }
            }
        }
        if (checkItemIn) {
            for (var type : recipeType) {
                if (type.getMaxInputs(ItemRecipeCapability.CAP) > 0) {
                    predicate = predicate.or(abilities(PartAbility.IMPORT_ITEMS).setPreviewCount(1));
                    break;
                }
            }
        }
        if (checkItemOut) {
            for (var type : recipeType) {
                if (type.getMaxOutputs(ItemRecipeCapability.CAP) > 0) {
                    predicate = predicate.or(abilities(PartAbility.EXPORT_ITEMS).setPreviewCount(1));
                    break;
                }
            }
        }
        if (checkFluidIn) {
            for (var type : recipeType) {
                if (type.getMaxInputs(FluidRecipeCapability.CAP) > 0) {
                    predicate = predicate.or(abilities(PartAbility.IMPORT_FLUIDS).setPreviewCount(1));
                    break;
                }
            }
        }
        if (checkFluidOut) {
            for (var type : recipeType) {
                if (type.getMaxOutputs(FluidRecipeCapability.CAP) > 0) {
                    predicate = predicate.or(abilities(PartAbility.EXPORT_FLUIDS).setPreviewCount(1));
                    break;
                }
            }
        }
        return predicate;
    }

    public static PatternPredicate autoAbilities(boolean checkMaintenance, boolean checkMuffler,
                                                 boolean checkParallel) {
        PatternPredicate predicate = new PatternPredicate();
        if (checkMaintenance) {
            predicate = predicate.or(abilities(PartAbility.MAINTENANCE)
                    .setMinGlobalLimited(ConfigHolder.INSTANCE.machines.enableMaintenance ? 1 : 0)
                    .setMaxGlobalLimited(1));
        }
        if (checkMuffler) {
            predicate = predicate.or(abilities(PartAbility.MUFFLER).setMinGlobalLimited(1).setMaxGlobalLimited(1));
        }
        if (checkParallel) {
            predicate = predicate.or(abilities(PartAbility.PARALLEL_HATCH).setMaxGlobalLimited(1).setPreviewCount(1));
        }
        return predicate;
    }

    public static PatternPredicate heatingCoils() {
        return fromBlockInfos(blockWorldState -> {
            var blockState = blockWorldState.getBlockState();
            for (Map.Entry<ICoilType, Supplier<CoilBlock>> entry : GTCEuAPI.HEATING_COILS.entrySet()) {
                if (blockState.is(entry.getValue().get())) {
                    var stats = entry.getKey();
                    Object currentCoil = blockWorldState.getFacts().getOrPut("CoilType", stats);
                    if (!currentCoil.equals(stats)) {
                        blockWorldState.setError(new PatternStringError("gtpm.multiblock.pattern.error.coils"));
                        return false;
                    }
                    return true;
                }
            }
            return false;
        }, () -> GTCEuAPI.HEATING_COILS.entrySet().stream()
                // sort to make autogenerated jei previews not pick random coils each game load
                .sorted(Comparator.comparingInt(value -> value.getKey().getTier()))
                .map(coil -> MultiblockBlockInfo.fromBlockState(coil.getValue().get().defaultBlockState()))
                .toArray(MultiblockBlockInfo[]::new))
                .addTooltips(Component.translatable("gtpm.multiblock.pattern.error.coils"));
    }

    public static PatternPredicate cleanroomFilters() {
        return fromBlockInfos(blockWorldState -> {
            var blockState = blockWorldState.getBlockState();
            for (var entry : GTCEuAPI.CLEANROOM_FILTERS.entrySet()) {
                if (blockState.is(entry.getValue().get())) {
                    var stats = entry.getKey();
                    Object currentCoil = blockWorldState.getFacts().getOrPut("FilterType", stats);
                    if (!currentCoil.equals(stats)) {
                        blockWorldState.setError(new PatternStringError("gtpm.multiblock.pattern.error.filters"));
                        return false;
                    }
                    return true;
                }
            }
            return false;
        }, () -> GTCEuAPI.CLEANROOM_FILTERS.values().stream()
                .map(blockSupplier -> MultiblockBlockInfo.fromBlockState(blockSupplier.get().defaultBlockState()))
                .toArray(MultiblockBlockInfo[]::new))
                .addTooltips(Component.translatable("gtpm.multiblock.pattern.error.filters"));
    }

    public static PatternPredicate powerSubstationBatteries() {
        return fromBlockInfos(blockWorldState -> {
            BlockState state = blockWorldState.getBlockState();
            for (Map.Entry<IBatteryData, Supplier<BatteryBlock>> entry : GTCEuAPI.PSS_BATTERIES.entrySet()) {
                if (state.is(entry.getValue().get())) {
                    IBatteryData battery = entry.getKey();
                    // Allow unfilled batteries in the structure, but do not add them to match context.
                    // This lets you use empty batteries as "filler slots" for convenience if desired.
                    if (battery.getTier() != -1 && battery.getCapacity() > 0) {
                        String key = PMC_BATTERY_HEADER + battery.getBatteryName();
                        PowerSubstationMachine.BatteryMatchWrapper wrapper = blockWorldState.getFacts().get(key);
                        if (wrapper == null) wrapper = new PowerSubstationMachine.BatteryMatchWrapper(battery);
                        blockWorldState.getFacts().set(key, wrapper.increment());
                    }
                    return true;
                }
            }
            return false;
        }, () -> GTCEuAPI.PSS_BATTERIES.entrySet().stream()
                .sorted(Comparator.comparingInt(entry -> entry.getKey().getTier()))
                .map(entry -> new MultiblockBlockInfo(entry.getValue().get().defaultBlockState(), null))
                .toArray(MultiblockBlockInfo[]::new))
                .addTooltips(Component.translatable("gtpm.multiblock.pattern.error.batteries"));
    }

    public static PatternPredicate dataHatchPredicate(PatternPredicate def) {
        // if research is enabled, require the data hatch, otherwise use a grate instead
        if (ConfigHolder.INSTANCE.machines.enableResearch) {
            return abilities(PartAbility.DATA_ACCESS, PartAbility.OPTICAL_DATA_RECEPTION)
                    .setExactLimit(1)
                    .or(def);
        }
        return def;
    }

    /**
     * Use this predicate for Frames in your Multiblock. Allows for Framed Pipes as well as normal Frame blocks.
     */
    public static PatternPredicate frames(Material... frameMaterials) {
        var frameBlocks = Arrays.stream(frameMaterials)
                .map(m -> GTMaterialBlocks.MATERIAL_BLOCKS.get(TagPrefix.frameGt, m))
                .filter(Objects::nonNull)
                .filter(RegistryEntry::isBound)
                .map(RegistryEntry::get)
                .toArray(Block[]::new);
        return blocks(frameBlocks)
                .or(fromBlockInfos(blockWorldState -> {
                    BlockEntity blockEntity = blockWorldState.getBlockEntity();
                    if (!(blockEntity instanceof IPipeNode<?, ?> pipeNode)) {
                        return false;
                    }
                    return ArrayUtils.contains(frameMaterials, pipeNode.getFrameMaterial());
                }, () -> Arrays.stream(frameMaterials)
                        .map(m -> GTMaterialBlocks.MATERIAL_BLOCKS.get(TagPrefix.frameGt, m))
                        .filter(Objects::nonNull)
                        .filter(RegistryEntry::isBound)
                        .map(RegistryEntry::get)
                        .map(MultiblockBlockInfo::fromBlock)
                        .toArray(MultiblockBlockInfo[]::new)));
    }

    private static PatternPredicate fromBlockInfos(Predicate<MultiblockState> predicate,
                                                   Supplier<MultiblockBlockInfo[]> candidates) {
        return new PatternPredicate(predicate, () -> {
            MultiblockBlockInfo[] infos = candidates.get();
            return infos.length == 0 ? MultiblockBlockInfo.EMPTY : infos[0];
        }, () -> Arrays.stream(candidates.get())
                .map(MultiblockBlockInfo::getBlockState)
                .map(BlockState::getBlock)
                .toArray(Block[]::new));
    }
}
