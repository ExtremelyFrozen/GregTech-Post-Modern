package com.gregtechceu.gtceu.api.multiblock.pattern.predicate;

import net.minecraft.resources.ResourceLocation;

import org.jspecify.annotations.NullMarked;

import java.util.concurrent.ConcurrentHashMap;

/** Explicit predicate type registry shared by Java, JSON and binary sources. */
@NullMarked
public final class PredicateRegistry {

    private static final ConcurrentHashMap<ResourceLocation, PredicateType> TYPES = new ConcurrentHashMap<>();

    private PredicateRegistry() {}

    public static void register(ResourceLocation id, PredicateType type) {
        if (TYPES.putIfAbsent(id, type) != null) {
            throw new IllegalArgumentException("Predicate type is already registered: " + id);
        }
    }

    public static PredicateType require(ResourceLocation id) {
        PredicateType type = TYPES.get(id);
        if (type == null) throw new IllegalArgumentException("Unknown predicate type: " + id);
        return type;
    }

    public static void clearForReload() {
        TYPES.clear();
    }
}
