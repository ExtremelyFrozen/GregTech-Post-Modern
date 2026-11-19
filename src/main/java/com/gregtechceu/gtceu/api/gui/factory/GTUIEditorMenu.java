package com.gregtechceu.gtceu.api.gui.factory;

import com.gregtechceu.gtceu.GTCEu;
import com.gregtechceu.gtceu.api.gui.editor.GTUIEditor;

import com.lowdragmc.lowdraglib2.editor.ui.EditorWindow;
import com.lowdragmc.lowdraglib2.gui.factory.IContainerUIHolder;
import com.lowdragmc.lowdraglib2.gui.holder.ModularUIContainerMenu;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UI;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;

public final class GTUIEditorMenu {

    public static final ResourceLocation WINDOW_ID = GTCEu.id("gt_ui_editor");

    private GTUIEditorMenu() {}

    public static void register() {
        UIRegistry.register(WINDOW_ID, (windowId, inventory, data) -> new ModularUIContainerMenu(
                UIRegistry.MENU.get(), windowId, inventory, createHolder(inventory.player)));
    }

    public static boolean open(ServerPlayer player) {
        return UIRegistry.open(player, WINDOW_ID, new MenuProvider() {

            @Override
            public Component getDisplayName() {
                return Component.translatable(WINDOW_ID.toLanguageKey());
            }

            @Override
            public ModularUIContainerMenu createMenu(int containerId, Inventory inventory, Player menuPlayer) {
                return new ModularUIContainerMenu(UIRegistry.MENU.get(), containerId, inventory,
                        createHolder(menuPlayer));
            }
        });
    }

    static IContainerUIHolder createHolder(Player ignored) {
        return new IContainerUIHolder() {

            @Override
            public boolean isStillValid(Player player) {
                return true;
            }

            @Override
            public ModularUI createUI(Player player) {
                return player.level().isClientSide ? createClientUI() : createServerUI();
            }
        };
    }

    static ModularUI createClientUI() {
        return UIRegistry.createUI(UI.of(EditorWindow.open(WINDOW_ID, GTUIEditor::new)), null)
                .shouldCloseOnEsc(false)
                .shouldCloseOnKeyInventory(false);
    }

    static ModularUI createServerUI() {
        return UIRegistry.createUI(UI.empty(), null);
    }
}
