package com.gregtechceu.gtceu.api.multiblock.autobuild.batch;

import com.gregtechceu.gtceu.api.multiblock.autobuild.MaterialSource;

import it.unimi.dsi.fastutil.objects.ObjectList;

/**
 * Request for one deterministic multi-structure operation.
 *
 * @param structures      selected named structures; omission represents an unselected structure
 * @param sharedOptions   behavior shared by the whole batch
 * @param materialSources material sources in strict allocation priority order
 */
public record BatchRequest(ObjectList<StructureRequest> structures,
                                    BuildPolicy sharedOptions,
                                    ObjectList<MaterialSource> materialSources) {}
