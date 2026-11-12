package com.gregtechceu.gtceu.api.multiblock.structurepredicate

import com.gregtechceu.gtceu.api.block.MetaMachineBlock
import com.gregtechceu.gtceu.api.block.property.GTBlockStateProperties.ACTIVE
import com.gregtechceu.gtceu.api.capability.recipe.EURecipeCapability
import com.gregtechceu.gtceu.api.capability.recipe.FluidRecipeCapability
import com.gregtechceu.gtceu.api.capability.recipe.IO
import com.gregtechceu.gtceu.api.capability.recipe.ItemRecipeCapability
import com.gregtechceu.gtceu.api.data.chemical.material.Material
import com.gregtechceu.gtceu.api.machine.MachineDefinition
import com.gregtechceu.gtceu.api.machine.MultiblockMachineDefinition
import com.gregtechceu.gtceu.api.machine.multiblock.PartAbility
import com.gregtechceu.gtceu.api.multiblock.MultiblockBlockInfo
import com.gregtechceu.gtceu.api.multiblock.MultiblockState
import com.gregtechceu.gtceu.api.multiblock.pattern.match.PatternDiagnostic
import com.gregtechceu.gtceu.api.multiblock.util.RelativeDirection
import com.gregtechceu.gtceu.api.recipe.GTRecipeType
import com.gregtechceu.gtceu.common.block.LampBlock
import com.gregtechceu.gtceu.common.data.GTBlocks.BORDERLESS_LAMPS
import com.gregtechceu.gtceu.common.data.GTBlocks.LAMPS
import com.gregtechceu.gtceu.config.ConfigHolder

import net.minecraft.core.Direction
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.network.chat.Component
import net.minecraft.tags.TagKey
import net.minecraft.world.item.DyeColor
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.material.Fluid

import com.tterrag.registrate.util.entry.BlockEntry
import it.unimi.dsi.fastutil.ints.IntOpenHashSet
import it.unimi.dsi.fastutil.longs.LongSet
import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap
import it.unimi.dsi.fastutil.objects.ObjectArrayList
import it.unimi.dsi.fastutil.objects.ObjectLinkedOpenHashSet
import it.unimi.dsi.fastutil.objects.ObjectList
import it.unimi.dsi.fastutil.objects.ObjectLists
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet

import java.util.EnumMap
import java.util.Optional
import java.util.function.Function
import java.util.function.Predicate
import java.util.function.Supplier

/** Composite runtime predicate used by the matcher and structure planner. */
open class PatternPredicate {
	/** Unknown Java rules keep their full observation; registered rules explicitly describe reusable inputs. */
	open fun hasPositionDependencies(): Boolean = javaClass == PatternPredicate::class.java && rulesHavePositionDependencies()

	protected fun rulesHavePositionDependencies(): Boolean = common.all { it.source?.hasPositionDependencies() == true } && limited.all { it.source?.hasPositionDependencies() == true }

	open fun canReuseObservation(): Boolean = javaClass == PatternPredicate::class.java && limited.isEmpty() && common.all { it.source?.canReuseObservation() == true }

	fun collectDependencies(state: MultiblockState, positions: LongSet) {
		positions.add(state.pos.asLong())
		for (rule in common) rule.source?.collectDependencies(state, positions)
		for (rule in limited) rule.source?.collectDependencies(state, positions)
		for (rule in common) (rule.observedPredicate as? CombinedPredicate)?.preparedBranches?.forEach { it.collectDependencies(state, positions) }
		for (rule in limited) (rule.observedPredicate as? CombinedPredicate)?.preparedBranches?.forEach { it.collectDependencies(state, positions) }
	}

	/** Builds the failure shown at this position; extensions retain its path and coordinates when adding detail. */
	open fun diagnose(failure: PatternDiagnostic): PatternDiagnostic = (limited.asSequence() + common.asSequence()).firstNotNullOfOrNull { it.source }?.diagnose(failure) ?: failure

	/** Observes the actual rule without constructing entities or mutating runtime metadata. */
	open fun observeCandidate(state: BlockState): PatternObservation = observeRulesCandidate(state) ?: PatternObservation.of(false)

	/** Definition-aware source choices retain their own observer and nested restrictions. */
	open fun observeCandidate(state: BlockState, definition: MultiblockMachineDefinition): PatternObservation {
		for (rule in limited.asSequence() + common.asSequence()) {
			val source = rule.source
			if (source == null) {
				val observation = rule.observeCandidate(state)
				if (observation.matched) return observation.withAliases(facts)
			} else {
				for (choice in source.previewChoices(definition)) {
					val observation = choice.observeCandidate(state)
					if (observation.matched) return observation.withAliases(facts)
				}
			}
		}
		return PatternObservation.of(false)
	}

	private fun observeRulesCandidate(state: BlockState): PatternObservation? {
		for (rule in limited.asSequence() + common.asSequence()) {
			val observation = rule.observeCandidate(state)
			if (observation.matched) return observation.withAliases(facts)
		}
		return null
	}

	private data class RulePosition(val rule: PredicateRule, val limited: Boolean, val observation: PatternObservation, val available: Boolean)
	private data class Position(val rules: ObjectList<RulePosition>) : PatternObservation.PositionMatch

	/** Captures selected rules once; failed limited rules retain only their minimum-count bookkeeping. */
	open fun observePosition(state: MultiblockState): PatternObservation = observeRules(state, null)

	/** Count reservations are replayed in layout order; only independent, unchanged rule inputs are reused. */
	internal fun observePosition(state: MultiblockState, previous: PatternObservation): PatternObservation = if (javaClass == PatternPredicate::class.java) observeRules(state, previous.positionMatch as Position) else observePosition(state)

	private fun observeRules(state: MultiblockState, previous: Position?): PatternObservation {
		val rules = ObjectArrayList<RulePosition>()
		val observations = ObjectArrayList<PatternObservation>()
		var index = 0
		fun observe(rule: PredicateRule, available: Boolean): PatternObservation {
			val cached = previous?.rules?.getOrNull(index++)
			if (!available) return PatternObservation.of(false)
			if (cached != null && cached.rule === rule && cached.available && rule.source?.canReuseObservation() == true) return cached.observation
			return rule.observeAvailablePosition(state)
		}
		for (rule in limited) {
			val available = rule.canAcceptPosition(state, true)
			val observation = observe(rule, available)
			rules.add(RulePosition(rule, true, observation, available))
			if (observation.matched) observations.add(observation)
		}
		if (observations.isEmpty) {
			for (rule in common) {
				val observation = observe(rule, true)
				rules.add(RulePosition(rule, false, observation, true))
				if (!observation.matched) continue
				observations.add(observation)
				break
			}
		}
		if (observations.isEmpty) return PatternObservation.of(false).withPositionMatch(Position(rules))
		val observedFacts = ObjectOpenHashSet<String>()
		val properties = Object2ObjectLinkedOpenHashMap<String, Any>()
		for ((_, facts1, properties1) in observations) {
			observedFacts.addAll(facts1)
			for ((key, value) in properties1) {
				val previous = properties.putIfAbsent(key, value)
				if (previous != null && previous != value) return PatternObservation.of(false)
			}
		}
		return PatternObservation(true, observedFacts, properties, Position(rules)).withAliases(facts)
	}

	/** Equal rule inputs retain the old count reservations and OR selection, including exhausted branches. */
	internal fun matchesObservation(state: MultiblockState, observation: PatternObservation): Boolean {
		if (javaClass != PatternPredicate::class.java) return false
		val position = observation.positionMatch as Position
		for ((rule, _, observation1, available) in position.rules) {
			if (!available) continue
			if (rule.source?.canReuseObservation() != true || rule.observeAvailablePosition(state) != observation1) return false
		}
		return true
	}

	/** Consumes only this position's captured selection, without evaluating its rules again. */
	open fun acceptPosition(state: MultiblockState, observation: PatternObservation): Boolean {
		if (!observation.matched) return false
		val position = observation.positionMatch as Position
		var matched = false
		for ((rule, limited, observed) in position.rules) {
			if (rule.acceptPosition(state, observed, limited)) matched = true
		}
		if (matched) {
			state.setError(null)
			facts.forEach(state.facts::addFact)
		}
		return matched
	}

	/** Called after legacy count checks to retain the selected semantic branch's observation. */
	fun observeAccepted(state: MultiblockState): PatternObservation {
		for (rule in limited.asSequence() + common.asSequence()) {
			if ((rule.maxCount >= 0 && state.globalCount.getInt(rule) > rule.maxCount) || (rule.maxLayerCount >= 0 && state.layerCount.getInt(rule) > rule.maxLayerCount)) continue
			val observation = rule.positionObserver?.apply(state) ?: continue
			if (observation.matched) return observation.withAliases(facts)
		}
		val definition = state.lastController?.definition ?: state.controller?.definition
		return if (definition == null) observeCandidate(state.blockState) else observeCandidate(state.blockState, definition)
	}

	open fun getPreviewCandidates(): ObjectList<PatternPreviewCandidate> {
		val states = ObjectLinkedOpenHashSet<BlockState>()
		for (rule in limited.asSequence() + common.asSequence()) {
			states.addAll(rule.candidateStates())
		}
		if (isAny() || isAir() || hasAir()) states.add(Blocks.AIR.defaultBlockState())
		val result = ObjectArrayList<PatternPreviewCandidate>()
		states.forEach { state ->
			val observation = observeCandidate(state)
			if (observation.matched) result.add(PatternPreviewCandidate(MultiblockBlockInfo(state), observation))
		}
		return ObjectLists.unmodifiable(result)
	}

	open fun getPreviewCandidates(definition: MultiblockMachineDefinition): ObjectList<PatternPreviewCandidate> {
		val result = ObjectArrayList<PatternPreviewCandidate>()
		for (rule in limited.asSequence() + common.asSequence()) {
			val source = rule.source
			if (source == null) {
				for (state in rule.candidateStates()) {
					val observation = rule.observeCandidate(state)
					if (observation.matched) result.add(PatternPreviewCandidate(MultiblockBlockInfo(state), observation.withAliases(facts)))
				}
			} else {
				for (choice in source.previewChoices(definition)) {
					for (info in choice.candidates) {
						val observation = choice.observeCandidate(info.blockState)
						if (observation.matched) result.add(PatternPreviewCandidate(info, observation.withAliases(facts)))
					}
				}
			}
		}
		return result
	}

	@JvmField
	var common: ObjectList<PredicateRule> = ObjectArrayList()

	@JvmField
	var limited: ObjectList<PredicateRule> = ObjectArrayList()

	@JvmField
	var structurePatternPredicates: ObjectList<StructurePredicate> = ObjectArrayList()

	@JvmField
	var facts: ObjectList<String> = ObjectArrayList()

	@JvmField
	var direction: Function<MultiblockState, Direction?> = DEFAULT_DIRECTION

	internal fun supportsPreparedDirection(): Boolean = relativeDirection != null || fixedDirection != null || direction === DEFAULT_DIRECTION

	/** Rebuilds the native cache payload from a previously computed rule selection without observing the world. */
	internal fun restorePreparedPosition(observation: PatternObservation, children: List<PatternObservation>, available: BooleanArray): PatternObservation {
		val positions = ObjectArrayList<RulePosition>(children.size)
		for (index in children.indices) {
			val counted = index < limited.size
			val rule = if (counted) limited[index] else common[index - limited.size]
			positions.add(RulePosition(rule, counted, children[index], available[index]))
		}
		return observation.withPositionMatch(Position(positions))
	}

	@JvmField
	var fixedDirection: Direction? = null

	@JvmField
	var relativeDirection: RelativeDirection? = null

	constructor()

	constructor(
		predicate: Predicate<MultiblockState>,
		blockInfo: Supplier<MultiblockBlockInfo>,
		candidates: Supplier<Array<Block>>?,
	) : this() {
		common.add(PredicateRule(predicate, blockInfo, candidates))
	}

	constructor(
		predicate: Predicate<MultiblockState>,
		candidates: Supplier<Array<MultiblockBlockInfo>>,
	) : this(
		predicate,
		Supplier {
			val infos = candidates.get()
			if (infos.isEmpty()) MultiblockBlockInfo.EMPTY else infos[0]
		},
		Supplier { candidates.get().map { it.blockState.block }.toTypedArray() },
	)

	constructor(simplePredicate: PredicateRule) : this() {
		if (simplePredicate.minCount != -1 || simplePredicate.maxCount != -1) {
			limited.add(simplePredicate)
		} else {
			common.add(simplePredicate)
		}
	}

	constructor(structurePredicate: StructurePredicate) : this(PredicateRule(structurePredicate)) {
		structurePatternPredicates.add(structurePredicate)
		structurePredicate.configureRuntime(this)
	}

	constructor(predicate: PatternPredicate) : this() {
		common.addAll(predicate.common)
		limited.addAll(predicate.limited)
		structurePatternPredicates.addAll(predicate.structurePatternPredicates)
		facts.addAll(predicate.facts)
		direction = predicate.direction
		fixedDirection = predicate.fixedDirection
		relativeDirection = predicate.relativeDirection
	}

	fun sort(): PatternPredicate {
		limited.sortBy { it.minCount }
		return this
	}

	fun setDirection(direction: Direction): PatternPredicate {
		fixedDirection = direction
		relativeDirection = null
		this.direction = Function { direction }
		return this
	}

	fun setRelativeDirection(direction: RelativeDirection): PatternPredicate {
		fixedDirection = null
		relativeDirection = direction
		return this
	}

	fun getDirection(state: MultiblockState, frontFacing: Direction, upwardsFacing: Direction, isFlipped: Boolean): Direction? {
		relativeDirection?.let { return it.getRelative(frontFacing, upwardsFacing, isFlipped) }
		fixedDirection?.let { return it }
		return direction.apply(state)
	}

	fun getPreviewDirection(): Direction? = relativeDirection?.global ?: fixedDirection

	/** Add tooltips for candidates. They are shown in JEI Pages. */
	fun addTooltips(vararg tips: Component): PatternPredicate {
		if (tips.isEmpty()) return this
		common.forEach { predicate ->
			if (predicate.candidates == null) return@forEach
			if (predicate.toolTips == null) predicate.toolTips = ObjectArrayList()
			predicate.toolTips!!.addAll(tips.asList())
		}
		limited.forEach { predicate ->
			if (predicate.candidates == null) return@forEach
			if (predicate.toolTips == null) predicate.toolTips = ObjectArrayList()
			predicate.toolTips!!.addAll(tips.asList())
		}
		return this
	}

	fun setMinGlobalLimited(min: Int): PatternPredicate {
		limited.addAll(common)
		common.clear()
		limited.forEach { it.minCount = min }
		return this
	}

	fun setMinGlobalLimited(min: Int, previewCount: Int): PatternPredicate = setMinGlobalLimited(min).setPreviewCount(previewCount)

	fun setMaxGlobalLimited(max: Int): PatternPredicate {
		limited.addAll(common)
		common.clear()
		limited.forEach { it.maxCount = max }
		return this
	}

	fun setMaxGlobalLimited(max: Int, previewCount: Int): PatternPredicate = setMaxGlobalLimited(max).setPreviewCount(previewCount)

	fun setMinLayerLimited(min: Int): PatternPredicate {
		limited.addAll(common)
		common.clear()
		limited.forEach { it.minLayerCount = min }
		return this
	}

	fun setMinLayerLimited(min: Int, previewCount: Int): PatternPredicate = setMinLayerLimited(min).setPreviewCount(previewCount)

	fun setMaxLayerLimited(max: Int): PatternPredicate {
		limited.addAll(common)
		common.clear()
		limited.forEach { it.maxLayerCount = max }
		return this
	}

	fun setMaxLayerLimited(max: Int, previewCount: Int): PatternPredicate = setMaxLayerLimited(max).setPreviewCount(previewCount)

	fun setExactLimit(limit: Int): PatternPredicate = setMinGlobalLimited(limit).setMaxGlobalLimited(limit)

	fun setPreviewCount(count: Int): PatternPredicate {
		common.forEach { it.previewCount = count }
		limited.forEach { it.previewCount = count }
		return this
	}

	fun disableRenderFormed(): PatternPredicate {
		common.forEach { it.disableRenderFormed = true }
		limited.forEach { it.disableRenderFormed = true }
		return this
	}

	fun setIO(io: IO): PatternPredicate {
		common.forEach { it.io = io }
		limited.forEach { it.io = io }
		return this
	}

	fun setSlotName(slotName: String): PatternPredicate {
		common.forEach { it.slotName = slotName }
		limited.forEach { it.slotName = slotName }
		return this
	}

	open fun test(blockWorldState: MultiblockState): Boolean {
		blockWorldState.io = IO.BOTH
		var matched = false
		limited.forEach { if (it.testLimited(blockWorldState)) matched = true }
		if (!matched) matched = common.any { it.test(blockWorldState) }
		if (matched) {
			blockWorldState.setError(null)
			facts.forEach(blockWorldState.facts::addFact)
		}
		return matched
	}

	fun withFacts(additionalFacts: List<String>): PatternPredicate {
		facts.addAll(additionalFacts)
		return this
	}

	fun or(other: PatternPredicate?): PatternPredicate {
		if (other == null) return this
		return PatternPredicate(this).also {
			it.common.addAll(other.common)
			it.limited.addAll(other.limited)
			it.structurePatternPredicates.addAll(other.structurePatternPredicates)
		}
	}

	open fun isAny(): Boolean = common.size == 1 && limited.isEmpty() &&
		common.first().source?.isAny() == true

	open fun addCache(): Boolean = common.any { it.addCache() } || limited.any { it.addCache() }

	open fun isAir(): Boolean = common.size == 1 && limited.isEmpty() &&
		common.first().source?.isAir() == true

	fun isSingle(): Boolean = !isAny() && !isAir() && common.size + limited.size == 1

	fun hasAir(): Boolean = common.any { it.source?.hasAir() == true }

	protected fun canAcceptObserved(state: MultiblockState): Boolean = if (limited.isNotEmpty()) {
		limited.any { it.canAcceptPosition(state, true) }
	} else {
		common.any { it.canAcceptPosition(state, false) }
	}

	/** A pure observation has already established the single observed rule; only its runtime metadata remains. */
	protected fun acceptObserved(state: MultiblockState): Boolean {
		var matched = false
		limited.forEach { if (it.acceptMatched(state, true, true)) matched = true }
		if (!matched) matched = common.any { it.acceptMatched(state, true, false) }
		if (matched) {
			state.setError(null)
			facts.forEach(state.facts::addFact)
		}
		return matched
	}

	companion object {
		private val DEFAULT_DIRECTION = Function<MultiblockState, Direction?> { null }

		@JvmStatic
		fun controller(definition: MachineDefinition): PatternPredicate = PredicateController(definition)

		@JvmStatic
		fun controller(predicate: PatternPredicate): PatternPredicate = PredicateController(predicate)

		@JvmStatic
		fun states(vararg allowedStates: BlockState): PatternPredicate {
			val candidates = ObjectArrayList<BlockState>()
			allowedStates.forEach { state ->
				candidates.add(state)
				if (state.hasProperty(ACTIVE)) {
					candidates.add(state.setValue(ACTIVE, !state.getValue(ACTIVE)))
				}
			}
			var result = PatternPredicate()
			for (candidate in candidates) {
				val properties = Object2ObjectLinkedOpenHashMap<String, String>()
				candidate.values.forEach { (property, value) -> properties[property.name] = value.toString() }
				result = result.or(PatternPredicate(BlockStatePredicate(candidate.block, properties)))
			}
			return result
		}

		@JvmStatic
		fun blocks(vararg blocks: Block): PatternPredicate = PatternPredicate(
			BlockPredicate {
				ObjectArrayList(
					blocks.map(
						BuiltInRegistries.BLOCK::getKey,
					),
				)
			},
		)

		@JvmStatic
		fun machines(vararg definitions: MachineDefinition): PatternPredicate {
			val machineBlocks = ObjectArrayList<MetaMachineBlock>(definitions.size)
			definitions.forEach { machineBlocks.add(it.get()) }
			return blocks(*machineBlocks.toTypedArray())
		}

		@JvmStatic
		fun blockTag(tag: TagKey<Block>): PatternPredicate = PatternPredicate(BlockTagPredicate(tag))

		@JvmStatic
		fun fluids(vararg fluids: Fluid): PatternPredicate = PatternPredicate(FluidPredicate(ObjectArrayList(fluids.asList())))

		@JvmStatic
		fun fluidTag(tag: TagKey<Fluid>): PatternPredicate = PatternPredicate(FluidTagPredicate(tag))

		@JvmStatic
		fun custom(predicate: Predicate<MultiblockState>, candidates: Supplier<Array<MultiblockBlockInfo>>): PatternPredicate = fromBlockInfos(predicate, candidates)

		@JvmStatic
		fun any(): PatternPredicate = PatternPredicate(AnyPredicate.INSTANCE)

		@JvmStatic
		fun air(): PatternPredicate = PatternPredicate(AirPredicate.INSTANCE)

		@JvmStatic
		fun lamps(vararg lampEntries: BlockEntry<LampBlock>): PatternPredicate = fromBlockInfos(
			{ state -> lampEntries.any { state.blockState.`is`(it.get()) } },
			{
				lampEntries.map { MultiblockBlockInfo(it.get().defaultBlockState(), null) }.toTypedArray()
			},
		)

		@JvmStatic
		fun anyLamp(): PatternPredicate {
			val all = ObjectArrayList<BlockEntry<LampBlock>>()
			all.addAll(LAMPS.values)
			all.addAll(BORDERLESS_LAMPS.values)
			return lamps(*all.toTypedArray())
		}

		private val lampsByColor = EnumMap<DyeColor, PatternPredicate>(DyeColor::class.java)

		@JvmStatic
		fun lampsByColor(color: DyeColor): PatternPredicate = lampsByColor.computeIfAbsent(color) { lamps(LAMPS[it]!!, BORDERLESS_LAMPS[it]!!) }

		@JvmStatic
		fun abilities(vararg abilities: PartAbility): PatternPredicate = PatternPredicate(AbilityPredicate(ObjectArrayList(abilities.asList())))

		@JvmStatic
		fun ability(ability: PartAbility, vararg tiers: Int): PatternPredicate = if (tiers.isEmpty()) {
			abilities(ability)
		} else {
			PatternPredicate(AbilityPredicate(ObjectList.of(ability), Optional.empty(), Optional.of(IntOpenHashSet(tiers)), Optional.empty(), Optional.empty()))
		}

		@JvmStatic
		fun autoAbilities(vararg recipeType: GTRecipeType): PatternPredicate = autoAbilities(recipeType, true, true, true, true, true, true)

		@JvmStatic
		fun autoAbilities(recipeType: Array<out GTRecipeType>, checkEnergyIn: Boolean, checkEnergyOut: Boolean, checkItemIn: Boolean, checkItemOut: Boolean, checkFluidIn: Boolean, checkFluidOut: Boolean): PatternPredicate {
			var predicate = PatternPredicate()
			if (checkEnergyIn && recipeType.any { it.getMaxInputs(EURecipeCapability.CAP) > 0 }) {
				predicate = predicate.or(
					abilities(PartAbility.INPUT_ENERGY).setMinGlobalLimited(1)
						.setMaxGlobalLimited(2).setPreviewCount(1),
				)
			}
			if (checkEnergyOut && recipeType.any { it.getMaxOutputs(EURecipeCapability.CAP) > 0 }) {
				predicate = predicate.or(
					abilities(PartAbility.OUTPUT_ENERGY).setMinGlobalLimited(1)
						.setMaxGlobalLimited(2).setPreviewCount(1),
				)
			}
			if (checkItemIn && recipeType.any { it.getMaxInputs(ItemRecipeCapability.CAP) > 0 }) {
				predicate = predicate.or(abilities(PartAbility.IMPORT_ITEMS).setPreviewCount(1))
			}
			if (checkItemOut && recipeType.any { it.getMaxOutputs(ItemRecipeCapability.CAP) > 0 }) {
				predicate = predicate.or(abilities(PartAbility.EXPORT_ITEMS).setPreviewCount(1))
			}
			if (checkFluidIn && recipeType.any { it.getMaxInputs(FluidRecipeCapability.CAP) > 0 }) {
				predicate = predicate.or(abilities(PartAbility.IMPORT_FLUIDS).setPreviewCount(1))
			}
			if (checkFluidOut && recipeType.any { it.getMaxOutputs(FluidRecipeCapability.CAP) > 0 }) {
				predicate = predicate.or(abilities(PartAbility.EXPORT_FLUIDS).setPreviewCount(1))
			}
			return predicate
		}

		@JvmStatic
		fun autoAbilities(checkMaintenance: Boolean, checkMuffler: Boolean, checkParallel: Boolean): PatternPredicate {
			var predicate = PatternPredicate()
			if (checkMaintenance) {
				predicate = predicate.or(
					abilities(PartAbility.MAINTENANCE)
						.setMinGlobalLimited(if (ConfigHolder.INSTANCE.machines.enableMaintenance) 1 else 0)
						.setMaxGlobalLimited(1),
				)
			}
			if (checkMuffler) {
				predicate = predicate.or(abilities(PartAbility.MUFFLER).setMinGlobalLimited(1).setMaxGlobalLimited(1))
			}
			if (checkParallel) {
				predicate = predicate.or(abilities(PartAbility.PARALLEL_HATCH).setMaxGlobalLimited(1).setPreviewCount(1))
			}
			return predicate
		}

		@JvmStatic
		fun heatingCoils(): PatternPredicate = PatternPredicate(HeatingCoilPredicate.INSTANCE).addTooltips(Component.translatable("gtpm.multiblock.pattern.error.coils"))

		@JvmStatic
		fun cleanroomFilters(): PatternPredicate = PatternPredicate(CleanroomFilterPredicate.INSTANCE).addTooltips(Component.translatable("gtpm.multiblock.pattern.error.filters"))

		@JvmStatic
		fun powerSubstationBatteries(): PatternPredicate = PatternPredicate(PowerSubstationBatteryPredicate.INSTANCE)
			.addTooltips(Component.translatable("gtpm.multiblock.pattern.error.batteries"))

		@JvmStatic
		fun dataHatchPredicate(definition: PatternPredicate): PatternPredicate = if (ConfigHolder.INSTANCE.machines.enableResearch) {
			abilities(PartAbility.DATA_ACCESS, PartAbility.OPTICAL_DATA_RECEPTION).setExactLimit(1).or(definition)
		} else {
			definition
		}

		@JvmStatic
		fun frames(vararg frameMaterials: Material): PatternPredicate = PatternPredicate(
			FramePredicate(ObjectArrayList(frameMaterials.map { it.resourceLocation })),
		)

		private fun fromBlockInfos(predicate: Predicate<MultiblockState>, candidates: Supplier<Array<MultiblockBlockInfo>>): PatternPredicate = PatternPredicate(
			predicate,
			{
				val infos = candidates.get()
				if (infos.isEmpty()) MultiblockBlockInfo.EMPTY else infos[0]
			},
			{ candidates.get().map { it.blockState.block }.toTypedArray() },
		)
	}
}
