package com.gregtechceu.gtceu.integration.emi.orevein;

import com.gregtechceu.gtceu.integration.xei.widgets.VeinDimensionMarkers;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import it.unimi.dsi.fastutil.objects.ObjectImmutableList;
import it.unimi.dsi.fastutil.objects.ObjectList;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.Set;

@NullMarked
final class VeinEmiIngredients {

    private VeinEmiIngredients() {}

    static ObjectList<EmiIngredient> dimensionCatalysts(@Nullable Set<ResourceKey<Level>> dimensions) {
        var markers = VeinDimensionMarkers.resolve(dimensions);
        if (markers.isEmpty()) return ObjectList.of();

        var catalysts = new EmiIngredient[markers.size()];
        // LDLib2 visits the dimension slots in reverse insertion order.
        for (int i = 0; i < catalysts.length; i++) {
            catalysts[i] = EmiStack.of(markers.get(catalysts.length - 1 - i).getIcon());
        }
        return new ObjectImmutableList<>(catalysts);
    }
}
