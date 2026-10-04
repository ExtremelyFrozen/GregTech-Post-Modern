package com.gregtechceu.gtceu.api.multiblock.pattern.model;

import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectMaps;
import org.jspecify.annotations.NullMarked;

import java.util.Map;

/** A reusable recursive pattern fragment with an explicit parameter scope. */
@NullMarked
public record PatternFragment(
                              Map<String, PatternParameter> parameters,
                              Map<String, PatternOrigin.Offset> anchors,
                              Map<String, Port> ports,
                              PatternNode body) {

    public PatternFragment {
        parameters = Object2ObjectMaps.unmodifiable(new Object2ObjectLinkedOpenHashMap<>(parameters));
        anchors = Object2ObjectMaps.unmodifiable(new Object2ObjectLinkedOpenHashMap<>(anchors));
        ports = Object2ObjectMaps.unmodifiable(new Object2ObjectLinkedOpenHashMap<>(ports));
        if (body == null) {
            throw new IllegalArgumentException("Pattern fragment body must not be null");
        }
    }

    public record Port(PatternOrigin.Offset offset, PatternDirection direction) {

        public Port {
            if (offset == null || direction == null) {
                throw new IllegalArgumentException("Pattern fragment ports require offset and direction");
            }
        }
    }
}
