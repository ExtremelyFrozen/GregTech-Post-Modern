package com.gregtechceu.gtceu.core.mixins.emi;

import com.gregtechceu.gtceu.integration.emi.multipage.MultiblockInfoEmiCategory;
import com.gregtechceu.gtceu.integration.emi.multipage.MultiblockInfoEmiRecipe;

import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.registry.EmiStackList.ComparisonHashStrategy;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(targets = "dev.emi.emi.registry.EmiRecipes$Manager", remap = false)
public abstract class EmiRecipesMixin {

    @Unique
    private static final ComparisonHashStrategy gtceu$comparison = new ComparisonHashStrategy();

    @Shadow
    public abstract List<EmiRecipe> getRecipes(EmiRecipeCategory category);

    @Inject(method = "getRecipesByInput", at = @At("RETURN"), cancellable = true)
    private void gtceu$currentMultiblockInputs(EmiStack stack, CallbackInfoReturnable<List<EmiRecipe>> cir) {
        var recipes = new ObjectArrayList<>(cir.getReturnValue());
        // EMI's baked input index cannot describe a page whose structure parameters can change.
        recipes.removeIf(MultiblockInfoEmiRecipe.class::isInstance);
        for (EmiRecipe recipe : getRecipes(MultiblockInfoEmiCategory.CATEGORY)) {
            if (recipe instanceof MultiblockInfoEmiRecipe && recipe.getInputs().stream()
                    .flatMap(ingredient -> ingredient.getEmiStacks().stream())
                    .anyMatch(input -> gtceu$comparison.equals(input, stack))) {
                recipes.add(recipe);
            }
        }
        cir.setReturnValue(recipes);
    }
}
