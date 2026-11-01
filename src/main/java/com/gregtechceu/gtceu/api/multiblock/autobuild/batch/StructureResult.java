package com.gregtechceu.gtceu.api.multiblock.autobuild.batch;

import com.gregtechceu.gtceu.api.multiblock.autobuild.BuildProblem;

import it.unimi.dsi.fastutil.objects.ObjectList;

/**
 * Execution outcome for one selected structure.
 */
public record StructureResult(String structureName, StructureOperation mode, boolean success,
                                       int placed, int removed, ObjectList<BuildProblem> problems) {}
