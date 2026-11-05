package com.gregtechceu.gtceu.api.machine.fancyconfigurator;

import com.gregtechceu.gtceu.api.gui.GuiTextures;
import com.gregtechceu.gtceu.api.gui.fancy.ConfiguratorPanel;
import com.gregtechceu.gtceu.api.gui.fancy.IFancyConfiguratorButton;
import com.gregtechceu.gtceu.api.machine.feature.multiblock.DistinctPart;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;

import java.util.List;

/**
 * LDLib2 Fancy configurator helper for multiblock distinct bus mode.
 *
 * <p>
 * The button updates the annotated distinct field on the client and flushes the owning machine's field changes.
 */
public final class DistinctPartConfigurator {

    private DistinctPartConfigurator() {}

    /**
     * Attaches a distinct part toggle to a migrated LDLib2 Fancy configurator panel.
     */
    public static void attachConfigurators(ConfiguratorPanel configuratorPanel, DistinctPart part) {
        configuratorPanel.attachConfigurators(createDistinctConfigurator(configuratorPanel, part));
    }

    static IFancyConfiguratorButton.Toggle createDistinctConfigurator(
                                                                      ConfiguratorPanel configuratorPanel,
                                                                      DistinctPart part) {
        return new IFancyConfiguratorButton.Toggle(
                GuiTextures.BUTTON_DISTINCT_BUSES.getSubTexture(0, 0.5, 1, 0.5),
                GuiTextures.BUTTON_DISTINCT_BUSES.getSubTexture(0, 0, 1, 0.5),
                part::isDistinct,
                (event, pressed) -> {
                    var machine = configuratorPanel.getHolder().getMachine();
                    if (machine != null && machine.isRemote()) {
                        part.setDistinct(pressed);
                        machine.sendServerSyncChanges();
                        event.stopImmediatePropagation();
                        event.hasHandler = true;
                    }
                })
                .setTooltipsSupplier(pressed -> List.of(
                        Component.translatable("gtpm.multiblock.universal.distinct")
                                .setStyle(Style.EMPTY.withColor(ChatFormatting.YELLOW))
                                .append(Component.translatable(pressed ? "gtpm.multiblock.universal.distinct.yes" :
                                        "gtpm.multiblock.universal.distinct.no"))));
    }
}
