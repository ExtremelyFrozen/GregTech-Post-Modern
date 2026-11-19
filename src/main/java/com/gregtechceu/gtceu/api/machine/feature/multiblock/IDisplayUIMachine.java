package com.gregtechceu.gtceu.api.machine.feature.multiblock;

import com.gregtechceu.gtceu.api.gui.GuiTextures;
import com.gregtechceu.gtceu.api.gui.texture.IGuiTexture;
import com.gregtechceu.gtceu.api.machine.feature.IMachineFeature;
import com.gregtechceu.gtceu.api.machine.multiblock.MultiblockControllerMachine;

import com.lowdragmc.lowdraglib2.gui.util.ClickData;

import net.minecraft.network.chat.Component;

import java.util.List;

/**
 * Supplies the status text and component actions for a multiblock controller's LDLib2 page.
 * Implementations read live machine state on the server. The menu displays the synced text.
 */
public interface IDisplayUIMachine extends IMachineFeature {

    /**
     * Appends current status lines to the caller's reusable list. Implementations must neither
     * retain the list nor mutate components after adding them.
     */
    default void addDisplayText(List<Component> textList) {
        for (var part : self().getParts()) {
            part.addMultiText(textList);
        }
    }

    default void handleDisplayClick(String componentData, ClickData clickData) {}

    default IGuiTexture getScreenTexture() {
        return GuiTextures.DISPLAY;
    }

    @Override
    default MultiblockControllerMachine self() {
        return (MultiblockControllerMachine) this;
    }
}
