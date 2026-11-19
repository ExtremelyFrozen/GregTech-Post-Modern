package com.gregtechceu.gtceu.api.multiblock.pattern.model.module

import it.unimi.dsi.fastutil.objects.ObjectArrayList
import it.unimi.dsi.fastutil.objects.ObjectList
import it.unimi.dsi.fastutil.objects.ObjectLists

/** Slots belong to a host fragment instance; ids stay stable through independent chain instances. */
sealed interface PatternModuleSlot {
	val id: String
	val hostPort: String

	data class One(override val id: String, override val hostPort: String, val module: PatternModule) : PatternModuleSlot
	data class Optional(override val id: String, override val hostPort: String, val module: PatternModule) : PatternModuleSlot
	data class Chain @JvmOverloads constructor(override val id: String, override val hostPort: String, val module: PatternModule, val minimum: Int, val maximum: Int, val backtrack: Boolean = true) : PatternModuleSlot
	data class OrderedChoice(override val id: String, override val hostPort: String, val alternatives: ObjectList<Alternative>, val priority: ObjectList<String> = ObjectLists.emptyList()) : PatternModuleSlot {
		constructor(id: String, hostPort: String, alternatives: List<Alternative>, priority: List<String> = ObjectLists.emptyList()) : this(id, hostPort, ObjectArrayList(alternatives), ObjectArrayList(priority))
		data class Alternative(val id: String, val module: PatternModule)
	}
}
