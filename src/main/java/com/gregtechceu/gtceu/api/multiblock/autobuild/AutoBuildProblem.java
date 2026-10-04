package com.gregtechceu.gtceu.api.multiblock.autobuild;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;

import org.jspecify.annotations.Nullable;

/**
 * A concrete reason an automatic multiblock build request could not continue.
 *
 * @param type    machine-readable problem category
 * @param pos     optional world position related to the problem
 * @param message user-facing diagnostic text
 */
public record AutoBuildProblem(Type type, @Nullable BlockPos pos, Component message) {

    public enum Type {
        UNKNOWN_STRUCTURE,
        INVALID_OPTIONS,
        PATTERN_UNAVAILABLE,
        PERMISSION_DENIED,
        UNLOADED,
        BLOCKED,
        UNSUPPORTED,
        MISSING_MATERIAL,
        PLACE_FAILED,
        DEMOLITION_FAILED,
        ME_UNAVAILABLE,
        PLAYER_INVENTORY_UNAVAILABLE,
        AE_NOT_INSTALLED,
        AE_NOT_LINKED,
        AE_LINKED_DIMENSION_MISSING,
        AE_WRONG_DIMENSION,
        AE_ACCESS_POINT_MISSING,
        AE_ACCESS_POINT_INACTIVE,
        AE_OUT_OF_RANGE,
        AE_GRID_UNAVAILABLE,
        STALE_PLAN,
        SOURCE_CHANGED,
        EXTRACTION_FAILED,
        REFUND_FAILED,
        COUNT_OVERFLOW,
        INVALID_TIER_SELECTION,
        STRUCTURE_CHECK_FAILED
    }
}
