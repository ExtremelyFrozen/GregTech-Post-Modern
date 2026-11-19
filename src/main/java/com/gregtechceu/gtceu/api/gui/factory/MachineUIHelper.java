package com.gregtechceu.gtceu.api.gui.factory;

import com.gregtechceu.gtceu.api.machine.MetaMachine;
import com.gregtechceu.gtceu.api.machine.feature.IUIMachine;

import net.minecraft.server.level.ServerPlayer;

/**
 * Centralizes LDLib2 machine UI opening for block-backed machines.
 */
public final class MachineUIHelper {

    private MachineUIHelper() {}

    /**
     * Opens the UI for a machine block.
     *
     * @return {@code true} when the machine provider accepted and opened the menu.
     */
    public static boolean open(MetaMachine machine, ServerPlayer player) {
        return machine instanceof IUIMachine provider && provider.openUI(machine, player);
    }
}
