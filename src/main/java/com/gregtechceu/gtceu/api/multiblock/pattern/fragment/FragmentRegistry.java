package com.gregtechceu.gtceu.api.multiblock.pattern.fragment;

import com.gregtechceu.gtceu.api.multiblock.pattern.model.PatternFragment;

import net.minecraft.resources.ResourceLocation;

import org.jspecify.annotations.NullMarked;

import java.util.concurrent.ConcurrentHashMap;

/** Namespace-aware registry for reusable recursive fragments. */
@NullMarked
public final class FragmentRegistry {

    private static final ConcurrentHashMap<ResourceLocation, PatternFragment> FRAGMENTS = new ConcurrentHashMap<>();

    private FragmentRegistry() {}

    public static void register(ResourceLocation id, PatternFragment fragment) {
        if (FRAGMENTS.putIfAbsent(id, fragment) != null) {
            throw new IllegalArgumentException("Pattern fragment is already registered: " + id);
        }
    }

    public static PatternFragment require(ResourceLocation id) {
        PatternFragment fragment = FRAGMENTS.get(id);
        if (fragment == null) throw new IllegalArgumentException("Unknown pattern fragment: " + id);
        return fragment;
    }

    public static void clearForReload() {
        FRAGMENTS.clear();
    }
}
