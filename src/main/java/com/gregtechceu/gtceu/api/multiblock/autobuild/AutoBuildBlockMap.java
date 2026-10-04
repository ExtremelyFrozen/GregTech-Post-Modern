package com.gregtechceu.gtceu.api.multiblock.autobuild;

import com.gregtechceu.gtceu.GTCEu;
import com.gregtechceu.gtceu.api.GTCEuAPI;
import com.gregtechceu.gtceu.api.block.ICoilType;
import com.gregtechceu.gtceu.api.block.IFilterType;
import com.gregtechceu.gtceu.api.machine.MachineDefinition;
import com.gregtechceu.gtceu.api.machine.multiblock.IBatteryData;
import com.gregtechceu.gtceu.common.data.GTBlocks;
import com.gregtechceu.gtceu.common.data.GTMachines;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;

import com.tterrag.registrate.util.entry.RegistryEntry;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * Maps selectable multiblock block categories to tier-ordered blocks.
 */
public final class AutoBuildBlockMap {

    public static final String HEATING_COILS = "heating_coils";
    public static final String CLEANROOM_FILTERS = "cleanroom_filters";
    public static final String POWER_SUBSTATION_BATTERIES = "power_substation_batteries";
    public static final String LAMPS = "lamps";
    public static final String BORDERLESS_LAMPS = "borderless_lamps";
    public static final String MUFFLER_HATCHES = "muffler_hatches";
    public static final String ROTOR_HOLDER = "rotor_holder";
    public static final String BATTERIES = "batteries";
    public static final String ROTOR_HOLDERS = "rotor_holders";

    private static final Object LOCK = new Object();
    private static final Object2ObjectOpenHashMap<String, Block[]> CATEGORIES = new Object2ObjectOpenHashMap<>();
    private static final Object2ObjectOpenHashMap<ResourceLocation, Block[]> RESOURCE_CATEGORIES = new Object2ObjectOpenHashMap<>();
    private static final Object2ObjectOpenHashMap<String, String> CANONICAL_CATEGORIES = new Object2ObjectOpenHashMap<>();
    private static final Reference2ObjectOpenHashMap<Block, String> BLOCK_CATEGORIES = new Reference2ObjectOpenHashMap<>();
    private static final Reference2ObjectOpenHashMap<Block, List<ResourceLocation>> BLOCK_RESOURCE_CATEGORIES = new Reference2ObjectOpenHashMap<>();
    private static boolean built;

    private AutoBuildBlockMap() {}

    public static Map<String, Block[]> categories() {
        ensureBuilt();
        Map<String, Block[]> categories = new HashMap<>();
        for (var entry : CATEGORIES.entrySet()) {
            categories.put(entry.getKey(), Arrays.copyOf(entry.getValue(), entry.getValue().length));
        }
        return Map.copyOf(categories);
    }

    @Nullable
    public static Block[] categoryBlocks(String category) {
        ensureBuilt();
        Block[] blocks = CATEGORIES.get(canonicalCategory(category));
        return blocks == null ? null : Arrays.copyOf(blocks, blocks.length);
    }

    @Nullable
    public static String category(Block block) {
        ensureBuilt();
        return BLOCK_CATEGORIES.get(block);
    }

    /**
     * Returns the stable resource id of a selectable group containing the block.
     */
    @Nullable
    public static ResourceLocation categoryId(Block block) {
        List<ResourceLocation> categories = categoryIds(block);
        return categories.isEmpty() ? null : categories.getFirst();
    }

    /**
     * Returns every explicitly registered tier group containing this block in registration order.
     */
    public static List<ResourceLocation> categoryIds(Block block) {
        ensureBuilt();
        return List.copyOf(BLOCK_RESOURCE_CATEGORIES.getOrDefault(block, List.of()));
    }

    /**
     * Converts the legacy category path to its stable resource id.
     */
    public static ResourceLocation categoryId(String category) {
        return ResourceLocation.fromNamespaceAndPath(GTCEu.MOD_ID, canonicalCategory(category));
    }

    /**
     * Canonicalizes a stable category id while preserving addon namespaces.
     */
    public static ResourceLocation canonicalCategory(ResourceLocation category) {
        if (!GTCEu.MOD_ID.equals(category.getNamespace())) {
            return category;
        }
        return categoryId(category.getPath());
    }

    /**
     * Returns the registered blocks for a stable category id.
     */
    @Nullable
    public static Block[] categoryBlocks(ResourceLocation category) {
        ensureBuilt();
        ResourceLocation canonical = canonicalCategory(category);
        Block[] blocks = RESOURCE_CATEGORIES.get(canonical);
        return blocks == null ? null : Arrays.copyOf(blocks, blocks.length);
    }

    /**
     * Resolves an exact selected block id.
     */
    @Nullable
    public static Block block(ResourceLocation blockId) {
        return BuiltInRegistries.BLOCK.containsKey(blockId) ? BuiltInRegistries.BLOCK.get(blockId) : null;
    }

    /**
     * Returns the stable registry id used in a tier selection.
     */
    public static ResourceLocation blockId(Block block) {
        return BuiltInRegistries.BLOCK.getKey(block);
    }

    public static String canonicalCategory(String category) {
        ensureBuilt();
        return CANONICAL_CATEGORIES.getOrDefault(category, category);
    }

    public static void registerCategory(String category, Block[] blocks) {
        synchronized (LOCK) {
            Block[] registeredBlocks = validateCandidates(category, blocks);
            Block[] existing = CATEGORIES.get(category);
            if (existing != null) {
                if (!Arrays.equals(existing, registeredBlocks)) {
                    throw new IllegalArgumentException("Tier group was registered with different candidates: " +
                            category);
                }
                return;
            }
            CATEGORIES.put(category, registeredBlocks);
            ResourceLocation resourceCategory = ResourceLocation.fromNamespaceAndPath(GTCEu.MOD_ID, category);
            RESOURCE_CATEGORIES.put(resourceCategory, registeredBlocks);
            CANONICAL_CATEGORIES.put(category, category);
            for (Block block : registeredBlocks) {
                BLOCK_CATEGORIES.put(block, category);
                addBlockCategory(block, resourceCategory);
            }
        }
    }

    /**
     * Registers an explicit addon or predicate-derived tier group.
     */
    public static void registerCategory(ResourceLocation category, Block[] blocks) {
        synchronized (LOCK) {
            Block[] registeredBlocks = validateCandidates(category, blocks);
            Block[] existing = RESOURCE_CATEGORIES.get(category);
            if (existing != null) {
                if (!Arrays.equals(existing, registeredBlocks)) {
                    throw new IllegalArgumentException("Tier group was registered with different candidates: " +
                            category);
                }
                return;
            }
            RESOURCE_CATEGORIES.put(category, registeredBlocks);
            for (Block block : registeredBlocks) {
                addBlockCategory(block, category);
            }
        }
    }

    private static Block[] validateCandidates(Object category, Block[] blocks) {
        if (blocks.length == 0) {
            throw new IllegalArgumentException("Tier group requires at least one candidate: " + category);
        }
        Block[] copy = Arrays.copyOf(blocks, blocks.length);
        Set<Block> encountered = Collections.newSetFromMap(new IdentityHashMap<>());
        for (Block block : copy) {
            if (block == null) {
                throw new IllegalArgumentException("Tier group contains a null candidate: " + category);
            }
            if (!encountered.add(block)) {
                throw new IllegalArgumentException("Tier group contains a duplicate candidate: " + category);
            }
        }
        return copy;
    }

    private static void addBlockCategory(Block block, ResourceLocation category) {
        List<ResourceLocation> current = BLOCK_RESOURCE_CATEGORIES.get(block);
        if (current != null && current.contains(category)) return;
        ArrayList<ResourceLocation> updated = new ArrayList<>(current == null ? List.of() : current);
        updated.add(category);
        BLOCK_RESOURCE_CATEGORIES.put(block, List.copyOf(updated));
    }

    public static void registerAlias(String alias, String category) {
        synchronized (LOCK) {
            Block[] blocks = CATEGORIES.get(category);
            if (blocks == null) {
                throw new IllegalArgumentException("Unknown tier group for alias '" + alias + "': " + category);
            }
            String existingCategory = CANONICAL_CATEGORIES.get(alias);
            Block[] existingBlocks = CATEGORIES.get(alias);
            ResourceLocation aliasId = ResourceLocation.fromNamespaceAndPath(GTCEu.MOD_ID, alias);
            Block[] existingResourceBlocks = RESOURCE_CATEGORIES.get(aliasId);
            if (existingCategory != null || existingBlocks != null || existingResourceBlocks != null) {
                if (category.equals(existingCategory) && Arrays.equals(blocks, existingBlocks) &&
                        Arrays.equals(blocks, existingResourceBlocks)) {
                    return;
                }
                throw new IllegalArgumentException("Tier group alias was registered with a different target: " +
                        alias);
            }
            CATEGORIES.put(alias, blocks);
            CANONICAL_CATEGORIES.put(alias, category);
            RESOURCE_CATEGORIES.put(aliasId, blocks);
        }
    }

    private static void ensureBuilt() {
        if (built) return;
        synchronized (LOCK) {
            if (built) return;
            registerCategory(HEATING_COILS, sortedBlocks(GTCEuAPI.HEATING_COILS.entrySet(),
                    Comparator.comparingInt(ICoilType::getTier)));
            registerCategory(CLEANROOM_FILTERS, sortedBlocks(GTCEuAPI.CLEANROOM_FILTERS.entrySet(),
                    Comparator.comparing(IFilterType::getSerializedName)));
            registerCategory(POWER_SUBSTATION_BATTERIES, sortedBlocks(GTCEuAPI.PSS_BATTERIES.entrySet(),
                    Comparator.comparingInt(IBatteryData::getTier)));
            registerAlias(BATTERIES, POWER_SUBSTATION_BATTERIES);
            registerCategory(LAMPS, GTBlocks.LAMPS.values().stream().map(RegistryEntry::get).toArray(Block[]::new));
            registerCategory(BORDERLESS_LAMPS,
                    GTBlocks.BORDERLESS_LAMPS.values().stream().map(RegistryEntry::get).toArray(Block[]::new));
            registerCategory(MUFFLER_HATCHES, sortedMachineBlocks(GTMachines.MUFFLER_HATCH));
            registerCategory(ROTOR_HOLDER, sortedMachineBlocks(GTMachines.ROTOR_HOLDER));
            registerAlias(ROTOR_HOLDERS, ROTOR_HOLDER);
            built = true;
        }
    }

    private static <K, V extends Supplier<? extends Block>> Block[] sortedBlocks(
                                                                                 Iterable<Map.Entry<K, V>> entries,
                                                                                 Comparator<? super K> comparator) {
        return stream(entries).sorted((first, second) -> comparator.compare(first.getKey(), second.getKey()))
                .map(entry -> entry.getValue().get())
                .toArray(Block[]::new);
    }

    private static Block[] sortedMachineBlocks(MachineDefinition[] definitions) {
        return Arrays.stream(definitions)
                .filter(definition -> definition != null)
                .sorted(Comparator.comparingInt(MachineDefinition::getTier))
                .map(MachineDefinition::getBlock)
                .toArray(Block[]::new);
    }

    private static <T> Stream<T> stream(Iterable<T> iterable) {
        return StreamSupport.stream(iterable.spliterator(), false);
    }
}
