package com.gregtechceu.gtceu.api.multiblock.pattern.layout

import it.unimi.dsi.fastutil.objects.ObjectList

/** One selected attachment; coordinates are its first fragment origin, or the host port if absent. */
data class PatternModuleInstance(val path: String, val descriptor: PatternModuleDescriptor, val selection: String, val children: ObjectList<PatternModuleInstance>, val fragment: String, val depth: Int, val height: Int, val width: Int, val segmentPaths: ObjectList<String>)
