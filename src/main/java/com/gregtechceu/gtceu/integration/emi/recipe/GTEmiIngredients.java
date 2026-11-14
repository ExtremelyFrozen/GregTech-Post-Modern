package com.gregtechceu.gtceu.integration.emi.recipe;

import com.gregtechceu.gtceu.api.capability.recipe.CWURecipeCapability;
import com.gregtechceu.gtceu.api.capability.recipe.EURecipeCapability;
import com.gregtechceu.gtceu.api.capability.recipe.FluidRecipeCapability;
import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.capability.recipe.ItemRecipeCapability;
import com.gregtechceu.gtceu.api.capability.recipe.RecipeCapability;
import com.gregtechceu.gtceu.api.recipe.GTRecipeDefinition;
import com.gregtechceu.gtceu.api.recipe.RecipeHelper;
import com.gregtechceu.gtceu.api.recipe.content.Content;
import com.gregtechceu.gtceu.api.recipe.content.ContentListMap;
import com.gregtechceu.gtceu.api.recipe.ingredient.IntCircuitIngredient;
import com.gregtechceu.gtceu.common.recipe.condition.ResearchCondition;
import com.gregtechceu.gtceu.config.ConfigHolder;
import com.gregtechceu.gtceu.integration.xei.GTRecipeUI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvent;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEventDispatcher;
import com.lowdragmc.lowdraglib2.integration.xei.emi.EMIUIEvents;
import com.lowdragmc.lowdraglib2.integration.xei.emi.handler.EMIRecipeIngredientHandler;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectList;
import org.jetbrains.annotations.ApiStatus;

import java.util.List;

/** Ingredient projection for one recipe display, independent of the UI used to render it. */
@ApiStatus.Internal
public record GTEmiIngredients(ObjectList<EmiIngredient> inputs, ObjectList<EmiIngredient> catalysts,
                               ObjectList<EmiStack> outputs) {

    public static GTEmiIngredients create(GTRecipeDefinition recipe) {
        GTEmiIngredients ingredients = new GTEmiIngredients(new ObjectArrayList<>(), new ObjectArrayList<>(),
                new ObjectArrayList<>());
        boolean needsUI = ingredients.addContents(recipe.inputs, recipe.tickInputs, IO.IN);
        needsUI |= ingredients.addContents(recipe.outputs, recipe.tickOutputs, IO.OUT);
        ingredients.addResearch(recipe);
        if (needsUI) {
            ingredients.addCustomCapabilities(recipe);
        }
        return ingredients;
    }

    private boolean addContents(ContentListMap contents, ContentListMap tickContents, IO io) {
        boolean needsUI = false;
        var regular = contents.fastIterator();
        while (regular.hasNext()) {
            var entry = regular.next();
            RecipeCapability<?> capability = entry.getKey();
            needsUI |= addContents(capability, entry.getValue(), io);
            needsUI |= addContents(capability, tickContents.getOrDefault(capability, ObjectList.of()), io);
        }
        var ticking = tickContents.fastIterator();
        while (ticking.hasNext()) {
            var entry = ticking.next();
            if (!contents.containsKey(entry.getKey())) {
                needsUI |= addContents(entry.getKey(), entry.getValue(), io);
            }
        }
        return needsUI;
    }

    private boolean addContents(RecipeCapability<?> capability, List<Content> contents, IO io) {
        if (capability == ItemRecipeCapability.CAP) {
            for (Content content : contents) {
                var ingredient = ItemRecipeCapability.CAP.of(content.content);
                boolean catalyst = content.chance == 0 ||
                        ingredient.ingredient().getCustomIngredient() instanceof IntCircuitIngredient;
                ObjectList<? super EmiStack> target = io == IO.OUT ? outputs : catalyst ? catalysts : inputs;
                for (var stack : ItemRecipeCapability.mapItem(ingredient).getStacks()) {
                    target.add(EmiStack.of(stack));
                }
            }
            return false;
        }
        if (capability == FluidRecipeCapability.CAP) {
            for (Content content : contents) {
                var ingredient = FluidRecipeCapability.CAP.of(content.content);
                ObjectList<? super EmiStack> target = io == IO.OUT ? outputs : content.chance == 0 ? catalysts : inputs;
                for (var stack : FluidRecipeCapability.mapFluid(ingredient).getStacks()) {
                    target.add(EmiStack.of(stack.getFluid(), stack.getComponentsPatch(), stack.getAmount()));
                }
            }
            return false;
        }
        return capability != EURecipeCapability.CAP && capability != CWURecipeCapability.CAP;
    }

    private void addResearch(GTRecipeDefinition recipe) {
        if (recipe.recipeType.isHasResearchSlot() && ConfigHolder.INSTANCE.machines.enableResearch) {
            for (var condition : recipe.conditions) {
                if (condition instanceof ResearchCondition research) {
                    for (var stack : ItemRecipeCapability.mapResearchItems(research, recipe.recipeType)) {
                        catalysts.add(EmiStack.of(stack));
                    }
                    break;
                }
            }
        }
    }

    private void addCustomCapabilities(GTRecipeDefinition recipe) {
        int tier = RecipeHelper.getRecipeEUtTier(recipe);
        UIElement root = new UIElement();
        GTRecipeUI.createXEIIngredientElements(recipe, tier, tier,
                capability -> capability != ItemRecipeCapability.CAP && capability != FluidRecipeCapability.CAP &&
                        capability != EURecipeCapability.CAP && capability != CWURecipeCapability.CAP)
                .forEach(root::addChild);
        EMIRecipeIngredientHandler ingredients = new EMIRecipeIngredientHandler();
        UIEvent event = UIEvent.create(EMIUIEvents.RECIPE_INGREDIENT);
        event.target = root;
        event.customData = ingredients;
        UIEventDispatcher.dispatchAllChildren(event);
        inputs.addAll(ingredients.inputs);
        catalysts.addAll(ingredients.catalysts);
        outputs.addAll(ingredients.outputs);
    }
}
