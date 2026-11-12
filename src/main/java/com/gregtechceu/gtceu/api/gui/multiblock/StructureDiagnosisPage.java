package com.gregtechceu.gtceu.api.gui.multiblock;

import com.gregtechceu.gtceu.api.gui.GuiTextures;
import com.gregtechceu.gtceu.api.gui.fancy.FancyMachineUIWidget;
import com.gregtechceu.gtceu.api.gui.fancy.IFancyUIProvider;
import com.gregtechceu.gtceu.api.gui.texture.IGuiTexture;
import com.gregtechceu.gtceu.api.machine.multiblock.MultiblockControllerMachine;
import com.gregtechceu.gtceu.api.multiblock.diagnosis.DiagnosisSelection;

import com.lowdragmc.lowdraglib2.gui.ui.UIElement;

import net.minecraft.network.chat.Component;

public final class StructureDiagnosisPage implements IFancyUIProvider {

    private final MultiblockControllerMachine controller;

    public StructureDiagnosisPage(MultiblockControllerMachine controller) {
        this.controller = controller;
    }

    @Override
    public UIElement createUIWidget(FancyMachineUIWidget shell) {
        return new StructureDiagnosisPanel(controller, shell.getOpeningPlayer(),
                () -> shell.getHolder().getMachine() == controller && !controller.isRemoved(),
                name -> new DiagnosisSelection(), true);
    }

    @Override
    public IGuiTexture getTabIcon() {
        return GuiTextures.itemStack(controller.getDefinition().getItem());
    }

    @Override
    public Component getTitle() {
        return Component.translatable("gtpm.multiblock.diagnosis.title");
    }

    @Override
    public int getPageWidth() {
        return 300;
    }

    @Override
    public int getPageHeight() {
        return 200;
    }
}
