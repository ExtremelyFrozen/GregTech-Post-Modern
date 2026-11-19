package com.gregtechceu.gtceu.common.machine.multiblock.electric.monitor;

import net.minecraft.core.BlockPos;

import org.jspecify.annotations.Nullable;

/** A group's selected grid component and data slot; a null position clears the target. */
public record CentralMonitorGroupTargetState(@Nullable BlockPos targetPos, int dataSlot) {}
