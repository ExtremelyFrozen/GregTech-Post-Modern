package com.gregtechceu.gtceu.api.multiblock.autobuild;

import com.gregtechceu.gtceu.api.GTCEuAPI;
import com.gregtechceu.gtceu.api.block.ICoilType;
import com.gregtechceu.gtceu.api.block.IFilterType;
import com.gregtechceu.gtceu.api.block.MetaMachineBlock;
import com.gregtechceu.gtceu.api.machine.MachineDefinition;
import com.gregtechceu.gtceu.api.machine.multiblock.IBatteryData;
import com.gregtechceu.gtceu.api.machine.multiblock.PartAbility;
import com.gregtechceu.gtceu.common.data.GTBlocks;
import com.gregtechceu.gtceu.common.data.GTMachines;

import net.minecraft.world.level.block.Block;

import com.tterrag.registrate.util.entry.RegistryEntry;
import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Reference2ObjectMap;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.function.Supplier;
import java.util.stream.Stream;

/** Setup-time category arrays preserve tier or declaration order and support identity-based block lookup. */
@NullMarked
public final class BlockCategories {

    public static final String HEATING_COILS = "heating_coils";
    public static final String CLEANROOM_FILTERS = "cleanroom_filters";
    public static final String POWER_SUBSTATION_BATTERIES = "power_substation_batteries";
    public static final String LAMPS = "lamps";
    public static final String BORDERLESS_LAMPS = "borderless_lamps";
    public static final String MUFFLER_HATCHES = "muffler_hatches";
    public static final String ROTOR_HOLDER = "rotor_holder";
    public static final String GLASS = "glass";
    public static final String MACHINE_CASINGS = "machine_casings";
    public static final String HERMETIC_CASINGS = "hermetic_casings";
    public static final String MACHINE_HULLS = "machine_hulls";
    public static final String COMPUTER_CASINGS = "computer_casings";
    public static final String COMPUTER_HEAT_VENTS = "computer_heat_vents";

    public static final Object2ObjectMap<String, Block[]> MAP = new Object2ObjectLinkedOpenHashMap<>();
    public static final Reference2ObjectMap<Block, String> BLOCK_CATEGORY_MAP = new Reference2ObjectOpenHashMap<>();

    private BlockCategories() {}

    public static void initialize() {
        registerCategory(HEATING_COILS, sortedBlocks(GTCEuAPI.HEATING_COILS.entrySet().stream(),
                Comparator.comparingInt(ICoilType::getTier)));
        registerCategory(CLEANROOM_FILTERS, sortedBlocks(GTCEuAPI.CLEANROOM_FILTERS.entrySet().stream(),
                Comparator.comparing(IFilterType::getSerializedName)));
        registerCategory(POWER_SUBSTATION_BATTERIES, sortedBlocks(GTCEuAPI.PSS_BATTERIES.entrySet().stream(),
                Comparator.comparingInt(IBatteryData::getTier)));
        registerCategory(LAMPS, GTBlocks.LAMPS.values().stream().map(RegistryEntry::get).toArray(Block[]::new));
        registerCategory(BORDERLESS_LAMPS,
                GTBlocks.BORDERLESS_LAMPS.values().stream().map(RegistryEntry::get).toArray(Block[]::new));
        registerCategory(MUFFLER_HATCHES, sortedMachineBlocks(GTMachines.MUFFLER_HATCH));
        registerCategory(ROTOR_HOLDER, PartAbility.ROTOR_HOLDER.getAllBlocks().stream()
                .filter(block -> block instanceof MetaMachineBlock)
                .sorted(Comparator.comparingInt(block -> ((MetaMachineBlock) block).getDefinition().getTier()))
                .toArray(Block[]::new));
        registerCategory(MACHINE_HULLS, sortedMachineBlocks(GTMachines.HULL));
        registerCategory(GLASS, GTBlocks.GLASS.toArray(Block[]::new));
        registerCategory(MACHINE_CASINGS, GTBlocks.MACHINE_CASINGS.values().toArray(Block[]::new));
        registerCategory(HERMETIC_CASINGS, GTBlocks.HERMETIC_CASINGS.values().toArray(Block[]::new));
        registerCategory(COMPUTER_CASINGS, GTBlocks.COMPUTER_CASINGS.values().toArray(Block[]::new));
        registerCategory(COMPUTER_HEAT_VENTS, GTBlocks.COMPUTER_HEAT_VENTS.values().toArray(Block[]::new));
    }

    /** Block arrays are ordered category data and are not copied or mutated after registration. */
    public static void registerCategory(String category, Block[] blocks) {
        MAP.put(category, blocks);
        for (Block block : blocks) BLOCK_CATEGORY_MAP.put(block, category);
    }

    public static @Nullable String getCategory(Block block) {
        return BLOCK_CATEGORY_MAP.get(block);
    }

    public static Block[] getBlocks(String category) {
        return MAP.get(category);
    }

    public static Block getBlock(String category, int index) {
        return getBlocks(category)[index];
    }

    /** Returns the declared category index, or -1 for a block outside the classification table. */
    public static int indexOf(Block block) {
        String category = getCategory(block);
        if (category == null) return -1;
        Block[] blocks = getBlocks(category);
        for (int index = 0; index < blocks.length; index++) if (blocks[index] == block) return index;
        return -1;
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
