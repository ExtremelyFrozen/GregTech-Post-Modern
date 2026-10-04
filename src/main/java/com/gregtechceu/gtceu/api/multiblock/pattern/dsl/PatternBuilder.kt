package com.gregtechceu.gtceu.api.multiblock.pattern.dsl

import com.google.common.base.Joiner
import com.gregtechceu.gtceu.api.machine.MultiblockMachineDefinition
import com.gregtechceu.gtceu.api.multiblock.CenterOffset
import com.gregtechceu.gtceu.api.multiblock.MultiblockState
import com.gregtechceu.gtceu.api.multiblock.PatternCondition
import com.gregtechceu.gtceu.api.multiblock.StructureDir
import com.gregtechceu.gtceu.api.multiblock.pattern.match.MultiBlockPattern
import com.gregtechceu.gtceu.api.multiblock.pattern.model.PatternAxes
import com.gregtechceu.gtceu.api.multiblock.pattern.model.PatternBinding
import com.gregtechceu.gtceu.api.multiblock.pattern.model.PatternConstraint
import com.gregtechceu.gtceu.api.multiblock.pattern.model.PatternDefinition
import com.gregtechceu.gtceu.api.multiblock.pattern.model.PatternDirection
import com.gregtechceu.gtceu.api.multiblock.pattern.model.PatternNode
import com.gregtechceu.gtceu.api.multiblock.pattern.model.PatternOrientation
import com.gregtechceu.gtceu.api.multiblock.pattern.model.PatternOrigin
import com.gregtechceu.gtceu.api.multiblock.pattern.model.PatternPredicateDefinition
import com.gregtechceu.gtceu.api.multiblock.pattern.model.PatternRepeatDirection
import com.gregtechceu.gtceu.api.multiblock.pattern.predicate.PatternPredicate
import com.gregtechceu.gtceu.api.multiblock.pattern.predicate.PatternPredicates
import com.gregtechceu.gtceu.api.multiblock.pattern.predicate.PredicateController
import com.gregtechceu.gtceu.api.multiblock.util.RelativeDirection
import com.gregtechceu.gtceu.data.pattern.StructurePatternKey
import com.gregtechceu.gtceu.data.pattern.StructurePatternResolver
import it.unimi.dsi.fastutil.chars.Char2ObjectArrayMap
import it.unimi.dsi.fastutil.chars.Char2ObjectMap
import it.unimi.dsi.fastutil.chars.CharArrayList
import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap
import it.unimi.dsi.fastutil.objects.ObjectArrayList
import java.util.function.Predicate

/** Java DSL entry point for defining a multiblock structure. */
class PatternBuilder private constructor(
    private val charDir: RelativeDirection,
    private val stringDir: RelativeDirection,
    private val aisleDir: RelativeDirection,
    private val definition: MultiblockMachineDefinition?,
    private val structureName: String,
) {
    private val definitionKey = definition?.let { StructurePatternKey(it.id, structureName) }
    private val units = ObjectArrayList<AisleUnit>()
    private val symbolMap: Char2ObjectMap<PatternPredicate?> = Char2ObjectArrayMap()
    private val structureDir = StructureDir(charDir, stringDir, aisleDir)
    private var repeatableGroup: ObjectArrayList<Array<String>>? = null
    private var condition: PatternCondition? = null
    private var aisleHeight = 0
    private var rowWidth = 0
    private val extraNodes = ObjectArrayList<PatternNode>()
    private val constraints = ObjectArrayList<PatternConstraint>()

    private data class AisleUnit(
        val slices: List<Array<String>>,
        val minRepeat: Int,
        val maxRepeat: Int,
    )

    init {
        structureDir.check()
        symbolMap[' '] = PatternPredicates.any()
        definition?.let { symbolMap['~'] = PatternPredicates.controller(PatternPredicates.blocks(it.block)) }
    }

    /** Adds a single aisle to this pattern. Multiple calls increase the aisleDir by 1. */
    fun aisle(vararg aisle: String): PatternBuilder {
        validateAisle(aisle)
        val group = repeatableGroup
        if (group == null) {
            units.add(AisleUnit(listOf(copyAisle(aisle)), 1, 1))
        } else {
            group.add(copyAisle(aisle))
        }
        return this
    }

    fun slice(vararg rows: String): PatternBuilder = aisle(*rows)

    fun sliceRepeatable(min: Int, max: Int, vararg rows: String): PatternBuilder =
        beginRepeatable().aisle(*rows).endRepeatable(min, max)

    fun use(fragmentId: String, bindings: Map<String, PatternBinding>): PatternBuilder {
        extraNodes.add(PatternNode.Fragment(fragmentId, bindings))
        return this
    }

    @Suppress("UNUSED_PARAMETER")
    fun place(fragmentId: String, anchor: PatternOrigin.Offset): PatternBuilder {
        extraNodes.add(PatternNode.Fragment(fragmentId, emptyMap()))
        return this
    }

    fun constraint(constraint: PatternConstraint): PatternBuilder {
        constraints.add(constraint)
        return this
    }

    fun appendDefinition(): PatternBuilder {
        val key = definitionKey ?: error("No multiblock definition was bound to this pattern builder")
        return appendDefinition(key)
    }

    fun appendDefinition(key: StructurePatternKey): PatternBuilder =
        StructurePatternResolver.appendDefinition(this, key)

    fun beginRepeatable(): PatternBuilder {
        check(repeatableGroup == null) { "Cannot begin a nested repeatable aisle group" }
        repeatableGroup = ObjectArrayList()
        return this
    }

    fun endRepeatable(minRepeat: Int, maxRepeat: Int): PatternBuilder {
        val group = repeatableGroup ?: error("No repeatable aisle group has been started")
        check(group.isNotEmpty()) { "Repeatable aisle group must contain at least one aisle" }
        require(minRepeat <= maxRepeat) {
            "Lower bound of repeat counting must smaller than upper bound!"
        }
        units.add(AisleUnit(group.toList(), minRepeat, maxRepeat))
        repeatableGroup = null
        return this
    }

    fun endRepeatable(repeatCount: Int): PatternBuilder = endRepeatable(repeatCount, repeatCount)

    private fun validateAisle(aisle: Array<out String>) {
        require(aisle.isNotEmpty() && aisle[0].isNotEmpty()) { "Empty pattern for aisle" }
        if (units.isEmpty() && (repeatableGroup == null || repeatableGroup!!.isEmpty())) {
            aisleHeight = aisle.size
            rowWidth = aisle[0].length
        }
        require(aisle.size == aisleHeight) {
            "Expected aisle with height of $aisleHeight, but was given one with a height of ${aisle.size})"
        }
        aisle.forEach { row ->
            require(row.length == rowWidth) {
                "Not all rows in the given aisle are the correct width (expected $rowWidth, found one with ${row.length})"
            }
            row.forEach { symbol ->
                if (!symbolMap.containsKey(symbol)) symbolMap[symbol] = null
            }
        }
    }

    private fun copyAisle(aisle: Array<out String>): Array<String> = Array(aisle.size) { index -> aisle[index] }

    fun where(symbol: String, blockMatcher: PatternPredicate): PatternBuilder =
        where(symbol[0], blockMatcher)

    fun where(symbol: Char, blockMatcher: PatternPredicate): PatternBuilder {
        symbolMap[symbol] = when {
            blockMatcher.isAny() || blockMatcher.isAir() -> blockMatcher
            blockMatcher is PredicateController -> blockMatcher.sort()
            else -> PatternPredicate(blockMatcher).sort()
        }
        return this
    }

    fun condition(condition: Predicate<MultiblockState>): PatternBuilder =
        condition(condition, "gtceu.recipe_logic.condition_fails")

    fun condition(condition: Predicate<MultiblockState>, translateKey: String): PatternBuilder {
        this.condition = PatternCondition(condition, translateKey)
        return this
    }

    private fun checkMissingPatternPredicates() {
        val missing = CharArrayList()
        symbolMap.char2ObjectEntrySet().forEach { entry ->
            if (entry.value == null) missing.add(entry.charKey)
        }
        check(missing.isEmpty()) {
            "PatternPredicates for character(s) ${Joiner.on(',').join(missing)} are missing"
        }
    }

    fun build(): MultiBlockPattern {
        check(repeatableGroup == null) {
            "Repeatable aisle group must be closed before building the pattern"
        }
        checkMissingPatternPredicates()
        val unitCount = units.size
        val size = units.sumOf { it.slices.size }
        var centerOffset: CenterOffset? = null
        val aisleRepetitions = Array(unitCount) { IntArray(2) }
        val unitStarts = IntArray(unitCount)
        val unitDepths = IntArray(unitCount)
        val structureSlices = arrayOfNulls<Array<String>>(size)
        val predicate = arrayOfNulls<Array<Array<PatternPredicate?>?>>(size)

        var sliceIndex = 0
        var minZ = 0
        var maxZ = 0
        units.forEachIndexed { unitIndex, unit ->
            val unitDepth = unit.slices.size
            unitStarts[unitIndex] = sliceIndex
            unitDepths[unitIndex] = unitDepth
            aisleRepetitions[unitIndex][0] = unit.minRepeat
            aisleRepetitions[unitIndex][1] = unit.maxRepeat
            unit.slices.forEachIndexed { inner, aisle ->
                structureSlices[sliceIndex] = aisle.copyOf()
                val layerPredicates = arrayOfNulls<Array<PatternPredicate?>>(aisleHeight)
                aisle.forEachIndexed { rowIndex, row ->
                    row.forEachIndexed { columnIndex, symbol ->
                        val matched = symbolMap[symbol]
                        if (matched != null) {
                            val rowPredicates: Array<PatternPredicate?> = layerPredicates[rowIndex]
                                ?: arrayOfNulls<PatternPredicate?>(rowWidth).also { layerPredicates[rowIndex] = it }
                            rowPredicates[columnIndex] = matched
                            if (matched is PredicateController) {
                                centerOffset = CenterOffset(columnIndex, rowIndex, sliceIndex, minZ + inner, maxZ + inner)
                            }
                        }
                    }
                }
                predicate[sliceIndex] = layerPredicates
                sliceIndex++
            }
            minZ += unitDepth * unit.minRepeat
            maxZ += unitDepth * unit.maxRepeat
        }

        @Suppress("UNCHECKED_CAST")
        val blockMatches = predicate as Array<Array<Array<PatternPredicate>>>
        @Suppress("UNCHECKED_CAST")
        val slices = structureSlices as Array<Array<String>>
        val pattern = MultiBlockPattern(
            blockMatches,
            structureDir,
            aisleRepetitions,
            unitStarts,
            unitDepths,
            slices,
            centerOffset ?: error("Pattern controller predicate is missing"),
            size,
            aisleHeight,
            rowWidth,
        )
        condition?.let { pattern.condition = it }
        definition?.let {
            val predicates = ObjectArrayList<PatternPredicate>(symbolMap.size)
            symbolMap.values.forEach { value -> if (value != null) predicates.add(value) }
            pattern.predicates = predicates
            pattern.attachDefinition(buildDefinition())
        }
        return pattern
    }

    /** Builds the canonical AST consumed by JSON, binary and compiler paths. */
    fun buildDefinition(): PatternDefinition {
        val machine = definition ?: error("A Java pattern definition must be bound to a machine")
        checkMissingPatternPredicates()
        val nodes = ObjectArrayList<PatternNode>(units.size + extraNodes.size)
        units.forEachIndexed { index, unit ->
            val layers = ObjectArrayList<List<String>>(unit.slices.size)
            unit.slices.forEach { slice -> layers.add(slice.toList()) }
            val fixed = PatternNode.Fixed(layers)
            nodes.add(
                if (unit.minRepeat == 1 && unit.maxRepeat == 1) fixed else PatternNode.Repeat(
                    "unit-$index",
                    PatternDirection.FRONT,
                    PatternRepeatDirection.POSITIVE,
                    unit.minRepeat,
                    unit.maxRepeat,
                    fixed,
                ),
            )
        }
        nodes.addAll(extraNodes)
        val body = if (nodes.size == 1) nodes.first() else PatternNode.Sequence(PatternDirection.FRONT, nodes)
        val predicates = Object2ObjectLinkedOpenHashMap<Char, PatternPredicateDefinition>()
        symbolMap.char2ObjectEntrySet().forEach { entry ->
            predicates[entry.charKey] = PatternPredicateDefinition("java", "", emptyMap(), emptyList())
        }
        return PatternDefinition(
            machine.id,
            definitionKey!!.resourceId(),
            PatternAxes(
                toPatternDirection(structureDir.aisleDir()),
                toPatternDirection(structureDir.stringDir()),
                toPatternDirection(structureDir.charDir()),
            ),
            PatternOrientation.defaults(),
            PatternOrigin.controller(),
            emptyMap(),
            emptyMap(),
            predicates,
            body,
            constraints,
        )
    }

    companion object {
        private val DEFAULT_STRUCTURE = StructurePatternKey.DEFAULT_STRUCTURE_NAME

        @JvmStatic
        fun start(): PatternBuilder = PatternBuilder(
            RelativeDirection.LEFT,
            RelativeDirection.UP,
            RelativeDirection.FRONT,
            null,
            DEFAULT_STRUCTURE,
        )

        @JvmStatic
        fun start(definition: MultiblockMachineDefinition): PatternBuilder = PatternBuilder(
            RelativeDirection.LEFT,
            RelativeDirection.UP,
            RelativeDirection.FRONT,
            definition,
            DEFAULT_STRUCTURE,
        )

        @JvmStatic
        fun start(definition: MultiblockMachineDefinition, structureName: String): PatternBuilder = PatternBuilder(
            RelativeDirection.LEFT,
            RelativeDirection.UP,
            RelativeDirection.FRONT,
            definition,
            structureName,
        )

        @JvmStatic
        fun start(
            charDir: RelativeDirection,
            stringDir: RelativeDirection,
            aisleDir: RelativeDirection,
        ): PatternBuilder = PatternBuilder(charDir, stringDir, aisleDir, null, DEFAULT_STRUCTURE)

        @JvmStatic
        fun start(
            definition: MultiblockMachineDefinition,
            charDir: RelativeDirection,
            stringDir: RelativeDirection,
            aisleDir: RelativeDirection,
        ): PatternBuilder = PatternBuilder(charDir, stringDir, aisleDir, definition, DEFAULT_STRUCTURE)

        @JvmStatic
        fun start(
            definition: MultiblockMachineDefinition,
            structureName: String,
            charDir: RelativeDirection,
            stringDir: RelativeDirection,
            aisleDir: RelativeDirection,
        ): PatternBuilder = PatternBuilder(charDir, stringDir, aisleDir, definition, structureName)

        private fun toPatternDirection(direction: RelativeDirection): PatternDirection =
            PatternDirection.valueOf(direction.name)
    }
}
