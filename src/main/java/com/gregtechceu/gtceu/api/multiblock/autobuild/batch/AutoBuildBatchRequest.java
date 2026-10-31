package com.gregtechceu.gtceu.api.multiblock.autobuild.batch;

import com.gregtechceu.gtceu.api.multiblock.autobuild.AutoBuildMaterialSource;

import java.util.List;

/**
 * Request for one deterministic multi-structure operation.
 *
 * @param structures      selected named structures; omission represents an unselected structure
 * @param sharedOptions   behavior shared by the whole batch
 * @param materialSources material sources in strict allocation priority order
 */
public record AutoBuildBatchRequest(List<AutoBuildStructureOptions> structures,
                                    AutoBuildSharedOptions sharedOptions,
                                    List<AutoBuildMaterialSource> materialSources) {}
