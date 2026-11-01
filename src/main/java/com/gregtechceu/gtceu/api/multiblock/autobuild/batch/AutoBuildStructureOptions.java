package com.gregtechceu.gtceu.api.multiblock.autobuild.batch;

import net.minecraft.resources.ResourceLocation;

import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;

/**
 * User-selected behavior for one named structure.
 *
 * @param structureName selected definition structure
 * @param repetitions   one exact repetition count per pattern unit
 * @param tierChoices   tier group name to exact block id selections
 * @param flipMode      whether the pattern transform is flipped
 * @param mode          whether this structure is built or demolished
 */
public record AutoBuildStructureOptions(String structureName, IntList repetitions,
                                        Object2ObjectMap<String, ResourceLocation> tierChoices,
                                        boolean flipMode, AutoBuildMode mode,
                                        Object2ObjectMap<String, String> choices) {}
