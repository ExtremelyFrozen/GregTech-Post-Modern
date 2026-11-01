package com.gregtechceu.gtceu.api.multiblock.structurepredicate;

import com.gregtechceu.gtceu.api.GTCEuAPI;
import com.gregtechceu.gtceu.api.machine.MultiblockMachineDefinition;
import com.gregtechceu.gtceu.api.machine.multiblock.IBatteryData;
import com.gregtechceu.gtceu.api.multiblock.MultiblockBlockInfo;
import com.gregtechceu.gtceu.api.multiblock.MultiblockState;
import com.gregtechceu.gtceu.common.machine.multiblock.electric.PowerSubstationMachine;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import com.mojang.serialization.MapCodec;
import it.unimi.dsi.fastutil.objects.ObjectImmutableList;
import it.unimi.dsi.fastutil.objects.ObjectList;
import org.jetbrains.annotations.Unmodifiable;

import java.util.Comparator;

import static com.gregtechceu.gtceu.common.machine.multiblock.electric.PowerSubstationMachine.PMC_BATTERY_HEADER;

/**
 * Serialized power substation battery predicate for JSON multiblock patterns.
 *
 * <p>
 * This predicate mirrors {@code PatternPredicates.powerSubstationBatteries()} by matching registered PSS battery blocks
 * and
 * recording matched non-empty battery data in the pattern match context.
 */
public enum PowerSubstationBatteryPredicate implements StructurePredicate {

    INSTANCE;

    /**
     * JSON codec for {@code gtpm:power_substation_batteries}; no extra fields are required.
     */
    public static final MapCodec<PowerSubstationBatteryPredicate> CODEC = MapCodec.unit(INSTANCE);

    @Override
    public StructurePredicateType<?> type() {
        return StructurePredicateType.POWER_SUBSTATION_BATTERIES;
    }

    @Override
    public boolean test(MultiblockState multiblockState, boolean mutateCount) {
        BlockState state = multiblockState.getBlockState();
        for (var entry : GTCEuAPI.PSS_BATTERIES.entrySet()) {
            if (state.is(entry.getValue().get())) {
                IBatteryData battery = entry.getKey();
                if (mutateCount && battery.getTier() != -1 && battery.getCapacity() > 0) {
                    String key = PMC_BATTERY_HEADER + battery.getBatteryName();
                    PowerSubstationMachine.BatteryMatchWrapper wrapper = multiblockState.getFacts().get(key);
                    if (wrapper == null) {
                        wrapper = new PowerSubstationMachine.BatteryMatchWrapper(battery);
                    }
                    multiblockState.getFacts().set(key, wrapper.increment());
                }
                return true;
            }
        }
        return false;
    }

    @Override
    public @Unmodifiable ObjectList<MultiblockBlockInfo> candidates() {
        return sortedBatteries().stream()
                .map(battery -> new MultiblockBlockInfo(GTCEuAPI.PSS_BATTERIES.get(battery).get().defaultBlockState(),
                        null))
                .collect(ObjectImmutableList.toList());
    }

    @Override
    public @Unmodifiable ObjectList<Block> blockCandidates() {
        return sortedBatteries().stream()
                .map(battery -> (Block) GTCEuAPI.PSS_BATTERIES.get(battery).get())
                .collect(ObjectImmutableList.toList());
    }

    @Override
    public @Unmodifiable ObjectList<StructurePreviewChoice> previewChoices(MultiblockMachineDefinition definition) {
        ObjectList<MultiblockBlockInfo> batteries = sortedBatteries().stream()
                .filter(battery -> battery.getTier() >= 0 && battery.getCapacity() > 0)
                .map(battery -> new MultiblockBlockInfo(GTCEuAPI.PSS_BATTERIES.get(battery).get().defaultBlockState(),
                        null))
                .collect(ObjectImmutableList.toList());
        return ObjectList.of(StructurePreviewChoice.unrestricted(batteries));
    }

    private ObjectList<IBatteryData> sortedBatteries() {
        return GTCEuAPI.PSS_BATTERIES.keySet().stream()
                .sorted(Comparator.comparingInt(IBatteryData::getTier))
                .collect(ObjectImmutableList.toList());
    }
}
