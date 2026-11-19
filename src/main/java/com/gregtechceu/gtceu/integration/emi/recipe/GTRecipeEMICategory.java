package com.gregtechceu.gtceu.integration.emi.recipe;

import com.gregtechceu.gtceu.GTCEu;
import com.gregtechceu.gtceu.api.gui.texture.IGuiTexture;
import com.gregtechceu.gtceu.api.machine.MachineDefinition;
import com.gregtechceu.gtceu.api.recipe.GTRecipeType;
import com.gregtechceu.gtceu.api.recipe.category.GTRecipeCategory;
import com.gregtechceu.gtceu.api.recipe.ui.GTRecipeTypeUI.RecipeUISize;
import com.gregtechceu.gtceu.api.registry.GTRegistries;
import com.gregtechceu.gtceu.common.data.GTRecipeTypes;
import com.gregtechceu.gtceu.integration.emi.GTEMIPlugin;

import net.minecraft.Util;
import net.minecraft.network.chat.Component;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.recipe.VanillaEmiRecipeCategories;
import dev.emi.emi.api.render.EmiRenderable;
import dev.emi.emi.api.stack.EmiStack;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

public class GTRecipeEMICategory extends EmiRecipeCategory {

    public static final Function<GTRecipeCategory, GTRecipeEMICategory> CATEGORIES = Util
            .memoize(GTRecipeEMICategory::new);
    private final GTRecipeCategory category;

    private GTRecipeEMICategory(GTRecipeCategory category) {
        super(category.registryKey, toRenderable(category.getIcon(), 16, 16));
        this.category = category;
    }

    public static void registerDisplays(EmiRegistry registry) {
        List<GTRecipeCategory> subCategories = new ObjectArrayList<>();
        Map<GTRecipeType, RecipeUISize> sizes = new Reference2ObjectOpenHashMap<>();
        // Resolve tags during registration so recipes only retain the ingredients used by EMI.
        var tags = new EmiTagStacks();
        // run main categories first
        for (GTRecipeCategory category : GTRegistries.RECIPE_CATEGORIES) {
            if (!category.shouldRegisterDisplays()) continue;
            var type = category.getRecipeType();
            if (category == type.getCategory()) {
                type.buildRepresentativeRecipes();
            } else {
                subCategories.add(category);
                continue;
            }
            EmiRecipeCategory emiCategory = CATEGORIES.apply(category);
            RecipeUISize size = sizes.computeIfAbsent(type,
                    recipeType -> recipeType.getRecipeUI().getXEIRecipeUISize());
            for (var recipe : type.getClientRecipesInCategory(category)) {
                var ingredients = GTEmiIngredients.create(recipe, tags);
                registry.addRecipe(new GTEmiRecipe(recipe, emiCategory, size, ingredients));
            }
        }
        // run subcategories
        for (var subCategory : subCategories) {
            if (!subCategory.shouldRegisterDisplays()) continue;
            var type = subCategory.getRecipeType();
            EmiRecipeCategory emiCategory = CATEGORIES.apply(subCategory);
            RecipeUISize size = sizes.computeIfAbsent(type,
                    recipeType -> recipeType.getRecipeUI().getXEIRecipeUISize());
            for (var recipe : type.getClientRecipesInCategory(subCategory)) {
                var ingredients = GTEmiIngredients.create(recipe, tags);
                registry.addRecipe(new GTEmiRecipe(recipe, emiCategory, size, ingredients));
            }
        }
    }

    public static void registerWorkStations(EmiRegistry registry) {
        for (MachineDefinition machine : GTEMIPlugin.SORTED_MACHINES) {
            for (GTRecipeType type : machine.getRecipeTypes()) {
                for (GTRecipeCategory category : type.getClientCategories()) {
                    if (!category.isXEIVisible() && !GTCEu.isDev()) continue;
                    registry.addWorkstation(machineCategory(category), EmiStack.of(machine.asStack()));
                }
            }
        }
    }

    public static EmiRecipeCategory machineCategory(GTRecipeCategory category) {
        if (category == GTRecipeTypes.FURNACE_RECIPES.getCategory()) return VanillaEmiRecipeCategories.SMELTING;
        else return CATEGORIES.apply(category);
    }

    @Override
    public Component getName() {
        return Component.translatable(category.getLanguageKey());
    }

    private static EmiRenderable toRenderable(IGuiTexture texture, int width, int height) {
        return (graphics, x, y, delta) -> {
            texture.draw(graphics, 0, 0, x, y, width, height);
            RenderSystem.enableDepthTest();
            RenderSystem.depthMask(true);
        };
    }
}
