package com.gregtechceu.gtceu.integration.jei.multipage;

import com.gregtechceu.gtceu.api.gui.widget.PatternPreviewWidget;
import com.gregtechceu.gtceu.api.machine.MultiblockMachineDefinition;
import com.gregtechceu.gtceu.common.item.datacomponents.TerminalMachine;

import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;

import org.jspecify.annotations.Nullable;

public class MultiblockInfoWrapper {

    public final MultiblockMachineDefinition definition;
    private @Nullable TerminalMachine machine;

    public MultiblockInfoWrapper(MultiblockMachineDefinition definition) {
        this.definition = definition;
    }

    public ModularUI createModularUI() {
        return PatternPreviewWidget.createModularUI(definition, machine, value -> machine = value);
    }
}
