package com.gregtechceu.gtceu.api.machine.fancyconfigurator;

import com.gregtechceu.gtceu.api.gui.GuiTextures;
import com.gregtechceu.gtceu.api.gui.fancy.ConfiguratorPanel;
import com.gregtechceu.gtceu.api.gui.fancy.IFancyConfiguratorButton;
import com.gregtechceu.gtceu.api.machine.feature.multiblock.BatchModeMachine;

import net.minecraft.network.chat.Component;

import java.util.List;

/**
 * LDLib2 Fancy configurator helper for multiblock batch mode.
 *
 * <p>
 * The button updates the client field; the owning UI synchronizes the change.
 */
public final class BatchModeConfigurator {

    private BatchModeConfigurator() {}

    /**
     * Attaches the batch mode toggle when the machine definition uses the batch recipe modifier.
     */
    public static void attachConfigurators(ConfiguratorPanel configuratorPanel,
                                           BatchModeMachine machine) {
        if (!machine.supportsBatchMode()) {
            return;
        }

        configuratorPanel.attachConfigurators(createBatchModeConfigurator(machine));
    }

    static IFancyConfiguratorButton.Toggle createBatchModeConfigurator(
                                                                       BatchModeMachine machine) {
        return new IFancyConfiguratorButton.Toggle(
                GuiTextures.BUTTON_BATCH.getSubTexture(0, 0, 1, 0.5),
                GuiTextures.BUTTON_BATCH.getSubTexture(0, 0.5, 1, 0.5),
                machine::isBatchEnabled,
                (event, pressed) -> {
                    machine.setBatchEnabled(pressed);
                    event.stopImmediatePropagation();
                    event.hasHandler = true;
                })
                .setTooltipsSupplier(pressed -> List.of(Component.translatable(
                        "gtpm.machine.batch_" + (pressed ? "enabled" : "disabled"))));
    }
}
