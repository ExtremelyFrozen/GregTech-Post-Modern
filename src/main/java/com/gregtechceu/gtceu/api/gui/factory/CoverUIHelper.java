package com.gregtechceu.gtceu.api.gui.factory;

import com.gregtechceu.gtceu.api.cover.CoverBehavior;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import java.util.function.BooleanSupplier;

/**
 * Centralizes cover UI opening through GTM's LDLib2 cover holder.
 */
public final class CoverUIHelper {

    private CoverUIHelper() {}

    /**
     * Opens the UI for a cover attached to a block face.
     *
     * @return {@code true} when the menu was opened by the current bridge implementation.
     */
    public static boolean open(CoverBehavior cover, ServerPlayer player) {
        return open(cover, player, cover.coverHolder.getBlockPos(), () -> true);
    }

    /**
     * Opens a cover UI whose interaction distance is anchored to another validated server position.
     *
     * <p>
     * The anchor validity belongs to this menu opening and is rechecked for every action. The client never supplies
     * the authoritative anchor or its validity.
     */
    public static boolean open(CoverBehavior cover, ServerPlayer player, BlockPos interactionAnchor,
                               BooleanSupplier interactionAnchorValid) {
        if (cover instanceof CoverUIProvider uiProvider) {
            CoverUIHolder holder = new CoverUIHolder(
                    player, cover, interactionAnchor, interactionAnchorValid);
            if (holder.isInteractionAnchorValid() && uiProvider.canCreateUI(player, holder) &&
                    holder.isInteractionAnchorValid()) {
                return UIRegistry.open(player, UIRegistry.COVER, holder);
            }
        }
        return false;
    }

    /**
     * Returns whether the cover exposes an LDLib2 cover UI route.
     */
    public static boolean hasUI(CoverBehavior cover) {
        return cover instanceof CoverUIProvider;
    }

    /**
     * Returns whether the cover can open its LDLib2 menu for the supplied player.
     */
    public static boolean canOpen(CoverBehavior cover, Player player) {
        if (cover instanceof CoverUIProvider uiProvider) {
            CoverUIHolder holder = new CoverUIHolder(player, cover);
            return uiProvider.canCreateUI(player, holder);
        }
        return false;
    }
}
