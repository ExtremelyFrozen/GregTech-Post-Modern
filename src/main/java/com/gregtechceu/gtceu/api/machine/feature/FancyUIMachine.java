package com.gregtechceu.gtceu.api.machine.feature;

import com.gregtechceu.gtceu.api.capability.IControllable;
import com.gregtechceu.gtceu.api.gui.GuiTextures;
import com.gregtechceu.gtceu.api.gui.factory.MachineUIHolder;
import com.gregtechceu.gtceu.api.gui.factory.MachineUIProvider;
import com.gregtechceu.gtceu.api.gui.fancy.ConfiguratorPanel;
import com.gregtechceu.gtceu.api.gui.fancy.FancyMachineUI;
import com.gregtechceu.gtceu.api.gui.fancy.FancyTabs;
import com.gregtechceu.gtceu.api.gui.fancy.FancyTooltipsPanel;
import com.gregtechceu.gtceu.api.gui.fancy.FancyUIProvider;
import com.gregtechceu.gtceu.api.gui.fancy.IFancyTooltip;
import com.gregtechceu.gtceu.api.gui.texture.IGuiTexture;
import com.gregtechceu.gtceu.api.machine.fancyconfigurator.AutoOutputConfigurator;
import com.gregtechceu.gtceu.api.machine.fancyconfigurator.BatchModeConfigurator;
import com.gregtechceu.gtceu.api.machine.fancyconfigurator.CircuitConfigurator;
import com.gregtechceu.gtceu.api.machine.fancyconfigurator.DirectionalConfigurator;
import com.gregtechceu.gtceu.api.machine.fancyconfigurator.DistinctPartConfigurator;
import com.gregtechceu.gtceu.api.machine.fancyconfigurator.MachineModeConfigurator;
import com.gregtechceu.gtceu.api.machine.fancyconfigurator.VoidingModeConfigurator;
import com.gregtechceu.gtceu.api.machine.fancyconfigurator.WorkingEnabledConfigurator;
import com.gregtechceu.gtceu.api.machine.feature.multiblock.BatchModeMachine;
import com.gregtechceu.gtceu.api.machine.feature.multiblock.DistinctPart;
import com.gregtechceu.gtceu.common.machine.trait.AutoOutputTrait;

import com.lowdragmc.lowdraglib2.gui.ui.UI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

import java.util.List;

/**
 * Bridges machine-owned Fancy pages into the LDLib2 machine UI opening path.
 *
 * <p>
 * This interface defines the machine-owned page contract used by the LDLib2 Fancy shell.
 */
public interface FancyUIMachine extends IMachineFeature, FancyActionMachine, MachineUIProvider,
                                FancyUIProvider {

    /**
     * Builds the machine's concrete LDLib2 Fancy page body.
     *
     * <p>
     * Implementations provide the real page element explicitly so the migration does not install a placeholder or
     * silently preserve the old {@code SceneWidget} preview path.
     */
    @Override
    UIElement createMainPage(FancyMachineUI shell);

    /**
     * Accepts the opened machine holder only while it still resolves to this machine instance.
     */
    @Override
    default boolean canCreateUI(Player player, MachineUIHolder holder) {
        return holder.getMachine() == self();
    }

    /**
     * Wraps this machine page provider in the shared LDLib2 Fancy shell.
     */
    @Override
    default UI createUI(Player player, MachineUIHolder holder) {
        return UI.of(new FancyMachineUI(this, player.getInventory(), holder, getPageWidth(),
                getPageHeight(), createContextualSubTabs(player, holder)));
    }

    /**
     * Creates side pages that must capture the player and validated holder for this particular menu opening.
     */
    default List<FancyUIProvider> createContextualSubTabs(Player player, MachineUIHolder holder) {
        return List.of(new DirectionalConfigurator(self(), player, holder));
    }

    /**
     * Uses the machine definition item as the title bar and tab icon.
     */
    @Override
    default IGuiTexture getTabIcon() {
        return GuiTextures.itemStack(self().getDefinition().getItem());
    }

    /**
     * Registers local LDLib2 side tabs for machine metadata pages.
     */
    @Override
    default void attachSideTabs(FancyTabs tabs) {
        if (this instanceof IRecipeLogicMachine recipeLogicMachine &&
                recipeLogicMachine.getRecipeTypes().length > 1) {
            tabs.attachSubTab(new MachineModeConfigurator(recipeLogicMachine));
        }
    }

    /**
     * Registers common LDLib2 machine configurator buttons.
     */
    @Override
    default void attachConfigurators(ConfiguratorPanel configuratorPanel) {
        if (this instanceof IVoidable voidable) {
            VoidingModeConfigurator.attachConfigurators(configuratorPanel, voidable);
        }
        if (this instanceof BatchModeMachine batchModeMachine) {
            BatchModeConfigurator.attachConfigurators(configuratorPanel, batchModeMachine);
        }
        if (this instanceof IControllable controllable) {
            configuratorPanel.attachConfigurators(new WorkingEnabledConfigurator(controllable,
                    configuratorPanel.getHolder()));
        }
        AutoOutputTrait autoOutputTrait = self().getTrait(AutoOutputTrait.TYPE);
        if (autoOutputTrait != null) {
            AutoOutputConfigurator.attachConfigurators(configuratorPanel, autoOutputTrait);
        }
        if (this instanceof IHasCircuitSlot circuitHolder && circuitHolder.isCircuitSlotEnabled()) {
            configuratorPanel.attachConfigurators(new CircuitConfigurator(circuitHolder,
                    configuratorPanel.getHolder()));
        }
        if (this instanceof DistinctPart distinctPart) {
            DistinctPartConfigurator.attachConfigurators(configuratorPanel, distinctPart);
        }
    }

    /**
     * Registers the machine tooltip and all trait-provided Fancy tooltips with the LDLib2 tooltip panel.
     */
    @Override
    default void attachTooltips(FancyTooltipsPanel tooltipsPanel) {
        tooltipsPanel.attachTooltips(self());
        self().getTraitHolder().getAllTraits().stream()
                .filter(IFancyTooltip.class::isInstance)
                .map(IFancyTooltip.class::cast)
                .forEach(tooltipsPanel::attachTooltips);
    }

    /**
     * Shows the machine's translated definition name when hovering its tab.
     */
    @Override
    default List<Component> getTabTooltips() {
        return List.of(Component.translatable(self().getDefinition().getDescriptionId()));
    }

    /**
     * Uses the machine's translated definition name as the Fancy title.
     */
    @Override
    default Component getTitle() {
        return Component.translatable(self().getDefinition().getDescriptionId());
    }
}
