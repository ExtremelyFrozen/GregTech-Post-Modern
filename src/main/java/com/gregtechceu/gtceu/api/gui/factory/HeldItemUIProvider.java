package com.gregtechceu.gtceu.api.gui.factory;

import com.lowdragmc.lowdraglib2.gui.ui.UI;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * Creates an LDLib2 held item UI through GTM's stable held item holder contract.
 *
 * <p>
 * The common registry transports the opened hand and stack snapshot through {@link HeldItemUIHolder}, so item
 * business code does not depend on an LDLib menu type.
 */
public interface HeldItemUIProvider {

    /**
     * Returns whether this item should use LDLib2 for the provided held item context.
     *
     * <p>
     * Composite items can implement the provider globally while using this method to route only components that
     * own a held item UI for the opened stack.
     */
    default boolean canCreateUI(Player player, HeldItemUIHolder holder) {
        return true;
    }

    /**
     * Returns whether the currently held stack still belongs to this opened LDLib2 UI.
     *
     * <p>
     * The default keeps strict stack matching. Mutable item UIs can override this through their component provider
     * and validate action payloads server-side.
     */
    default boolean isUIStillValid(Player player, HeldItemUIHolder holder) {
        return ItemStack.matches(holder.getHeld(), holder.getOpenedStack());
    }

    /**
     * Builds the LDLib2 held item UI tree for the provided player and opened item identity.
     *
     * @param player player building the UI in the current runtime.
     * @param holder GTM holder that exposes the player, hand, current stack, and opened stack snapshot.
     * @return non-null LDLib2 UI tree.
     */
    UI createUI(Player player, HeldItemUIHolder holder);
}
