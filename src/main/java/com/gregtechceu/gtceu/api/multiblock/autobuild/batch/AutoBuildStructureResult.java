package com.gregtechceu.gtceu.api.multiblock.autobuild.batch;

import com.gregtechceu.gtceu.api.multiblock.autobuild.AutoBuildProblem;

import java.util.List;

/**
 * Execution outcome for one selected structure.
 */
public record AutoBuildStructureResult(String structureName, AutoBuildMode mode, boolean success,
                                       int placed, int removed, List<AutoBuildProblem> problems) {}
