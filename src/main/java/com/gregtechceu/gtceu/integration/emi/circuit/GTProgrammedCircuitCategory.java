package com.gregtechceu.gtceu.integration.emi.circuit;

import com.gregtechceu.gtceu.GTCEu;
import com.gregtechceu.gtceu.common.data.GTItems;
import com.gregtechceu.gtceu.common.item.behavior.IntCircuitBehaviour;
import com.gregtechceu.gtceu.integration.xei.widgets.GTProgrammedCircuitWidget;

import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.integration.xei.emi.ModularUIEMIRecipe;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import it.unimi.dsi.fastutil.objects.ObjectImmutableList;
import it.unimi.dsi.fastutil.objects.ObjectList;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class GTProgrammedCircuitCategory extends EmiRecipeCategory {

    public static final GTProgrammedCircuitCategory CATEGORY = new GTProgrammedCircuitCategory();

    public GTProgrammedCircuitCategory() {
        super(GTCEu.id("programmed_circuit"), EmiStack.of(GTItems.PROGRAMMED_CIRCUIT.asItem()));
    }

    public static void registerDisplays(EmiRegistry registry) {
        registry.addRecipe(new GTProgrammedCircuitCategory.GTProgrammedCircuitWrapper());
    }

    @Override
    public Component getName() {
        return Component.translatable("gtpm.jei.programmed_circuit");
    }

    public static class GTProgrammedCircuitWrapper extends ModularUIEMIRecipe {

        private final GTProgrammedCircuitWidget widget = new GTProgrammedCircuitWidget();
        private final ObjectList<EmiIngredient> inputs;
        private final ObjectList<EmiStack> outputs;

        public GTProgrammedCircuitWrapper() {
            super(GTProgrammedCircuitWrapper::createModularUI);
            int max = IntCircuitBehaviour.CIRCUIT_MAX;
            var inputStacks = new EmiIngredient[max - 1];
            var outputStacks = new EmiStack[max + 1];
            outputStacks[0] = EmiStack.of(IntCircuitBehaviour.stack(0));
            for (int configuration = 1; configuration < max; configuration++) {
                var stack = EmiStack.of(IntCircuitBehaviour.stack(configuration));
                inputStacks[configuration - 1] = stack;
                outputStacks[configuration] = stack.copy();
            }
            outputStacks[max] = EmiStack.of(IntCircuitBehaviour.stack(max));
            inputs = new ObjectImmutableList<>(inputStacks);
            outputs = new ObjectImmutableList<>(outputStacks);
        }

        @Override
        public EmiRecipeCategory getCategory() {
            return CATEGORY;
        }

        @Override
        public int getDisplayWidth() {
            return GTProgrammedCircuitWidget.WIDTH;
        }

        @Override
        public int getDisplayHeight() {
            return GTProgrammedCircuitWidget.HEIGHT;
        }

        @Override
        public @Nullable ResourceLocation getId() {
            return GTCEu.id("/programmed_circuit");
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
        public boolean supportsRecipeTree() {
            return false;
        }

        @Override
        public boolean hideCraftable() {
            return true;
        }

        private static ModularUI createModularUI(ModularUIEMIRecipe recipe) {
            if (recipe instanceof GTProgrammedCircuitWrapper circuitRecipe) {
                return circuitRecipe.widget.createModularUI();
            }
            GTCEu.LOGGER.error("Expected GTProgrammedCircuitWrapper, got {}", recipe.getClass().getName());
            throw new IllegalArgumentException("Expected GTProgrammedCircuitWrapper, got " +
                    recipe.getClass().getName());
        }
    }
}
