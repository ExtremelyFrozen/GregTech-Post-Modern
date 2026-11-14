package com.gregtechceu.gtceu.api.machine.fancyconfigurator;

import com.gregtechceu.gtceu.api.gui.GuiTextures;
import com.gregtechceu.gtceu.api.gui.UITemplate;
import com.gregtechceu.gtceu.api.gui.element.GTIntInputElement;
import com.gregtechceu.gtceu.api.gui.element.GTLabelElement;
import com.gregtechceu.gtceu.api.gui.fancy.IFancyConfigurator;
import com.gregtechceu.gtceu.api.gui.texture.IGuiTexture;
import com.gregtechceu.gtceu.common.data.GTItems;
import com.gregtechceu.gtceu.config.ConfigHolder;
import com.gregtechceu.gtceu.integration.ae2.machine.feature.multiblock.AutoStockingPart;

import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Button.State;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.network.chat.Component;

import javax.annotation.ParametersAreNonnullByDefault;

/**
 * LDLib2 Fancy page for advanced auto-stocking item and fluid thresholds.
 *
 * <p>
 * The page updates managed fields; the owning UI synchronizes their changes.
 */
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class AutoStockingConfigurator implements IFancyConfigurator {

    private static final int CONFIGURATOR_WIDTH = 90;
    private static final int CONFIGURATOR_HEIGHT = 70;

    private final AutoStockingPart machine;

    /**
     * Creates an advanced auto-stocking configurator for the machine's annotated fields.
     */
    public AutoStockingConfigurator(AutoStockingPart machine) {
        this.machine = machine;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("gtpm.gui.adv_stocking_config.title");
    }

    @Override
    public IGuiTexture getIcon(State state) {
        return GuiTextures.itemStack(GTItems.TOOL_DATA_STICK.asStack());
    }

    @Override
    public UIElement createConfigurator() {
        UIElement root = new UIElement();
        UITemplate.setBounds(root, 0, 0, CONFIGURATOR_WIDTH, CONFIGURATOR_HEIGHT);

        String suffix = switch (machine.getStockingTarget()) {
            case ITEM -> "min_item_count";
            case FLUID -> "min_fluid_count";
        };
        root.addChild(new GTLabelElement(4, 2, 81, 10,
                "gtpm.gui.title.adv_stocking_config." + suffix, true));

        GTIntInputElement minStackSizeInput = new GTIntInputElement(4, 12, 81, 14, machine::getMinStackSize,
                machine::setMinStackSize);
        minStackSizeInput.setMin(1);
        minStackSizeInput.style(style -> style.tooltips(Component.translatable(
                "gtpm.gui.adv_stocking_config." + suffix)));
        root.addChild(minStackSizeInput);

        root.addChild(new GTLabelElement(4, 36, 81, 10,
                "gtpm.gui.title.adv_stocking_config.ticks_per_cycle", true));

        GTIntInputElement ticksPerCycleInput = new GTIntInputElement(4, 46, 81, 14, machine::getTicksPerCycle,
                machine::setTicksPerCycle);
        ticksPerCycleInput.setMin(ConfigHolder.INSTANCE.compat.ae2.updateIntervals);
        ticksPerCycleInput.style(style -> style.tooltips(Component.translatable(
                "gtpm.gui.adv_stocking_config.ticks_per_cycle")));
        root.addChild(ticksPerCycleInput);

        return root;
    }

    @Override
    public int getConfiguratorWidth() {
        return CONFIGURATOR_WIDTH;
    }

    @Override
    public int getConfiguratorHeight() {
        return CONFIGURATOR_HEIGHT;
    }
}
