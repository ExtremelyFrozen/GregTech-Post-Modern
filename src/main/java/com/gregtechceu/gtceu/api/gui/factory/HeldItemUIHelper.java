package com.gregtechceu.gtceu.api.gui.factory;

import com.gregtechceu.gtceu.api.sync_system.SyncActionData;
import com.gregtechceu.gtceu.common.network.packets.CPacketItemActionToServer;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Centralizes held item UI opening through GTM's LDLib2 held item bridge.
 */
public final class HeldItemUIHelper {

    private HeldItemUIHelper() {}

    /**
     * Opens the UI for the item currently held in {@code hand}.
     *
     * @return {@code true} when the menu was opened by the current bridge implementation.
     */
    public static boolean open(ServerPlayer player, InteractionHand hand) {
        HeldItemUIHolderContext holder = new HeldItemUIHolderContext(player, hand);
        if (holder.getOpenedStack().getItem() instanceof HeldItemUIProvider uiProvider &&
                uiProvider.canCreateUI(player, holder)) {
            return UIRegistry.open(player, UIRegistry.HELD_ITEM, holder);
        }
        return false;
    }

    /**
     * Sends a held item UI sync action to the server.
     */
    public static void sendAction(HeldItemUIHolder holder, SyncActionData action) {
        PacketDistributor
                .sendToServer(new CPacketItemActionToServer(holder.getHand(), holder.getOpenedStack(), action));
    }
}
