package com.gregtechceu.gtceu.api.gui.factory;

import com.gregtechceu.gtceu.api.gui.element.GTDynamicItemSlotSessionElement;
import com.gregtechceu.gtceu.api.machine.MetaMachine;
import com.gregtechceu.gtceu.api.machine.feature.IUIMachine;

import com.lowdragmc.lowdraglib2.gui.holder.ModularUIContainerMenu;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;

/**
 * Opens an LDLib2 machine UI through GTM's session-bound dynamic item-slot menu.
 *
 * <p>
 * The returned UI tree must contain exactly one {@link GTDynamicItemSlotSessionElement}. That element owns every
 * runtime slot append and keeps removed ranges as opening-local tombstones.
 * </p>
 */
public interface DynamicItemSlotMachineUIProvider extends IUIMachine {

    /**
     * Captures the machine session so runtime slots can be acknowledged before they become interactive.
     */
    @Override
    default boolean openUI(MetaMachine machine, ServerPlayer player) {
        return DynamicItemSlotMachineUIHolder.open(machine, player);
    }

    @Override
    default ModularUIContainerMenu createClientMenu(int windowId, Inventory inventory, MachineBlockUIHolder holder,
                                                    RegistryFriendlyByteBuf data) {
        return DynamicItemSlotMachineUIHolder.read(windowId, inventory, holder.getPos(), data);
    }
}
