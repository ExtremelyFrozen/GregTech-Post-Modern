package com.gregtechceu.gtceu.api.multiblock.structurepredicate;

import com.gregtechceu.gtceu.api.machine.MachineDefinition;
import com.gregtechceu.gtceu.api.machine.MultiblockMachineDefinition;
import com.gregtechceu.gtceu.api.machine.multiblock.PartAbility;
import com.gregtechceu.gtceu.api.multiblock.MultiblockBlockInfo;
import com.gregtechceu.gtceu.api.multiblock.MultiblockState;
import com.gregtechceu.gtceu.common.data.GTBlocks;
import com.gregtechceu.gtceu.common.data.GTMachines;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.common.util.Lazy;

import com.mojang.serialization.MapCodec;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectImmutableList;
import it.unimi.dsi.fastutil.objects.ObjectList;
import org.jetbrains.annotations.Unmodifiable;

/**
 * Matches all blocks accepted by the central monitor body predicate.
 */
public enum CentralMonitorComponentPredicate implements StructurePredicate {

    INSTANCE;

    public static final MapCodec<CentralMonitorComponentPredicate> CODEC = MapCodec.unit(INSTANCE);
    private static final Lazy<ObjectList<StructurePredicate>> PREDICATES = Lazy
            .of(CentralMonitorComponentPredicate::createPatternPredicates);

    @Override
    public StructurePredicateType<?> type() {
        return StructurePredicateType.CENTRAL_MONITOR_COMPONENTS;
    }

    @Override
    public boolean test(MultiblockState multiblockState, boolean mutateCount) {
        boolean matched = false;
        for (StructurePredicate predicate : predicates()) {
            matched |= predicate.test(multiblockState, mutateCount);
        }
        return matched;
    }

    @Override
    public @Unmodifiable ObjectList<MultiblockBlockInfo> candidates() {
        return predicates().stream().flatMap(predicate -> predicate.candidates().stream())
                .collect(ObjectImmutableList.toList());
    }

    @Override
    public @Unmodifiable ObjectList<Block> blockCandidates() {
        return predicates().stream().flatMap(predicate -> predicate.blockCandidates().stream())
                .collect(ObjectImmutableList.toList());
    }

    @Override
    public @Unmodifiable ObjectList<StructurePreviewChoice> previewChoices(MultiblockMachineDefinition definition) {
        return predicates().stream().flatMap(predicate -> predicate.previewChoices(definition).stream())
                .collect(ObjectImmutableList.toList());
    }

    private static ObjectList<StructurePredicate> predicates() {
        return PREDICATES.get();
    }

    private static ObjectList<StructurePredicate> createPatternPredicates() {
        ObjectList<StructurePredicate> predicates = new ObjectArrayList<>();
        predicates.add(RestrictedPredicate.builder()
                .base(new AbilityPredicate(ObjectList.of(PartAbility.INPUT_ENERGY)))
                .minCount(1)
                .maxCount(2)
                .previewCount(1)
                .build());
        predicates.add(RestrictedPredicate.builder()
                .base(new ConcatenatedPredicate(ObjectList.of(
                        new AbilityPredicate(ObjectList.of(PartAbility.DATA_ACCESS)),
                        new BlockPredicate(CentralMonitorComponentPredicate::batteryBuffer4Blocks),
                        new BlockPredicate(CentralMonitorComponentPredicate::batteryBuffer16Blocks))))
                .maxCount(4)
                .previewCount(1)
                .build());
        predicates.add(new BlockPredicate(CentralMonitorComponentPredicate::machineHullBlocks));
        predicates.add(new BlockPredicate(CentralMonitorComponentPredicate::monitorBlocks));
        return predicates;
    }

    private static ObjectList<ResourceLocation> batteryBuffer4Blocks() {
        return machineBlocks(GTMachines.BATTERY_BUFFER_4);
    }

    private static ObjectList<ResourceLocation> batteryBuffer16Blocks() {
        return machineBlocks(GTMachines.BATTERY_BUFFER_16);
    }

    private static ObjectList<ResourceLocation> machineHullBlocks() {
        return machineBlocks(GTMachines.HULL);
    }

    private static ObjectList<ResourceLocation> monitorBlocks() {
        ObjectList<ResourceLocation> blocks = new ObjectArrayList<>();
        blocks.add(GTMachines.MONITOR.getId());
        blocks.add(GTMachines.ADVANCED_MONITOR.getId());
        blocks.add(GTBlocks.CASING_ALUMINIUM_FROSTPROOF.getId());
        return blocks;
    }

    private static ObjectList<ResourceLocation> machineBlocks(MachineDefinition[] definitions) {
        ObjectList<ResourceLocation> blocks = new ObjectArrayList<>();
        for (var definition : definitions) {
            if (definition != null) {
                blocks.add(definition.getId());
            }
        }
        return blocks;
    }
}
