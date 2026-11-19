package com.gregtechceu.gtceu.integration.emi.orevein;

import com.gregtechceu.gtceu.GTCEu;
import com.gregtechceu.gtceu.api.data.worldgen.GTOreDefinition;
import com.gregtechceu.gtceu.integration.xei.widgets.GTOreVeinWidget;

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

public class GTEmiOreVein extends ModularUIEMIRecipe {

    private final Holder<GTOreDefinition> oreDefinition;
    private final ObjectList<EmiIngredient> catalysts;
    private final ObjectList<EmiStack> outputs;

    public GTEmiOreVein(Holder<GTOreDefinition> oreDefinition) {
        super(GTEmiOreVein::createModularUI);
        this.oreDefinition = oreDefinition;
        var definition = oreDefinition.value();
        catalysts = VeinEmiIngredients.dimensionCatalysts(definition.dimensionFilter());
        outputs = GTOreVeinWidget.getContainedOresAndBlocks(definition).stream()
                .map(EmiStack::of)
                .collect(ObjectImmutableList.toList());
    }

    @Override
    public EmiRecipeCategory getCategory() {
        return GTOreVeinEmiCategory.CATEGORY;
    }

    @Override
    public @Nullable ResourceLocation getId() {
        return oreDefinition.getKey()
                .location()
                .withPrefix("/ore_vein_diagram/");
    }

    @Override
    public int getDisplayWidth() {
        return GTOreVeinWidget.WIDTH;
    }

    @Override
    public int getDisplayHeight() {
        return GTOreVeinWidget.HEIGHT;
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
        if (recipe instanceof GTEmiOreVein oreVein) {
            return GTOreVeinWidget.createModularUI(oreVein.oreDefinition, GTOreVeinWidget.HEIGHT);
        }
        GTCEu.LOGGER.error("Expected GTEmiOreVein, got {}", recipe.getClass().getName());
        throw new IllegalArgumentException("Expected GTEmiOreVein, got " + recipe.getClass().getName());
    }
}
