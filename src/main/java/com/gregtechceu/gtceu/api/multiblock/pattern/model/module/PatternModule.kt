package com.gregtechceu.gtceu.api.multiblock.pattern.model.module

import com.gregtechceu.gtceu.api.multiblock.pattern.model.PatternBinding
import com.gregtechceu.gtceu.api.multiblock.pattern.model.PatternSourceEntries

import it.unimi.dsi.fastutil.objects.Object2ObjectMap
import it.unimi.dsi.fastutil.objects.Object2ObjectMaps
import it.unimi.dsi.fastutil.objects.ObjectArrayList
import it.unimi.dsi.fastutil.objects.ObjectList
import it.unimi.dsi.fastutil.objects.ObjectLists

/** One reusable fragment attachment and the slots hosted by that attachment. */
data class PatternModule @JvmOverloads constructor(val fragment: String, val input: String, val output: String = "", val bindings: Object2ObjectMap<String, PatternBinding> = Object2ObjectMaps.emptyMap(), val children: ObjectList<PatternModuleSlot> = ObjectLists.emptyList()) {
	constructor(fragment: String, input: String, output: String, bindings: Map<String, PatternBinding>, children: List<PatternModuleSlot>) : this(fragment, input, output, PatternSourceEntries.objects(bindings), ObjectArrayList(children))
}
