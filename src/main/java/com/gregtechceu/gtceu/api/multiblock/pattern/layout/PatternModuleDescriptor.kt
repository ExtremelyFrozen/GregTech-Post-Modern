package com.gregtechceu.gtceu.api.multiblock.pattern.layout

import com.gregtechceu.gtceu.api.multiblock.pattern.model.module.PatternModuleSlot

import it.unimi.dsi.fastutil.objects.AbstractObjectList
import it.unimi.dsi.fastutil.objects.ObjectList

import java.util.RandomAccess

/** Runtime decision contract for one expanded module slot. */
data class PatternModuleDescriptor(val id: String, val path: String, val kind: Kind, val candidates: ObjectList<String>, val explicit: Boolean, val backtrack: Boolean, val minimum: Int, val maximum: Int, val slot: PatternModuleSlot) {
	enum class Kind { ONE, OPTIONAL, CHAIN, ORDERED_CHOICE }

	internal class ChainCandidates(private val minimum: Int, private val maximum: Int) :
		AbstractObjectList<String>(),
		RandomAccess {
		override val size: Int = Math.toIntExact(maximum.toLong() - minimum.toLong() + 1L)
		override fun get(index: Int): String = (maximum - index).toString()
		override fun indexOf(element: String): Int = element.toIntOrNull()?.takeIf { it in minimum..maximum && it.toString() == element }?.let { maximum - it } ?: -1
		override fun lastIndexOf(element: String): Int = indexOf(element)
		override fun contains(element: String): Boolean = indexOf(element) >= 0
	}
}
