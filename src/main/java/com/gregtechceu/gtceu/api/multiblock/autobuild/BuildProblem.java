package com.gregtechceu.gtceu.api.multiblock.autobuild;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;

import org.jspecify.annotations.Nullable;

/**
 * A concrete reason an automatic multiblock build request could not continue.
 *
 * @param pos             optional world position related to the problem
 * @param message         server-side diagnostic text
 * @param preventsPreview whether candidate resolution must stop before producing preview geometry
 */
public record BuildProblem(@Nullable BlockPos pos, Component message, boolean preventsPreview) {

    public BuildProblem(@Nullable BlockPos pos, Component message) {
        this(pos, message, true);
    }
}
