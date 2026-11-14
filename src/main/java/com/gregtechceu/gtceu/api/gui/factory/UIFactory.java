package com.gregtechceu.gtceu.api.gui.factory;

import com.lowdragmc.lowdraglib2.gui.holder.ModularUIContainerMenu;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;

import org.jspecify.annotations.NullMarked;

/**
 * Reconstructs one client menu from its server-owned opening data. Factories are registered during mod initialization
 * and invoked on the client thread after the common factory identifier has been decoded.
 */
@NullMarked
@FunctionalInterface
public interface UIFactory {

    /**
     * Consumes this factory's opening payload and creates its holder and menu exactly once. The payload must identify
     * the same owner captured by the server; invalid or unavailable owners must be rejected rather than substituted.
     * A factory that allocates a detached view must release it if construction fails.
     *
     * @param windowId  vanilla container identifier assigned by the server
     * @param inventory inventory of the player receiving this opening
     * @param data      opening payload positioned immediately after the factory identifier
     * @return the constructed menu, using {@link UIRegistry#MENU}
     * @throws IllegalArgumentException if the payload does not identify a supported owner
     * @throws IllegalStateException    if the opened owner is no longer available
     */
    ModularUIContainerMenu create(int windowId, Inventory inventory, RegistryFriendlyByteBuf data);
}
