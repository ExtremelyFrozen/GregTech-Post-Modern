package com.gregtechceu.gtceu.api.multiblock.structurepredicate

import com.gregtechceu.gtceu.api.multiblock.MultiblockState
import com.gregtechceu.gtceu.common.registry.GTRegistration.REGISTRATE

import com.mojang.serialization.MapCodec
import com.mojang.serialization.codecs.RecordCodecBuilder
import it.unimi.dsi.fastutil.objects.ObjectArrayList
import it.unimi.dsi.fastutil.objects.ObjectList

@JvmRecord
data class ConcatenatedPredicate(val predicates: ObjectList<StructurePredicate>) : AlternativePredicate {
	override fun type(): StructurePredicateType<*> = TYPE
	override fun alternatives(state: MultiblockState?): ObjectList<StructurePredicate> = predicates

	companion object {
		val TYPE: StructurePredicateType<ConcatenatedPredicate> by lazy { REGISTRATE.structurePredicate("concatenated", CODEC) }

		@JvmField
		val CODEC: MapCodec<ConcatenatedPredicate> = RecordCodecBuilder.mapCodec { instance ->
			instance.group(
				StructurePredicate.CODEC.listOf().xmap<ObjectList<StructurePredicate>>({ ObjectArrayList(it) }, { it })
					.fieldOf("predicates").forGetter(ConcatenatedPredicate::predicates),
			).apply(instance, ::ConcatenatedPredicate)
		}
		internal fun concat(first: StructurePredicate, second: StructurePredicate): ConcatenatedPredicate {
			val predicates = ObjectArrayList<StructurePredicate>()
			if (first is ConcatenatedPredicate) predicates.addAll(first.predicates) else predicates.add(first)
			if (second is ConcatenatedPredicate) predicates.addAll(second.predicates) else predicates.add(second)
			return ConcatenatedPredicate(predicates)
		}
	}
}
