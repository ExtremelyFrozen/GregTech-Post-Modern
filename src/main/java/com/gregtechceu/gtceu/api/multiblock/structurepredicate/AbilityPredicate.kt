package com.gregtechceu.gtceu.api.multiblock.structurepredicate

import com.gregtechceu.gtceu.GTCEu
import com.gregtechceu.gtceu.api.block.MetaMachineBlock
import com.gregtechceu.gtceu.api.capability.recipe.IO
import com.gregtechceu.gtceu.api.machine.MetaMachine
import com.gregtechceu.gtceu.api.machine.feature.ITieredMachine
import com.gregtechceu.gtceu.api.machine.feature.multiblock.IMultiPart
import com.gregtechceu.gtceu.api.machine.multiblock.PartAbility
import com.gregtechceu.gtceu.api.multiblock.MultiblockBlockInfo
import com.gregtechceu.gtceu.api.multiblock.MultiblockState
import com.gregtechceu.gtceu.api.multiblock.util.RelativeDirection

import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.state.BlockState

import com.mojang.serialization.Codec
import com.mojang.serialization.DataResult
import com.mojang.serialization.MapCodec
import com.mojang.serialization.codecs.RecordCodecBuilder
import it.unimi.dsi.fastutil.ints.IntOpenHashSet
import it.unimi.dsi.fastutil.ints.IntSet
import it.unimi.dsi.fastutil.objects.ObjectArrayList
import it.unimi.dsi.fastutil.objects.ObjectImmutableList
import it.unimi.dsi.fastutil.objects.ObjectList

import java.util.Optional

/** One ability definition owns block candidates, real part/tier checks and all published ability facts. */
@JvmRecord
data class AbilityPredicate(val abilities: ObjectList<PartAbility>, val range: Optional<TierRange>, val allowedTiers: Optional<IntSet>, val io: Optional<String>, val facing: Optional<RelativeDirection>) : StructurePredicate {
	constructor(abilities: ObjectList<PartAbility>) : this(abilities, Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty())

	override fun type(): StructurePredicateType<*> = TYPE
	override fun observeCandidate(candidateState: BlockState): PatternObservation {
		val block = candidateState.block as? MetaMachineBlock
		return observe(candidateState, block?.definition?.tier, block != null)
	}
	override fun observePosition(state: MultiblockState): PatternObservation {
		val machine = MetaMachine.getMachine(state.world, state.pos)
		val required = facing.orElse(null)
		if (required != null) {
			val controller = state.controller ?: return PatternObservation.of(false)
			val direction = required.getRelative(controller.frontFacing, controller.upwardsFacing, state.isNeededFlip)
			if (machine?.frontFacing != direction) return PatternObservation.of(false)
		}
		return observe(state.blockState, (machine as? ITieredMachine)?.tier ?: machine?.definition?.tier, machine is IMultiPart)
	}
	private fun observe(state: BlockState, tier: Int?, part: Boolean): PatternObservation {
		val facts = ObjectArrayList<String>()
		val limits = range.orElse(null)
		if (part && tier != null && (limits == null || tier in limits.min..limits.max) && (!allowedTiers.isPresent || allowedTiers.get().contains(tier))) {
			abilities.forEach { if (it.isApplicable(state.block)) facts.add("part.${it.name}") }
		}
		val mode = io.orElse(null)
		val matched = facts.isNotEmpty() && (
			mode == null || mode == "both" || facts.any {
				if (mode == "input") it.contains("input") || it.contains("import") else it.contains("output") || it.contains("export")
			}
			)
		if (matched) facts.add("part")
		return PatternObservation.of(matched, facts)
	}
	override fun configureRuntime(predicate: PatternPredicate) {
		facing.ifPresent(predicate::setRelativeDirection)
		io.ifPresent { mode ->
			predicate.setIO(
				when (mode) {
					"input" -> IO.IN
					"output" -> IO.OUT
					else -> IO.BOTH
				},
			)
		}
	}
	override fun test(multiblockState: MultiblockState, mutateCount: Boolean): Boolean = observePosition(multiblockState).matched
	override fun candidates(): ObjectList<MultiblockBlockInfo> = blockCandidates().stream().map(::MultiblockBlockInfo).collect(ObjectImmutableList.toList())
	override fun blockCandidates(): ObjectList<Block> = abilities.stream().flatMap { ability ->
		val limits = range.orElse(null)
		(if (limits == null) ability.allBlocks else ability.getBlockRange(limits.min, limits.max)).stream()
	}.filter { block -> !allowedTiers.isPresent || (block is MetaMachineBlock && allowedTiers.get().contains(block.definition.tier)) }
		.collect(ObjectImmutableList.toList())

	data class TierRange(val min: Int, val max: Int)

	companion object {
		val ABILITY_CODEC: Codec<PartAbility> = Codec.STRING.comapFlatMap(
			{ name -> PartAbility.byName(name).map { DataResult.success(it) }.orElseGet { DataResult.error { "Unknown part ability: $name" } } },
			PartAbility::getName,
		)
		private val RANGE_CODEC: Codec<TierRange> = RecordCodecBuilder.create { instance ->
			instance.group(Codec.INT.fieldOf("min").forGetter(TierRange::min), Codec.INT.fieldOf("max").forGetter(TierRange::max)).apply(instance, ::TierRange)
		}
		private val TIERS_CODEC: Codec<IntSet> = Codec.INT.listOf().xmap({ IntOpenHashSet(it) }, { ObjectArrayList(it) })
		val TYPE: StructurePredicateType<AbilityPredicate> by lazy { StructurePredicateType.register(GTCEu.id("part"), CODEC) }
		val CODEC: MapCodec<AbilityPredicate> = RecordCodecBuilder.mapCodec { instance ->
			instance.group(
				PredicateCodecs.oneOrMore(ABILITY_CODEC).fieldOf("abilities").forGetter(AbilityPredicate::abilities),
				RANGE_CODEC.optionalFieldOf("tiers").forGetter(AbilityPredicate::range),
				TIERS_CODEC.optionalFieldOf("allowedTiers").forGetter(AbilityPredicate::allowedTiers),
				Codec.STRING.optionalFieldOf("io").forGetter(AbilityPredicate::io),
				Codec.STRING.xmap({ RelativeDirection.valueOf(it.uppercase()) }, { it.name.lowercase() }).optionalFieldOf("facing").forGetter(AbilityPredicate::facing),
			).apply(instance, ::AbilityPredicate)
		}
	}
}
