package com.gregtechceu.gtceu.api.multiblock.pattern.model;

import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectMaps;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectLists;
import org.jspecify.annotations.NullMarked;

import java.util.List;
import java.util.Map;

/** A serialized predicate reference and its stable fact aliases. */
@NullMarked
public record PatternPredicateDefinition(
                                         String type,
                                         String name,
                                         Map<String, Object> properties,
                                         List<String> facts) {

    public PatternPredicateDefinition {
        if (type == null || type.isBlank()) throw new IllegalArgumentException("Predicate type must not be blank");
        name = name == null ? "" : name;
        properties = Object2ObjectMaps.unmodifiable(new Object2ObjectLinkedOpenHashMap<>(properties));
        facts = ObjectLists.unmodifiable(new ObjectArrayList<>(facts));
    }
}
