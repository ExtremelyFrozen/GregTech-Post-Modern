package com.gregtechceu.gtceu.api.gui.factory;

import com.gregtechceu.gtceu.api.machine.MetaMachine;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** Holds the machine whose part page belongs to the current menu. */
@RequiredArgsConstructor
public final class MachineUIHolderContext implements MachineUIHolder {

    @Getter
    private final MetaMachine machine;

    @Override
    public BlockPos getPos() {
        return machine.getBlockPos();
    }

    @Override
    public ResourceLocation getMachineDefinitionId() {
        return machine.getDefinition().getId();
    }
}
