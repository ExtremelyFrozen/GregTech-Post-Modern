package com.gregtechceu.gtceu.api.multiblock.pattern.model;

import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectMaps;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectLists;
import org.jspecify.annotations.NullMarked;

import java.util.List;
import java.util.Map;

/** A node in the recursive pattern AST. */
@NullMarked
public sealed interface PatternNode
                                    permits PatternNode.Fixed, PatternNode.Sequence, PatternNode.Repeat,
                                    PatternNode.Choice, PatternNode.Fragment {

    record Fixed(List<List<String>> layers) implements PatternNode {

        public static Fixed fromSlices(String[]... slices) {
            var layers = new ObjectArrayList<List<String>>(slices.length);
            for (String[] slice : slices) {
                var rows = new ObjectArrayList<String>(slice.length);
                for (String row : slice) rows.add(row);
                layers.add(rows);
            }
            return new Fixed(layers);
        }

        public Fixed {
            if (layers == null || layers.isEmpty()) {
                throw new IllegalArgumentException("Fixed pattern must contain at least one layer");
            }
            var normalizedLayers = new ObjectArrayList<List<String>>(layers.size());
            int height = -1;
            int width = -1;
            for (List<String> layer : layers) {
                if (layer == null || layer.isEmpty()) {
                    throw new IllegalArgumentException("Fixed pattern layers must not be empty");
                }
                if (height < 0) {
                    height = layer.size();
                } else if (height != layer.size()) {
                    throw new IllegalArgumentException("Fixed pattern layers must have equal heights");
                }
                var normalizedRows = new ObjectArrayList<String>(layer.size());
                for (String row : layer) {
                    if (row == null || row.isEmpty()) {
                        throw new IllegalArgumentException("Fixed pattern rows must not be empty");
                    }
                    if (width < 0) {
                        width = row.length();
                    } else if (width != row.length()) {
                        throw new IllegalArgumentException("Fixed pattern rows must have equal widths");
                    }
                    normalizedRows.add(row);
                }
                normalizedLayers.add(List.copyOf(normalizedRows));
            }
            layers = ObjectLists.unmodifiable(normalizedLayers);
        }

        public int depth() {
            return layers.size();
        }

        public int height() {
            return layers.getFirst().size();
        }

        public int width() {
            return layers.getFirst().getFirst().length();
        }
    }

    record Sequence(PatternDirection axis, List<PatternNode> children) implements PatternNode {

        public Sequence {
            if (axis == null || children == null || children.isEmpty()) {
                throw new IllegalArgumentException("Sequence requires an axis and at least one child");
            }
            children = ObjectLists.unmodifiable(new ObjectArrayList<>(children));
        }
    }

    record Repeat(String id, PatternDirection axis, PatternRepeatDirection direction, int minimum, int maximum,
                  PatternNode body)
            implements PatternNode {

        public Repeat {
            if (id == null || id.isBlank() || axis == null || direction == null || body == null) {
                throw new IllegalArgumentException("Repeat requires an id, axis, direction and body");
            }
            if (minimum < 0 || maximum < minimum || maximum == Integer.MAX_VALUE) {
                throw new IllegalArgumentException("Repeat bounds must be finite and ordered");
            }
        }
    }

    record Choice(String id, List<Alternative> alternatives) implements PatternNode {

        public Choice {
            if (id == null || id.isBlank() || alternatives == null || alternatives.isEmpty()) {
                throw new IllegalArgumentException("Choice requires an id and alternatives");
            }
            alternatives = List.copyOf(alternatives);
        }

        public record Alternative(String id, PatternNode node) {

            public Alternative {
                if (id == null || id.isBlank() || node == null) {
                    throw new IllegalArgumentException("Choice alternatives require an id and node");
                }
            }
        }
    }

    record Fragment(String id, Map<String, PatternBinding> bindings) implements PatternNode {

        public Fragment {
            if (id == null || id.isBlank()) {
                throw new IllegalArgumentException("Fragment reference requires an id");
            }
            var normalized = new Object2ObjectLinkedOpenHashMap<String, PatternBinding>();
            bindings.forEach((key, value) -> {
                if (key == null || key.isBlank() || value == null) {
                    throw new IllegalArgumentException("Fragment bindings must have non-empty names and values");
                }
                normalized.put(key, value);
            });
            bindings = Object2ObjectMaps.unmodifiable(normalized);
        }
    }
}
