package com.gregtechceu.gtceu.api.multiblock.structurepredicate;

import com.gregtechceu.gtceu.GTCEu;
import com.gregtechceu.gtceu.api.addon.AddonFinder;
import com.gregtechceu.gtceu.api.addon.IGTAddon;
import com.gregtechceu.gtceu.api.registry.GTRegistries;

import net.minecraft.resources.ResourceLocation;

import com.mojang.serialization.MapCodec;
import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public record StructurePredicateType<T extends StructurePredicate>(MapCodec<T> codec) {

    private static final Object2ObjectMap<ResourceLocation, StructurePredicateType<?>> EARLY_TYPES = new Object2ObjectLinkedOpenHashMap<>();

    public static final StructurePredicateType<BlockPredicate> BLOCKS = register(GTCEu.id("blocks"),
            BlockPredicate.CODEC);
    public static final StructurePredicateType<BlockStatePredicate> BLOCK_STATES = register(GTCEu.id("block_states"),
            BlockStatePredicate.CODEC);
    public static final StructurePredicateType<BlockTagPredicate> BLOCK_TAGS = register(GTCEu.id("block_tags"),
            BlockTagPredicate.CODEC);
    public static final StructurePredicateType<FluidPredicate> FLUIDS = register(GTCEu.id("fluids"),
            FluidPredicate.CODEC);
    public static final StructurePredicateType<FluidTagPredicate> FLUID_TAGS = register(GTCEu.id("fluid_tags"),
            FluidTagPredicate.CODEC);
    public static final StructurePredicateType<AbilityPredicate> ABILITIES = register(GTCEu.id("abilities"),
            AbilityPredicate.CODEC);
    public static final StructurePredicateType<TieredAbilityPredicate> TIERED_ABILITY = register(
            GTCEu.id("tiered_ability"), TieredAbilityPredicate.CODEC);
    public static final StructurePredicateType<AutoRecipeAbilityPredicate> AUTO_RECIPE_ABILITIES = register(
            GTCEu.id("auto_recipe_abilities"), AutoRecipeAbilityPredicate.CODEC);
    public static final StructurePredicateType<AutoMaintenancePredicate> AUTO_MAINTENANCE = register(
            GTCEu.id("auto_maintenance"), AutoMaintenancePredicate.CODEC);
    public static final StructurePredicateType<ActiveTransformerHatchPredicate> ACTIVE_TRANSFORMER_HATCHES = register(
            GTCEu.id("active_transformer_hatches"), ActiveTransformerHatchPredicate.CODEC);
    public static final StructurePredicateType<CentralMonitorComponentPredicate> CENTRAL_MONITOR_COMPONENTS = register(
            GTCEu.id("central_monitor_components"), CentralMonitorComponentPredicate.CODEC);
    public static final StructurePredicateType<CharcoalLogOrAirPredicate> CHARCOAL_LOG_OR_AIR = register(
            GTCEu.id("charcoal_log_or_air"), CharcoalLogOrAirPredicate.CODEC);
    public static final StructurePredicateType<CleanroomDoorPredicate> CLEANROOM_DOORS = register(
            GTCEu.id("cleanroom_doors"), CleanroomDoorPredicate.CODEC);
    public static final StructurePredicateType<CleanroomInnerPredicate> CLEANROOM_INNER = register(
            GTCEu.id("cleanroom_inner"), CleanroomInnerPredicate.CODEC);
    public static final StructurePredicateType<CleanroomBasePredicate> CLEANROOM_BASE = register(
            GTCEu.id("cleanroom_base"), CleanroomBasePredicate.CODEC);
    public static final StructurePredicateType<HeatingCoilPredicate> HEATING_COILS = register(GTCEu.id("heating_coils"),
            HeatingCoilPredicate.CODEC);
    public static final StructurePredicateType<CleanroomFilterPredicate> CLEANROOM_FILTERS = register(
            GTCEu.id("cleanroom_filters"), CleanroomFilterPredicate.CODEC);
    public static final StructurePredicateType<FramePredicate> FRAMES = register(GTCEu.id("frames"),
            FramePredicate.CODEC);
    public static final StructurePredicateType<PowerSubstationBatteryPredicate> POWER_SUBSTATION_BATTERIES = register(
            GTCEu.id("power_substation_batteries"), PowerSubstationBatteryPredicate.CODEC);
    public static final StructurePredicateType<SnowPredicate> SNOW = register(GTCEu.id("snow"), SnowPredicate.CODEC);
    public static final StructurePredicateType<DataHatchOrPredicate> DATA_HATCH_OR = register(GTCEu.id("data_hatch_or"),
            DataHatchOrPredicate.CODEC);
    public static final StructurePredicateType<RotorHolderPredicate> ROTOR_HOLDER = register(GTCEu.id("rotor_holder"),
            RotorHolderPredicate.CODEC);

    public static final StructurePredicateType<RestrictedPredicate> RESTRICTED = register(GTCEu.id("restricted"),
            RestrictedPredicate.CODEC);
    public static final StructurePredicateType<ConcatenatedPredicate> CONCATENATED = register(GTCEu.id("concatenated"),
            ConcatenatedPredicate.CODEC);

    public static final StructurePredicateType<AnyPredicate> ANY = register(GTCEu.id("any"), AnyPredicate.CODEC);
    public static final StructurePredicateType<AirPredicate> AIR = register(GTCEu.id("air"), AirPredicate.CODEC);

    public static <T extends StructurePredicate> StructurePredicateType<T> register(ResourceLocation id,
                                                                                    MapCodec<T> codec) {
        StructurePredicateType<T> type = new StructurePredicateType<>(codec);
        StructurePredicateType<?> previous = EARLY_TYPES.putIfAbsent(id, type);
        if (previous != null) {
            throw new IllegalArgumentException("Duplicate structure predicate type: " + id);
        }
        return GTRegistries.register(GTRegistries.STRUCTURE_PREDICATE_TYPES, id, type);
    }

    public static @Nullable StructurePredicateType<?> byId(ResourceLocation id) {
        return EARLY_TYPES.get(id);
    }

    public static @Nullable ResourceLocation id(StructurePredicateType<?> type) {
        for (var entry : EARLY_TYPES.object2ObjectEntrySet()) {
            if (entry.getValue() == type) {
                return entry.getKey();
            }
        }
        return null;
    }

    /** Runs addon registration while machine definitions can still depend on the registered predicates. */
    public static void init() {
        AddonFinder.getAddonList().forEach(IGTAddon::registerStructurePredicateTypes);
    }
}
