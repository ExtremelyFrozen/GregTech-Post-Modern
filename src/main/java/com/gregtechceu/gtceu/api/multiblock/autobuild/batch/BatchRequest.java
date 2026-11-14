package com.gregtechceu.gtceu.api.multiblock.autobuild.batch;

import com.gregtechceu.gtceu.api.multiblock.autobuild.MaterialRequest;

import it.unimi.dsi.fastutil.objects.ObjectList;

/**
 * Request for one deterministic multi-structure operation.
 *
 * @param structures      selected named structures; omission represents an unselected structure
 * @param replaceMode     whether occupied target positions may be replaced
 * @param noHatchMode     whether hatch candidates are filtered for every build in the batch
 * @param materialSources registered source bindings in strict allocation priority order
 */
public record BatchRequest(ObjectList<StructureRequest> structures,
                           boolean replaceMode, boolean noHatchMode,
                           ObjectList<MaterialRequest.Result> materialSources) {}
