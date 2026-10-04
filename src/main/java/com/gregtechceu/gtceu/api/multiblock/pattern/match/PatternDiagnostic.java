package com.gregtechceu.gtceu.api.multiblock.pattern.match;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

import org.jspecify.annotations.Nullable;

/** A single actionable mismatch with both local and world coordinates. */
public record PatternDiagnostic(
                                String path,
                                String repeatPath,
                                PatternCoordinate local,
                                BlockPos world,
                                @Nullable BlockState actual,
                                String expected,
                                String missingFact,
                                int difference) {

    public PatternDiagnostic {
        if (path == null || path.isBlank() || repeatPath == null || local == null || world == null ||
                expected == null || missingFact == null) {
            throw new IllegalArgumentException("Pattern diagnostics require complete location and expectation data");
        }
    }
}
