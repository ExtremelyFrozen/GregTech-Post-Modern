package com.gregtechceu.gtceu.integration.emi.orevein;

import com.gregtechceu.gtceu.GTCEu;
import com.gregtechceu.gtceu.api.data.worldgen.bedrockore.BedrockOreDefinition;
import com.gregtechceu.gtceu.integration.xei.widgets.GTBedrockOreWidget;

import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.integration.xei.emi.ModularUIEMIRecipe;

import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;

import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import it.unimi.dsi.fastutil.objects.ObjectImmutableList;
import it.unimi.dsi.fastutil.objects.ObjectList;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class GTBedrockOre extends ModularUIEMIRecipe {

    private final Holder<BedrockOreDefinition> bedrockOre;
    private final ObjectList<EmiIngredient> catalysts;
    private final ObjectList<EmiStack> outputs;

    public GTBedrockOre(Holder<BedrockOreDefinition> bedrockOre) {
        super(GTBedrockOre::createModularUI);
        this.bedrockOre = bedrockOre;
        var definition = bedrockOre.value();
        catalysts = VeinEmiIngredients.dimensionCatalysts(definition.dimensionFilter());
        outputs = GTBedrockOreWidget.getRawMaterialList(definition).stream()
                .map(EmiStack::of)
                .collect(ObjectImmutableList.toList());
    }

    @Override
    public EmiRecipeCategory getCategory() {
        return GTBedrockOreEmiCategory.CATEGORY;
    }

    @Override
    public @Nullable ResourceLocation getId() {
        return bedrockOre.getKey()
                .location()
                .withPrefix("/bedrock_ore_diagram/");
    }

    @Override
    public int getDisplayWidth() {
        return GTBedrockOreWidget.WIDTH;
    }

    @Override
    public int getDisplayHeight() {
        return GTBedrockOreWidget.HEIGHT;
    }

    @Override
    public List<EmiIngredient> getInputs() {
        return ObjectList.of();
    }

    @Override
    public List<EmiIngredient> getCatalysts() {
        return catalysts;
    }

    @Override
    public List<EmiStack> getOutputs() {
        return outputs;
    }

    private static ModularUI createModularUI(ModularUIEMIRecipe recipe) {
        if (recipe instanceof GTBedrockOre bedrockOre) {
            return GTBedrockOreWidget.createModularUI(bedrockOre.bedrockOre, GTBedrockOreWidget.HEIGHT);
        }
        GTCEu.LOGGER.error("Expected GTBedrockOre, got {}", recipe.getClass().getName());
        throw new IllegalArgumentException("Expected GTBedrockOre, got " + recipe.getClass().getName());
    }
}
