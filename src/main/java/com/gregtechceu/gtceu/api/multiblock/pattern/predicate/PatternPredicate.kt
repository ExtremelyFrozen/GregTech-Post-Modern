package com.gregtechceu.gtceu.api.multiblock.pattern.predicate

import com.gregtechceu.gtceu.api.capability.recipe.IO
import com.gregtechceu.gtceu.api.multiblock.MultiblockBlockInfo
import com.gregtechceu.gtceu.api.multiblock.MultiblockState
import com.gregtechceu.gtceu.api.multiblock.structurepredicate.StructurePredicate
import com.gregtechceu.gtceu.api.multiblock.util.RelativeDirection
import it.unimi.dsi.fastutil.objects.ObjectArrayList
import net.minecraft.core.Direction
import net.minecraft.network.chat.Component
import net.minecraft.world.level.block.Block
import java.util.function.Function
import java.util.function.Predicate
import java.util.function.Supplier

/** Composite runtime predicate used by the matcher and structure planner. */
open class PatternPredicate {
    @JvmField
    var common: MutableList<PredicateRule> = ObjectArrayList()

    @JvmField
    var limited: MutableList<PredicateRule> = ObjectArrayList()

    @JvmField
    var structurePatternPredicates: MutableList<StructurePredicate> = ObjectArrayList()

    @JvmField
    var facts: MutableList<String> = ObjectArrayList()

    @JvmField
    var direction: Function<MultiblockState, Direction?> = Function { null }

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

    constructor(structurePredicate: StructurePredicate) : this() {
        structurePatternPredicates.add(structurePredicate)
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

    fun getDirection(
        state: MultiblockState,
        frontFacing: Direction,
        upwardsFacing: Direction,
        isFlipped: Boolean,
    ): Direction? {
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

    fun setMinGlobalLimited(min: Int, previewCount: Int): PatternPredicate =
        setMinGlobalLimited(min).setPreviewCount(previewCount)

    fun setMaxGlobalLimited(max: Int): PatternPredicate {
        limited.addAll(common)
        common.clear()
        limited.forEach { it.maxCount = max }
        return this
    }

    fun setMaxGlobalLimited(max: Int, previewCount: Int): PatternPredicate =
        setMaxGlobalLimited(max).setPreviewCount(previewCount)

    fun setMinLayerLimited(min: Int): PatternPredicate {
        limited.addAll(common)
        common.clear()
        limited.forEach { it.minLayerCount = min }
        return this
    }

    fun setMinLayerLimited(min: Int, previewCount: Int): PatternPredicate =
        setMinLayerLimited(min).setPreviewCount(previewCount)

    fun setMaxLayerLimited(max: Int): PatternPredicate {
        limited.addAll(common)
        common.clear()
        limited.forEach { it.maxLayerCount = max }
        return this
    }

    fun setMaxLayerLimited(max: Int, previewCount: Int): PatternPredicate =
        setMaxLayerLimited(max).setPreviewCount(previewCount)

    fun setExactLimit(limit: Int): PatternPredicate =
        setMinGlobalLimited(limit).setMaxGlobalLimited(limit)

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

    fun test(blockWorldState: MultiblockState): Boolean {
        blockWorldState.io = IO.BOTH
        var matched = false
        limited.forEach { if (it.testLimited(blockWorldState)) matched = true }
        if (!matched) matched = common.any { it.test(blockWorldState) }
        if (!matched) matched = structurePatternPredicates.any { it.test(blockWorldState, true) }
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

    fun isAny(): Boolean =
        (common.size == 1 && limited.isEmpty() && structurePatternPredicates.isEmpty() &&
            common.first() === PredicateRule.ANY) ||
            (common.isEmpty() && limited.isEmpty() && structurePatternPredicates.size == 1 &&
                structurePatternPredicates.first().isAny())

    fun addCache(): Boolean =
        common.any { it.addCache() } || limited.any { it.addCache() } ||
            structurePatternPredicates.any { it.addCache() }

    fun isAir(): Boolean =
        (common.size == 1 && limited.isEmpty() && structurePatternPredicates.isEmpty() &&
            common.first() === PredicateRule.AIR) ||
            (common.isEmpty() && limited.isEmpty() && structurePatternPredicates.size == 1 &&
                structurePatternPredicates.first().isAir())

    fun isSingle(): Boolean =
        !isAny() && !isAir() && common.size + limited.size + structurePatternPredicates.size == 1

    fun hasAir(): Boolean =
        PredicateRule.AIR in common || structurePatternPredicates.any { it.hasAir() }
}
