package com.gregtechceu.gtceu.api.multiblock.structurepredicate

import com.gregtechceu.gtceu.GTCEu
import com.gregtechceu.gtceu.api.multiblock.MultiblockBlockInfo
import com.gregtechceu.gtceu.api.multiblock.MultiblockState

import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.tags.BlockTags
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.state.BlockState

import com.mojang.serialization.MapCodec
import it.unimi.dsi.fastutil.longs.Long2BooleanOpenHashMap
import it.unimi.dsi.fastutil.objects.ObjectImmutableList
import it.unimi.dsi.fastutil.objects.ObjectList

/** Burnable-log positions remain available to the charcoal conversion stage. */
enum class CharcoalLogOrAirPredicate : StructurePredicate {
	INSTANCE,
	;

	override fun type(): StructurePredicateType<*> = TYPE
	override fun observeCandidate(state: BlockState): PatternObservation = PatternObservation.of(state.isAir || state.`is`(BlockTags.LOGS_THAT_BURN))
	private data class Position(val log: Boolean) : PatternObservation.PositionMatch
	override fun observePosition(state: MultiblockState): PatternObservation {
		val log = state.blockState.`is`(BlockTags.LOGS_THAT_BURN)
		return PatternObservation.of(log || state.blockState.isAir).withPositionMatch(Position(log))
	}
	override fun acceptPosition(state: MultiblockState, observation: PatternObservation): Boolean {
		if (!observation.matched) return false
		val position = observation.positionMatch as Position
		state.facts.getOrCreate("logPos") { Long2BooleanOpenHashMap() }.put(state.pos.asLong(), position.log)
		return true
	}

	override fun test(multiblockState: MultiblockState, mutateCount: Boolean): Boolean {
		val log = multiblockState.blockState.`is`(BlockTags.LOGS_THAT_BURN)
		if (!log && !multiblockState.blockState.isAir) return false
		if (mutateCount) multiblockState.facts.getOrCreate("logPos") { Long2BooleanOpenHashMap() }.put(multiblockState.pos.asLong(), log)
		return true
	}
	override fun candidates(): ObjectList<MultiblockBlockInfo> = blockCandidates().stream().map(MultiblockBlockInfo::fromBlock).collect(ObjectImmutableList.toList())
	override fun blockCandidates(): ObjectList<Block> = BuiltInRegistries.BLOCK.getTag(BlockTags.LOGS_THAT_BURN).stream()
		.flatMap { it.stream() }.map { it.value() }.collect(ObjectImmutableList.toList())
	override fun hasAir(): Boolean = true

	companion object {
		val TYPE: StructurePredicateType<CharcoalLogOrAirPredicate> by lazy { StructurePredicateType.register(GTCEu.id("charcoal_log_or_air"), CODEC) }

		@JvmField
		val CODEC: MapCodec<CharcoalLogOrAirPredicate> = MapCodec.unit(INSTANCE)
	}
}
