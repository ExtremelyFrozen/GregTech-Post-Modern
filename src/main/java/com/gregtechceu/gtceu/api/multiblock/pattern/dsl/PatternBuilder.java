package com.gregtechceu.gtceu.api.multiblock.pattern.dsl;

import com.gregtechceu.gtceu.api.machine.MultiblockMachineDefinition;
import com.gregtechceu.gtceu.api.multiblock.CenterOffset;
import com.gregtechceu.gtceu.api.multiblock.MultiblockState;
import com.gregtechceu.gtceu.api.multiblock.PatternCondition;
import com.gregtechceu.gtceu.api.multiblock.StructureDir;
import com.gregtechceu.gtceu.api.multiblock.pattern.match.MultiBlockPattern;
import com.gregtechceu.gtceu.api.multiblock.pattern.model.PatternAxes;
import com.gregtechceu.gtceu.api.multiblock.pattern.model.PatternBinding;
import com.gregtechceu.gtceu.api.multiblock.pattern.model.PatternConstraint;
import com.gregtechceu.gtceu.api.multiblock.pattern.model.PatternDefinition;
import com.gregtechceu.gtceu.api.multiblock.pattern.model.PatternDirection;
import com.gregtechceu.gtceu.api.multiblock.pattern.model.PatternNode;
import com.gregtechceu.gtceu.api.multiblock.pattern.model.PatternOrientation;
import com.gregtechceu.gtceu.api.multiblock.pattern.model.PatternOrigin;
import com.gregtechceu.gtceu.api.multiblock.pattern.model.PatternPredicateDefinition;
import com.gregtechceu.gtceu.api.multiblock.pattern.model.PatternRepeatDirection;
import com.gregtechceu.gtceu.api.multiblock.pattern.predicate.PatternPredicate;
import com.gregtechceu.gtceu.api.multiblock.pattern.predicate.PatternPredicates;
import com.gregtechceu.gtceu.api.multiblock.pattern.predicate.PredicateController;
import com.gregtechceu.gtceu.api.multiblock.util.RelativeDirection;
import com.gregtechceu.gtceu.data.pattern.StructurePatternKey;
import com.gregtechceu.gtceu.data.pattern.StructurePatternResolver;

import com.google.common.base.Joiner;
import it.unimi.dsi.fastutil.chars.Char2ObjectArrayMap;
import it.unimi.dsi.fastutil.chars.Char2ObjectMap;
import it.unimi.dsi.fastutil.chars.CharArrayList;
import it.unimi.dsi.fastutil.chars.CharList;
import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

public class PatternBuilder {

    private static final Joiner COMMA_JOIN = Joiner.on(",");
    private final MultiblockMachineDefinition definition;
    private final StructurePatternKey definitionKey;
    private final List<AisleUnit> units;
    private final Char2ObjectMap<PatternPredicate> symbolMap;
    private final StructureDir structureDir;
    private List<String[]> repeatableGroup;
    private PatternCondition condition;
    private int definitionAisleIndex;
    private int aisleHeight;
    private int rowWidth;
    private final List<PatternNode> extraNodes = new ObjectArrayList<>();
    private final List<PatternConstraint> constraints = new ObjectArrayList<>();

    private record AisleUnit(List<String[]> slices, int minRepeat, int maxRepeat) {}

    private PatternBuilder(RelativeDirection charDir, RelativeDirection stringDir, RelativeDirection aisleDir,
                           MultiblockMachineDefinition definition, String structureName) {
        this.definition = definition;
        this.definitionKey = definition == null ? null : new StructurePatternKey(definition.getId(), structureName);
        units = new ObjectArrayList<>();
        symbolMap = new Char2ObjectArrayMap<>();
        structureDir = new StructureDir(charDir, stringDir, aisleDir);
        structureDir.check();
        this.symbolMap.put(' ', PatternPredicates.any());
        if (definition != null) {
            this.symbolMap.put('~', PatternPredicates.controller(PatternPredicates.blocks(definition.getBlock())));
        }
    }

    /**
     * Adds a single aisle to this pattern. Multiple calls increase the aisleDir by 1.
     */
    public PatternBuilder aisle(String... aisle) {
        validateAisle(aisle);
        if (repeatableGroup == null) {
            units.add(new AisleUnit(List.<String[]>of(copyAisle(aisle)), 1, 1));
        } else {
            repeatableGroup.add(copyAisle(aisle));
        }
        return this;
    }

    public PatternBuilder slice(String... rows) {
        return aisle(rows);
    }

    public PatternBuilder sliceRepeatable(int min, int max, String... rows) {
        return beginRepeatable().aisle(rows).endRepeatable(min, max);
    }

    public PatternBuilder use(String fragmentId, Map<String, PatternBinding> bindings) {
        extraNodes.add(new PatternNode.Fragment(fragmentId, bindings));
        return this;
    }

    public PatternBuilder place(String fragmentId, PatternOrigin.Offset anchor) {
        extraNodes.add(new PatternNode.Fragment(fragmentId, Map.of()));
        return this;
    }

    public PatternBuilder constraint(PatternConstraint constraint) {
        constraints.add(constraint);
        return this;
    }

    public PatternBuilder aisleFromDefinition(String... aisle) {
        return aisle(aisle);
    }

    public PatternBuilder aisleFromDefinition() {
        if (definition == null) {
            throw new IllegalStateException("No multiblock definition was bound to this pattern builder");
        }
        return aisleFromDefinition(definitionKey, definitionAisleIndex++);
    }

    public PatternBuilder aisleFromDefinition(int index) {
        if (definition == null) {
            throw new IllegalStateException("No multiblock definition was bound to this pattern builder");
        }
        return aisleFromDefinition(definitionKey, index);
    }

    public PatternBuilder aisleFromDefinition(MultiblockMachineDefinition definition, String structureName) {
        return aisleFromDefinition(new StructurePatternKey(definition.getId(), structureName), definitionAisleIndex++);
    }

    public PatternBuilder aisleFromDefinition(MultiblockMachineDefinition definition, String structureName,
                                              int index) {
        return aisleFromDefinition(new StructurePatternKey(definition.getId(), structureName), index);
    }

    public PatternBuilder aisleFromDefinition(StructurePatternKey key, int index) {
        List<String[]> aisles = StructurePatternResolver.loadStringArrayDefinition(key).aisles();
        if (index < 0 || index >= aisles.size()) {
            throw new IllegalArgumentException("Json structure definition for " + key + " does not contain aisle " +
                    index + "; found " + aisles.size() + " aisles");
        }
        return aisle(aisles.get(index));
    }

    public PatternBuilder aislesFromDefinition() {
        if (definition == null) {
            throw new IllegalStateException("No multiblock definition was bound to this pattern builder");
        }
        return aislesFromDefinition(definitionKey);
    }

    public PatternBuilder aislesFromDefinition(MultiblockMachineDefinition definition, String structureName) {
        return aislesFromDefinition(new StructurePatternKey(definition.getId(), structureName));
    }

    public PatternBuilder aislesFromDefinition(StructurePatternKey key) {
        return StructurePatternResolver.applyStringArrayDefinition(this, key);
    }

    public PatternBuilder beginRepeatable() {
        if (repeatableGroup != null) {
            throw new IllegalStateException("Cannot begin a nested repeatable aisle group");
        }
        repeatableGroup = new ObjectArrayList<>();
        return this;
    }

    public PatternBuilder endRepeatable(int minRepeat, int maxRepeat) {
        if (repeatableGroup == null) {
            throw new IllegalStateException("No repeatable aisle group has been started");
        }
        if (repeatableGroup.isEmpty()) {
            throw new IllegalStateException("Repeatable aisle group must contain at least one aisle");
        }
        if (minRepeat > maxRepeat) {
            throw new IllegalArgumentException("Lower bound of repeat counting must smaller than upper bound!");
        }
        units.add(new AisleUnit(List.copyOf(repeatableGroup), minRepeat, maxRepeat));
        repeatableGroup = null;
        return this;
    }

    public PatternBuilder endRepeatable(int repeatCount) {
        return endRepeatable(repeatCount, repeatCount);
    }

    private void validateAisle(String... aisle) {
        if (!ArrayUtils.isEmpty(aisle) && !StringUtils.isEmpty(aisle[0])) {
            if (this.units.isEmpty() && (repeatableGroup == null || repeatableGroup.isEmpty())) {
                this.aisleHeight = aisle.length;
                this.rowWidth = aisle[0].length();
            }

            if (aisle.length != this.aisleHeight) {
                throw new IllegalArgumentException("Expected aisle with height of " + this.aisleHeight +
                        ", but was given one with a height of " + aisle.length + ")");
            } else {
                for (String s : aisle) {
                    if (s.length() != this.rowWidth) {
                        throw new IllegalArgumentException(
                                "Not all rows in the given aisle are the correct width (expected " + this.rowWidth +
                                        ", found one with " + s.length() + ")");
                    }

                    for (char c0 : s.toCharArray()) {
                        if (!this.symbolMap.containsKey(c0)) {
                            this.symbolMap.put(c0, null);
                        }
                    }
                }
            }
        } else {
            throw new IllegalArgumentException("Empty pattern for aisle");
        }
    }

    private String[] copyAisle(String[] aisle) {
        return aisle.clone();
    }

    public static PatternBuilder start() {
        return new PatternBuilder(RelativeDirection.LEFT, RelativeDirection.UP, RelativeDirection.FRONT, null,
                StructurePatternKey.DEFAULT_STRUCTURE_NAME);
    }

    public static PatternBuilder start(MultiblockMachineDefinition definition) {
        return new PatternBuilder(RelativeDirection.LEFT, RelativeDirection.UP, RelativeDirection.FRONT,
                definition, StructurePatternKey.DEFAULT_STRUCTURE_NAME);
    }

    public static PatternBuilder start(MultiblockMachineDefinition definition, String structureName) {
        return new PatternBuilder(RelativeDirection.LEFT, RelativeDirection.UP, RelativeDirection.FRONT,
                definition, structureName);
    }

    public static PatternBuilder start(RelativeDirection charDir, RelativeDirection stringDir,
                                       RelativeDirection aisleDir) {
        return new PatternBuilder(charDir, stringDir, aisleDir, null,
                StructurePatternKey.DEFAULT_STRUCTURE_NAME);
    }

    public static PatternBuilder start(MultiblockMachineDefinition definition, RelativeDirection charDir,
                                       RelativeDirection stringDir,
                                       RelativeDirection aisleDir) {
        return new PatternBuilder(charDir, stringDir, aisleDir, definition,
                StructurePatternKey.DEFAULT_STRUCTURE_NAME);
    }

    public static PatternBuilder start(MultiblockMachineDefinition definition, String structureName,
                                       RelativeDirection charDir, RelativeDirection stringDir,
                                       RelativeDirection aisleDir) {
        return new PatternBuilder(charDir, stringDir, aisleDir, definition, structureName);
    }

    public PatternBuilder where(String symbol, PatternPredicate blockMatcher) {
        return this.where(symbol.charAt(0), blockMatcher);
    }

    public PatternBuilder where(char symbol, PatternPredicate blockMatcher) {
        if (blockMatcher.isAny() || blockMatcher.isAir()) {
            this.symbolMap.put(symbol, blockMatcher);
        } else if (blockMatcher instanceof PredicateController) {
            this.symbolMap.put(symbol, blockMatcher.sort());
        } else {
            this.symbolMap.put(symbol, new PatternPredicate(blockMatcher).sort());
        }
        return this;
    }

    public PatternBuilder condition(Predicate<MultiblockState> condition) {
        return condition(condition, "gtceu.recipe_logic.condition_fails");
    }

    public PatternBuilder condition(Predicate<MultiblockState> condition, String translateKey) {
        this.condition = new PatternCondition(condition, translateKey);
        return this;
    }

    private void checkMissingPatternPredicates() {
        CharList list = new CharArrayList();

        for (var entry : this.symbolMap.char2ObjectEntrySet()) {
            if (entry.getValue() == null) {
                list.add(entry.getCharKey());
            }
        }

        if (!list.isEmpty()) {
            throw new IllegalStateException(
                    "PatternPredicates for character(s) " + COMMA_JOIN.join(list) + " are missing");
        }
    }

    public MultiBlockPattern build() {
        if (repeatableGroup != null) {
            throw new IllegalStateException("Repeatable aisle group must be closed before building the pattern");
        }
        this.checkMissingPatternPredicates();
        int unitCount = this.units.size();
        int size = this.units.stream().mapToInt(unit -> unit.slices().size()).sum();
        CenterOffset centerOffset = null;
        int[][] aisleRepetitions = new int[unitCount][];
        int[] unitStarts = new int[unitCount];
        int[] unitDepths = new int[unitCount];
        String[][] structureSlices = new String[size][];
        PatternPredicate[][][] predicate = new PatternPredicate[size][][];

        for (int unitIndex = 0, sliceIndex = 0, minZ = 0, maxZ = 0; unitIndex < unitCount; unitIndex++) {
            AisleUnit unit = units.get(unitIndex);
            int unitDepth = unit.slices().size();
            unitStarts[unitIndex] = sliceIndex;
            unitDepths[unitIndex] = unitDepth;
            aisleRepetitions[unitIndex] = new int[] { unit.minRepeat(), unit.maxRepeat() };

            for (int inner = 0; inner < unitDepth; inner++, sliceIndex++) {
                String[] aisle = unit.slices().get(inner);
                structureSlices[sliceIndex] = aisle.clone();
                for (int j = 0; j < this.aisleHeight; j++) {
                    for (int k = 0; k < this.rowWidth; k++) {
                        var tp = this.symbolMap.get(aisle[j].charAt(k));
                        if (tp != null) {
                            var pi = predicate[sliceIndex];
                            if (pi == null) {
                                predicate[sliceIndex] = pi = new PatternPredicate[this.aisleHeight][];
                            }
                            var pj = pi[j];
                            if (pj == null) {
                                pi[j] = pj = new PatternPredicate[this.rowWidth];
                            }
                            pj[k] = tp;
                            if (tp instanceof PredicateController) {
                                centerOffset = new CenterOffset(k, j, sliceIndex, minZ + inner, maxZ + inner);
                            }
                        }
                    }
                }
            }

            minZ += unitDepth * unit.minRepeat();
            maxZ += unitDepth * unit.maxRepeat();
        }

        var pattern = new MultiBlockPattern(predicate, structureDir, aisleRepetitions, unitStarts, unitDepths,
                structureSlices, centerOffset, size, this.aisleHeight, this.rowWidth);
        if (condition != null) pattern.condition = condition;
        if (definition != null) {
            pattern.predicates = symbolMap.values();
            pattern.attachDefinition(buildDefinition());
            // definition.setCheckPriority(-(pattern.fingerLength * pattern.thumbLength * pattern.palmLength));
        }
        return pattern;
    }

    /** Builds the canonical AST consumed by JSON, binary and compiler paths. */
    public PatternDefinition buildDefinition() {
        if (definition == null) {
            throw new IllegalStateException("A Java pattern definition must be bound to a machine");
        }
        checkMissingPatternPredicates();
        var nodes = new ObjectArrayList<PatternNode>();
        for (int index = 0; index < units.size(); index++) {
            AisleUnit unit = units.get(index);
            var layers = new ObjectArrayList<List<String>>();
            for (String[] slice : unit.slices()) layers.add(List.of(slice));
            PatternNode.Fixed fixed = new PatternNode.Fixed(layers);
            if (unit.minRepeat() == 1 && unit.maxRepeat() == 1) {
                nodes.add(fixed);
            } else {
                nodes.add(new PatternNode.Repeat("unit-" + index, PatternDirection.FRONT,
                        PatternRepeatDirection.POSITIVE, unit.minRepeat(), unit.maxRepeat(), fixed));
            }
        }
        nodes.addAll(extraNodes);
        PatternNode body = nodes.size() == 1 ? nodes.getFirst() :
                new PatternNode.Sequence(PatternDirection.FRONT, nodes);
        var predicates = new Object2ObjectLinkedOpenHashMap<Character, PatternPredicateDefinition>();
        symbolMap.forEach((symbol, value) -> predicates.put(symbol,
                new PatternPredicateDefinition("java", "", Map.of(), List.of())));
        return new PatternDefinition(definition.getId(), definitionKey.resourceId(),
                new PatternAxes(
                        toPatternDirection(structureDir.aisleDir()), toPatternDirection(structureDir.stringDir()),
                        toPatternDirection(structureDir.charDir())),
                PatternOrientation.defaults(), PatternOrigin.controller(),
                Map.of(), Map.of(), predicates, body, constraints);
    }

    private static PatternDirection toPatternDirection(RelativeDirection direction) {
        return PatternDirection.valueOf(direction.name());
    }
}
