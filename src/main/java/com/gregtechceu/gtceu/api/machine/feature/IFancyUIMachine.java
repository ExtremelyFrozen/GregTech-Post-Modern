package com.gregtechceu.gtceu.api.machine.feature;

import com.gregtechceu.gtceu.api.capability.IControllable;
import com.gregtechceu.gtceu.api.gui.GuiTextures;
import com.gregtechceu.gtceu.api.gui.factory.MachineUIHolder;
import com.gregtechceu.gtceu.api.gui.fancy.ConfiguratorPanel;
import com.gregtechceu.gtceu.api.gui.fancy.FancyMachineUIWidget;
import com.gregtechceu.gtceu.api.gui.fancy.IFancyTooltip;
import com.gregtechceu.gtceu.api.gui.fancy.IFancyUIProvider;
import com.gregtechceu.gtceu.api.gui.fancy.TabsWidget;
import com.gregtechceu.gtceu.api.gui.fancy.TooltipsPanel;
import com.gregtechceu.gtceu.api.gui.texture.IGuiTexture;
import com.gregtechceu.gtceu.api.machine.fancyconfigurator.AutoOutputConfigurator;
import com.gregtechceu.gtceu.api.machine.fancyconfigurator.BatchModeConfigurator;
import com.gregtechceu.gtceu.api.machine.fancyconfigurator.CircuitFancyConfigurator;
import com.gregtechceu.gtceu.api.machine.fancyconfigurator.CombinedDirectionalFancyConfigurator;
import com.gregtechceu.gtceu.api.machine.fancyconfigurator.DistinctPartConfigurator;
import com.gregtechceu.gtceu.api.machine.fancyconfigurator.MachineModeFancyConfigurator;
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
public interface IFancyUIMachine extends IMachineFeature, FancyActionMachine, IUIMachine,
                                 IFancyUIProvider {

    /**
     * Builds the machine's concrete LDLib2 Fancy page body.
     *
     * <p>
     * Implementations provide the real page element explicitly so the migration does not install a placeholder or
     * silently preserve the old {@code SceneWidget} preview path.
     */
    @Override
    UIElement createUIWidget(FancyMachineUIWidget shell);

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
        return UI.of(new FancyMachineUIWidget(this, player.getInventory(), holder, getPageWidth(),
                getPageHeight(), createContextualSubTabs(player, holder)));
    }

    /**
     * Creates side pages that must capture the player and validated holder for this particular menu opening.
     */
    default List<IFancyUIProvider> createContextualSubTabs(Player player, MachineUIHolder holder) {
        return List.of(new CombinedDirectionalFancyConfigurator(self(), player, holder));
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
    default void attachSideTabs(TabsWidget tabs) {
        if (this instanceof IRecipeLogicMachine recipeLogicMachine &&
                recipeLogicMachine.getRecipeTypes().length > 1) {
            tabs.attachSubTab(new MachineModeFancyConfigurator(recipeLogicMachine));
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
            configuratorPanel.attachConfigurators(new CircuitFancyConfigurator(circuitHolder,
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
    default void attachTooltips(TooltipsPanel tooltipsPanel) {
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
