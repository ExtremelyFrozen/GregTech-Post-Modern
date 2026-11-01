package com.gregtechceu.gtceu.api.multiblock.autobuild;

import com.gregtechceu.gtceu.api.GTCEuAPI;
import com.gregtechceu.gtceu.api.block.ICoilType;
import com.gregtechceu.gtceu.api.block.IFilterType;
import com.gregtechceu.gtceu.api.machine.MachineDefinition;
import com.gregtechceu.gtceu.api.machine.multiblock.IBatteryData;
import com.gregtechceu.gtceu.common.data.GTBlocks;
import com.gregtechceu.gtceu.common.data.GTMachines;

import net.minecraft.world.level.block.Block;

import com.tterrag.registrate.util.entry.RegistryEntry;
import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Reference2ObjectMap;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import org.jspecify.annotations.NullMarked;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.function.Supplier;
import java.util.stream.Stream;

/** Tier-ordered groups, populated during common setup and read by previews and automatic construction. */
@NullMarked
public final class AutoBuildBlockMap {

    public static final Object2ObjectMap<String, Block[]> MAP = new Object2ObjectLinkedOpenHashMap<>();
    public static final Reference2ObjectMap<Block, String> BLOCK_CATEGORY_MAP = new Reference2ObjectOpenHashMap<>();

    private AutoBuildBlockMap() {}

    public static void initialize() {
        registerCategory("heating_coils", sortedBlocks(GTCEuAPI.HEATING_COILS.entrySet().stream(),
                Comparator.comparingInt(ICoilType::getTier)));
        registerCategory("cleanroom_filters", sortedBlocks(GTCEuAPI.CLEANROOM_FILTERS.entrySet().stream(),
                Comparator.comparing(IFilterType::getSerializedName)));
        registerCategory("power_substation_batteries", sortedBlocks(GTCEuAPI.PSS_BATTERIES.entrySet().stream(),
                Comparator.comparingInt(IBatteryData::getTier)));
        registerCategory("lamps", GTBlocks.LAMPS.values().stream().map(RegistryEntry::get).toArray(Block[]::new));
        registerCategory("borderless_lamps",
                GTBlocks.BORDERLESS_LAMPS.values().stream().map(RegistryEntry::get).toArray(Block[]::new));
        registerCategory("muffler_hatches", sortedMachineBlocks(GTMachines.MUFFLER_HATCH));
        registerCategory("rotor_holder", sortedMachineBlocks(GTMachines.ROTOR_HOLDER));
    }

    /** Register each group during setup; block arrays are ordered by tier and are not mutated afterwards. */
    public static void registerCategory(String category, Block[] blocks) {
        MAP.put(category, blocks);
        for (Block block : blocks) BLOCK_CATEGORY_MAP.put(block, category);
    }

    private static <K, V extends Supplier<? extends Block>> Block[] sortedBlocks(Stream<Entry<K, V>> entries,
                                                                                 Comparator<? super K> order) {
        return entries.sorted((first, second) -> order.compare(first.getKey(), second.getKey()))
                .map(entry -> entry.getValue().get()).toArray(Block[]::new);
    }

    private static Block[] sortedMachineBlocks(MachineDefinition[] definitions) {
        return Arrays.stream(definitions).filter(Objects::nonNull)
                .sorted(Comparator.comparingInt(MachineDefinition::getTier))
                .map(MachineDefinition::getBlock).toArray(Block[]::new);
    }
}
