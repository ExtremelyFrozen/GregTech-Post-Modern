package com.gregtechceu.gtceu.api.gui.fancy;

import com.gregtechceu.gtceu.api.gui.texture.IGuiTexture;

import com.lowdragmc.lowdraglib2.gui.ui.UIElement;

import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.tooltip.TooltipComponent;

import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Describes a Fancy UI page that can build its page body with LDLib2 elements.
 *
 * <p>
 * This contract is the LDLib2-side entry point for the Fancy shell. It keeps page metadata, page sizing,
 * local side-tab registration, configurator registration, and tooltip registration outside concrete machine UI
 * implementations so machine pages can migrate one page at a time.
 */
public interface FancyUIProvider {

    /**
     * Builds the LDLib2 element tree for the page content area.
     *
     * @param shell shell element that owns navigation, side tabs, configurators, and tooltips.
     * @return page root element sized by {@link #getPageWidth()} and {@link #getPageHeight()}.
     */
    UIElement createMainPage(FancyMachineUI shell);

    /**
     * Returns the icon rendered in the title bar and tab buttons.
     */
    IGuiTexture getTabIcon();

    /**
     * Returns the title rendered in the title bar.
     */
    Component getTitle();

    /**
     * Returns the page content width used by the fixed-pixel Fancy shell.
     */
    default int getPageWidth() {
        return 176;
    }

    /**
     * Returns the page content height used by the fixed-pixel Fancy shell.
     */
    default int getPageHeight() {
        return 166;
    }

    /**
     * Registers local side tabs for this page's home page.
     */
    default void attachSideTabs(FancyTabs tabs) {}

    /**
     * Registers LDLib2 configurators for this page.
     */
    default void attachConfigurators(ConfiguratorPanel configuratorPanel) {}

    /**
     * Registers right-side tooltip indicators for this page.
     */
    default void attachTooltips(FancyTooltipsPanel tooltipsPanel) {}

    /**
     * Returns whether the shell should show the player inventory under this page.
     */
    default boolean hasPlayerInventory() {
        return true;
    }

    /**
     * Returns pages shown in the page switcher and home side-tab list.
     */
    default List<FancyUIProvider> getSubTabs() {
        return List.of();
    }

    /**
     * Returns tooltip text for this page's tab button.
     */
    default List<Component> getTabTooltips() {
        return List.of();
    }

    /**
     * Returns an optional custom tooltip component for this page's tab button.
     */
    @Nullable
    default TooltipComponent getTabTooltipComponent() {
        return null;
    }

    /**
     * Returns optional page switcher grouping metadata.
     */
    @Nullable
    default PageGroupingData getPageGroupingData() {
        return null;
    }

    /**
     * Groups page switcher entries and controls group ordering.
     *
     * @param groupKey            translated group key, or {@code null} for the default unlabelled group.
     * @param groupPositionWeight lower values appear earlier in the switcher.
     */
    record PageGroupingData(@Nullable String groupKey, int groupPositionWeight) {}
}
