package com.gregtechceu.gtceu.api.gui.factory;

import com.gregtechceu.gtceu.api.sync_system.managed.ISyncManaged;
import com.gregtechceu.gtceu.api.sync_system.managed.ItemSyncHolder;
import com.gregtechceu.gtceu.api.sync_system.managed.SyncMenu;

import com.lowdragmc.lowdraglib2.gui.holder.ModularUIContainerMenu;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;

import org.jspecify.annotations.Nullable;

/** Owns automatic item field synchronization and the final save when a held-item menu closes. */
public final class HeldItemMenu extends ModularUIContainerMenu implements SyncMenu {

    private final @Nullable ItemSyncHolder syncHolder;

    public HeldItemMenu(int containerId, Inventory inventory, HeldItemUIHolderContext holder) {
        super(UIRegistry.MENU.get(), containerId, inventory, holder);
        syncHolder = holder.getSyncHolder();
        if (syncHolder != null) syncHolder.setMenuId(containerId);
    }

    @Override
    public @Nullable ISyncManaged getSyncManaged() {
        return syncHolder;
    }

    @Override
    public void broadcastChanges() {
        super.broadcastChanges();
        if (inventory.player instanceof ServerPlayer player) syncToPlayer(player, containerId);
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        if (!player.level().isClientSide && syncHolder != null) syncHolder.saveToStack();
    }
}
