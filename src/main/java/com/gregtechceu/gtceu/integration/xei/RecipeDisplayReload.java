package com.gregtechceu.gtceu.integration.xei;

import com.gregtechceu.gtceu.api.recipe.GTRecipeDefinition;
import com.gregtechceu.gtceu.api.recipe.GTRecipeType;

import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectList;
import it.unimi.dsi.fastutil.objects.Reference2ObjectMap;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import org.jetbrains.annotations.ApiStatus;

/** Replaces recipe-viewer data only after the complete, accepted recipe generation is available. */
@ApiStatus.Internal
public final class RecipeDisplayReload {

    private RecipeDisplayReload() {}

    public static void replaceRecipes(RecipeManager manager, Iterable<? extends RecipeType<?>> types) {
        Reference2ObjectMap<RecipeType<?>, ObjectList<RecipeHolder<?>>> byType = new Reference2ObjectOpenHashMap<>();
        for (RecipeHolder<?> holder : manager.getRecipes()) {
            byType.computeIfAbsent(holder.value().getType(), type -> new ObjectArrayList<>()).add(holder);
        }
        for (RecipeType<?> type : types) {
            if (type instanceof GTRecipeType gtType) {
                ObjectList<GTRecipeDefinition> recipes = new ObjectArrayList<>();
                for (RecipeHolder<?> holder : byType.getOrDefault(type, ObjectList.of())) {
                    GTRecipeDefinition definition = (GTRecipeDefinition) holder.value();
                    definition.setId(holder.id());
                    recipes.add(definition);
                }
                // Proxy displays are not themselves entries in the synchronized RecipeManager.
                for (RecipeType<?> proxy : gtType.getProxyRecipes().keySet()) {
                    for (RecipeHolder<?> holder : byType.getOrDefault(proxy, ObjectList.of())) {
                        recipes.add(GTRecipeDefinition.fromRuntime(gtType.toGTRecipe(holder).value()));
                    }
                }
                // Include empty types so removals and server switches cannot retain the previous generation.
                gtType.replaceClientRecipes(recipes);
            }
        }
    }
}
