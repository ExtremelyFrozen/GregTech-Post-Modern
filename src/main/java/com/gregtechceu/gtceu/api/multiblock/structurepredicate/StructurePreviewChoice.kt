package com.gregtechceu.gtceu.api.multiblock.structurepredicate

import com.gregtechceu.gtceu.api.multiblock.MultiblockBlockInfo
import com.gregtechceu.gtceu.data.lang.LangHandler

import net.minecraft.network.chat.Component
import net.minecraft.world.level.block.state.BlockState

import it.unimi.dsi.fastutil.objects.ObjectArrayList
import it.unimi.dsi.fastutil.objects.ObjectLinkedOpenHashSet
import it.unimi.dsi.fastutil.objects.ObjectList

import java.util.function.Function

/** A representative family owns its ordered candidates, nested restrictions and pure observation callback. */
@JvmRecord
data class StructurePreviewChoice(val candidates: ObjectList<MultiblockBlockInfo>, val constraints: ObjectList<StructurePreviewConstraint>, val tooltips: ObjectList<Component>, val observer: Function<BlockState, PatternObservation>) {
	constructor(candidates: ObjectList<MultiblockBlockInfo>, constraints: ObjectList<StructurePreviewConstraint>, tooltips: ObjectList<Component>) :
		this(candidates, constraints, tooltips, declaredStates(candidates))

	fun withObserver(observer: Function<BlockState, PatternObservation>): StructurePreviewChoice = copy(observer = observer)
	fun observeCandidate(state: BlockState): PatternObservation = observer.apply(state)
	fun restrictedBy(constraint: StructurePreviewConstraint, additionalTooltips: ObjectList<Component>): StructurePreviewChoice {
		val combinedConstraints = ObjectArrayList(constraints)
		combinedConstraints.add(constraint)
		val combinedTooltips = ObjectArrayList(tooltips)
		combinedTooltips.addAll(additionalTooltips)
		return StructurePreviewChoice(candidates, combinedConstraints, combinedTooltips, observer)
	}
	fun getTooltips(owner: PatternPredicate): ObjectList<Component> {
		val result = ObjectArrayList(tooltips)
		for ((_, minCount, maxCount) in constraints) {
			val min = minCount.orElse(-1)
			val max = maxCount.orElse(-1)
			when {
				min == max && max >= 0 -> result.add(Component.translatable("gtpm.multiblock.pattern.error.limited_exact", min))

				min >= 0 && max >= 0 -> result.add(Component.translatable("gtpm.multiblock.pattern.error.limited_within", min, max))

				else -> {
					if (min >= 0) result.add(LangHandler.getFromMultiLang("gtpm.multiblock.pattern.error.limited", 1, min))
					if (max >= 0) result.add(LangHandler.getFromMultiLang("gtpm.multiblock.pattern.error.limited", 0, max))
				}
			}
		}
		if (owner.isSingle()) result.add(Component.translatable("gtpm.multiblock.pattern.single"))
		if (owner.hasAir()) result.add(Component.translatable("gtpm.multiblock.pattern.replaceable_air"))
		return result
	}

	companion object {
		@JvmStatic
		fun unrestricted(candidates: ObjectList<MultiblockBlockInfo>): StructurePreviewChoice = StructurePreviewChoice(candidates, ObjectList.of(), ObjectList.of())

		private fun declaredStates(candidates: ObjectList<MultiblockBlockInfo>): Function<BlockState, PatternObservation> {
			val states = ObjectLinkedOpenHashSet<BlockState>()
			candidates.forEach { states.add(it.blockState) }
			return Function { PatternObservation.of(states.contains(it)) }
		}
	}
}
