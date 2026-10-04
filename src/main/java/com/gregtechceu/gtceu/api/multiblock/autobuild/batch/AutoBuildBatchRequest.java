package com.gregtechceu.gtceu.api.multiblock.autobuild.batch;

import com.gregtechceu.gtceu.api.multiblock.autobuild.AutoBuildMaterialSource;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Immutable request for one deterministic multi-structure operation.
 *
 * @param structures      selected named structures; omission represents an unselected structure
 * @param sharedOptions   behavior shared by the whole batch
 * @param materialSources material sources in strict allocation priority order
 */
public record AutoBuildBatchRequest(List<AutoBuildStructureOptions> structures,
                                    AutoBuildSharedOptions sharedOptions,
                                    List<AutoBuildMaterialSource> materialSources) {

    public AutoBuildBatchRequest {
        structures = List.copyOf(structures);
        if (structures.isEmpty()) {
            throw new IllegalArgumentException("An auto-build batch must select at least one structure");
        }
        Set<String> names = new HashSet<>();
        for (AutoBuildStructureOptions structure : structures) {
            if (!names.add(structure.structureName())) {
                throw new IllegalArgumentException("Duplicate structure in auto-build batch: " +
                        structure.structureName());
            }
        }
        materialSources = List.copyOf(materialSources);
    }
}
