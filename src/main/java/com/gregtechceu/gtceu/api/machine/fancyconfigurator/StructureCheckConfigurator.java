package com.gregtechceu.gtceu.api.machine.fancyconfigurator;

import com.gregtechceu.gtceu.api.gui.GuiTextures;
import com.gregtechceu.gtceu.api.gui.fancy.IFancyConfiguratorButton;
import com.gregtechceu.gtceu.api.gui.texture.IGuiTexture;
import com.gregtechceu.gtceu.api.machine.multiblock.MultiblockControllerMachine;

import com.lowdragmc.lowdraglib2.gui.ui.elements.Button.State;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvent;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

import it.unimi.dsi.fastutil.objects.ObjectList;
import lombok.RequiredArgsConstructor;

/** Requests a full formation check from the opened controller's main page. */
@RequiredArgsConstructor
public final class StructureCheckConfigurator implements IFancyConfiguratorButton {

    private static final IGuiTexture ICON = GuiTextures.BUTTON_STRUCTURE_CHECK.getSubTexture(0, 0, 1, 0.5);
    private static final IGuiTexture PRESSED_ICON = GuiTextures.BUTTON_STRUCTURE_CHECK.getSubTexture(0, 0.5, 1, 0.5);
    private static final ObjectList<Component> TOOLTIPS = ObjectList.of(
            Component.translatable("gtpm.multiblock.check_structure"),
            Component.translatable("gtpm.multiblock.check_structure.recheck").withStyle(ChatFormatting.GRAY));
    private final MultiblockControllerMachine controller;

    @Override
    public IGuiTexture getIcon(State state) {
        return controller.isFormed() ? PRESSED_ICON : ICON;
    }

    @Override
    public ObjectList<Component> getTooltips() {
        return TOOLTIPS;
    }

    @Override
    public void onClick(UIEvent event) {
        if (event.isShiftDown() || !controller.isFormed())
            controller.rpcToServer("requestStructureCheck", event.isShiftDown());
        event.stopImmediatePropagation();
        event.hasHandler = true;
    }
}
