package com.gregtechceu.gtceu.api.machine.fancyconfigurator;

import com.gregtechceu.gtceu.api.gui.GuiTextures;
import com.gregtechceu.gtceu.api.gui.UITemplate;
import com.gregtechceu.gtceu.api.gui.element.GTButtonElement;
import com.gregtechceu.gtceu.api.gui.element.GTItemSlotElement;
import com.gregtechceu.gtceu.api.gui.element.GTLabelElement;
import com.gregtechceu.gtceu.api.gui.fancy.IFancyConfigurator;
import com.gregtechceu.gtceu.api.gui.fancy.IFancyCustomMiddleClickAction;
import com.gregtechceu.gtceu.api.gui.fancy.IFancyCustomMouseWheelAction;
import com.gregtechceu.gtceu.api.gui.texture.IGuiTexture;
import com.gregtechceu.gtceu.api.machine.MetaMachine;
import com.gregtechceu.gtceu.api.machine.feature.IHasCircuitSlot;
import com.gregtechceu.gtceu.api.machine.trait.NotifiableItemStackHandler;
import com.gregtechceu.gtceu.common.item.behavior.IntCircuitBehaviour;
import com.gregtechceu.gtceu.config.ConfigHolder;
import com.gregtechceu.gtceu.data.lang.LangHandler;

import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.data.Horizontal;
import com.lowdragmc.lowdraglib2.gui.ui.data.Vertical;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Button.State;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvent;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;

/**
 * Fancy configurator for machine programmed-circuit slots.
 *
 * <p>
 * The configurator keeps the client-visible circuit slot responsive locally, then sends GTM machine actions for the
 * authoritative server write when the opened holder currently resolves to a remote machine.
 */
public class CircuitFancyConfigurator implements IFancyConfigurator, IFancyCustomMouseWheelAction,
                                      IFancyCustomMiddleClickAction {

    private static final int WIDTH = 174;
    private static final int HEIGHT = 132;
    private static final int SLOT_SIZE = 18;
    private static final int SELECTED_SLOT = 0;
    private static final int NO_CONFIG = -1;

    private final IHasCircuitSlot circuitMachine;

    public CircuitFancyConfigurator(IHasCircuitSlot circuitMachine) {
        this.circuitMachine = circuitMachine;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("gtpm.gui.circuit.title");
    }

    @Override
    public IGuiTexture getIcon(State state) {
        ItemStack circuitStack = circuitMachine.getCircuitInventory().getStackInSlot(SELECTED_SLOT);
        if (IntCircuitBehaviour.isIntegratedCircuit(circuitStack)) {
            return GuiTextures.itemStack(circuitStack);
        }
        return GuiTextures.group(GuiTextures.itemStack(IntCircuitBehaviour.stack(0)),
                GuiTextures.itemStack(Items.BARRIER));
    }

    @Override
    public int getConfiguratorWidth() {
        return WIDTH;
    }

    @Override
    public int getConfiguratorHeight() {
        return HEIGHT;
    }

    @Override
    public UIElement createConfigurator() {
        UIElement root = UITemplate.setBounds(new UIElement(), 0, 0, WIDTH, HEIGHT);

        GTLabelElement label = new GTLabelElement(9, 8, 166, 10,
                "Programmed Circuit Configuration", false);
        label.textStyle(style -> style
                .textColor(0x404040)
                .textShadow(false)
                .textAlignHorizontal(Horizontal.LEFT)
                .textAlignVertical(Vertical.CENTER));
        root.addChild(label);

        int selectedSlotX = (WIDTH - SLOT_SIZE) / 2;
        boolean canModifySlot = !ConfigHolder.INSTANCE.machines.ghostCircuit;
        GTItemSlotElement selectedSlot = new GTItemSlotElement(circuitMachine.getCircuitInventory().storage,
                SELECTED_SLOT);
        selectedSlot.setBackgroundTexture(GuiTextures.group(GuiTextures.SLOT, GuiTextures.INT_CIRCUIT_OVERLAY))
                .setCanPlace(stack -> canModifySlot)
                .setCanTake(player -> canModifySlot);
        UITemplate.setBounds(selectedSlot, selectedSlotX, 20, SLOT_SIZE, SLOT_SIZE);
        root.addChild(selectedSlot);

        if (ConfigHolder.INSTANCE.machines.ghostCircuit) {
            GTButtonElement clearButton = new GTButtonElement(selectedSlotX, 20, SLOT_SIZE, SLOT_SIZE,
                    IGuiTexture.EMPTY, event -> setMachineCircuitConfiguration(NO_CONFIG));
            clearButton.noText();
            root.addChild(clearButton);
        }

        int configuration = 0;
        for (int x = 0; x <= 2; x++) {
            for (int y = 0; y <= 8; y++) {
                root.addChild(createCircuitButton(configuration, 5 + SLOT_SIZE * y, 48 + SLOT_SIZE * x));
                configuration++;
            }
        }
        for (int x = 0; x <= 5; x++) {
            int finalConfiguration = x + 27;
            root.addChild(createCircuitButton(finalConfiguration, 5 + SLOT_SIZE * x, 102));
        }
        return root;
    }

    @Override
    public List<Component> getTooltips() {
        List<Component> tooltips = new ArrayList<>(IFancyConfigurator.super.getTooltips());
        tooltips.addAll(LangHandler.getMultiLang("gtpm.gui.configurator_slot.tooltip"));
        return tooltips;
    }

    @Override
    public boolean mouseWheelMove(UIEvent event) {
        if (event.deltaY == 0) {
            return false;
        }
        if (!ConfigHolder.INSTANCE.machines.ghostCircuit &&
                circuitMachine.getCircuitInventory().getStackInSlot(SELECTED_SLOT).isEmpty()) {
            return false;
        }

        setMachineCircuitConfiguration(getNextValue(event.deltaY > 0));
        return true;
    }

    @Override
    public void onMiddleClick(UIEvent event) {
        setMachineCircuitConfiguration(NO_CONFIG);
    }

    private GTButtonElement createCircuitButton(int configuration, int x, int y) {
        IGuiTexture texture = GuiTextures.group(GuiTextures.SLOT,
                GuiTextures.itemStack(IntCircuitBehaviour.stack(configuration)).scale(16f / SLOT_SIZE));
        GTButtonElement button = new GTButtonElement(x, y, SLOT_SIZE, SLOT_SIZE, texture,
                event -> setCircuitButtonConfiguration(configuration));
        button.noText();
        return button;
    }

    private void setCircuitButtonConfiguration(int configuration) {
        if (writeCircuitButtonConfiguration(circuitMachine.getCircuitInventory(), configuration)) {
            sendCircuitConfiguration(configuration);
        }
    }

    private void setMachineCircuitConfiguration(int configuration) {
        writeMachineCircuitConfiguration(circuitMachine.getCircuitInventory(), configuration);
        sendCircuitConfiguration(configuration);
    }

    private void sendCircuitConfiguration(int configuration) {
        var machine = (MetaMachine) circuitMachine;
        if (machine.isRemote()) {
            machine.rpcToServer("configureCircuit", configuration);
        }
    }

    private int getNextValue(boolean increment) {
        ItemStack currentStack = circuitMachine.getCircuitInventory().getStackInSlot(SELECTED_SLOT);
        int currentValue = IntCircuitBehaviour.getCircuitConfiguration(currentStack);
        if (increment) {
            if (currentValue == IntCircuitBehaviour.CIRCUIT_MAX) {
                return 0;
            }
            if (currentStack.isEmpty()) {
                return 1;
            }
            return currentValue + 1;
        }
        if (currentStack.isEmpty() || (currentValue == 0 && !ConfigHolder.INSTANCE.machines.ghostCircuit)) {
            return IntCircuitBehaviour.CIRCUIT_MAX;
        }
        if (currentValue == 1 && ConfigHolder.INSTANCE.machines.ghostCircuit) {
            return NO_CONFIG;
        }
        return currentValue - 1;
    }

    private static boolean writeCircuitButtonConfiguration(NotifiableItemStackHandler circuitInventory,
                                                           int configuration) {
        ItemStack stack = circuitInventory.getStackInSlot(SELECTED_SLOT).copy();
        if (IntCircuitBehaviour.isIntegratedCircuit(stack)) {
            IntCircuitBehaviour.setCircuitConfiguration(stack, configuration);
            circuitInventory.setStackInSlot(SELECTED_SLOT, stack);
            return true;
        }
        if (ConfigHolder.INSTANCE.machines.ghostCircuit) {
            circuitInventory.setStackInSlot(SELECTED_SLOT, IntCircuitBehaviour.stack(configuration));
            return true;
        }
        return false;
    }

    public static void writeMachineCircuitConfiguration(NotifiableItemStackHandler circuitInventory,
                                                        int configuration) {
        if (configuration == NO_CONFIG) {
            if (ConfigHolder.INSTANCE.machines.ghostCircuit ||
                    circuitInventory.getStackInSlot(SELECTED_SLOT).isEmpty()) {
                circuitInventory.setStackInSlot(SELECTED_SLOT, ItemStack.EMPTY);
            } else {
                circuitInventory.setStackInSlot(SELECTED_SLOT, IntCircuitBehaviour.stack(0));
            }
            return;
        }
        if (ConfigHolder.INSTANCE.machines.ghostCircuit ||
                !circuitInventory.getStackInSlot(SELECTED_SLOT).isEmpty()) {
            circuitInventory.setStackInSlot(SELECTED_SLOT, IntCircuitBehaviour.stack(configuration));
        }
    }
}
