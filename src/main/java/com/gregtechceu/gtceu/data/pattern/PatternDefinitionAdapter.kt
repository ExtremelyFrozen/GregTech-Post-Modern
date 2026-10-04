package com.gregtechceu.gtceu.data.pattern

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.gregtechceu.gtceu.api.machine.MultiblockMachineDefinition
import com.gregtechceu.gtceu.api.multiblock.CenterOffset
import com.gregtechceu.gtceu.api.multiblock.pattern.compile.PatternCompiler
import com.gregtechceu.gtceu.api.multiblock.pattern.dsl.PatternBuilder
import com.gregtechceu.gtceu.api.multiblock.pattern.match.MultiBlockPattern
import com.gregtechceu.gtceu.api.multiblock.pattern.model.PatternDefinition
import com.gregtechceu.gtceu.api.multiblock.pattern.model.PatternDirection
import com.gregtechceu.gtceu.api.multiblock.pattern.model.PatternFragment
import com.gregtechceu.gtceu.api.multiblock.pattern.model.PatternNode
import com.gregtechceu.gtceu.api.multiblock.pattern.model.PatternPredicateDefinition
import com.gregtechceu.gtceu.api.multiblock.pattern.predicate.PatternPredicate
import com.gregtechceu.gtceu.api.multiblock.pattern.predicate.PatternPredicates
import com.gregtechceu.gtceu.api.multiblock.pattern.predicate.PredicateController
import com.gregtechceu.gtceu.api.multiblock.structurepredicate.StructurePredicate
import com.mojang.serialization.JsonOps
import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap
import it.unimi.dsi.fastutil.objects.ObjectArrayList
import org.jspecify.annotations.NullMarked

@NullMarked
object PatternDefinitionAdapter {
    private val gson = Gson()

    @JvmStatic
    fun appendToBuilder(builder: PatternBuilder, definition: PatternDefinition) {
        flatten(definition.body, definition).forEach { unit ->
            if (unit.minimum == 1 && unit.maximum == 1) {
                builder.aisle(*unit.slices.first())
            } else {
                builder.beginRepeatable()
                unit.slices.forEach { builder.aisle(*it) }
                builder.endRepeatable(unit.minimum, unit.maximum)
            }
        }
        definition.predicates.forEach { (symbol, predicate) -> builder.where(symbol, compilePredicate(predicate)) }
    }

    @JvmStatic
    fun withBody(source: PatternDefinition, nodes: List<PatternNode>): PatternDefinition {
        val body = if (nodes.size == 1) nodes.first() else PatternNode.Sequence(PatternDirection.FRONT, nodes)
        return PatternDefinition(source.machine, source.structure, source.axes, source.orientation, source.origin,
            source.parameters, source.fragments, source.predicates, body, source.constraints)
    }

    @JvmStatic
    fun compile(owner: MultiblockMachineDefinition, key: StructurePatternKey, baseline: MultiBlockPattern,
                definition: PatternDefinition): MultiBlockPattern {
        PatternCompiler().compile(definition)
        val units = flatten(definition.body, definition)
        require(units.isNotEmpty()) { "Pattern body is empty for $key" }
        val height = units.first().slices.first().size
        val width = units.first().slices.first().first().length
        val size = units.sumOf { it.slices.size }
        val matches = Array(size) { Array(height) { arrayOfNulls<PatternPredicate>(width) } }
        val slices = arrayOfNulls<Array<String>>(size)
        val repetitions = Array(units.size) { IntArray(2) }
        val starts = IntArray(units.size)
        val depths = IntArray(units.size)
        val predicates = collectPredicates(owner, definition)
        var center: CenterOffset? = null
        var sliceIndex = 0
        var minZ = 0
        var maxZ = 0
        units.forEachIndexed { unitIndex, unit ->
            starts[unitIndex] = sliceIndex
            depths[unitIndex] = unit.slices.size
            repetitions[unitIndex] = intArrayOf(unit.minimum, unit.maximum)
            unit.slices.forEachIndexed { inner, slice ->
                slices[sliceIndex] = slice.copyOf()
                slice.forEachIndexed { row, text ->
                    text.forEachIndexed { column, symbol ->
                        val predicate = predicates[symbol] ?: error("Unknown pattern symbol at $key")
                        matches[sliceIndex][row][column] = predicate
                        if (predicate is PredicateController) center = CenterOffset(column, row, sliceIndex, minZ + inner, maxZ + inner)
                    }
                }
                sliceIndex++
            }
            minZ += unit.slices.size * unit.minimum
            maxZ += unit.slices.size * unit.maximum
        }
        val resolvedCenter = center ?: error("Pattern definition has no controller token: $key")
        @Suppress("UNCHECKED_CAST")
        val pattern = MultiBlockPattern(matches as Array<Array<Array<PatternPredicate>>>, baseline.structureDir,
            repetitions, starts, depths, slices.map { it ?: error("Missing pattern slice") }.toTypedArray(),
            resolvedCenter, size, height, width)
        pattern.condition = baseline.condition
        pattern.predicates = predicates.values.toList()
        pattern.attachDefinition(definition)
        return pattern
    }

    private fun collectPredicates(owner: MultiblockMachineDefinition, definition: PatternDefinition): Map<Char, PatternPredicate> {
        val result = Object2ObjectLinkedOpenHashMap<Char, PatternPredicate>()
        result[' '] = PatternPredicates.any()
        definition.predicates.forEach { (symbol, predicate) -> result[symbol] = compilePredicate(predicate) }
        result['~'] = PatternPredicates.controller(PatternPredicates.blocks(owner.block))
        return result
    }

    private fun compilePredicate(definition: PatternPredicateDefinition): PatternPredicate {
        if (definition.type == "gtpm:any" || definition.type == "any") return PatternPredicates.any()
        if (definition.type == "gtpm:air" || definition.type == "air") return PatternPredicates.air()
        val json = JsonObject().apply {
            addProperty("type", definition.type)
            definition.properties.forEach { (key, value) -> add(key, gson.toJsonTree(value)) }
        }
        return PatternPredicate(StructurePredicate.CODEC.parse(JsonOps.INSTANCE, json)
            .getOrThrow { error -> IllegalArgumentException("Failed to compile predicate ${definition.type}: $error") })
            .withFacts(definition.facts)
    }

    private fun flatten(node: PatternNode, definition: PatternDefinition): ObjectArrayList<Unit> = ObjectArrayList<Unit>().also {
        flattenInto(node, definition, it)
    }

    private fun flattenInto(node: PatternNode, definition: PatternDefinition, result: ObjectArrayList<Unit>) {
        when (node) {
            is PatternNode.Fixed -> result.add(Unit(node.layers.map { it.toTypedArray() }, 1, 1))
            is PatternNode.Sequence -> node.children.forEach { flattenInto(it, definition, result) }
            is PatternNode.Repeat -> result.add(Unit(flatten(node.body, definition).flatMapTo(ObjectArrayList()) { it.slices }, node.minimum, node.maximum))
            is PatternNode.Fragment -> {
                val target = definition.fragments[node.id] ?: error("Unknown fragment ${node.id}")
                flattenInto(target.body, definition, result)
            }
            is PatternNode.Choice -> error("Choice nodes require a selected runtime branch")
        }
    }

    private data class Unit(val slices: List<Array<String>>, val minimum: Int, val maximum: Int)
}
