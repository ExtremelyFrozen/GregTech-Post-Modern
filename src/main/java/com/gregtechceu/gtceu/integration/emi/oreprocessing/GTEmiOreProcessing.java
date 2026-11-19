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
        var fluidInputs = diagram.getFluidInputs();
        inputs = new ObjectArrayList<>(itemInputs.size() + fluidInputs.size());
        outputs = new ObjectArrayList<>(itemOutputs.size());
        // LDLib2 dispatches ingredient events to siblings at the same z-index in reverse insertion order.
        for (int slot = fluidInputs.size() - 1; slot >= 0; slot--) {
            for (var fluid : fluidInputs.get(slot).getStacks()) {
                if (!fluid.isEmpty()) {
                    inputs.add(EmiStack.of(fluid.getFluid(), fluid.getComponentsPatch(), fluid.getAmount()));
                }
            }
        }
        for (int slot = itemOutputs.size() - 1; slot >= 0; slot--) {
            var item = itemOutputs.get(slot);
            if (item.isEmpty()) continue;
            outputs.add(EmiStack.of(item));
            if (GTOreByProductWidget.isIntermediateOutput(slot)) {
                inputs.add(EmiStack.of(item));
            }
        }
        for (int slot = itemInputs.size() - 1; slot >= 0; slot--) {
            for (var item : itemInputs.get(slot).getStacks()) {
                if (!item.isEmpty()) {
                    inputs.add(EmiStack.of(item));
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
