package com.gregtechceu.gtceu.api.machine.fancyconfigurator;

import com.gregtechceu.gtceu.api.gui.fancy.ConfiguratorPanel;
import com.gregtechceu.gtceu.api.machine.feature.IVoidable;

import net.minecraft.network.chat.Component;

import java.util.List;

/**
 * LDLib2 Fancy configurator helper for multiblock output voiding mode.
 *
 * <p>
 * The selector updates the client-side sync field and flushes it through the GTM machine sync channel.
 */
public final class VoidingModeConfigurator {

    private VoidingModeConfigurator() {}

    /**
     * Attaches a voiding mode selector to a migrated LDLib2 Fancy configurator panel.
     */
    public static void attachConfigurators(ConfiguratorPanel configuratorPanel, IVoidable controller) {
        configuratorPanel.attachConfigurators(new SelectorConfigurator<>(
                IVoidable.VoidingMode.VALUES,
                controller::getVoidingMode,
                (event, mode) -> {
                    var machine = configuratorPanel.getHolder().getMachine();
                    if (machine == controller.self() && machine.isRemote()) {
                        controller.setVoidingMode(mode);
                        machine.sendServerSyncChanges();
                        event.stopImmediatePropagation();
                        event.hasHandler = true;
                    }
                })
                .setTooltip(mode -> List.of(Component.translatable("gtpm.gui.multiblock.voiding_mode"),
                        Component.translatable(mode.getTooltip()))));
    }
}
