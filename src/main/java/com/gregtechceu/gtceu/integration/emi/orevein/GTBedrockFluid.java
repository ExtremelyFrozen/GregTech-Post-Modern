package com.gregtechceu.gtceu.integration.emi.orevein;

import com.gregtechceu.gtceu.GTCEu;
import com.gregtechceu.gtceu.api.data.worldgen.bedrockfluid.BedrockFluidDefinition;
import com.gregtechceu.gtceu.integration.xei.widgets.GTBedrockFluidWidget;

import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.integration.xei.emi.ModularUIEMIRecipe;

import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;

import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import it.unimi.dsi.fastutil.objects.ObjectList;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class GTBedrockFluid extends ModularUIEMIRecipe {

    private final Holder<BedrockFluidDefinition> fluid;
    private final ObjectList<EmiIngredient> catalysts;
    private final ObjectList<EmiStack> outputs;

    public GTBedrockFluid(Holder<BedrockFluidDefinition> fluid) {
        super(GTBedrockFluid::createModularUI);
        this.fluid = fluid;
        var definition = fluid.value();
        catalysts = VeinEmiIngredients.dimensionCatalysts(definition.getDimensionFilter());
        outputs = ObjectList.of(EmiStack.of(definition.getStoredFluid(), GTBedrockFluidWidget.DISPLAY_AMOUNT));
    }

    @Override
    public EmiRecipeCategory getCategory() {
        return GTBedrockFluidEmiCategory.CATEGORY;
    }

    @Override
    public @Nullable ResourceLocation getId() {
        return fluid.getKey()
                .location()
                .withPrefix("/bedrock_fluid_diagram/");
    }

    @Override
    public int getDisplayWidth() {
        return GTBedrockFluidWidget.WIDTH;
    }

    @Override
    public int getDisplayHeight() {
        return GTBedrockFluidWidget.HEIGHT;
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
        if (recipe instanceof GTBedrockFluid bedrockFluid) {
            return GTBedrockFluidWidget.createModularUI(bedrockFluid.fluid, GTBedrockFluidWidget.HEIGHT);
        }
        GTCEu.LOGGER.error("Expected GTBedrockFluid, got {}", recipe.getClass().getName());
        throw new IllegalArgumentException("Expected GTBedrockFluid, got " + recipe.getClass().getName());
    }
}
