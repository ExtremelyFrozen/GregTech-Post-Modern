package com.gregtechceu.gtceu.api.machine.fancyconfigurator;

import com.gregtechceu.gtceu.api.gui.GuiTextures;
import com.gregtechceu.gtceu.api.gui.fancy.IFancyConfiguratorButton;
import com.gregtechceu.gtceu.api.gui.texture.IGuiTexture;
import com.gregtechceu.gtceu.api.machine.multiblock.MultiblockControllerMachine;

import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvent;

import net.minecraft.network.chat.Component;

import it.unimi.dsi.fastutil.objects.ObjectList;
import lombok.RequiredArgsConstructor;

/** Requests a full formation check from the opened controller's main page. */
@RequiredArgsConstructor
public final class StructureCheckConfigurator implements IFancyConfiguratorButton {

    private static final ObjectList<Component> TOOLTIPS = ObjectList.of(
            Component.translatable("gtpm.multiblock.check_structure"));
    private final MultiblockControllerMachine controller;

    @Override
    public IGuiTexture getIcon() {
        return GuiTextures.BUTTON_CHECK;
    }

    @Override
    public ObjectList<Component> getTooltips() {
        return TOOLTIPS;
    }

    @Override
    public void onClick(UIEvent event) {
        controller.rpcToServer("requestStructureCheck");
        event.stopImmediatePropagation();
        event.hasHandler = true;
    }
}
