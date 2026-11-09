package com.gregtechceu.gtceu.api.multiblock.structurepredicate

import com.gregtechceu.gtceu.GTCEu
import com.gregtechceu.gtceu.api.machine.MultiblockMachineDefinition
import com.gregtechceu.gtceu.api.multiblock.MultiblockBlockInfo
import com.gregtechceu.gtceu.api.multiblock.MultiblockState
import com.gregtechceu.gtceu.api.multiblock.error.PatternStringError

import net.minecraft.network.chat.Component
import net.minecraft.network.chat.ComponentSerialization
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.state.BlockState

import com.mojang.serialization.Codec
import com.mojang.serialization.MapCodec
import com.mojang.serialization.codecs.RecordCodecBuilder
import it.unimi.dsi.fastutil.objects.ObjectArrayList
import it.unimi.dsi.fastutil.objects.ObjectImmutableList
import it.unimi.dsi.fastutil.objects.ObjectList

import java.util.Optional

@JvmRecord
data class RestrictedPredicate(val predicate: StructurePredicate, val minCount: Optional<Int>, val maxCount: Optional<Int>, val minCountByLayer: Optional<Int>, val maxCountByLayer: Optional<Int>, val previewCount: Optional<Int>, val tooltips: Optional<ObjectList<Component>>) : StructurePredicate {
	override fun type(): StructurePredicateType<*> = TYPE
	override fun observeCandidate(candidateState: BlockState): PatternObservation = predicate.observeCandidate(candidateState)
	private data class Position(val child: PatternObservation) : PatternObservation.PositionMatch
	private fun hasCapacity(state: MultiblockState): Boolean = (maxCount.isEmpty || state.structureGlobalCount.getInt(this) < maxCount.get()) &&
		(maxCountByLayer.isEmpty || state.structureLayerCount.getInt(this) < maxCountByLayer.get())
	override fun observePosition(state: MultiblockState): PatternObservation = if (hasCapacity(state)) predicate.observePosition(state).let { it.withPositionMatch(Position(it)) } else PatternObservation.of(false)
	override fun canAcceptPosition(state: MultiblockState): Boolean = hasCapacity(state) && predicate.canAcceptPosition(state)
	override fun acceptPosition(state: MultiblockState, observation: PatternObservation): Boolean {
		if (!observation.matched) return reserveMinimumCounters(state)
		val observed = observation.positionMatch as Position
		return testGlobal(state, true) && testLayer(state, true) && predicate.acceptPosition(state, observed.child)
	}
	override fun configureRuntime(predicate: PatternPredicate) = this@RestrictedPredicate.predicate.configureRuntime(predicate)
	override fun candidateStates(): ObjectList<BlockState> = predicate.candidateStates()
	override fun candidates(): ObjectList<MultiblockBlockInfo> = predicate.candidates()
	override fun blockCandidates(): ObjectList<Block> = predicate.blockCandidates()
	override fun previewChoices(definition: MultiblockMachineDefinition): ObjectList<StructurePreviewChoice> {
		val constraint = StructurePreviewConstraint(this, minCount, maxCount, minCountByLayer, maxCountByLayer, previewCount)
		val tips = tooltips.orElse(ObjectList.of())
		return predicate.previewChoices(definition).stream().map { it.restrictedBy(constraint, tips) }.collect(ObjectImmutableList.toList())
	}
	override fun test(multiblockState: MultiblockState, mutateCount: Boolean): Boolean {
		if (!predicate.observePosition(multiblockState).matched) {
			if (mutateCount) reserveMinimumCounters(multiblockState)
			return false
		}
		return testGlobal(multiblockState, mutateCount) && testLayer(multiblockState, mutateCount) && predicate.test(multiblockState, mutateCount)
	}
	fun reserveMinimumCounters(state: MultiblockState): Boolean {
		if (minCount.isPresent) state.structureGlobalCount.putIfAbsent(this, 0)
		if (minCountByLayer.isPresent) state.structureLayerCount.putIfAbsent(this, 0)
		return false
	}
	private fun testGlobal(state: MultiblockState, mutateCount: Boolean): Boolean {
		if (minCount.isEmpty && maxCount.isEmpty) return true
		val count = if (mutateCount) state.structureGlobalCount.mergeInt(this, 1, Integer::sum) else state.structureGlobalCount.getInt(this)
		if (maxCount.isEmpty || count <= maxCount.get()) return true
		state.setError(PatternStringError("gtpm.multiblock.pattern.error.limited"))
		return false
	}
	private fun testLayer(state: MultiblockState, mutateCount: Boolean): Boolean {
		if (minCountByLayer.isEmpty && maxCountByLayer.isEmpty) return true
		val count = if (mutateCount) state.structureLayerCount.mergeInt(this, 1, Integer::sum) else state.structureLayerCount.getInt(this)
		if (maxCountByLayer.isEmpty || count <= maxCountByLayer.get()) return true
		state.setError(PatternStringError("gtpm.multiblock.pattern.error.limited"))
		return false
	}
	override fun hasAir(): Boolean = predicate.hasAir()

	class Builder {
		lateinit var base: StructurePredicate
		var minCount: Int? = null
		var maxCount: Int? = null
		var minCountByLayer: Int? = null
		var maxCountByLayer: Int? = null
		var previewCount: Int? = null
		var tooltips: ObjectList<Component>? = null

		fun base(value: StructurePredicate): Builder = apply { base = value }
		fun minCount(value: Int?): Builder = apply { minCount = value }
		fun maxCount(value: Int?): Builder = apply { maxCount = value }
		fun minCountByLayer(value: Int?): Builder = apply { minCountByLayer = value }
		fun maxCountByLayer(value: Int?): Builder = apply { maxCountByLayer = value }
		fun previewCount(value: Int?): Builder = apply { previewCount = value }
		fun tooltips(value: ObjectList<Component>?): Builder = apply { tooltips = value }
		fun exactCount(count: Int): Builder = minCount(count).maxCount(count)
		fun exactCountByLayer(count: Int): Builder = minCountByLayer(count).maxCountByLayer(count)
		fun build(): RestrictedPredicate = RestrictedPredicate(
			base,
			Optional.ofNullable(minCount),
			Optional.ofNullable(maxCount),
			Optional.ofNullable(minCountByLayer),
			Optional.ofNullable(maxCountByLayer),
			Optional.ofNullable(previewCount),
			Optional.ofNullable(tooltips),
		)
	}

	companion object {
		val TYPE: StructurePredicateType<RestrictedPredicate> by lazy { StructurePredicateType.register(GTCEu.id("restricted"), CODEC) }

		@JvmStatic
		fun builder(): Builder = Builder()

		@JvmField
		val CODEC: MapCodec<RestrictedPredicate> = RecordCodecBuilder.mapCodec { instance ->
			instance.group(
				StructurePredicate.CODEC.fieldOf("predicate").forGetter(RestrictedPredicate::predicate),
				Codec.INT.optionalFieldOf("minCount").forGetter(RestrictedPredicate::minCount),
				Codec.INT.optionalFieldOf("maxCount").forGetter(RestrictedPredicate::maxCount),
				Codec.INT.optionalFieldOf("minCountByLayer").forGetter(RestrictedPredicate::minCountByLayer),
				Codec.INT.optionalFieldOf("maxCountByLayer").forGetter(RestrictedPredicate::maxCountByLayer),
				Codec.INT.optionalFieldOf("previewCount").forGetter(RestrictedPredicate::previewCount),
				ComponentSerialization.CODEC.listOf().xmap<ObjectList<Component>>({ ObjectArrayList(it) }, { it }).optionalFieldOf("tooltips").forGetter(RestrictedPredicate::tooltips),
			).apply(instance, ::RestrictedPredicate)
		}
	}
}
