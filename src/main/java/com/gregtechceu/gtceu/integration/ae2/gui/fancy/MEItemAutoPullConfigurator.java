package com.gregtechceu.gtceu.integration.ae2.gui.fancy;

import com.gregtechceu.gtceu.api.gui.GuiTextures;
import com.gregtechceu.gtceu.api.gui.fancy.IFancyConfiguratorButton;
import com.gregtechceu.gtceu.integration.ae2.machine.MEStockingBusPartMachine;

import net.minecraft.network.chat.Component;

import java.util.List;

public final class MEItemAutoPullConfigurator extends IFancyConfiguratorButton.Toggle {

    public MEItemAutoPullConfigurator(MEStockingBusPartMachine machine) {
        super(GuiTextures.BUTTON_AUTO_PULL.getSubTexture(0, 0, 1, 0.5),
                GuiTextures.BUTTON_AUTO_PULL.getSubTexture(0, 0.5, 1, 0.5),
                machine::isAutoPull, (event, pressed) -> {
                    machine.setAutoPull(pressed);
                    event.stopImmediatePropagation();
                    event.hasHandler = true;
                });
        setTooltipsSupplier(pressed -> List.of(Component.translatable("gtpm.gui.me_bus.auto_pull_button")));
    }
}
