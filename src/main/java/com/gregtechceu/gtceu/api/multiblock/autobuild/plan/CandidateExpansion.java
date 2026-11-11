package com.gregtechceu.gtceu.api.multiblock.autobuild.plan;

import com.gregtechceu.gtceu.api.machine.MultiblockMachineDefinition;
import com.gregtechceu.gtceu.api.machine.multiblock.PartAbility;
import com.gregtechceu.gtceu.api.multiblock.MultiblockBlockInfo;
import com.gregtechceu.gtceu.api.multiblock.autobuild.BlockCategories;
import com.gregtechceu.gtceu.api.multiblock.autobuild.BuildProblem;
import com.gregtechceu.gtceu.api.multiblock.autobuild.MaterialKey;
import com.gregtechceu.gtceu.api.multiblock.autobuild.batch.StructureRequest;
import com.gregtechceu.gtceu.api.multiblock.pattern.constraint.PatternConstraintEvaluator;
import com.gregtechceu.gtceu.api.multiblock.pattern.constraint.PatternRequirement;
import com.gregtechceu.gtceu.api.multiblock.pattern.layout.PatternLayout;
import com.gregtechceu.gtceu.api.multiblock.pattern.layout.PatternLayoutCell;
import com.gregtechceu.gtceu.api.multiblock.pattern.match.MultiBlockPattern;
import com.gregtechceu.gtceu.api.multiblock.structurepredicate.AbilityPredicate;
import com.gregtechceu.gtceu.api.multiblock.structurepredicate.AlternativePredicate;
import com.gregtechceu.gtceu.api.multiblock.structurepredicate.PatternObservation;
import com.gregtechceu.gtceu.api.multiblock.structurepredicate.PatternPredicate;
import com.gregtechceu.gtceu.api.multiblock.structurepredicate.PredicateRule;
import com.gregtechceu.gtceu.api.multiblock.structurepredicate.RestrictedPredicate;
import com.gregtechceu.gtceu.api.multiblock.structurepredicate.StructurePredicate;
import com.gregtechceu.gtceu.api.multiblock.structurepredicate.StructurePreviewChoice;
import com.gregtechceu.gtceu.api.multiblock.structurepredicate.StructurePreviewConstraint;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectMaps;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectImmutableList;
import it.unimi.dsi.fastutil.objects.ObjectLinkedOpenHashSet;
import it.unimi.dsi.fastutil.objects.ObjectList;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import it.unimi.dsi.fastutil.objects.ObjectSet;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import org.jspecify.annotations.Nullable;

import java.util.function.Function;
import java.util.stream.Stream;

import static com.gregtechceu.gtceu.api.multiblock.autobuild.plan.BatchPlanner.*;

public final class CandidateExpansion {

    public static Object2ObjectMap<String, ObjectList<ResourceLocation>> tierChoicesForPredicates(
                                                                                                  MultiblockMachineDefinition definition,
                                                                                                  ObjectList<PatternPredicate> predicates) {
        ObjectList<BuildProblem> problems = new ObjectArrayList<>();
        ObjectList<ResolvedCandidate> candidates = new ObjectArrayList<>();
        ConstraintIds ids = new ConstraintIds();
        for (PatternPredicate predicate : predicates) {
            candidates.addAll(resolveCandidates(definition, predicate, CANONICAL_FRONT, CANONICAL_UP, false,
                    ids, problems, null, null));
        }
        if (!problems.isEmpty()) {
            throw new IllegalStateException(problems.getFirst().message().getString());
        }
        Object2ObjectLinkedOpenHashMap<String, ObjectLinkedOpenHashSet<ResourceLocation>> encountered = new Object2ObjectLinkedOpenHashMap<>();
        for (ResolvedCandidate candidate : candidates) {
            for (String tierGroup : candidate.tierGroups()) {
                encountered.computeIfAbsent(tierGroup, ignored -> new ObjectLinkedOpenHashSet<>())
                        .add(BuiltInRegistries.BLOCK.getKey(candidate.state().getBlock()));
            }
        }
        Object2ObjectLinkedOpenHashMap<String, ObjectList<ResourceLocation>> result = new Object2ObjectLinkedOpenHashMap<>();
        encountered.forEach((group, candidateIds) -> {
            ObjectArrayList<ResourceLocation> ordered = new ObjectArrayList<>();
            Block[] registered = BlockCategories.getBlocks(group);
            if (registered != null) {
                for (Block block : registered) {
                    ResourceLocation blockId = BuiltInRegistries.BLOCK.getKey(block);
                    if (candidateIds.contains(blockId)) ordered.add(blockId);
                }
            }
            candidateIds.stream().filter(candidate -> !ordered.contains(candidate)).forEach(ordered::add);
            result.put(group, new ObjectImmutableList<>(ordered));
        });
        return Object2ObjectMaps.unmodifiable(result);
    }

    public static ObjectList<UnresolvedCell> expandStructure(MultiblockMachineDefinition definition,
                                                             MultiBlockPattern pattern,
                                                             StructureRequest options,
                                                             ResolutionContext context,
                                                             ConstraintIds constraintIds,
                                                             ObjectList<ConstraintLimit> requirements,
                                                             ObjectList<BuildProblem> problems) {
        ObjectList<UnresolvedCell> cells = new ObjectArrayList<>();
        PatternLayout layout;
        try {
            layout = pattern.expandLayout(options.repetitions().toIntArray(), options.choices());
        } catch (IllegalArgumentException exception) {
            problems.add(problem(context.worldPos(),
                    "gtpm.multiblock.autobuild.invalid_layout", options.structureName(), exception.getMessage()));
            return cells;
        }
        for (PatternRequirement requirement : PatternConstraintEvaluator.requirements(pattern.getSourceDefinition(),
                layout)) {
            ConstraintLimit limit = new ConstraintLimit(requirement, constraintIds.id(requirement),
                    requirement.minimum(), requirement.maximum(), -1, -1, -1);
            requirements.add(limit);
        }
        int minimumDepth = layout.cells().stream().mapToInt(PatternLayoutCell::depth).min().orElse(0);
        BuildProblem unavailable = null;
        for (var cell : layout.cells()) {
            PatternPredicate predicate = cell.predicate();
            PatternCellKey cellKey = new PatternCellKey(cell.unit(), cell.repetition(), cell.inner(), cell.row(),
                    cell.column());
            BlockPos relativePos = pattern.getActualRelativeOffset(cell.width(), cell.height(), cell.depth(),
                    context.frontFacing(), context.upwardsFacing(), options.flipMode());
            BlockPos worldPos = context.canonical() ? null : context.origin().offset(relativePos);
            BlockState observed = null;
            if (!context.canonical()) {
                Level level = context.level();
                if (level.isOutsideBuildHeight(worldPos)) {
                    if (unavailable == null) {
                        unavailable = new BuildProblem(worldPos,
                                Component.translatable("gtpm.multiblock.autobuild.outside_build_height",
                                        position(worldPos)),
                                false);
                    }
                } else if (!level.getChunkSource().hasChunk(SectionPos.blockToSectionCoord(worldPos.getX()),
                        SectionPos.blockToSectionCoord(worldPos.getZ()))) {
                            if (unavailable == null) {
                                unavailable = new BuildProblem(worldPos,
                                        Component.translatable("gtpm.multiblock.autobuild.unloaded",
                                                position(worldPos)),
                                        false);
                            }
                        } else {
                            observed = level.getBlockState(worldPos);
                        }
            }
            ObjectList<ResolvedCandidate> candidates = resolveCandidates(definition, predicate,
                    context.frontFacing(), context.upwardsFacing(), options.flipMode(), constraintIds, problems,
                    worldPos, layout);
            for (ResolvedCandidate candidate : candidates) {
                PatternObservation observation = candidate.observation();
                for (ConstraintLimit limit : requirements) {
                    PatternRequirement requirement = (PatternRequirement) limit.key();
                    if ((requirement.scope().equals("all") || cell.scopes().contains(requirement.scope())) &&
                            (requirement.fact().equals("token:" + cell.token()) ||
                                    observation.facts().contains(requirement.fact()) ||
                                    predicate.facts.contains(requirement.fact()))) {
                        candidate.limits().add(limit);
                    }
                }
            }
            Direction requiredDirection = resolveDirection(predicate, context.frontFacing(), context.upwardsFacing(),
                    options.flipMode());
            cells.add(new UnresolvedCell(options.structureName(), cellKey, relativePos, worldPos, observed,
                    cell.depth() - minimumDepth, predicate, candidates, requiredDirection));
        }
        if (unavailable != null) problems.add(unavailable);
        return cells;
    }

    public static ObjectList<ResolvedCandidate> resolveCandidates(MultiblockMachineDefinition definition,
                                                                  PatternPredicate predicate,
                                                                  Direction frontFacing,
                                                                  Direction upwardsFacing,
                                                                  boolean flipped,
                                                                  ConstraintIds constraintIds,
                                                                  ObjectList<BuildProblem> problems,
                                                                  @Nullable BlockPos pos,
                                                                  @Nullable PatternLayout layout) {
        if (predicate.isAny()) {
            return ObjectArrayList.of(ResolvedCandidate.wildcard());
        }
        Object2ObjectLinkedOpenHashMap<BlockState, CandidateAccumulator> candidates = new Object2ObjectLinkedOpenHashMap<>();
        CandidateOrder order = new CandidateOrder();
        Direction direction = resolveDirection(predicate, frontFacing, upwardsFacing, flipped);
        for (PredicateRule simple : predicate.limited) {
            addSimpleCandidates(candidates, simple, order, constraintIds, direction);
        }
        for (PredicateRule simple : predicate.common) {
            addSimpleCandidates(candidates, simple, order, constraintIds, direction);
        }
        for (StructurePredicate structurePredicate : predicate.structurePatternPredicates) {
            ObjectList<StructureChoice> choices = structureChoices(structurePredicate, definition, constraintIds,
                    layout);
            for (StructureChoice choice : choices) {
                ObjectSet<String> tierGroups = tierGroups(choice.candidates().stream()
                        .map(info -> info.getBlockState().getBlock()));
                for (MultiblockBlockInfo blockInfo : choice.candidates()) {
                    addCandidate(candidates, blockInfo, choice.limits(), choice.hatch(),
                            choice.candidates().size() > 1, tierGroups, order.next(), direction, choice.observer());
                }
            }
        }
        if (predicate.hasAir() && candidates.keySet().stream().noneMatch(BlockState::isAir)) {
            addCandidate(candidates, MultiblockBlockInfo.EMPTY, ObjectList.of(), false, false, ObjectSet.of(),
                    order.next(),
                    direction, state -> PatternObservation.of(state.isAir()));
        }

        ObjectArrayList<ResolvedCandidate> result = new ObjectArrayList<>(candidates.size());
        for (CandidateAccumulator candidate : candidates.values()) {
            PlannedBlockInfo blockInfo = candidate.blockInfo();
            BlockState state = blockInfo.blockState();
            MaterialKey materialKey = materialKey(blockInfo.itemStack());
            result.add(new ResolvedCandidate(state, blockInfo, candidate.limits(),
                    candidate.hatch(),
                    candidate.tierGroups(), candidate.priority() * order.next + candidate.order(),
                    materialKey, false, candidate.observation()));
        }
        if (result.isEmpty()) {
            problems.add(problem(pos,
                    "gtpm.multiblock.autobuild.no_candidate", position(pos)));
        }
        return result;
    }

    public static void addSimpleCandidates(Object2ObjectMap<BlockState, CandidateAccumulator> result,
                                           PredicateRule simple, CandidateOrder order, ConstraintIds constraintIds,
                                           @Nullable Direction direction) {
        if (simple.isAny()) return;
        ConstraintLimit limit = ConstraintLimit.fromSimple(simple, constraintIds.id(simple));
        if (simple.isAir()) {
            addCandidate(result, MultiblockBlockInfo.EMPTY, ObjectList.of(limit), false, false, ObjectSet.of(),
                    order.next(),
                    direction, state -> PatternObservation.of(state.isAir()));
            return;
        }
        MultiblockBlockInfo representative = simple.blockInfo.get();
        Function<BlockState, PatternObservation> observer = simple.getCandidateObserver();
        if (observer == null) observer = state -> PatternObservation.of(true);
        ObjectList<BlockState> states = simple.candidateStates();
        ObjectSet<String> tierGroups = tierGroups(states.stream().map(BlockState::getBlock));
        for (BlockState state : states) {
            MultiblockBlockInfo info = representative != null &&
                    (representative.getBlockState().equals(state) || simple.getPreviewStates() == null &&
                            representative.getBlockState().getBlock() == state.getBlock()) ?
                                    representative : new MultiblockBlockInfo(state);
            addCandidate(result, info, ObjectList.of(limit), false, states.size() > 1,
                    tierGroups, order.next(), direction, observer);
        }
    }

    /** Only a list that actually offers multiple blocks in a category participates in its tier selection. */
    public static ObjectSet<String> tierGroups(Stream<Block> blocks) {
        Object2ObjectMap<String, Block> firstBlocks = new Object2ObjectOpenHashMap<>();
        ObjectSet<String> groups = new ObjectOpenHashSet<>();
        blocks.forEach(block -> {
            String group = BlockCategories.getCategory(block);
            if (group != null) {
                Block first = firstBlocks.putIfAbsent(group, block);
                if (first != null && first != block) groups.add(group);
            }
        });
        return groups;
    }

    public static ObjectList<StructureChoice> structureChoices(StructurePredicate predicate,
                                                               MultiblockMachineDefinition definition,
                                                               ConstraintIds constraintIds,
                                                               @Nullable PatternLayout layout) {
        if (predicate instanceof RestrictedPredicate restricted) {
            ConstraintLimit limit = ConstraintLimit.fromStructure(new StructurePreviewConstraint(restricted,
                    restricted.minCount(), restricted.maxCount(), restricted.minCountByLayer(),
                    restricted.maxCountByLayer(), restricted.previewCount()), constraintIds);
            return structureChoices(restricted.predicate(), definition, constraintIds, layout).stream()
                    .map(choice -> choice.withLimit(limit))
                    .collect(ObjectImmutableList.toList());
        }
        if (predicate instanceof AlternativePredicate alternatives) {
            return alternatives.previewAlternatives(definition, layout).stream()
                    .flatMap(child -> structureChoices(child, definition, constraintIds, layout).stream())
                    .collect(ObjectImmutableList.toList());
        }
        boolean hatch = predicate instanceof AbilityPredicate;
        ObjectArrayList<StructureChoice> result = new ObjectArrayList<>();
        for (StructurePreviewChoice choice : predicate.previewChoices(definition)) {
            ObjectList<ConstraintLimit> limits = choice.constraints().stream()
                    .map(constraint -> ConstraintLimit.fromStructure(constraint, constraintIds))
                    .collect(ObjectImmutableList.toList());
            result.add(new StructureChoice(choice.candidates(), limits, hatch, choice.observer()));
        }
        return result;
    }

    public static void addCandidate(Object2ObjectMap<BlockState, CandidateAccumulator> result,
                                    MultiblockBlockInfo blockInfo,
                                    ObjectList<ConstraintLimit> limits, boolean hatch,
                                    boolean replaceable, ObjectSet<String> tierGroups,
                                    int order, @Nullable Direction direction,
                                    Function<BlockState, PatternObservation> observer) {
        BlockState state = blockInfo.getBlockState();
        if (direction != null && !state.isAir()) {
            state = MultiBlockPattern.applyDirectionalState(state, direction);
            if (!MultiBlockPattern.matchesDirectionalState(state, direction)) return;
        }
        PatternObservation observation = observer.apply(state);
        if (!observation.matched()) return;
        PlannedBlockInfo planned = PlannedBlockInfo.from(blockInfo).withState(state);
        hatch = hatch || isHatch(state.getBlock());
        int priority = hatch ? 2 : replaceable ? 1 : 0;
        String category = BlockCategories.getCategory(state.getBlock());
        ObjectList<String> groups = category != null && tierGroups.contains(category) ? ObjectList.of(category) :
                ObjectList.of();
        CandidateAccumulator existing = result.get(planned.blockState());
        if (existing == null) {
            result.put(planned.blockState(), new CandidateAccumulator(planned, new ObjectArrayList<>(limits), hatch,
                    groups, priority, order, observation));
            return;
        }
        for (ConstraintLimit limit : limits) {
            if (existing.limits().stream().noneMatch(current -> current.key() == limit.key())) {
                existing.limits().add(limit);
            }
        }
        if (priority < existing.priority()) {
            result.put(planned.blockState(), new CandidateAccumulator(existing.blockInfo(), existing.limits(),
                    hatch, groups, priority, order, existing.observation()));
        }
    }

    public static @Nullable Direction resolveDirection(PatternPredicate predicate, Direction frontFacing,
                                                       Direction upwardsFacing, boolean flipped) {
        if (predicate.relativeDirection != null) {
            return predicate.relativeDirection.getRelative(frontFacing, upwardsFacing, flipped);
        }
        return predicate.fixedDirection;
    }

    public static @Nullable MaterialKey materialKey(ItemStack stack) {
        return stack.isEmpty() || stack.is(Items.AIR) ? null : MaterialKey.of(stack);
    }

    public static boolean isHatch(Block block) {
        return PartAbility.values().stream().anyMatch(ability -> ability.isApplicable(block));
    }

    public static void validateTierChoices(MultiblockMachineDefinition definition, StructureRequest options,
                                           ObjectList<UnresolvedCell> cells,
                                           ObjectList<BuildProblem> problems, @Nullable BlockPos pos) {
        Object2ObjectMap<String, ResourceLocation> selections = new Object2ObjectLinkedOpenHashMap<>(
                options.tierChoices());

        Object2ObjectLinkedOpenHashMap<String, ObjectLinkedOpenHashSet<ResourceLocation>> available = new Object2ObjectLinkedOpenHashMap<>();
        for (UnresolvedCell cell : cells) {
            for (ResolvedCandidate candidate : cell.candidates()) {
                for (String tierGroup : candidate.tierGroups()) {
                    available.computeIfAbsent(tierGroup, ignored -> new ObjectLinkedOpenHashSet<>())
                            .add(BuiltInRegistries.BLOCK.getKey(candidate.state().getBlock()));
                }
            }
        }
        available.forEach((group, candidates) -> selections.putIfAbsent(group, candidates.getFirst()));
        var declared = tierChoicesForPredicates(definition,
                definition.getPattern(options.structureName()).getAllPredicates());
        for (var entry : selections.entrySet()) {
            var candidates = declared.get(entry.getKey());
            if (candidates == null || !candidates.contains(entry.getValue())) {
                problems.add(problem(pos,
                        "gtpm.multiblock.autobuild.tier_candidate_invalid", entry.getValue().toString(),
                        entry.getKey(), options.structureName()));
            }
        }
        if (!problems.isEmpty()) return;

        for (int index = 0; index < cells.size(); index++) {
            UnresolvedCell cell = cells.get(index);
            ObjectList<ResolvedCandidate> filtered = cell.candidates().stream().filter(candidate -> {
                ResourceLocation blockId = BuiltInRegistries.BLOCK.getKey(candidate.state().getBlock());
                for (String tierGroup : candidate.tierGroups()) {
                    ResourceLocation selected = selections.get(tierGroup);
                    if (selected != null && !selected.equals(blockId)) return false;
                }
                return true;
            }).collect(ObjectImmutableList.toList());
            if (filtered.isEmpty() && !cell.predicate().isAny()) {
                problems.add(problem(cell.worldPos(),
                        "gtpm.multiblock.autobuild.tier_domain_empty", cell.cellKey().toString()));
            } else {
                cells.set(index, cell.withCandidates(filtered));
            }
        }
    }

    public record UnresolvedCell(String structureName, PatternCellKey cellKey, BlockPos relativePos,
                                 @Nullable BlockPos worldPos, @Nullable BlockState observedState, int layer,
                                 PatternPredicate predicate,
                                 ObjectList<ResolvedCandidate> candidates, @Nullable Direction requiredDirection) {

        public UnresolvedCell withCandidates(ObjectList<ResolvedCandidate> replacement) {
            return new UnresolvedCell(structureName, cellKey, relativePos, worldPos, observedState, layer, predicate,
                    new ObjectImmutableList<>(replacement), requiredDirection);
        }

        public UnresolvedCell withObservedState(@Nullable BlockState observed) {
            return new UnresolvedCell(structureName, cellKey, relativePos, worldPos, observed, layer, predicate,
                    candidates, requiredDirection);
        }
    }

    public record ResolvedCandidate(BlockState state, PlannedBlockInfo blockInfo, ObjectList<ConstraintLimit> limits,
                                    boolean hatch, ObjectList<String> tierGroups, int order,
                                    @Nullable MaterialKey materialKey, boolean any,
                                    PatternObservation observation) {

        public static ResolvedCandidate wildcard() {
            return new ResolvedCandidate(Blocks.AIR.defaultBlockState(),
                    PlannedBlockInfo.from(Blocks.AIR.defaultBlockState()),
                    new ObjectArrayList<>(), false, ObjectList.of(), 0, null, true, PatternObservation.of(true));
        }
    }

    public record CandidateAccumulator(PlannedBlockInfo blockInfo, ObjectList<ConstraintLimit> limits, boolean hatch,
                                       ObjectList<String> tierGroups, int priority, int order,
                                       PatternObservation observation) {}

    public record StructureChoice(ObjectList<MultiblockBlockInfo> candidates, ObjectList<ConstraintLimit> limits,
                                  boolean hatch,
                                  Function<BlockState, PatternObservation> observer) {

        private StructureChoice withLimit(ConstraintLimit limit) {
            ObjectArrayList<ConstraintLimit> result = new ObjectArrayList<>(limits.size() + 1);
            result.addAll(limits);
            result.add(limit);
            return new StructureChoice(candidates, result, hatch, observer);
        }
    }

    public record ConstraintLimit(Object key, String id, int min, int max, int layerMin, int layerMax,
                                  int previewCount) {

        private static ConstraintLimit fromSimple(PredicateRule predicate, String id) {
            return new ConstraintLimit(predicate, id, predicate.minCount, predicate.maxCount,
                    predicate.minLayerCount, predicate.maxLayerCount, predicate.previewCount);
        }

        private static ConstraintLimit fromStructure(StructurePreviewConstraint constraint, ConstraintIds ids) {
            StructurePredicate key = ids.predicates.computeIfAbsent(constraint.key(), Function.identity());
            return new ConstraintLimit(key, ids.id(key), constraint.minCount().orElse(-1),
                    constraint.maxCount().orElse(-1), constraint.minCountByLayer().orElse(-1),
                    constraint.maxCountByLayer().orElse(-1), constraint.previewCount().orElse(-1));
        }
    }

    public static final class ConstraintIds {

        private final Reference2ObjectOpenHashMap<Object, String> ids = new Reference2ObjectOpenHashMap<>();
        // Runtime structure matching shares counters between value-equal predicates as well.
        private final Object2ObjectMap<StructurePredicate, StructurePredicate> predicates = new Object2ObjectOpenHashMap<>();

        private String id(Object key) {
            return ids.computeIfAbsent(key, ignored -> "constraint_" + ids.size());
        }
    }

    public static final class CandidateOrder {

        private int next;

        private int next() {
            return next++;
        }
    }

    public record SelectedCell(UnresolvedCell cell, ResolvedCandidate candidate) {}
}
