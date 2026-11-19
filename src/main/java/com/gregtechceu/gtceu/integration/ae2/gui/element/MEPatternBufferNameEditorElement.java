package com.gregtechceu.gtceu.integration.ae2.gui.element;

import com.gregtechceu.gtceu.api.gui.GuiTextures;
import com.gregtechceu.gtceu.api.gui.UITemplate;
import com.gregtechceu.gtceu.api.gui.element.GTButtonElement;
import com.gregtechceu.gtceu.api.gui.element.GTTextFieldElement;
import com.gregtechceu.gtceu.api.gui.texture.IGuiTexture;
import com.gregtechceu.gtceu.integration.ae2.machine.MEPatternBufferPartMachine;

import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvent;

import net.minecraft.network.chat.Component;

import lombok.Getter;

/**
 * Opening-scoped Pattern Buffer name editor that keeps draft text local until explicit confirmation.
 */
public final class MEPatternBufferNameEditorElement extends UIElement {

    private static final int WIDTH = 70;
    private static final int HEIGHT = 10;
    private static final int TEXT_WIDTH = 58;
    private static final int BUTTON_X = 60;

    private final MEPatternBufferPartMachine machine;
    @Getter
    private final GTTextFieldElement textField;
    @Getter
    private final GTButtonElement toggleButton;
    @Getter
    private boolean editing;

    public MEPatternBufferNameEditorElement(int x, int y, MEPatternBufferPartMachine machine) {
        this.machine = machine;
        UITemplate.setBounds(this, x, y, WIDTH, HEIGHT);

        textField = new GTTextFieldElement(0, 0, TEXT_WIDTH, HEIGHT)
                .setAnyString()
                .setText(machine.getCustomName(), false);
        textField.setVisible(false);
        textField.setActive(false);
        addChild(textField);

        toggleButton = new GTButtonElement(BUTTON_X, 0, HEIGHT, HEIGHT,
                editTexture(), this::toggleEditing).noText();
        toggleButton.style(style -> style.tooltips(Component.translatable("gui.gtpm.rename.desc")));
        addChild(toggleButton);
    }

    @Override
    public void screenTick() {
        if (!editing) {
            textField.setText(machine.getCustomName(), false);
        }
        super.screenTick();
    }

    private void toggleEditing(UIEvent event) {
        if (!editing) {
            if (!machine.isRemote()) {
                return;
            }
            textField.setText(machine.getCustomName(), false);
            editing = true;
            textField.setVisible(true);
            textField.setActive(true);
            toggleButton.setButtonTexture(confirmTexture());
            return;
        }

        editing = false;
        textField.setVisible(false);
        textField.setActive(false);
        toggleButton.setButtonTexture(editTexture());
        if (machine.isRemote()) {
            machine.rpcToServer("setMEPatternBufferName", textField.getText());
        }
    }

    private static IGuiTexture editTexture() {
        return GuiTextures.group(GuiTextures.VANILLA_BUTTON, GuiTextures.text("\u270e"));
    }

    private static IGuiTexture confirmTexture() {
        return GuiTextures.group(GuiTextures.VANILLA_BUTTON, GuiTextures.text("\u2714"));
    }
}
