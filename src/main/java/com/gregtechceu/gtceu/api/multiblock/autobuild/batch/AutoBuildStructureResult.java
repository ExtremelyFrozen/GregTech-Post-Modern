package com.gregtechceu.gtceu.api.multiblock.autobuild.batch;

import com.gregtechceu.gtceu.api.multiblock.autobuild.AutoBuildProblem;

import it.unimi.dsi.fastutil.objects.ObjectList;

/**
 * Execution outcome for one selected structure.
 */
public record AutoBuildStructureResult(String structureName, AutoBuildMode mode, boolean success,
                                       int placed, int removed, ObjectList<AutoBuildProblem> problems) {}
