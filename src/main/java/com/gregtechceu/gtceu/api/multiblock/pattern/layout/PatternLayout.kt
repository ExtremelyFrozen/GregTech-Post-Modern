package com.gregtechceu.gtceu.api.multiblock.pattern.layout

import it.unimi.dsi.fastutil.objects.ObjectList
import it.unimi.dsi.fastutil.objects.ObjectLists

/** A resolved, immutable layout shared by matching, preview and construction. */
@JvmRecord
data class PatternLayout @JvmOverloads constructor(val cells: ObjectList<PatternLayoutCell>, val repeats: ObjectList<PatternRepeatInstance> = ObjectLists.emptyList(), val choices: ObjectList<PatternChoiceInstance> = ObjectLists.emptyList()) {
	fun size(axis: PatternAxis): Int {
		var minimum = Int.MAX_VALUE
		var maximum = Int.MIN_VALUE
		for (cell in cells) {
			val coordinate = when (axis) {
				PatternAxis.DEPTH -> cell.depth
				PatternAxis.HEIGHT -> cell.height
				PatternAxis.WIDTH -> cell.width
			}
			minimum = minOf(minimum, coordinate)
			maximum = maxOf(maximum, coordinate)
		}
		return if (cells.isEmpty()) 0 else maximum - minimum + 1
	}
}
