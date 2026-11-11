package com.gregtechceu.gtceu.api.sync_system.managed;

import com.gregtechceu.gtceu.common.network.packets.PacketMenuSync;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.PacketDistributor;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Exposes a menu's annotated state to the GT field-sync and RPC transports. Implementations own their state for one
 * opening and call synchronization after their server tick. Client packets only target the currently open menu id.
 */
@NullMarked
public interface SyncMenu {

    /** Returns this opening's root, or null when the menu has no managed state. */
    @Nullable
    ISyncManaged getSyncManaged();

    /** Saves item fields and sends detected changes to the menu owner on the server game thread. */
    default void syncToPlayer(ServerPlayer player, int containerId) {
        var managed = getSyncManaged();
        if (managed == null) return;
        if (managed instanceof ItemSyncHolder item) item.saveToStack();
        var storage = managed.getSyncDataHolder();
        boolean fullSync = storage.isFullSyncPending();
        if (storage.scanAndMarkChanges(player.registryAccess())) {
            PacketDistributor.sendToPlayer(player,
                    new PacketMenuSync(containerId, fullSync, storage.getPendingChanges()));
        }
    }

    /** Sends changes to writable annotated fields from the client tick of this menu opening. */
    default void syncToServer(Player player, int containerId) {
        var managed = getSyncManaged();
        if (managed == null) return;
        var changes = managed.getSyncDataHolder().collectServerNetworkChanges(player.registryAccess());
        if (!changes.isEmpty()) {
            PacketDistributor.sendToServer(new PacketMenuSync(containerId, false, changes));
        }
    }
}
