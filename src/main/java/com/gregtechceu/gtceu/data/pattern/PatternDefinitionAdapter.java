package com.gregtechceu.gtceu.data.pattern;

import com.gregtechceu.gtceu.api.machine.MultiblockMachineDefinition;
import com.gregtechceu.gtceu.api.multiblock.CenterOffset;
import com.gregtechceu.gtceu.api.multiblock.pattern.dsl.PatternBuilder;
import com.gregtechceu.gtceu.api.multiblock.pattern.compile.PatternCompiler;
import com.gregtechceu.gtceu.api.multiblock.pattern.match.MultiBlockPattern;
import com.gregtechceu.gtceu.api.multiblock.pattern.model.PatternDefinition;
import com.gregtechceu.gtceu.api.multiblock.pattern.model.PatternDirection;
import com.gregtechceu.gtceu.api.multiblock.pattern.model.PatternFragment;
import com.gregtechceu.gtceu.api.multiblock.pattern.model.PatternNode;
import com.gregtechceu.gtceu.api.multiblock.pattern.model.PatternPredicateDefinition;
import com.gregtechceu.gtceu.api.multiblock.pattern.predicate.PatternPredicate;
import com.gregtechceu.gtceu.api.multiblock.pattern.predicate.PatternPredicates;
import com.gregtechceu.gtceu.api.multiblock.pattern.predicate.PredicateController;
import com.gregtechceu.gtceu.api.multiblock.structurepredicate.StructurePredicate;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.jspecify.annotations.NullMarked;

import java.util.List;
import java.util.Map;

/** Converts the canonical AST to the existing world matcher at the single data boundary. */
@NullMarked
final class PatternDefinitionAdapter {

    private static final Gson GSON = new Gson();

    private PatternDefinitionAdapter() {}

    static void appendToBuilder(PatternBuilder builder, PatternDefinition definition) {
        ObjectArrayList<Unit> units = flatten(definition.body(), definition);
        for (Unit unit : units) {
            if (unit.minimum == 1 && unit.maximum == 1) {
                builder.aisle(unit.slices.getFirst());
            } else {
                builder.beginRepeatable();
                for (String[] slice : unit.slices) builder.aisle(slice);
                builder.endRepeatable(unit.minimum, unit.maximum);
            }
        }
        definition.predicates().forEach((symbol, predicate) -> builder.where(symbol, compilePredicate(predicate)));
    }

    static PatternDefinition withBody(PatternDefinition source, List<PatternNode> nodes) {
        PatternNode body = nodes.size() == 1 ? nodes.getFirst() : new PatternNode.Sequence(
                PatternDirection.FRONT, nodes);
        return new PatternDefinition(source.machine(), source.structure(), source.axes(), source.orientation(),
                source.origin(),
                source.parameters(), source.fragments(), source.predicates(), body, source.constraints());
    }

    static MultiBlockPattern compile(MultiblockMachineDefinition owner, StructurePatternKey key,
                                     MultiBlockPattern baseline, PatternDefinition definition) {
        new PatternCompiler().compile(definition);
        ObjectArrayList<Unit> units = flatten(definition.body(), definition);
        if (units.isEmpty()) throw new IllegalArgumentException("Pattern body is empty for " + key);
        int height = units.getFirst().slices.getFirst().length;
        int width = units.getFirst().slices.getFirst()[0].length();
        int unitCount = units.size();
        int size = units.stream().mapToInt(unit -> unit.slices.size()).sum();
        PatternPredicate[][][] matches = new PatternPredicate[size][height][width];
        String[][] slices = new String[size][];
        int[][] repetitions = new int[unitCount][];
        int[] starts = new int[unitCount];
        int[] depths = new int[unitCount];
        CenterOffset center = null;
        Map<Character, PatternPredicate> predicates = collectPredicates(owner, definition);
        for (int unitIndex = 0, sliceIndex = 0, minZ = 0, maxZ = 0; unitIndex < unitCount; unitIndex++) {
            Unit unit = units.get(unitIndex);
            starts[unitIndex] = sliceIndex;
            depths[unitIndex] = unit.slices.size();
            repetitions[unitIndex] = new int[] { unit.minimum, unit.maximum };
            for (int inner = 0; inner < unit.slices.size(); inner++, sliceIndex++) {
                String[] slice = unit.slices.get(inner);
                slices[sliceIndex] = slice.clone();
                for (int row = 0; row < height; row++) {
                    for (int column = 0; column < width; column++) {
                        PatternPredicate predicate = predicates.get(slice[row].charAt(column));
                        if (predicate == null) throw new IllegalArgumentException("Unknown pattern symbol at " + key);
                        matches[sliceIndex][row][column] = predicate;
                        if (predicate instanceof PredicateController) {
                            center = new CenterOffset(column, row, sliceIndex, minZ + inner, maxZ + inner);
                        }
                    }
                }
            }
            minZ += unit.slices.size() * unit.minimum;
            maxZ += unit.slices.size() * unit.maximum;
        }
        if (center == null) throw new IllegalArgumentException("Pattern definition has no controller token: " + key);
        MultiBlockPattern pattern = new MultiBlockPattern(matches, baseline.structureDir, repetitions, starts, depths,
                slices, center, size, height, width);
        pattern.condition = baseline.condition;
        pattern.predicates = List.copyOf(predicates.values());
        pattern.attachDefinition(definition);
        return pattern;
    }

    private static Map<Character, PatternPredicate> collectPredicates(MultiblockMachineDefinition owner,
                                                                      PatternDefinition definition) {
        var result = new Object2ObjectLinkedOpenHashMap<Character, PatternPredicate>();
        result.put(' ', PatternPredicates.any());
        definition.predicates().forEach((symbol, predicate) -> result.put(symbol, compilePredicate(predicate)));
        result.put('~', PatternPredicates.controller(PatternPredicates.blocks(owner.getBlock())));
        return result;
    }

    private static PatternPredicate compilePredicate(PatternPredicateDefinition definition) {
        if (definition.type().equals("gtpm:any") || definition.type().equals("any")) return PatternPredicates.any();
        if (definition.type().equals("gtpm:air") || definition.type().equals("air")) return PatternPredicates.air();
        JsonObject json = new JsonObject();
        json.addProperty("type", definition.type());
        definition.properties().forEach((key, value) -> json.add(key, GSON.toJsonTree(value)));
        return new PatternPredicate(StructurePredicate.CODEC.parse(JsonOps.INSTANCE, json)
                .getOrThrow(error -> new IllegalArgumentException(
                        "Failed to compile predicate " + definition.type() + ": " + error)));
    }

    private static ObjectArrayList<Unit> flatten(PatternNode node, PatternDefinition definition) {
        var result = new ObjectArrayList<Unit>();
        flattenInto(node, definition, result);
        return result;
    }

    private static void flattenInto(PatternNode node, PatternDefinition definition, ObjectArrayList<Unit> result) {
        if (node instanceof PatternNode.Fixed fixed) {
            var slices = new ObjectArrayList<String[]>(fixed.depth());
            fixed.layers().forEach(layer -> slices.add(layer.toArray(String[]::new)));
            result.add(new Unit(slices, 1, 1));
        } else if (node instanceof PatternNode.Sequence sequence) {
            sequence.children().forEach(child -> flattenInto(child, definition, result));
        } else if (node instanceof PatternNode.Repeat repeat) {
            var body = flatten(repeat.body(), definition);
            var slices = new ObjectArrayList<String[]>();
            body.forEach(unit -> slices.addAll(unit.slices));
            result.add(new Unit(slices, repeat.minimum(), repeat.maximum()));
        } else if (node instanceof PatternNode.Fragment fragment) {
            PatternFragment target = definition.fragments().get(fragment.id());
            if (target == null) throw new IllegalArgumentException("Unknown fragment " + fragment.id());
            flattenInto(target.body(), definition, result);
        } else {
            throw new IllegalArgumentException("Choice nodes require a selected runtime branch");
        }
    }

    private record Unit(List<String[]> slices, int minimum, int maximum) {}
}
