package com.gregtechceu.gtceu.integration.emi.recipe;

import com.gregtechceu.gtceu.GTCEu;
import com.gregtechceu.gtceu.api.recipe.GTRecipeDefinition;
import com.gregtechceu.gtceu.api.recipe.RecipeHelper;
import com.gregtechceu.gtceu.api.recipe.ui.GTRecipeTypeUI.RecipeUISize;
import com.gregtechceu.gtceu.integration.xei.GTRecipeUI;

import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.integration.xei.emi.ModularUIEMIRecipe;

import net.minecraft.resources.ResourceLocation;

import com.google.common.base.Suppliers;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Supplier;

/**
 * EMI recipe display for GT recipes.
 */
public class GTEmiRecipe extends ModularUIEMIRecipe {

    private final EmiRecipeCategory category;
    private final GTRecipeDefinition recipe;
    private final RecipeUISize size;
    private final Supplier<GTEmiIngredients> xeiIngredients;

    public GTEmiRecipe(GTRecipeDefinition recipe, EmiRecipeCategory category, RecipeUISize size) {
        this(recipe, category, size, new EmiTagStacks());
    }

    GTEmiRecipe(GTRecipeDefinition recipe, EmiRecipeCategory category, RecipeUISize size, EmiTagStacks tags) {
        super(GTEmiRecipe::createModularUI);
        this.category = category;
        this.recipe = recipe;
        this.size = size;
        this.xeiIngredients = Suppliers.memoize(() -> GTEmiIngredients.create(recipe, tags));
    }

    @Override
    public EmiRecipeCategory getCategory() {
        return category;
    }

    @Override
    public @Nullable ResourceLocation getId() {
        return recipe.getId();
    }

    @Override
    public int getDisplayWidth() {
        return size.width();
    }

    @Override
    public int getDisplayHeight() {
        return size.height();
    }

    @Override
    public List<EmiIngredient> getInputs() {
        return xeiIngredients.get().inputs();
    }

    @Override
    public List<EmiIngredient> getCatalysts() {
        return xeiIngredients.get().catalysts();
    }

    @Override
    public List<EmiStack> getOutputs() {
        return xeiIngredients.get().outputs();
    }

    private static ModularUI createModularUI(ModularUIEMIRecipe recipe) {
        if (recipe instanceof GTEmiRecipe gtRecipe) {
            int tier = RecipeHelper.getRecipeEUtTier(gtRecipe.recipe);
            return GTRecipeUI.createModularUI(gtRecipe.recipe, tier, tier);
        }
        GTCEu.LOGGER.error("Expected GTEmiRecipe, got {}", recipe.getClass().getName());
        throw new IllegalArgumentException("Expected GTEmiRecipe, got " + recipe.getClass().getName());
    }
}
