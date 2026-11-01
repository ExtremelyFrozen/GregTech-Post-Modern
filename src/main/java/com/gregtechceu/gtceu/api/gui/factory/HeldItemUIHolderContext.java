package com.gregtechceu.gtceu.api.gui.factory;

import com.lowdragmc.lowdraglib2.gui.factory.IContainerUIHolder;
import com.lowdragmc.lowdraglib2.gui.holder.ModularUIContainerMenu;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UI;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

/**
 * Default GTM held item UI holder used by LDLib2 held item providers.
 */
public final class HeldItemUIHolderContext implements HeldItemUIHolder, MenuProvider, IContainerUIHolder {

    private final Player player;
    private final InteractionHand hand;
    private final ItemStack openedStack;

    public HeldItemUIHolderContext(Player player, InteractionHand hand) {
        this(player, hand, player.getItemInHand(hand));
    }

    public HeldItemUIHolderContext(Player player, InteractionHand hand, ItemStack openedStack) {
        this.player = player;
        this.hand = hand;
        this.openedStack = openedStack.copy();
    }

    static ModularUIContainerMenu read(int windowId, Inventory inventory, RegistryFriendlyByteBuf data) {
        InteractionHand hand = data.readEnum(InteractionHand.class);
        ItemStack openedStack = ItemStack.OPTIONAL_STREAM_CODEC.decode(data);
        return new ModularUIContainerMenu(UIRegistry.MENU.get(), windowId, inventory,
                new HeldItemUIHolderContext(inventory.player, hand, openedStack));
    }

    @Override
    public Player getPlayer() {
        return player;
    }

    @Override
    public InteractionHand getHand() {
        return hand;
    }

    @Override
    public ItemStack getHeld() {
        return player.getItemInHand(hand);
    }

    @Override
    public ItemStack getOpenedStack() {
        return openedStack;
    }

    @Override
    public boolean isStillValid(Player player) {
        return openedStack.getItem() instanceof HeldItemUIProvider provider &&
                provider.isUIStillValid(player, this);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable(openedStack.getDescriptionId());
    }

    @Override
    public ModularUIContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new ModularUIContainerMenu(UIRegistry.MENU.get(), containerId, inventory, this);
    }

    @Override
    public void writeClientSideData(AbstractContainerMenu menu, RegistryFriendlyByteBuf buffer) {
        buffer.writeEnum(hand);
        ItemStack.OPTIONAL_STREAM_CODEC.encode(buffer, openedStack);
    }

    @Override
    public ModularUI createUI(Player player) {
        if (!(openedStack.getItem() instanceof HeldItemUIProvider provider) ||
                !provider.canCreateUI(player, this)) {
            throw new IllegalStateException("Held item does not expose a UI for the opened stack.");
        }
        UI ui = provider.createUI(player, this);
        if (ui == null) {
            throw new IllegalStateException("Held item UI provider returned null.");
        }
        return UIRegistry.createUI(ui, player);
    }
}
