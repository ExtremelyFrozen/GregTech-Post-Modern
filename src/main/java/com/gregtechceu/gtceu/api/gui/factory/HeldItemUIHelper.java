package com.gregtechceu.gtceu.api.gui.factory;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;

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
}
