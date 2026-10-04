package com.gregtechceu.gtceu.api.multiblock.pattern.compile;

import com.gregtechceu.gtceu.api.multiblock.pattern.model.PatternDefinition;
import com.gregtechceu.gtceu.api.multiblock.pattern.model.PatternNode;

import org.jspecify.annotations.NullMarked;

import java.util.List;
import java.util.Map;

/** Immutable compiler output shared by matching, preview and auto-build consumers. */
@NullMarked
public record CompiledPattern(
                              PatternDefinition definition,
                              List<Cell> cells,
                              Map<String, Integer> nodeCounts,
                              int maximumRecursionDepth) {

    public CompiledPattern {
        if (definition == null || cells == null || nodeCounts == null) {
            throw new IllegalArgumentException("Compiled pattern fields must not be null");
        }
        cells = List.copyOf(cells);
        nodeCounts = Map.copyOf(nodeCounts);
        if (maximumRecursionDepth < 0) {
            throw new IllegalArgumentException("Maximum recursion depth must not be negative");
        }
    }

    public record Cell(String path, PatternNode.Fixed fixed, int depthOffset, int heightOffset, int widthOffset) {

        public Cell {
            if (path == null || path.isBlank() || fixed == null) {
                throw new IllegalArgumentException("Compiled cells require a path and fixed node");
            }
        }
    }
}
