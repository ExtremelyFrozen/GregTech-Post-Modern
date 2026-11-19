package com.gregtechceu.gtceu.integration.xei.widgets;

import com.gregtechceu.gtceu.api.data.DimensionMarker;
import com.gregtechceu.gtceu.api.registry.GTRegistries;
import com.gregtechceu.gtceu.common.recipe.condition.DimensionCondition;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectList;
import org.jetbrains.annotations.ApiStatus;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.Comparator;
import java.util.Set;

/** Resolves the same dimension icons for vein diagrams and the recipe viewer index. */
@ApiStatus.Internal
@NullMarked
public final class VeinDimensionMarkers {

    private static final Comparator<DimensionMarker> BY_TIER = Comparator.comparingInt(DimensionMarker::getTier);

    private VeinDimensionMarkers() {}

    public static ObjectList<DimensionMarker> resolve(@Nullable Set<ResourceKey<Level>> dimensions) {
        if (dimensions == null) return ObjectList.of(); // Unrestricted veins have no dimension slots.

        var markers = new ObjectArrayList<DimensionMarker>(dimensions.size());
        for (var dimension : dimensions) {
            markers.add(GTRegistries.DIMENSION_MARKERS.getOptional(dimension.location())
                    .orElseGet(() -> new DimensionMarker(DimensionMarker.MAX_TIER, () -> Blocks.BARRIER,
                            DimensionCondition.getDimensionName(dimension))));
        }
        markers.sort(BY_TIER);
        return markers;
    }
}
