package com.gregtechceu.gtceu.api.multiblock.autobuild.batch;

import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.Map;

/**
 * User-selected behavior for one named structure.
 *
 * @param structureName selected definition structure
 * @param repetitions   one exact repetition count per pattern unit
 * @param tierChoices   stable tier group id to exact block id selections
 * @param flipMode      whether the pattern transform is flipped
 * @param mode          whether this structure is built or demolished
 */
public record AutoBuildStructureOptions(String structureName, List<Integer> repetitions,
                                        Map<ResourceLocation, ResourceLocation> tierChoices,
                                        boolean flipMode, AutoBuildMode mode) {

    public AutoBuildStructureOptions {
        if (structureName.isBlank()) {
            throw new IllegalArgumentException("structureName must not be blank");
        }
        repetitions = List.copyOf(repetitions);
        tierChoices = Map.copyOf(tierChoices);
    }
}
