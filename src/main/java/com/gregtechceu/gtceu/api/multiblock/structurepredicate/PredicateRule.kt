package com.gregtechceu.gtceu.api.multiblock.structurepredicate

import com.gregtechceu.gtceu.api.capability.recipe.IO
import com.gregtechceu.gtceu.api.machine.MultiblockMachineDefinition
import com.gregtechceu.gtceu.api.multiblock.MultiblockBlockInfo
import com.gregtechceu.gtceu.api.multiblock.MultiblockState
import com.gregtechceu.gtceu.api.multiblock.error.SinglePredicateError
import com.gregtechceu.gtceu.api.multiblock.error.StructureIssue
import com.gregtechceu.gtceu.api.multiblock.pattern.match.PatternDiagnostic
import com.gregtechceu.gtceu.data.lang.LangHandler

import net.minecraft.network.chat.Component
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.LiquidBlock
import net.minecraft.world.level.block.state.BlockState
import net.neoforged.api.distmarker.Dist
import net.neoforged.api.distmarker.OnlyIn

import it.unimi.dsi.fastutil.longs.Long2ObjectArrayMap
import it.unimi.dsi.fastutil.longs.Long2ObjectMap
import it.unimi.dsi.fastutil.longs.LongOpenHashSet
import it.unimi.dsi.fastutil.objects.ObjectArrayList
import it.unimi.dsi.fastutil.objects.ObjectList
import it.unimi.dsi.fastutil.objects.ObjectLists
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet
import it.unimi.dsi.fastutil.objects.ObjectSet

import java.util.function.Function
import java.util.function.Predicate
import java.util.function.Supplier

/** One block matching rule, including its limits and build metadata. */
open class PredicateRule {
	/** The actual registered rule remains attached when fluent metadata is flattened by OR. */
	internal var source: StructurePredicate? = null

	/** Retains the real observed runtime when OR/copy flattens its metadata rule. */
	internal var observedPredicate: ObservedPatternPredicate? = null

	fun description(state: MultiblockState, excluded: Block? = null): Component? {
		val source = source
		return if (source == null) StructureIssue.blocks(candidateStates().mapTo(ObjectArrayList()) { it.block }, excluded) else source.description(state, excluded)
	}

	internal fun diagnose(state: MultiblockState, failure: PatternDiagnostic): PatternDiagnostic {
		if (maxCount >= 0 && state.globalCount.getInt(this) >= maxCount) return failure.copy(issue = StructureIssue.count(failure, StructureIssue.Count.MAXIMUM, description(state), maxCount, maxCount + 1))
		if (maxLayerCount >= 0 && state.layerCount.getInt(this) >= maxLayerCount) return failure.copy(issue = StructureIssue.count(failure, StructureIssue.Count.LAYER_MAXIMUM, description(state), maxLayerCount, maxLayerCount + 1))
		return source?.diagnose(state, failure) ?: observedPredicate?.diagnose(state, failure) ?: failure.copy(issue = StructureIssue.mismatch(failure, state.predicate.description(state, failure.actual?.block)))
	}

	private var preparedPredicate: Predicate<MultiblockState>? = null
	private var preparedObserver: Function<MultiblockState, PatternObservation>? = null

	internal fun supportsPreparation(): Boolean = javaClass == PredicateRule::class.java &&
		(source != null || observedPredicate is CombinedPredicate) && predicate === preparedPredicate && positionObserver === preparedObserver

	internal fun sealPreparedCallbacks() {
		preparedPredicate = predicate
		preparedObserver = positionObserver
	}

	var candidateObserver: Function<BlockState, PatternObservation>? = null
	var positionObserver: Function<MultiblockState, PatternObservation>? = null
	var previewStates: Supplier<ObjectList<BlockState>>? = null

	/** Extension point for pure candidate semantics; metadata-only rules default to their declared blocks. */
	open fun observeCandidate(state: BlockState): PatternObservation = candidateObserver?.apply(state)
		?: PatternObservation.of(candidates?.get()?.contains(state.block) == true || (candidates == null && blockInfo.get()?.blockState == state))

	open fun candidateStates(): ObjectList<BlockState> = previewStates?.get() ?: ObjectArrayList<BlockState>().also { out ->
		val blocks = candidates
		if (blocks == null) {
			blockInfo.get()?.let { out.add(it.blockState) }
		} else {
			blocks.get().forEach { out.add(it.defaultBlockState()) }
		}
	}

	/** Keeps each registered candidate family intact when inspecting a selected preview position. */
	fun getPreviewChoices(definition: MultiblockMachineDefinition): ObjectList<StructurePreviewChoice> = source?.previewChoices(definition)
		?: ObjectLists.singleton(
			StructurePreviewChoice.unrestricted(candidateStates().mapTo(ObjectArrayList(), ::MultiblockBlockInfo)).withObserver(::observeCandidate),
		)

	@JvmField
	var candidates: Supplier<Array<Block>>? = null

	@JvmField
	var blockInfo: Supplier<MultiblockBlockInfo?> = NULL_BLOCK_INFO

	@JvmField
	var predicate: Predicate<MultiblockState> = Predicate { false }

	@JvmField
	var toolTips: ObjectList<Component>? = null

	@JvmField
	var minCount: Int = -1

	@JvmField
	var maxCount: Int = -1

	@JvmField
	var minLayerCount: Int = -1

	@JvmField
	var maxLayerCount: Int = -1

	@JvmField
	var previewCount: Int = -1

	@JvmField
	var disableRenderFormed: Boolean = false

	@JvmField
	var io: IO = IO.BOTH

	@JvmField
	var slotName: String? = null

	constructor()

	constructor(source: StructurePredicate) : this() {
		this.source = source
		predicate = Predicate { source.test(it, true) }
		blockInfo = Supplier { source.candidates().firstOrNull() }
		candidates = Supplier { source.blockCandidates().toTypedArray() }
		candidateObserver = Function(source::observeCandidate)
		positionObserver = Function(source::observePosition)
		previewStates = Supplier(source::candidateStates)
		preparedPredicate = predicate
		preparedObserver = positionObserver
	}

	constructor(
		predicate: Predicate<MultiblockState>,
		blockInfo: Supplier<out MultiblockBlockInfo?>?,
		candidates: Supplier<Array<Block>>?,
	) {
		this.predicate = predicate
		this.blockInfo = blockInfo?.let { supplier -> Supplier { supplier.get() } } ?: NULL_BLOCK_INFO
		this.candidates = candidates
	}

	constructor(predicate: Predicate<MultiblockState>, candidates: Supplier<Array<MultiblockBlockInfo>>?) : this(
		predicate,
		candidates?.let { supplier ->
			Supplier {
				val infos = supplier.get()
				if (infos.isEmpty()) MultiblockBlockInfo.EMPTY else infos[0]
			}
		},
		candidates?.let { supplier ->
			Supplier {
				supplier.get().map { it.blockState.block }.toTypedArray()
			}
		},
	)

	open fun buildPredicate(): PredicateRule = this

	@OnlyIn(Dist.CLIENT)
	fun getToolTips(predicates: PatternPredicate?): ObjectList<Component> {
		val result = ObjectArrayList<Component>()
		toolTips?.let(result::addAll)
		when {
			minCount == maxCount && maxCount != -1 ->
				result.add(Component.translatable("gtpm.multiblock.pattern.error.limited_exact", minCount))

			minCount != maxCount && minCount != -1 && maxCount != -1 ->
				result.add(Component.translatable("gtpm.multiblock.pattern.error.limited_within", minCount, maxCount))

			else -> {
				if (minCount != -1) {
					result.add(LangHandler.getFromMultiLang("gtpm.multiblock.pattern.error.limited", 1, minCount))
				}
				if (maxCount != -1) {
					result.add(LangHandler.getFromMultiLang("gtpm.multiblock.pattern.error.limited", 0, maxCount))
				}
			}
		}
		if (predicates == null) return result
		if (predicates.isSingle()) result.add(Component.translatable("gtpm.multiblock.pattern.single"))
		if (predicates.hasAir()) result.add(Component.translatable("gtpm.multiblock.pattern.replaceable_air"))
		return result
	}

	fun test(blockWorldState: MultiblockState): Boolean = if (observedPredicate != null) {
		acceptPosition(blockWorldState, observePosition(blockWorldState, false), false)
	} else {
		source?.let { testSource(it, blockWorldState, false) }
			?: (predicate.test(blockWorldState) && checkInnerConditions(blockWorldState))
	}

	internal fun observePosition(state: MultiblockState, limited: Boolean): PatternObservation {
		if (!canAcceptPosition(state, limited)) return PatternObservation.of(false)
		return observeAvailablePosition(state)
	}

	internal fun observeAvailablePosition(state: MultiblockState): PatternObservation = positionObserver?.apply(state) ?: PatternObservation.of(predicate.test(state))

	internal fun canAcceptPosition(state: MultiblockState, limited: Boolean): Boolean = (
		!limited || (
			(maxCount == -1 || state.globalCount.getInt(this) < maxCount) &&
				(maxLayerCount == -1 || state.layerCount.getInt(this) < maxLayerCount)
			)
		)

	private fun testSource(source: StructurePredicate, state: MultiblockState, limited: Boolean): Boolean {
		val matched = source.observePosition(state).matched
		if (limited && (!checkGlobal(state, matched) || !checkLayer(state, matched))) return false
		return matched && source.test(state, true) && checkInnerConditions(state)
	}

	fun testLimited(blockWorldState: MultiblockState): Boolean = if (observedPredicate != null) {
		acceptPosition(blockWorldState, observePosition(blockWorldState, true), true)
	} else {
		source?.let { testSource(it, blockWorldState, true) }
			?: acceptMatched(blockWorldState, predicate.test(blockWorldState), true)
	}

	/** The observer has already matched this rule; only count reservation and its real domain commit remain. */
	internal fun acceptPosition(state: MultiblockState, observation: PatternObservation, limited: Boolean): Boolean {
		if (!observation.matched) {
			if (limited) {
				if (minCount != -1) state.globalCount.putIfAbsent(this, 0)
				if (minLayerCount != -1) state.layerCount.putIfAbsent(this, 0)
			}
			source?.acceptPosition(state, observation)
			return false
		}
		if (limited && (!checkGlobal(state, true) || !checkLayer(state, true))) return false
		if (source?.acceptPosition(state, observation) == false) return false
		if (observedPredicate?.acceptObservation(state, observation) == false) return false
		return checkInnerConditions(state)
	}

	internal fun acceptMatched(state: MultiblockState, matched: Boolean, limited: Boolean): Boolean {
		if (limited && (!checkGlobal(state, matched) || !checkLayer(state, matched))) return false
		return matched && checkInnerConditions(state)
	}

	private fun checkInnerConditions(blockWorldState: MultiblockState): Boolean {
		if (disableRenderFormed) {
			blockWorldState.facts.getOrCreate("renderMask") { LongOpenHashSet() }
				.add(blockWorldState.pos.asLong())
		}
		if (io != IO.BOTH) {
			if (blockWorldState.io == IO.BOTH) {
				blockWorldState.io = io
			} else if (blockWorldState.io != io) {
				blockWorldState.io = null
			}
		}
		slotName?.let { name ->
			val slots: Long2ObjectMap<ObjectSet<String>> = blockWorldState.facts.getOrCreate(
				"slots",
			) { Long2ObjectArrayMap() }
			slots.computeIfAbsent(blockWorldState.pos.asLong()) { ObjectOpenHashSet() }.add(name)
		}
		return true
	}

	fun testGlobal(blockWorldState: MultiblockState): Boolean = if (minCount == -1 && maxCount == -1) {
		true
	} else {
		checkGlobal(blockWorldState, predicate.test(blockWorldState))
	}

	private fun checkGlobal(state: MultiblockState, matched: Boolean): Boolean {
		if (minCount == -1 && maxCount == -1) return true
		val count = state.globalCount.mergeInt(this, if (matched) 1 else 0, Integer::sum)
		if (maxCount == -1 || count <= maxCount) return matched
		state.setError(SinglePredicateError(this, 0))
		return false
	}

	fun testLayer(blockWorldState: MultiblockState): Boolean = if (minLayerCount == -1 && maxLayerCount == -1) {
		true
	} else {
		checkLayer(blockWorldState, predicate.test(blockWorldState))
	}

	private fun checkLayer(state: MultiblockState, matched: Boolean): Boolean {
		if (minLayerCount == -1 && maxLayerCount == -1) return true
		val count = state.layerCount.mergeInt(this, if (matched) 1 else 0, Integer::sum)
		if (maxLayerCount == -1 || count <= maxLayerCount) return matched
		state.setError(SinglePredicateError(this, 2))
		return false
	}

	fun getCandidates(): ObjectList<ItemStack> {
		val source = candidates ?: return ObjectLists.emptyList()
		val result = ObjectArrayList<ItemStack>()
		source.get().forEach { block ->
			val item = toItem(block)
			if (item != Items.AIR) result.add(item.defaultInstance)
		}
		return ObjectLists.unmodifiable(result)
	}

	fun isAny(): Boolean = source?.isAny() == true

	fun isAir(): Boolean = source?.isAir() == true

	fun addCache(): Boolean = source?.addCache() ?: true

	companion object {
		private val NULL_BLOCK_INFO = Supplier<MultiblockBlockInfo?> { null }

		@JvmStatic
		fun toItem(block: Block): Item = if (block is LiquidBlock) {
			block.fluid.bucket
		} else {
			block.asItem()
		}
	}
}
