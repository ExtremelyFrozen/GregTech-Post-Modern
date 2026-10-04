package com.gregtechceu.gtceu.api.multiblock.pattern.model;

import net.minecraft.resources.ResourceLocation;

import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectMaps;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectLists;
import org.jspecify.annotations.NullMarked;

import java.util.List;
import java.util.Map;

/** Canonical source model shared by Java DSL, JSON and compressed binary definitions. */
@NullMarked
public record PatternDefinition(
                                ResourceLocation machine,
                                ResourceLocation structure,
                                PatternAxes axes,
                                PatternOrientation orientation,
                                PatternOrigin origin,
                                Map<String, PatternParameter> parameters,
                                Map<String, PatternFragment> fragments,
                                Map<Character, PatternPredicateDefinition> predicates,
                                PatternNode body,
                                List<PatternConstraint> constraints) {

    public PatternDefinition {
        if (machine == null || structure == null || axes == null || orientation == null || origin == null ||
                body == null) {
            throw new IllegalArgumentException("Pattern definition metadata and body are required");
        }
        parameters = copyMap(parameters);
        fragments = copyMap(fragments);
        predicates = copyMap(predicates);
        constraints = ObjectLists.unmodifiable(new ObjectArrayList<>(constraints));
    }

    private static <K, V> Map<K, V> copyMap(Map<K, V> input) {
        if (input.isEmpty()) return Map.of();
        var copy = new Object2ObjectLinkedOpenHashMap<K, V>();
        input.forEach((key, value) -> {
            if (key == null || value == null) throw new IllegalArgumentException("Pattern maps cannot contain nulls");
            copy.put(key, value);
        });
        return Object2ObjectMaps.unmodifiable(copy);
    }
}
