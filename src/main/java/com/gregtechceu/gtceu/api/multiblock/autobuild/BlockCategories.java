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
        registerCategory(GLASS,
                new Block[] { GTBlocks.CASING_TEMPERED_GLASS.get(), GTBlocks.CASING_LAMINATED_GLASS.get(),
                        GTBlocks.FUSION_GLASS.get(), GTBlocks.CLEANROOM_GLASS.get() });
        registerCategory(MACHINE_CASINGS, new Block[] { GTBlocks.MACHINE_CASING_ULV.get(),
                GTBlocks.MACHINE_CASING_LV.get(),
                GTBlocks.MACHINE_CASING_MV.get(), GTBlocks.MACHINE_CASING_HV.get(), GTBlocks.MACHINE_CASING_EV.get(),
                GTBlocks.MACHINE_CASING_IV.get(), GTBlocks.MACHINE_CASING_LuV.get(), GTBlocks.MACHINE_CASING_ZPM.get(),
                GTBlocks.MACHINE_CASING_UV.get(), GTBlocks.MACHINE_CASING_UHV.get(), GTBlocks.MACHINE_CASING_UEV.get(),
                GTBlocks.MACHINE_CASING_UIV.get(), GTBlocks.MACHINE_CASING_UXV.get(), GTBlocks.MACHINE_CASING_OpV.get(),
                GTBlocks.MACHINE_CASING_MAX.get() });
        registerCategory(HERMETIC_CASINGS,
                new Block[] { GTBlocks.HERMETIC_CASING_LV.get(), GTBlocks.HERMETIC_CASING_MV.get(),
                        GTBlocks.HERMETIC_CASING_HV.get(), GTBlocks.HERMETIC_CASING_EV.get(),
                        GTBlocks.HERMETIC_CASING_IV.get(),
                        GTBlocks.HERMETIC_CASING_LuV.get(), GTBlocks.HERMETIC_CASING_ZPM.get(),
                        GTBlocks.HERMETIC_CASING_UV.get(),
                        GTBlocks.HERMETIC_CASING_UHV.get() });
        registerCategory(COMPUTER_CASINGS,
                new Block[] { GTBlocks.COMPUTER_CASING.get(), GTBlocks.ADVANCED_COMPUTER_CASING.get() });
        registerCategory(COMPUTER_HEAT_VENTS, new Block[] { GTBlocks.COMPUTER_HEAT_VENT.get() });
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
