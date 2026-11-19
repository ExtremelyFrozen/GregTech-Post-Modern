package com.gregtechceu.gtceu.api.machine.fancyconfigurator;

import com.gregtechceu.gtceu.api.gui.fancy.ConfiguratorPanel;
import com.gregtechceu.gtceu.api.machine.feature.IVoidable;

import net.minecraft.network.chat.Component;

import java.util.List;

/**
 * LDLib2 Fancy configurator helper for multiblock output voiding mode.
 *
 * <p>
 * The selector updates the client field; the owning UI synchronizes the change.
 */
public final class VoidingModeConfigurator {

    private VoidingModeConfigurator() {}

    /**
     * Attaches a voiding mode selector to a migrated LDLib2 Fancy configurator panel.
     */
    public static void attachConfigurators(ConfiguratorPanel configuratorPanel, IVoidable controller) {
        configuratorPanel.attachConfigurators(new FancySelectorConfigurator<>(
                IVoidable.VoidingMode.VALUES,
                controller::getVoidingMode,
                (event, mode) -> {
                    controller.setVoidingMode(mode);
                    event.stopImmediatePropagation();
                    event.hasHandler = true;
                })
                .setTooltip(mode -> List.of(Component.translatable("gtpm.gui.multiblock.voiding_mode"),
                        Component.translatable(mode.getTooltip()))));
    }
}
