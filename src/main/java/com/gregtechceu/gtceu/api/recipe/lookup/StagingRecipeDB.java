package com.gregtechceu.gtceu.api.recipe.lookup;

import com.gregtechceu.gtceu.GTCEu;
import com.gregtechceu.gtceu.api.capability.recipe.RecipeCapability;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import com.gregtechceu.gtceu.api.recipe.GTRecipeDefinition;
import com.gregtechceu.gtceu.api.recipe.content.Content;
import com.gregtechceu.gtceu.api.recipe.lookup.ingredient.AbstractMapIngredient;
import com.gregtechceu.gtceu.api.recipe.lookup.ingredient.MapIngredientTypeManager;

import it.unimi.dsi.fastutil.Pair;
import it.unimi.dsi.fastutil.objects.*;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import java.util.*;

@ApiStatus.Internal
public final class StagingRecipeDB {

    private final @NotNull ObjectOpenHashSet<GTRecipeDefinition> recipes = new ObjectOpenHashSet<>();

    /**
     * Add a definition recipe to the DB
     *
     * @param recipe the recipe
     * @return if successful
     */
    public boolean add(@NotNull GTRecipeDefinition recipe) {
        return recipes.add(recipe);
    }

    /**
     * Add a generated runtime recipe when no definition object exists at the source boundary.
     *
     * @param recipe the recipe
     * @return if successful
     */
    public boolean addRuntime(@NotNull GTRecipe recipe) {
        return add(GTRecipeDefinition.fromRuntime(recipe));
    }

    /**
     * Clear the DB
     */
    public void clear() {
        recipes.clear();
        recipes.trim();
    }

    /**
     * Populate a DB with the contents of the staging DB
     *
     * @param db the db to populate
     */
    public void populateDB(@NotNull RecipeDB db) {
        var frequencies = inputFrequencies();
        for (GTRecipeDefinition definition : recipes) {
            GTRecipe runtime = definition.toRuntime();
            ObjectList<Pair<RecipeCapability<?>, Object>> flattedContent = flattenedContent(definition);
            flattedContent.sort(Comparator.comparingInt(entry -> frequencies.getInt(entry.right())));
            ObjectList<List<AbstractMapIngredient>> inputs = new ObjectArrayList<>(flattedContent.size());
            for (var entry : flattedContent) {
                var ingredients = MapIngredientTypeManager.getFrom(entry.right(), entry.left());
                MapIngredientPool.applyPooling(ingredients);
                inputs.add(ingredients);
            }
            boolean result = db.add(runtime, inputs);
            if (result) {
                definition.recipeCategory.addRecipe(definition);
            } else {
                GTCEu.LOGGER.warn("failed to add recipe from staging into lookup DB: {}", definition.getId());
            }
        }
    }

    /**
     * @return a map of the amount of times every input is used
     */
    private @NotNull Object2IntMap<Object> inputFrequencies() {
        var map = new Object2IntOpenHashMap<>();
        for (GTRecipeDefinition recipe : recipes) {
            recipe.inputs.forEach((cap, list) -> {
                for (var input : compressedContent(list, cap)) {
                    map.addTo(input, 1);
                }
            });
            recipe.tickInputs.forEach((cap, list) -> {
                for (var input : compressedContent(list, cap)) {
                    map.addTo(input, 1);
                }
            });
        }
        return map;
    }

    /**
     * @param list the list of content
     * @param cap  the RecipeCapability for the content
     * @return the compressed ingredient form of the content
     */
    private static @NotNull List<Object> compressedContent(@NotNull List<Content> list,
                                                           @NotNull RecipeCapability<?> cap) {
        var contentList = list.stream()
                .map(Content::getContent)
                .toList();
        return cap.compressIngredients(contentList);
    }

    /**
     * Returns the flattened content of a recipe
     *
     * @param recipe the recipe
     * @return the flattened content
     */
    private static @NotNull ObjectList<Pair<RecipeCapability<?>, Object>> flattenedContent(
                                                                                     @NotNull GTRecipeDefinition recipe) {
        var map = new Object2ObjectOpenHashMap<RecipeCapability<?>, List<Content>>();
        int size = 0;
        var inputs = recipe.inputs.fastIterator();
        while (inputs.hasNext()) {
            var entry = inputs.next();
            size += buildInputsByCap(map, entry.getKey(), entry.getValue());
        }
        var tickInputs = recipe.tickInputs.fastIterator();
        while (tickInputs.hasNext()) {
            var entry = tickInputs.next();
            size += buildInputsByCap(map, entry.getKey(), entry.getValue());
        }
        ObjectList<Pair<RecipeCapability<?>, Object>> list = new ObjectArrayList<>(size);
        map.forEach((k, v) -> {
            for (var content : v) {
                list.add(Pair.of(k, content.getContent()));
            }
        });
        return list;
    }

    /**
     * Builds a map of inputs by RecipeCapability
     *
     * @param map  the map to populate
     * @param cap  the recipe capability for the list
     * @param list the list of inputs
     * @return the number of inputs added to the map
     */
    private static int buildInputsByCap(@NotNull Object2ObjectMap<RecipeCapability<?>, List<Content>> map,
                                         @NotNull RecipeCapability<?> cap, @NotNull List<Content> list) {
        if (!cap.isRecipeSearchFilter()) {
            return 0;
        }
        map.compute(cap, (k, v) -> {
            if (v == null) {
                return list;
            }
            ObjectList<Content> merged = new ObjectArrayList<>(v.size() + list.size());
            merged.addAll(v);
            merged.addAll(list);
            return merged;
        });
        return list.size();
    }
}
