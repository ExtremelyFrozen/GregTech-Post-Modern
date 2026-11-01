package com.gregtechceu.gtceu.integration.emi.multipage;

import com.gregtechceu.gtceu.api.gui.widget.PatternPreviewWidget;
import com.gregtechceu.gtceu.api.machine.MultiblockMachineDefinition;
import com.gregtechceu.gtceu.api.machine.multiblock.MultiblockControllerMachine;

import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvent;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEventDispatcher;
import com.lowdragmc.lowdraglib2.integration.xei.emi.EMIUIEvents;
import com.lowdragmc.lowdraglib2.integration.xei.emi.ModularUIEMIRecipe;
import com.lowdragmc.lowdraglib2.integration.xei.emi.handler.EMIRecipeIngredientHandler;

import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.widget.SlotWidget;
import dev.emi.emi.api.widget.WidgetHolder;
import it.unimi.dsi.fastutil.objects.ObjectImmutableList;
import it.unimi.dsi.fastutil.objects.ObjectList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.ref.WeakReference;
import java.util.List;

public class MultiblockInfoEmiRecipe extends ModularUIEMIRecipe {

    private final MultiblockMachineDefinition definition;
    private WeakReference<ModularUI> displayedUI = new WeakReference<>(null);

    public MultiblockInfoEmiRecipe(MultiblockMachineDefinition definition) {
        super(ignored -> PatternPreviewWidget.createModularUI(definition));
        this.definition = definition;
    }

    @Override
    public void addWidgets(@NotNull WidgetHolder widgets) {
        super.addWidgets(widgets);
        widgets.add(new SlotWidget(EmiIngredient.of(getOutputs()),
                PatternPreviewWidget.WIDTH - 22, 4)
                .recipeContext(this)
                .drawBack(false));
    }

    @Override
    public EmiRecipeCategory getCategory() {
        return MultiblockInfoEmiCategory.CATEGORY;
    }

    @Override
    public @Nullable ResourceLocation getId() {
        return definition.getId().withPrefix("/");
    }

    @Override
    public @NotNull ObjectList<EmiStack> getOutputs() {
        return definition.getStructureOrder().stream().map(name -> {
            var stack = definition.asStack();
            if (!MultiblockControllerMachine.DEFAULT_STRUCTURE.equals(name)) {
                stack.set(DataComponents.CUSTOM_NAME,
                        stack.getHoverName().copy().append(" - ").append(Component.translatable(name)));
            }
            return EmiStack.of(stack);
        }).collect(ObjectImmutableList.toList());
    }

    @Override
    public @NotNull ModularUI getModularUI() {
        var ui = super.getModularUI();
        displayedUI = new WeakReference<>(ui);
        return ui;
    }

    @Override
    public @NotNull List<EmiIngredient> getInputs() {
        // Registration indexes stable outputs; material inputs belong to the currently configured page.
        var ui = displayedUI.get();
        if (ui == null) return ObjectList.of();
        var ingredients = new EMIRecipeIngredientHandler();
        var event = UIEvent.create(EMIUIEvents.RECIPE_INGREDIENT);
        event.target = ui.ui.rootElement;
        event.customData = ingredients;
        UIEventDispatcher.dispatchAllChildren(event);
        return ingredients.inputs;
    }

    @Override
    public @NotNull List<EmiIngredient> getCatalysts() {
        return ObjectList.of();
    }

    @Override
    public boolean supportsRecipeTree() {
        return false;
    }

    @Override
    public int getDisplayWidth() {
        return PatternPreviewWidget.WIDTH;
    }

    @Override
    public int getDisplayHeight() {
        return PatternPreviewWidget.HEIGHT;
    }
}
