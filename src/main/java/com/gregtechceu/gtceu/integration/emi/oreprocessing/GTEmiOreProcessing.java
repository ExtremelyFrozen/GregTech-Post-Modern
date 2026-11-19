package com.gregtechceu.gtceu.integration.emi.oreprocessing;

import com.gregtechceu.gtceu.GTCEu;
import com.gregtechceu.gtceu.api.data.chemical.material.Material;
import com.gregtechceu.gtceu.integration.xei.widgets.GTOreByProduct;
import com.gregtechceu.gtceu.integration.xei.widgets.GTOreByProductWidget;

import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.integration.xei.emi.ModularUIEMIRecipe;

import net.minecraft.resources.ResourceLocation;

import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectList;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class GTEmiOreProcessing extends ModularUIEMIRecipe {

    final Material material;
    private final ObjectList<EmiIngredient> inputs;
    private final ObjectList<EmiStack> outputs;

    public GTEmiOreProcessing(Material material) {
        super(GTEmiOreProcessing::createModularUI);
        this.material = material;

        var diagram = new GTOreByProduct(material);
        var itemInputs = diagram.getVisibleItemInputs();
        var itemOutputs = diagram.getItemOutputs();
        inputs = new ObjectArrayList<>(itemInputs.size() + diagram.getFluidInputs().size());
        outputs = new ObjectArrayList<>(itemOutputs.size());
        for (var entry : itemInputs) {
            for (var item : entry.getStacks()) {
                if (!item.isEmpty()) {
                    inputs.add(EmiStack.of(item));
                }
            }
        }
        for (int slot = 0; slot < itemOutputs.size(); slot++) {
            var item = itemOutputs.get(slot);
            if (item.isEmpty()) continue;
            outputs.add(EmiStack.of(item));
            if (GTOreByProductWidget.isIntermediateOutput(slot)) {
                inputs.add(EmiStack.of(item));
            }
        }
        for (var entry : diagram.getFluidInputs()) {
            for (var fluid : entry.getStacks()) {
                if (!fluid.isEmpty()) {
                    inputs.add(EmiStack.of(fluid.getFluid(), fluid.getComponentsPatch(), fluid.getAmount()));
                }
            }
        }
    }

    @Override
    public List<EmiIngredient> getInputs() {
        return inputs;
    }

    @Override
    public List<EmiIngredient> getCatalysts() {
        return ObjectList.of();
    }

    @Override
    public List<EmiStack> getOutputs() {
        return outputs;
    }

    @Override
    public EmiRecipeCategory getCategory() {
        return GTOreProcessingEmiCategory.CATEGORY;
    }

    @Override
    public @Nullable ResourceLocation getId() {
        return material.getResourceLocation().withPrefix("/");
    }

    @Override
    public boolean supportsRecipeTree() {
        return false;
    }

    @Override
    public int getDisplayWidth() {
        return GTOreByProductWidget.WIDTH;
    }

    @Override
    public int getDisplayHeight() {
        return GTOreByProductWidget.HEIGHT;
    }

    private static ModularUI createModularUI(ModularUIEMIRecipe recipe) {
        if (recipe instanceof GTEmiOreProcessing oreProcessing) {
            return GTOreByProductWidget.createModularUI(oreProcessing.material);
        }
        GTCEu.LOGGER.error("Expected GTEmiOreProcessing, got {}", recipe.getClass().getName());
        throw new IllegalArgumentException("Expected GTEmiOreProcessing, got " + recipe.getClass().getName());
    }
}
