package com.gregtechceu.gtceu.api.multiblock.pattern.layout

import com.gregtechceu.gtceu.api.multiblock.structurepredicate.PatternPredicate

import it.unimi.dsi.fastutil.objects.ObjectArrayList
import it.unimi.dsi.fastutil.objects.ObjectList
import it.unimi.dsi.fastutil.objects.ObjectLists

/** A resolved, immutable layout shared by matching, preview and construction. */
class PatternLayout(
	cells: ObjectList<PatternLayoutCell>,
	repeats: ObjectList<PatternRepeatInstance> = ObjectLists.emptyList(),
	choices: ObjectList<PatternChoiceInstance> = ObjectLists.emptyList(),
	modules: ObjectList<PatternModuleInstance> = ObjectLists.emptyList(),
	rejections: ObjectList<PatternLayoutRejection> = ObjectLists.emptyList(),
) {
	val packedCells: PackedPatternCells = if (cells is PackedPatternCells) cells else PackedPatternCells(cells)

	val cells: ObjectList<PatternLayoutCell> = ObjectLists.unmodifiable(packedCells)

	val repeats: ObjectList<PatternRepeatInstance> = ObjectLists.unmodifiable(ObjectArrayList(repeats))

	val choices: ObjectList<PatternChoiceInstance> = ObjectLists.unmodifiable(ObjectArrayList(choices))

	val modules: ObjectList<PatternModuleInstance> = ObjectLists.unmodifiable(ObjectArrayList(modules))
	val rejections: ObjectList<PatternLayoutRejection> = ObjectLists.unmodifiable(ObjectArrayList(rejections))
	val moduleChoices: ObjectList<PatternModuleInstance> = ObjectLists.unmodifiable(
		ObjectArrayList<PatternModuleInstance>().also { out ->
			fun visit(instances: ObjectList<PatternModuleInstance>) {
				for (instance in instances) {
					if (instance.descriptor.kind != PatternModuleDescriptor.Kind.ONE) out.add(instance)
					visit(instance.children)
				}
			}
			visit(this.modules)
		},
	)

	val layerCount: Int get() = packedCells.layerCount

	fun depth(index: Int): Int = packedCells.depth(index)
	fun height(index: Int): Int = packedCells.height(index)
	fun width(index: Int): Int = packedCells.width(index)
	fun predicate(index: Int): PatternPredicate = packedCells.predicate(index)
	fun minimum(axis: PatternAxis): Int = packedCells.minimum(axis)
	fun maximum(axis: PatternAxis): Int = packedCells.maximum(axis)
	fun size(axis: PatternAxis): Int = packedCells.size(axis)
	fun layerStart(index: Int): Int = packedCells.layerStart(index)
	fun layerEnd(index: Int): Int = packedCells.layerEnd(index)
}
