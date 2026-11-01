package com.gregtechceu.gtceu.api.multiblock.structurepredicate

import com.gregtechceu.gtceu.GTCEu
import com.gregtechceu.gtceu.api.capability.recipe.IO
import com.gregtechceu.gtceu.api.multiblock.MultiblockState
import com.gregtechceu.gtceu.api.multiblock.pattern.model.PatternPredicateDefinition

import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.world.level.block.state.BlockState

import com.mojang.serialization.Codec
import com.mojang.serialization.Dynamic
import com.mojang.serialization.JavaOps
import com.mojang.serialization.MapCodec
import com.mojang.serialization.codecs.RecordCodecBuilder
import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap
import it.unimi.dsi.fastutil.objects.ObjectArrayList
import it.unimi.dsi.fastutil.objects.ObjectLinkedOpenHashSet
import it.unimi.dsi.fastutil.objects.ObjectList
import it.unimi.dsi.fastutil.objects.ObjectLists
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet

import java.util.Optional
import java.util.function.Function
import java.util.function.Supplier

/** Logical composition publishes only successful selected branches' facts and consistent scalar properties. */
class CombinedPredicate private constructor(private val expression: Expression) :
	ObservedPatternPredicate(
		Function(expression::observePosition),
		Supplier { expression.states().map { it.block }.toTypedArray() },
		candidateObservation = Function(expression::observeCandidate),
		states = Supplier(expression::states),
	) {
	internal override fun acceptObservation(state: MultiblockState, observation: PatternObservation): Boolean = expression.accept(state, observation)

	private enum class Selection { ALL, PRIORITY, EXCLUSIVE, NOT }
	private data class Branch(val id: String, val definition: PatternPredicateDefinition, val runtime: PatternPredicate)
	private class Expression(val selection: Selection, val branches: ObjectList<Branch>, val priority: ObjectList<String>) {
		fun observePosition(state: MultiblockState): PatternObservation = observe(true) { PatternPredicateCompiler.observe(it, state) }
		fun observeCandidate(state: BlockState): PatternObservation = observe(false) { it.observeCandidate(state) }

		private data class Position(val branches: ObjectList<Pair<Branch, PatternObservation>>) : PatternObservation.PositionMatch

		fun accept(state: MultiblockState, observation: PatternObservation): Boolean {
			val selected = observation.positionMatch as Position
			for ((branch, observed) in selected.branches) {
				val previousIO = state.io
				if (!branch.runtime.acceptPosition(state, observed)) return false
				val currentIO = state.io
				state.io = when {
					previousIO == null || currentIO == null -> null
					previousIO == IO.BOTH -> currentIO
					currentIO == IO.BOTH || currentIO == previousIO -> previousIO
					else -> null
				}
			}
			return true
		}

		private fun observe(position: Boolean, visit: (PatternPredicate) -> PatternObservation): PatternObservation {
			val success = ObjectArrayList<Pair<Branch, PatternObservation>>()
			branches.forEach { branch -> visit(branch.runtime).let { if (it.matched) success.add(branch to it) } }
			val accepted = when (selection) {
				Selection.ALL -> success.size == branches.size
				Selection.PRIORITY -> success.isNotEmpty()
				Selection.EXCLUSIVE -> success.size == 1
				Selection.NOT -> success.isEmpty()
			}
			if (!accepted) return PatternObservation.of(false)
			val chosen = when {
				selection == Selection.NOT -> ObjectList.of()

				selection == Selection.ALL -> success

				else -> ObjectLists.singleton(
					if (priority.isEmpty()) success.first() else success.minBy { priority.indexOf(it.first.id).let { index -> if (index < 0) Int.MAX_VALUE else index } },
				)
			}
			val facts = ObjectOpenHashSet<String>()
			val properties = Object2ObjectLinkedOpenHashMap<String, Any>()
			for ((_, observation) in chosen) {
				facts.addAll(observation.facts)
				for ((key, value) in observation.properties) {
					val previous = properties.putIfAbsent(key, value)
					if (previous != null && previous != value) return PatternObservation.of(false)
				}
			}
			return if (position) PatternObservation(true, facts, properties, Position(chosen)) else PatternObservation(true, facts, properties)
		}

		fun states(): ObjectList<BlockState> {
			val result = ObjectLinkedOpenHashSet<BlockState>()
			if (selection == Selection.NOT) {
				BuiltInRegistries.BLOCK.firstOrNull { observeCandidate(it.defaultBlockState()).matched }?.let { result.add(it.defaultBlockState()) }
			} else {
				branches.forEach { branch -> branch.runtime.getPreviewCandidates().forEach { result.add(it.blockInfo.blockState) } }
			}
			return ObjectArrayList(result)
		}
	}

	private data class Alternative(val id: String, val predicate: PatternPredicateDefinition, val facts: ObjectList<String>)

	companion object {
		val ALL_OF_TYPE: PredicateType<CombinedPredicate> by lazy {
			PredicateType(ALL_OF_CODEC) { it }.also { PredicateRegistry.register(GTCEu.id("all_of"), it) }
		}
		val ANY_OF_TYPE: PredicateType<CombinedPredicate> by lazy {
			PredicateType(ANY_OF_CODEC) { it }.also { PredicateRegistry.register(GTCEu.id("any_of"), it) }
		}
		val ONE_OF_TYPE: PredicateType<CombinedPredicate> by lazy {
			PredicateType(ONE_OF_CODEC) { it }.also { PredicateRegistry.register(GTCEu.id("one_of"), it) }
		}
		val NOT_TYPE: PredicateType<CombinedPredicate> by lazy {
			PredicateType(NOT_CODEC) { it }.also { PredicateRegistry.register(GTCEu.id("not"), it) }
		}
		private val DEFINITION_CODEC: Codec<PatternPredicateDefinition> = Codec.PASSTHROUGH.xmap(
			{ dynamic -> nested(dynamic.convert(JavaOps.INSTANCE).value as Map<*, *>) },
			{ definition ->
				val values = Object2ObjectLinkedOpenHashMap<String, Any>(definition.properties)
				values["type"] = definition.type
				values["name"] = definition.name
				values["facts"] = definition.facts
				Dynamic(JavaOps.INSTANCE, values)
			},
		)
		private val DEFINITIONS_CODEC: Codec<ObjectList<PatternPredicateDefinition>> = DEFINITION_CODEC.listOf().xmap({ ObjectArrayList(it) }, { it })
		private val STRINGS_CODEC: Codec<ObjectList<String>> = Codec.STRING.listOf().xmap({ ObjectArrayList(it) }, { it })
		private val ALTERNATIVE_CODEC: Codec<Alternative> = RecordCodecBuilder.create { instance ->
			instance.group(
				Codec.STRING.fieldOf("id").forGetter(Alternative::id),
				DEFINITION_CODEC.fieldOf("predicate").forGetter(Alternative::predicate),
				STRINGS_CODEC.optionalFieldOf("facts", ObjectList.of()).forGetter(Alternative::facts),
			).apply(instance, ::Alternative)
		}
		private val ALTERNATIVES_CODEC: Codec<ObjectList<Alternative>> = ALTERNATIVE_CODEC.listOf().xmap({ ObjectArrayList(it) }, { it })

		val ALL_OF_CODEC: MapCodec<CombinedPredicate> = DEFINITIONS_CODEC.fieldOf("items").xmap(
			{ definitions ->
				create(
					Selection.ALL,
					ObjectArrayList<Branch>().also { out ->
						definitions.forEachIndexed { index, definition -> out.add(Branch(index.toString(), definition, PatternPredicateCompiler.compile(definition))) }
					},
					ObjectList.of(),
				)
			},
			{ combined -> ObjectArrayList(combined.expression.branches.map { it.definition }) },
		)
		val ANY_OF_CODEC: MapCodec<CombinedPredicate> = alternativesCodec(false)
		val ONE_OF_CODEC: MapCodec<CombinedPredicate> = alternativesCodec(true)
		val NOT_CODEC: MapCodec<CombinedPredicate> = DEFINITION_CODEC.fieldOf("item").xmap(
			{ definition -> create(Selection.NOT, ObjectList.of(Branch("not", definition, PatternPredicateCompiler.compile(definition))), ObjectList.of()) },
			{ combined -> combined.expression.branches.first().definition },
		)

		private fun alternativesCodec(exclusive: Boolean): MapCodec<CombinedPredicate> = RecordCodecBuilder.mapCodec { instance ->
			instance.group(
				ALTERNATIVES_CODEC.fieldOf("alternatives").forGetter<CombinedPredicate> { combined ->
					ObjectArrayList(combined.expression.branches.map { Alternative(it.id, it.definition, ObjectList.of()) })
				},
				Codec.STRING.optionalFieldOf("selection").forGetter<CombinedPredicate> {
					if (it.expression.selection == Selection.EXCLUSIVE) Optional.of("exclusive") else Optional.empty()
				},
				STRINGS_CODEC.optionalFieldOf("priority", ObjectList.of()).forGetter<CombinedPredicate> { it.expression.priority },
			).apply(instance) { alternatives, selection, priority ->
				val branches = ObjectArrayList<Branch>()
				for (alternative in alternatives) {
					val facts = ObjectArrayList(alternative.predicate.facts)
					facts.addAll(alternative.facts)
					val definition = alternative.predicate.copy(facts = facts)
					branches.add(Branch(alternative.id, definition, PatternPredicateCompiler.compile(definition)))
				}
				create(if (exclusive || selection.orElse(null) == "exclusive") Selection.EXCLUSIVE else Selection.PRIORITY, branches, priority)
			}
		}
		private fun create(selection: Selection, branches: ObjectList<Branch>, priority: ObjectList<String>): CombinedPredicate = CombinedPredicate(Expression(selection, branches, priority))

		private fun nested(map: Map<*, *>): PatternPredicateDefinition {
			val properties = Object2ObjectLinkedOpenHashMap<String, Any>()
			map.forEach { (key, value) -> if (key != "type" && key != "facts" && key != "name" && value != null) properties[key.toString()] = value }
			val facts = ObjectArrayList<String>()
			(map["facts"] as? List<*>)?.forEach { facts.add(it.toString()) }
			return PatternPredicateDefinition(map["type"].toString(), map["name"]?.toString() ?: "", properties, facts)
		}
	}
}
