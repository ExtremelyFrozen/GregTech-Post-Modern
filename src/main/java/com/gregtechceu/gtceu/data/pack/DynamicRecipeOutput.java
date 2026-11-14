package com.gregtechceu.gtceu.data.pack;

import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import lombok.Getter;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.recipes.RecipeBuilder;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.Recipe;
import net.neoforged.neoforge.common.conditions.ICondition;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/** Encodes one dynamic recipe generation with a shared, reload-scoped registry serialization context. */
@NullMarked
public final class DynamicRecipeOutput implements RecipeOutput {

    private static final FileToIdConverter RECIPES = FileToIdConverter.json("recipe");
    private static final FileToIdConverter ADVANCEMENTS = FileToIdConverter.json("advancement");
    private static final AdvancementHolder ROOT = Advancement.Builder.recipeAdvancement()
            .build(RecipeBuilder.ROOT_RECIPE_ADVANCEMENT);

    private final RegistryOps<JsonElement> ops;
    @Getter
    private int recipeCount;

    public DynamicRecipeOutput(HolderLookup.Provider registries) {
        ops = registries.createSerializationContext(JsonOps.INSTANCE);
    }

    @Override
    public Advancement.Builder advancement() {
        return Advancement.Builder.recipeAdvancement().parent(ROOT);
    }

    @Override
    public void accept(ResourceLocation id, Recipe<?> recipe, @Nullable AdvancementHolder advancement,
                       ICondition... conditions) {
        GTDynamicDataPack.addResource(RECIPES.idToFile(id), Recipe.CODEC.encodeStart(ops, recipe).getOrThrow());
        if (advancement != null) {
            GTDynamicDataPack.addResource(ADVANCEMENTS.idToFile(advancement.id()),
                    Advancement.CODEC.encodeStart(ops, advancement.value()).getOrThrow());
        }
        recipeCount++;
    }
}
