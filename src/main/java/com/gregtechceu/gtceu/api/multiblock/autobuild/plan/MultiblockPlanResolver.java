package com.gregtechceu.gtceu.api.multiblock.autobuild.plan;

import com.gregtechceu.gtceu.GTCEu;
import com.gregtechceu.gtceu.api.machine.MultiblockMachineDefinition;
import com.gregtechceu.gtceu.api.machine.multiblock.MultiblockControllerMachine;
import com.gregtechceu.gtceu.api.machine.multiblock.PartAbility;
import com.gregtechceu.gtceu.api.multiblock.MultiblockBlockInfo;
import com.gregtechceu.gtceu.api.multiblock.autobuild.AutoBuildBlockMap;
import com.gregtechceu.gtceu.api.multiblock.autobuild.AutoBuildItemKey;
import com.gregtechceu.gtceu.api.multiblock.autobuild.AutoBuildMaterialSource;
import com.gregtechceu.gtceu.api.multiblock.autobuild.AutoBuildProblem;
import com.gregtechceu.gtceu.api.multiblock.autobuild.batch.AutoBuildBatchRequest;
import com.gregtechceu.gtceu.api.multiblock.autobuild.batch.AutoBuildMode;
import com.gregtechceu.gtceu.api.multiblock.autobuild.batch.AutoBuildSharedOptions;
import com.gregtechceu.gtceu.api.multiblock.autobuild.batch.AutoBuildStructureOptions;
import com.gregtechceu.gtceu.api.multiblock.pattern.layout.PatternLayout;
import com.gregtechceu.gtceu.api.multiblock.pattern.match.MultiBlockPattern;
import com.gregtechceu.gtceu.api.multiblock.pattern.predicate.PatternPredicate;
import com.gregtechceu.gtceu.api.multiblock.pattern.predicate.PredicateRule;
import com.gregtechceu.gtceu.api.multiblock.structurepredicate.AbilityPredicate;
import com.gregtechceu.gtceu.api.multiblock.structurepredicate.ConcatenatedPredicate;
import com.gregtechceu.gtceu.api.multiblock.structurepredicate.RestrictedPredicate;
import com.gregtechceu.gtceu.api.multiblock.structurepredicate.StructurePredicate;
import com.gregtechceu.gtceu.api.multiblock.structurepredicate.StructurePreviewChoice;
import com.gregtechceu.gtceu.api.multiblock.structurepredicate.StructurePreviewConstraint;
import com.gregtechceu.gtceu.api.multiblock.structurepredicate.TieredAbilityPredicate;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntComparator;
import it.unimi.dsi.fastutil.ints.IntImmutableList;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongImmutableList;
import it.unimi.dsi.fastutil.longs.LongList;
import it.unimi.dsi.fastutil.objects.Object2IntLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2LongLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2LongMap;
import it.unimi.dsi.fastutil.objects.Object2LongMaps;
import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectMaps;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectCollection;
import it.unimi.dsi.fastutil.objects.ObjectImmutableList;
import it.unimi.dsi.fastutil.objects.ObjectLinkedOpenHashSet;
import it.unimi.dsi.fastutil.objects.ObjectList;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import it.unimi.dsi.fastutil.objects.ObjectSet;
import it.unimi.dsi.fastutil.objects.Reference2IntMap;
import it.unimi.dsi.fastutil.objects.Reference2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import org.jspecify.annotations.Nullable;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Locale;
import java.util.Objects;

/**
 * Resolves multiblock patterns into one deterministic session plan used by preview and execution.
 *
 * <p>
 * This class is intentionally concrete: it has one algorithm and no interchangeable implementation.
 * </p>
 */
public final class MultiblockPlanResolver {

    private static final Direction CANONICAL_FRONT = Direction.NORTH;
    private static final Direction CANONICAL_UP = Direction.NORTH;
    private static final long FINGERPRINT_OFFSET = 0xcbf29ce484222325L;
    private static final long FINGERPRINT_PRIME = 0x100000001b3L;

    /**
     * Resolves an XEI-style plan against an empty world and unlimited representative materials.
     */
    public AutoBuildPlan resolveCanonical(MultiblockMachineDefinition definition, AutoBuildBatchRequest request) {
        return resolve(definition, request, new ResolutionContext(null, null, null, BlockPos.ZERO, CANONICAL_FRONT,
                CANONICAL_UP, true, true)).plan();
    }

    /**
     * Creates the persisted first-use defaults for one structure from its current pattern definition.
     */
    public AutoBuildStructureOptions defaultStructureOptions(MultiblockMachineDefinition definition,
                                                             String structureName) {
        MultiBlockPattern pattern = definition.getPattern(structureName);
        IntList repetitions = new IntArrayList(pattern.getRepeatDescriptors().size());
        for (var repeat : pattern.getRepeatDescriptors()) repetitions.add(repeat.minimum());
        Object2ObjectMap<String, ObjectList<ResourceLocation>> availableTiers = tierChoiceOptions(definition,
                structureName);
        Object2ObjectLinkedOpenHashMap<String, ResourceLocation> tiers = new Object2ObjectLinkedOpenHashMap<>();
        availableTiers.forEach((group, candidates) -> tiers.put(group, candidates.getFirst()));
        Object2ObjectMap<String, String> choices = new Object2ObjectLinkedOpenHashMap<>();
        for (var choice : pattern.getChoiceDescriptors()) choices.put(choice.path(), choice.alternatives().getFirst());
        return new AutoBuildStructureOptions(structureName, repetitions, tiers, false, AutoBuildMode.BUILD, choices);
    }

    /**
     * Returns every reliable tier group and its exact candidates in stable selection order.
     */
    public Object2ObjectMap<String, ObjectList<ResourceLocation>> tierChoiceOptions(MultiblockMachineDefinition definition,
                                                                                    String structureName) {
        return tierChoicesForPredicates(definition, definition.getPattern(structureName).getAllPredicates());
    }

    /** Returns tier domains of the current layout, including the chosen branch and repetition counts. */
    public Object2ObjectMap<String, ObjectList<ResourceLocation>> activeTierChoiceOptions(MultiblockMachineDefinition definition,
                                                                                          AutoBuildStructureOptions options) {
        var layout = definition.getPattern(options.structureName()).expandLayout(options.repetitions().toIntArray(),
                options.choices());
        return tierChoicesForPredicates(definition, layout.cells().stream().map(cell -> cell.predicate()).distinct()
                .collect(ObjectArrayList.toList()));
    }

    private static Object2ObjectMap<String, ObjectList<ResourceLocation>> tierChoicesForPredicates(
                                                                                                   MultiblockMachineDefinition definition,
                                                                                                   ObjectList<PatternPredicate> predicates) {
        ObjectList<AutoBuildProblem> problems = new ObjectArrayList<>();
        ObjectList<ResolvedCandidate> candidates = new ObjectArrayList<>();
        ConstraintIds ids = new ConstraintIds();
        for (PatternPredicate predicate : predicates) {
            candidates.addAll(resolveCandidates(definition, predicate, CANONICAL_FRONT, CANONICAL_UP, false,
                    ids, problems, null));
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
            Block[] registered = AutoBuildBlockMap.MAP.get(group);
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

    /**
     * Resolves a controller-oriented preview without requiring a player or reading material storage.
     */
    public AutoBuildPlan resolveControllerPreview(MultiblockControllerMachine controller,
                                                  AutoBuildBatchRequest request) {
        return resolve(controller.getDefinition(), request,
                new ResolutionContext(controller, null, controller.getLevel(), controller.getBlockPos(),
                        controller.getFrontFacing(), controller.getUpwardsFacing(), false, true))
                .plan();
    }

    /**
     * Resolves a server-authoritative plan against the controller's current world and material snapshots.
     */
    public AutoBuildPlan resolveWorld(MultiblockControllerMachine controller, ServerPlayer player,
                                      AutoBuildBatchRequest request) {
        return resolveWorldSnapshot(controller, player, request).plan();
    }

    /**
     * Resolves a server-authoritative plan and exposes the exact material snapshots and assignments it consumed.
     */
    public ResolvedAutoBuildSnapshot resolveWorldSnapshot(MultiblockControllerMachine controller,
                                                          ServerPlayer player,
                                                          AutoBuildBatchRequest request) {
        if (controller.getLevel() != player.level()) {
            AutoBuildProblem problem = problem(AutoBuildProblem.Type.INVALID_OPTIONS, controller.getBlockPos(),
                    "gtpm.multiblock.autobuild.invalid_target_level");
            return sharedFailureSnapshot(request, problem);
        }
        return resolve(controller.getDefinition(), request,
                new ResolutionContext(controller, player, player.level(), controller.getBlockPos(),
                        controller.getFrontFacing(), controller.getUpwardsFacing(), false, false));
    }

    private ResolvedAutoBuildSnapshot resolve(MultiblockMachineDefinition definition,
                                              AutoBuildBatchRequest request,
                                              ResolutionContext context) {
        try {
            return resolveChecked(definition, request, context);
        } catch (ArithmeticException exception) {
            GTCEu.LOGGER.error("Automatic-build material count overflow for {}", definition.getId(), exception);
            return sharedFailureSnapshot(request, problem(AutoBuildProblem.Type.COUNT_OVERFLOW,
                    context.worldPos(), "gtpm.multiblock.autobuild.count_overflow"));
        }
    }

    private ResolvedAutoBuildSnapshot resolveChecked(MultiblockMachineDefinition definition,
                                                     AutoBuildBatchRequest request,
                                                     ResolutionContext context) {
        Object2IntMap<String> order = structureOrder(definition);
        ObjectArrayList<PendingStructure> pending = new ObjectArrayList<>(request.structures().size());
        ObjectArrayList<AutoBuildProblem> sharedProblems = new ObjectArrayList<>();
        validateSharedSources(request, context, sharedProblems);

        ObjectSet<String> requestedBuilds = new ObjectOpenHashSet<>();
        ObjectSet<String> requestedDemolitions = new ObjectOpenHashSet<>();
        for (AutoBuildStructureOptions options : request.structures()) {
            (options.mode() == AutoBuildMode.BUILD ? requestedBuilds : requestedDemolitions)
                    .add(options.structureName());
        }

        ConstraintIds constraintIds = new ConstraintIds();
        for (AutoBuildStructureOptions options : request.structures()) {
            ObjectArrayList<AutoBuildProblem> problems = new ObjectArrayList<>();
            MultiBlockPattern pattern = null;
            IntList repetitions = IntList.of();
            if (!order.containsKey(options.structureName())) {
                problems.add(problem(AutoBuildProblem.Type.UNKNOWN_STRUCTURE, context.worldPos(),
                        "gtpm.multiblock.autobuild.unknown_structure", options.structureName()));
            } else {
                pattern = definition.getPattern(options.structureName());
                repetitions = validateRepetitions(pattern, options, problems);
                if (options.flipMode() && !definition.isAllowFlip()) {
                    problems.add(problem(AutoBuildProblem.Type.INVALID_OPTIONS, context.worldPos(),
                            "gtpm.multiblock.autobuild.flip_not_allowed"));
                }
                validateDependencies(definition, options, requestedBuilds, requestedDemolitions, problems,
                        context.worldPos());
            }

            ObjectList<UnresolvedCell> cells = ObjectList.of();
            if (pattern != null && problems.isEmpty()) {
                cells = expandStructure(definition, pattern, options, repetitions, context, constraintIds, problems);
                validateTierChoices(definition, options, cells, problems, context.worldPos());
            }
            pending.add(new PendingStructure(options, repetitions, cells, problems));
        }

        pending.sort(executionComparator(order));
        ObjectLinkedOpenHashSet<AutoBuildItemKey> materialKeys = new ObjectLinkedOpenHashSet<>();
        for (PendingStructure structure : pending) {
            if (structure.options().mode() == AutoBuildMode.BUILD) {
                structure.cells().stream().flatMap(cell -> cell.candidates().stream())
                        .map(ResolvedCandidate::materialKey)
                        .filter(Objects::nonNull)
                        .forEach(materialKeys::add);
            }
        }
        boolean hasBuild = pending.stream().anyMatch(
                structure -> structure.options().mode() == AutoBuildMode.BUILD);
        MaterialAllocation materials = MaterialAllocation.create(request.materialSources(), materialKeys, context,
                sharedProblems, hasBuild, request.sharedOptions().useME());

        ObjectArrayList<ResolvedStructurePlan> demolitionPlans = new ObjectArrayList<>();
        ObjectArrayList<ResolvedStructurePlan> buildPlans = new ObjectArrayList<>();
        for (PendingStructure structure : pending) {
            ResolvedStructurePlan plan = resolveStructure(definition, structure, request.sharedOptions(), context,
                    materials);
            if (structure.options().mode() == AutoBuildMode.DEMOLISH) {
                demolitionPlans.add(plan);
            } else {
                buildPlans.add(plan);
            }
        }

        Object2ObjectLinkedOpenHashMap<BlockPos, MergedPlanCell> merged = mergeCells(demolitionPlans, buildPlans);
        Object2ObjectMap<AutoBuildItemKey, ResolvedAutoBuildSnapshot.MaterialAllocation> allocations = materials
                .resolveAllocations(demolitionPlans, buildPlans);
        long fingerprint = fingerprint(demolitionPlans, buildPlans, merged, materials.sourceFingerprints(),
                allocations);
        AutoBuildPlan plan = new AutoBuildPlan(demolitionPlans, buildPlans, merged, sharedProblems, fingerprint);
        return new ResolvedAutoBuildSnapshot(plan, materials.snapshots(), allocations);
    }

    private static ResolvedAutoBuildSnapshot sharedFailureSnapshot(AutoBuildBatchRequest request,
                                                                   AutoBuildProblem problem) {
        ObjectArrayList<ResolvedStructurePlan> demolitions = new ObjectArrayList<>();
        ObjectArrayList<ResolvedStructurePlan> builds = new ObjectArrayList<>();
        for (AutoBuildStructureOptions options : request.structures()) {
            ResolvedStructurePlan plan = new ResolvedStructurePlan(options.structureName(), options.mode(),
                    options.repetitions(), options.flipMode(), ObjectList.of(), Object2LongMaps.emptyMap(),
                    ObjectList.of(problem));
            (options.mode() == AutoBuildMode.DEMOLISH ? demolitions : builds).add(plan);
        }
        AutoBuildPlan plan = new AutoBuildPlan(demolitions, builds, Object2ObjectMaps.emptyMap(),
                ObjectList.of(problem), FINGERPRINT_OFFSET);
        return new ResolvedAutoBuildSnapshot(plan, ObjectList.of(), Object2ObjectMaps.emptyMap());
    }

    private static Object2IntMap<String> structureOrder(MultiblockMachineDefinition definition) {
        Object2IntMap<String> result = new Object2IntLinkedOpenHashMap<>();
        var order = definition.getStructureOrder();
        for (int index = 0; index < order.size(); index++) {
            result.put(order.get(index), index);
        }
        return result;
    }

    private static Comparator<PendingStructure> executionComparator(Object2IntMap<String> order) {
        return Comparator.<PendingStructure, AutoBuildMode>comparing(pending -> pending.options().mode(),
                Comparator.comparingInt(mode -> mode == AutoBuildMode.DEMOLISH ? 0 : 1))
                .thenComparingInt(pending -> {
                    int index = order.getOrDefault(pending.options().structureName(), Integer.MAX_VALUE);
                    return pending.options().mode() == AutoBuildMode.DEMOLISH ? -index : index;
                });
    }

    private static void validateSharedSources(AutoBuildBatchRequest request, ResolutionContext context,
                                              ObjectList<AutoBuildProblem> problems) {
        if (context.canonical() || request.structures().stream()
                .noneMatch(options -> options.mode() == AutoBuildMode.BUILD)) {
            return;
        }
        boolean hasME = false;
        for (AutoBuildMaterialSource source : request.materialSources()) {
            if (source.kind() == AutoBuildMaterialSource.SourceKind.ME) {
                hasME = true;
                continue;
            }
            if (source.kind() == AutoBuildMaterialSource.SourceKind.UNAVAILABLE) {
                AutoBuildProblem unavailable = source.unavailableProblem();
                hasME |= unavailable != null && isStrictAEProblem(unavailable.type());
            }
        }
        if (request.sharedOptions().useME() && !hasME) {
            problems.add(problem(AutoBuildProblem.Type.ME_UNAVAILABLE, context.worldPos(),
                    "gtpm.multiblock.autobuild.me_unavailable"));
        }
    }

    private static boolean isStrictAEProblem(AutoBuildProblem.Type type) {
        return switch (type) {
            case ME_UNAVAILABLE, AE_NOT_INSTALLED, AE_NOT_LINKED, AE_LINKED_DIMENSION_MISSING, AE_WRONG_DIMENSION, AE_ACCESS_POINT_MISSING, AE_ACCESS_POINT_INACTIVE, AE_OUT_OF_RANGE, AE_GRID_UNAVAILABLE -> true;
            default -> false;
        };
    }

    private static void validateDependencies(MultiblockMachineDefinition definition,
                                             AutoBuildStructureOptions options,
                                             ObjectSet<String> requestedBuilds,
                                             ObjectSet<String> requestedDemolitions,
                                             ObjectList<AutoBuildProblem> problems,
                                             @Nullable BlockPos pos) {
        if (options.mode() != AutoBuildMode.BUILD) return;
        for (String dependency : definition.getRequiredStructures(options.structureName())) {
            if (!requestedBuilds.contains(dependency)) {
                problems.add(problem(AutoBuildProblem.Type.INVALID_OPTIONS, pos,
                        "gtpm.multiblock.autobuild.missing_dependency", options.structureName(), dependency));
            }
            if (requestedDemolitions.contains(dependency)) {
                problems.add(problem(AutoBuildProblem.Type.INVALID_OPTIONS, pos,
                        "gtpm.multiblock.autobuild.dependency_mode_conflict", dependency));
            }
        }
    }

    private static IntList validateRepetitions(MultiBlockPattern pattern, AutoBuildStructureOptions options,
                                               ObjectList<AutoBuildProblem> problems) {
        var descriptors = pattern.getRepeatDescriptors();
        if (options.repetitions().size() != descriptors.size()) {
            problems.add(problem(AutoBuildProblem.Type.INVALID_OPTIONS, null,
                    "gtpm.multiblock.autobuild.repetition_count_mismatch", options.structureName(),
                    descriptors.size(), options.repetitions().size()));
            return IntList.of();
        }
        for (var descriptor : descriptors) {
            int index = descriptor.index();
            int value = options.repetitions().getInt(index);
            int min = descriptor.minimum();
            int max = descriptor.maximum();
            if (value < min || value > max) {
                problems.add(problem(AutoBuildProblem.Type.INVALID_OPTIONS, null,
                        "gtpm.multiblock.autobuild.repetition_out_of_range", index, value, min, max));
            }
        }
        return problems.isEmpty() ? options.repetitions() : IntList.of();
    }

    private static ObjectList<UnresolvedCell> expandStructure(MultiblockMachineDefinition definition,
                                                              MultiBlockPattern pattern,
                                                              AutoBuildStructureOptions options,
                                                              IntList repetitions,
                                                              ResolutionContext context,
                                                              ConstraintIds constraintIds,
                                                              ObjectList<AutoBuildProblem> problems) {
        ObjectList<UnresolvedCell> cells = new ObjectArrayList<>();
        PatternLayout layout;
        try {
            layout = pattern.expandLayout(options.repetitions().toIntArray(), options.choices());
        } catch (IllegalArgumentException exception) {
            problems.add(problem(AutoBuildProblem.Type.INVALID_OPTIONS, context.worldPos(),
                    "gtpm.multiblock.autobuild.invalid_layout", options.structureName(), exception.getMessage()));
            return cells;
        }
        int minimumDepth = layout.cells().stream().mapToInt(cell -> cell.depth()).min().orElse(0);
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
                if (level == null || worldPos == null || !level.isLoaded(worldPos)) {
                    problems.add(problem(AutoBuildProblem.Type.UNLOADED, worldPos,
                            "gtpm.multiblock.autobuild.unloaded", position(worldPos)));
                    return ObjectList.of();
                }
                observed = level.getBlockState(worldPos);
            }
            ObjectList<ResolvedCandidate> candidates = resolveCandidates(definition, predicate,
                    context.frontFacing(), context.upwardsFacing(), options.flipMode(), constraintIds, problems,
                    worldPos);
            Direction requiredDirection = resolveDirection(predicate, context.frontFacing(), context.upwardsFacing(),
                    options.flipMode());
            cells.add(new UnresolvedCell(options.structureName(), cellKey, relativePos, worldPos, observed,
                    cell.depth() - minimumDepth, predicate, candidates, requiredDirection));
        }
        return cells;
    }

    private static ObjectList<ResolvedCandidate> resolveCandidates(MultiblockMachineDefinition definition,
                                                                   PatternPredicate predicate,
                                                                   Direction frontFacing,
                                                                   Direction upwardsFacing,
                                                                   boolean flipped,
                                                                   ConstraintIds constraintIds,
                                                                   ObjectList<AutoBuildProblem> problems,
                                                                   @Nullable BlockPos pos) {
        if (predicate.isAny()) {
            return ObjectList.of(ResolvedCandidate.wildcard());
        }
        Object2ObjectLinkedOpenHashMap<BlockState, CandidateAccumulator> candidates = new Object2ObjectLinkedOpenHashMap<>();
        CandidateOrder order = new CandidateOrder();
        for (PredicateRule simple : predicate.limited) {
            addSimpleCandidates(candidates, simple, order, constraintIds);
        }
        for (PredicateRule simple : predicate.common) {
            addSimpleCandidates(candidates, simple, order, constraintIds);
        }
        for (StructurePredicate structurePredicate : predicate.structurePatternPredicates) {
            ObjectList<StructureChoice> choices = structureChoices(structurePredicate, definition, constraintIds);
            for (StructureChoice choice : choices) {
                ObjectList<MultiblockBlockInfo> stableCandidates = choice.tierGroups().isEmpty() ?
                        new ObjectImmutableList<>(choice.candidates()) : choice.candidates().stream()
                                .sorted(Comparator
                                        .comparing((MultiblockBlockInfo info) -> BuiltInRegistries.BLOCK
                                                .getKey(info.getBlockState().getBlock()).toString())
                                        .thenComparing(info -> info.getBlockState().toString())
                                        .thenComparing(info -> info.getItemStackForm().toString()))
                                .collect(ObjectImmutableList.toList());
                for (MultiblockBlockInfo blockInfo : stableCandidates) {
                    addCandidate(candidates, blockInfo, choice.limits(), choice.hatch(), choice.tierGroups(),
                            order.next());
                }
            }
        }
        if (predicate.hasAir() && candidates.keySet().stream().noneMatch(BlockState::isAir)) {
            addCandidate(candidates, MultiblockBlockInfo.EMPTY, ObjectList.of(), false, ObjectList.of(), order.next());
        }

        Direction direction = resolveDirection(predicate, frontFacing, upwardsFacing, flipped);
        ObjectArrayList<ResolvedCandidate> result = new ObjectArrayList<>(candidates.size());
        for (CandidateAccumulator candidate : candidates.values()) {
            BlockState state = candidate.blockInfo().blockState();
            if (direction != null && !state.isAir()) {
                state = MultiBlockPattern.applyDirectionalState(state, direction);
                if (!MultiBlockPattern.matchesDirectionalState(state, direction)) continue;
            }
            PlannedBlockInfo blockInfo = candidate.blockInfo().withState(state);
            AutoBuildItemKey materialKey = materialKey(blockInfo.itemStack());
            String category = AutoBuildBlockMap.BLOCK_CATEGORY_MAP.get(state.getBlock());
            ObjectList<String> tierGroups = mergeTierGroups(candidate.tierGroups(),
                    category == null ? ObjectList.of() : ObjectList.of(category));
            result.add(new ResolvedCandidate(state, blockInfo, new ObjectImmutableList<>(candidate.limits()),
                    candidate.hatch(),
                    tierGroups, candidate.order(), materialKey, false));
        }
        if (result.isEmpty()) {
            problems.add(problem(AutoBuildProblem.Type.UNSUPPORTED, pos,
                    "gtpm.multiblock.autobuild.no_candidate", position(pos)));
        }
        return new ObjectImmutableList<>(result);
    }

    private static void addSimpleCandidates(Object2ObjectMap<BlockState, CandidateAccumulator> result,
                                            PredicateRule simple,
                                            CandidateOrder order, ConstraintIds constraintIds) {
        ConstraintLimit limit = ConstraintLimit.fromSimple(simple, constraintIds.id(simple));
        if (simple == PredicateRule.air) {
            addCandidate(result, MultiblockBlockInfo.EMPTY, ObjectList.of(limit), false, ObjectList.of(), order.next());
            return;
        }
        if (simple == PredicateRule.any) return;
        MultiblockBlockInfo representative = simple.blockInfo.get();
        if (simple.getPreviewStates() != null) {
            for (BlockState state : simple.candidateStates()) {
                if (!simple.observeCandidate(state).matched()) continue;
                MultiblockBlockInfo info = representative != null && representative.getBlockState().equals(state) ?
                        representative : new MultiblockBlockInfo(state);
                addCandidate(result, info, ObjectList.of(limit), isHatch(state.getBlock()), ObjectList.of(),
                        order.next());
            }
            return;
        }
        if (simple.candidates == null) {
            if (representative != null && (simple.getCandidateObserver() == null ||
                    simple.observeCandidate(representative.getBlockState()).matched())) {
                addCandidate(result, representative, ObjectList.of(limit),
                        isHatch(representative.getBlockState().getBlock()),
                        ObjectList.of(), order.next());
            }
            return;
        }
        Block[] blocks = simple.candidates.get();
        if (blocks == null) return;
        for (Block block : blocks) {
            if (block == null) continue;
            MultiblockBlockInfo info = representative != null && representative.getBlockState().is(block) ?
                    representative : MultiblockBlockInfo.fromBlock(block);
            if (simple.getCandidateObserver() != null && !simple.observeCandidate(info.getBlockState()).matched()) {
                continue;
            }
            addCandidate(result, info, ObjectList.of(limit), isHatch(block), ObjectList.of(), order.next());
        }
    }

    private static ObjectList<StructureChoice> structureChoices(StructurePredicate predicate,
                                                                MultiblockMachineDefinition definition,
                                                                ConstraintIds constraintIds) {
        if (predicate instanceof RestrictedPredicate restricted) {
            ConstraintLimit limit = ConstraintLimit.fromStructure(new StructurePreviewConstraint(restricted,
                    restricted.minCount(), restricted.maxCount(), restricted.minCountByLayer(),
                    restricted.maxCountByLayer(), restricted.previewCount()), constraintIds.id(restricted));
            return structureChoices(restricted.predicate(), definition, constraintIds).stream()
                    .map(choice -> choice.withLimit(limit))
                    .collect(ObjectImmutableList.toList());
        }
        if (predicate instanceof ConcatenatedPredicate concatenated) {
            return concatenated.predicates().stream()
                    .flatMap(child -> structureChoices(child, definition, constraintIds).stream())
                    .collect(ObjectImmutableList.toList());
        }
        String abilityGroup = abilityGroup(predicate);
        boolean hatch = predicate instanceof AbilityPredicate || predicate instanceof TieredAbilityPredicate;
        ObjectArrayList<StructureChoice> result = new ObjectArrayList<>();
        for (StructurePreviewChoice choice : predicate.previewChoices(definition)) {
            ObjectList<ConstraintLimit> limits = choice.constraints().stream()
                    .map(constraint -> ConstraintLimit.fromStructure(constraint, constraintIds.id(constraint.key())))
                    .collect(ObjectImmutableList.toList());
            ObjectList<MultiblockBlockInfo> candidates = choice.candidates().stream()
                    .filter(info -> choice.observeCandidate(info.getBlockState()).matched())
                    .collect(ObjectImmutableList.toList());
            result.add(new StructureChoice(candidates, limits, hatch,
                    abilityGroup == null ? ObjectList.of() : ObjectList.of(abilityGroup)));
        }
        return result;
    }

    private static @Nullable String abilityGroup(StructurePredicate predicate) {
        String ability = predicate instanceof AbilityPredicate abilities && abilities.abilities().size() == 1 ?
                abilities.abilities().getFirst().getName() :
                predicate instanceof TieredAbilityPredicate tiered ? tiered.ability().getName() : null;
        if (ability == null) return null;
        String path = ability.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9/._-]", "_");
        return "ability/" + path;
    }

    private static void addCandidate(Object2ObjectMap<BlockState, CandidateAccumulator> result,
                                     MultiblockBlockInfo blockInfo,
                                     ObjectList<ConstraintLimit> limits, boolean hatch,
                                     ObjectList<String> tierGroups, int order) {
        PlannedBlockInfo planned = PlannedBlockInfo.from(blockInfo);
        CandidateAccumulator existing = result.get(planned.blockState());
        if (existing == null) {
            result.put(planned.blockState(), new CandidateAccumulator(planned, new ObjectArrayList<>(limits), hatch,
                    new ObjectImmutableList<>(tierGroups), order));
            return;
        }
        for (ConstraintLimit limit : limits) {
            if (existing.limits().stream().noneMatch(current -> current.key() == limit.key())) {
                existing.limits().add(limit);
            }
        }
        ObjectList<String> mergedGroups = mergeTierGroups(existing.tierGroups(), tierGroups);
        if (hatch && !existing.hatch() || !mergedGroups.equals(existing.tierGroups())) {
            result.put(planned.blockState(), new CandidateAccumulator(existing.blockInfo(), existing.limits(),
                    existing.hatch() || hatch, mergedGroups,
                    existing.order()));
        }
    }

    private static ObjectList<String> mergeTierGroups(ObjectList<String> first,
                                                      ObjectList<String> second) {
        ObjectLinkedOpenHashSet<String> result = new ObjectLinkedOpenHashSet<>(first);
        result.addAll(second);
        return new ObjectImmutableList<>(result);
    }

    private static @Nullable Direction resolveDirection(PatternPredicate predicate, Direction frontFacing,
                                                        Direction upwardsFacing, boolean flipped) {
        if (predicate.relativeDirection != null) {
            return predicate.relativeDirection.getRelative(frontFacing, upwardsFacing, flipped);
        }
        return predicate.fixedDirection;
    }

    private static @Nullable AutoBuildItemKey materialKey(ItemStack stack) {
        return stack.isEmpty() || stack.is(Items.AIR) ? null : AutoBuildItemKey.of(stack);
    }

    private static boolean isHatch(Block block) {
        return PartAbility.values().stream().anyMatch(ability -> ability.isApplicable(block));
    }

    private static void validateTierChoices(MultiblockMachineDefinition definition, AutoBuildStructureOptions options,
                                            ObjectList<UnresolvedCell> cells,
                                            ObjectList<AutoBuildProblem> problems, @Nullable BlockPos pos) {
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
                problems.add(problem(AutoBuildProblem.Type.INVALID_TIER_SELECTION, pos,
                        "gtpm.multiblock.autobuild.tier_candidate_invalid", entry.getValue().toString(),
                        entry.getKey().toString(), options.structureName()));
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
                problems.add(problem(AutoBuildProblem.Type.INVALID_TIER_SELECTION, cell.worldPos(),
                        "gtpm.multiblock.autobuild.tier_domain_empty", cell.cellKey().toString()));
            } else {
                cells.set(index, cell.withCandidates(filtered));
            }
        }
    }

    private static ResolvedStructurePlan resolveStructure(MultiblockMachineDefinition definition,
                                                          PendingStructure pending,
                                                          AutoBuildSharedOptions sharedOptions,
                                                          ResolutionContext context,
                                                          MaterialAllocation materials) {
        ObjectArrayList<AutoBuildProblem> problems = new ObjectArrayList<>(pending.problems());
        if (!problems.isEmpty()) {
            return new ResolvedStructurePlan(pending.options().structureName(), pending.options().mode(),
                    pending.repetitions(), pending.options().flipMode(), ObjectList.of(), Object2LongMaps.emptyMap(),
                    problems);
        }
        boolean buildMode = pending.options().mode() == AutoBuildMode.BUILD;
        ConstraintCandidateSolver solver = new ConstraintCandidateSolver(
                sharedOptions.noHatchMode() && buildMode, materials, buildMode, context.canonical());
        ObjectList<UnresolvedCell> observedCells = materials.applyVirtualWorld(pending.cells());
        ObjectList<SelectedCell> selected = solver.solve(observedCells, problems);
        if (!problems.isEmpty()) {
            return new ResolvedStructurePlan(pending.options().structureName(), pending.options().mode(),
                    pending.repetitions(), pending.options().flipMode(), ObjectList.of(), Object2LongMaps.emptyMap(),
                    problems);
        }

        ObjectArrayList<PlannedCell> planned = new ObjectArrayList<>(selected.size());
        Object2LongMap<AutoBuildItemKey> materialCounts = new Object2LongLinkedOpenHashMap<>();
        for (SelectedCell selectedCell : selected) {
            UnresolvedCell cell = selectedCell.cell();
            ResolvedCandidate candidate = selectedCell.candidate();
            BlockState observed = cell.observedState();
            CellAction action = resolveAction(definition, pending.options().mode(), cell, candidate,
                    observed, context);
            BlockState targetState = candidate.state();
            if (pending.options().mode() == AutoBuildMode.BUILD && observed != null) {
                if (action == CellAction.KEEP) {
                    targetState = observed;
                } else if (action == CellAction.UPDATE_DIRECTION && cell.requiredDirection() != null) {
                    targetState = MultiBlockPattern.applyDirectionalState(observed, cell.requiredDirection());
                }
            }
            ObjectList<AutoBuildItemKey> candidateKeys = cell.candidates().stream().map(ResolvedCandidate::materialKey)
                    .filter(Objects::nonNull).distinct().collect(ObjectImmutableList.toList());
            ObjectList<BlockState> stateCandidates = cell.candidates().stream()
                    .filter(candidateState -> !candidateState.any())
                    .map(ResolvedCandidate::state).distinct().collect(ObjectImmutableList.toList());
            AutoBuildItemKey materialKey = candidate.materialKey();
            if (action == CellAction.PLACE && materialKey != null) {
                materialCounts.mergeLong(materialKey, 1L, Math::addExact);
            }
            planned.add(new PlannedCell(cell.structureName(), cell.cellKey(), cell.relativePos(), cell.worldPos(),
                    targetState, observed, candidate.blockInfo().withState(targetState), action, cell.predicate(),
                    stateCandidates,
                    cell.requiredDirection(), candidateKeys, materialKey, candidate.tierGroups(),
                    candidate.limits().stream().map(ConstraintLimit::id).collect(ObjectImmutableList.toList())));
        }
        PlannedCell missingCell = buildMode ? materials.commitStructure(planned) : null;
        if (missingCell != null) {
            problems.addAll(materials.structureProblems());
            problems.add(problem(AutoBuildProblem.Type.MISSING_MATERIAL, missingCell.worldPos(),
                    "gtpm.multiblock.autobuild.missing_material", position(missingCell.worldPos())));
        }
        ResolvedStructurePlan result = new ResolvedStructurePlan(pending.options().structureName(),
                pending.options().mode(),
                pending.repetitions(), pending.options().flipMode(), planned, materialCounts, problems);
        materials.applyPlan(result);
        return result;
    }

    private static CellAction resolveAction(MultiblockMachineDefinition definition, AutoBuildMode mode,
                                            UnresolvedCell cell, ResolvedCandidate candidate,
                                            @Nullable BlockState observed, ResolutionContext context) {
        if (candidate.any()) return CellAction.IGNORE_ANY;
        boolean controllerCell = cell.relativePos().equals(BlockPos.ZERO) &&
                candidate.state().is(definition.getBlock());
        if (controllerCell) return CellAction.CONTROLLER;
        if (mode == AutoBuildMode.DEMOLISH) {
            if (context.canonical()) {
                return candidate.state().isAir() ? CellAction.KEEP :
                        CellAction.DEMOLISH_CANDIDATE;
            }
            return observed != null && matchesCandidateState(cell, observed) ?
                    CellAction.DEMOLISH_CANDIDATE : CellAction.KEEP;
        }
        if (candidate.state().isAir()) {
            return observed == null || observed.isAir() ? CellAction.KEEP :
                    CellAction.CLEAR_FOR_AIR;
        }
        if (observed != null) {
            if (matchesCandidateDomain(cell, candidate, observed)) return CellAction.KEEP;
            if (canUpdateDirection(cell, candidate)) {
                return CellAction.UPDATE_DIRECTION;
            }
        }
        return CellAction.PLACE;
    }

    private static boolean matchesCandidateState(UnresolvedCell cell, BlockState observed) {
        for (ResolvedCandidate candidate : cell.candidates()) {
            if (candidate.any() || candidate.state().isAir()) continue;
            if (observed.equals(candidate.state())) {
                return true;
            }
        }
        return false;
    }

    private static boolean matchesCandidateDomain(UnresolvedCell cell, ResolvedCandidate candidate,
                                                  BlockState observed) {
        return observed.equals(candidate.state()) &&
                (cell.requiredDirection() == null ||
                        MultiBlockPattern.matchesDirectionalState(observed, cell.requiredDirection()));
    }

    private static boolean canUpdateDirection(UnresolvedCell cell, ResolvedCandidate candidate) {
        BlockState observed = cell.observedState();
        Direction requiredDirection = cell.requiredDirection();
        if (observed == null || requiredDirection == null || !observed.is(candidate.state().getBlock())) {
            return false;
        }
        BlockState corrected = MultiBlockPattern.applyDirectionalState(observed, requiredDirection);
        return !observed.equals(corrected) && corrected.equals(candidate.state());
    }

    private static Object2ObjectLinkedOpenHashMap<BlockPos, MergedPlanCell> mergeCells(ObjectList<ResolvedStructurePlan> demolitions,
                                                                                       ObjectList<ResolvedStructurePlan> builds) {
        Object2ObjectLinkedOpenHashMap<BlockPos, ObjectArrayList<PlannedCell>> grouped = new Object2ObjectLinkedOpenHashMap<>();
        for (ResolvedStructurePlan plan : concat(demolitions, builds)) {
            for (PlannedCell cell : plan.cells()) {
                grouped.computeIfAbsent(cell.relativePos(), ignored -> new ObjectArrayList<>()).add(cell);
            }
        }
        Object2ObjectLinkedOpenHashMap<BlockPos, MergedPlanCell> result = new Object2ObjectLinkedOpenHashMap<>();
        grouped.forEach((pos, contributors) -> {
            ObjectArrayList<PlannedCell> ordered = new ObjectArrayList<>(contributors.size());
            contributors.stream().filter(cell -> cell.action() == CellAction.IGNORE_ANY).forEach(ordered::add);
            contributors.stream().filter(cell -> cell.action() != CellAction.IGNORE_ANY).forEach(ordered::add);
            MergeState state = compatible(ordered) ? MergeState.COMPATIBLE :
                    MergeState.CONFLICT;
            result.put(pos, new MergedPlanCell(pos, ordered.getFirst().worldPos(), ordered, state));
        });
        return result;
    }

    private static ObjectList<ResolvedStructurePlan> concat(ObjectList<ResolvedStructurePlan> first,
                                                            ObjectList<ResolvedStructurePlan> second) {
        ObjectArrayList<ResolvedStructurePlan> result = new ObjectArrayList<>(first.size() + second.size());
        result.addAll(first);
        result.addAll(second);
        return result;
    }

    private static boolean compatible(ObjectList<PlannedCell> contributors) {
        PlannedCell first = null;
        for (PlannedCell other : contributors) {
            if (other.action() == CellAction.IGNORE_ANY) continue;
            if (first == null) {
                first = other;
                continue;
            }
            boolean firstDemolishes = first.action() == CellAction.DEMOLISH_CANDIDATE;
            boolean otherDemolishes = other.action() == CellAction.DEMOLISH_CANDIDATE;
            if (firstDemolishes != otherDemolishes || !first.blockState().equals(other.blockState())) {
                return false;
            }
        }
        return true;
    }

    private static long fingerprint(
                                    ObjectList<ResolvedStructurePlan> demolitions,
                                    ObjectList<ResolvedStructurePlan> builds,
                                    Object2ObjectMap<BlockPos, MergedPlanCell> merged,
                                    LongList sourceFingerprints,
                                    Object2ObjectMap<AutoBuildItemKey, ResolvedAutoBuildSnapshot.MaterialAllocation> allocations) {
        long value = FINGERPRINT_OFFSET;
        for (ResolvedStructurePlan structure : concat(demolitions, builds)) {
            value = hash(value, structure.structureName().hashCode());
            value = hash(value, structure.mode().ordinal());
            value = hash(value, structure.repetitions().hashCode());
            value = hash(value, structure.flipped() ? 1 : 0);
            for (PlannedCell cell : structure.cells()) {
                value = hash(value, cell.relativePos().asLong());
                value = hash(value, cell.blockState().hashCode());
                value = hash(value, cell.observedState() == null ? 0 : cell.observedState().hashCode());
                value = hash(value, cell.action().ordinal());
                value = hash(value, cell.stateCandidates().hashCode());
                value = hash(value, cell.requiredDirection() == null ? -1 : cell.requiredDirection().ordinal());
                value = hash(value, cell.materialKey() == null ? 0 : cell.materialKey().hashCode());
                value = hash(value, cell.tierGroups().hashCode());
            }
        }
        for (MergedPlanCell cell : merged.values()) {
            value = hash(value, cell.mergeState().ordinal());
        }
        for (long sourceFingerprint : sourceFingerprints) {
            value = hash(value, sourceFingerprint);
        }
        for (var entry : allocations.object2ObjectEntrySet()) {
            value = hash(value, entry.getKey().hashCode());
            ResolvedAutoBuildSnapshot.MaterialAllocation allocation = entry.getValue();
            value = hash(value, allocation.required());
            for (long allocated : allocation.allocatedBySource()) {
                value = hash(value, allocated);
            }
            value = hash(value, allocation.missing());
            value = hash(value, allocation.unlimited() ? 1 : 0);
        }
        return value;
    }

    private static long hash(long current, long value) {
        return (current ^ value) * FINGERPRINT_PRIME;
    }

    private static AutoBuildProblem problem(AutoBuildProblem.Type type, @Nullable BlockPos pos,
                                            String translationKey, Object... args) {
        return new AutoBuildProblem(type, pos, Component.translatable(translationKey, args));
    }

    private static String position(@Nullable BlockPos pos) {
        return pos == null ? "?" : pos.toShortString();
    }

    private record ResolutionContext(@Nullable MultiblockControllerMachine controller,
                                     @Nullable ServerPlayer player, @Nullable Level level, BlockPos origin,
                                     Direction frontFacing, Direction upwardsFacing, boolean canonical,
                                     boolean unlimitedMaterials) {

        private @Nullable BlockPos worldPos() {
            return canonical ? null : origin;
        }
    }

    private record PendingStructure(AutoBuildStructureOptions options, IntList repetitions,
                                    ObjectList<UnresolvedCell> cells, ObjectList<AutoBuildProblem> problems) {}

    private record UnresolvedCell(String structureName, PatternCellKey cellKey, BlockPos relativePos,
                                  @Nullable BlockPos worldPos, @Nullable BlockState observedState, int layer,
                                  PatternPredicate predicate,
                                  ObjectList<ResolvedCandidate> candidates, @Nullable Direction requiredDirection) {

        private UnresolvedCell withCandidates(ObjectList<ResolvedCandidate> replacement) {
            return new UnresolvedCell(structureName, cellKey, relativePos, worldPos, observedState, layer, predicate,
                    new ObjectImmutableList<>(replacement), requiredDirection);
        }

        private UnresolvedCell withObservedState(@Nullable BlockState observed) {
            return new UnresolvedCell(structureName, cellKey, relativePos, worldPos, observed, layer, predicate,
                    candidates, requiredDirection);
        }
    }

    private record ResolvedCandidate(BlockState state, PlannedBlockInfo blockInfo, ObjectList<ConstraintLimit> limits,
                                     boolean hatch, ObjectList<String> tierGroups, int order,
                                     @Nullable AutoBuildItemKey materialKey, boolean any) {

        private static ResolvedCandidate wildcard() {
            return new ResolvedCandidate(Blocks.AIR.defaultBlockState(),
                    PlannedBlockInfo.from(Blocks.AIR.defaultBlockState()),
                    ObjectList.of(), false, ObjectList.of(), 0, null, true);
        }
    }

    private record CandidateAccumulator(PlannedBlockInfo blockInfo, ObjectList<ConstraintLimit> limits, boolean hatch,
                                        ObjectList<String> tierGroups, int order) {}

    private record StructureChoice(ObjectList<MultiblockBlockInfo> candidates, ObjectList<ConstraintLimit> limits,
                                   boolean hatch, ObjectList<String> tierGroups) {

        private StructureChoice withLimit(ConstraintLimit limit) {
            ObjectArrayList<ConstraintLimit> result = new ObjectArrayList<>(limits.size() + 1);
            result.addAll(limits);
            result.add(limit);
            return new StructureChoice(candidates, result, hatch, tierGroups);
        }
    }

    private record ConstraintLimit(Object key, String id, int min, int max, int layerMin, int layerMax,
                                   int previewCount) {

        private static ConstraintLimit fromSimple(PredicateRule predicate, String id) {
            return new ConstraintLimit(predicate, id, predicate.minCount, predicate.maxCount,
                    predicate.minLayerCount, predicate.maxLayerCount, predicate.previewCount);
        }

        private static ConstraintLimit fromStructure(StructurePreviewConstraint constraint, String id) {
            return new ConstraintLimit(constraint.key(), id, constraint.minCount().orElse(-1),
                    constraint.maxCount().orElse(-1), constraint.minCountByLayer().orElse(-1),
                    constraint.maxCountByLayer().orElse(-1), constraint.previewCount().orElse(-1));
        }
    }

    private static final class ConstraintIds {

        private final Reference2ObjectOpenHashMap<Object, String> ids = new Reference2ObjectOpenHashMap<>();

        private String id(Object key) {
            return ids.computeIfAbsent(key, ignored -> "constraint_" + ids.size());
        }
    }

    private static final class CandidateOrder {

        private int next;

        private int next() {
            return next++;
        }
    }

    private record SelectedCell(UnresolvedCell cell, ResolvedCandidate candidate) {}

    /**
     * Deterministic constraint selector that satisfies declared minima before applying user preferences.
     */
    private static final class ConstraintCandidateSolver {

        private final boolean minimizeHatches;
        private final MaterialAllocation materials;
        private final boolean consumeMaterials;
        private final boolean canonical;
        private final Reference2IntMap<Object> globalCounts = new Reference2IntOpenHashMap<>();
        private final Int2ObjectMap<Reference2IntMap<Object>> layerCounts = new Int2ObjectOpenHashMap<>();
        private final Reference2ObjectOpenHashMap<Object, ConstraintLimit> limits = new Reference2ObjectOpenHashMap<>();
        private final Int2ObjectMap<Reference2ObjectOpenHashMap<Object, ConstraintLimit>> layerLimits = new Int2ObjectOpenHashMap<>();
        private final Object2ObjectMap<SearchState, SelectionScore> visitedStates = new Object2ObjectOpenHashMap<>();
        private ObjectList<ConstraintLimit> orderedLimits = ObjectList.of();
        private ObjectList<LayerLimit> orderedLayerLimits = ObjectList.of();
        private ObjectList<AutoBuildItemKey> budgetKeys = ObjectList.of();
        private @Nullable SelectionScore bestScore;
        private @Nullable int[][] bestGroupCounts;

        private ConstraintCandidateSolver(boolean minimizeHatches, MaterialAllocation materials,
                                          boolean consumeMaterials, boolean canonical) {
            this.minimizeHatches = minimizeHatches;
            this.materials = materials;
            this.consumeMaterials = consumeMaterials;
            this.canonical = canonical;
        }

        private ObjectList<SelectedCell> solve(ObjectList<UnresolvedCell> cells,
                                               ObjectList<AutoBuildProblem> problems) {
            registerLimits(cells);
            ObjectList<VariableGroup> groups = groupCells(cells);
            int[][] groupCounts = new int[groups.size()][];
            for (int index = 0; index < groups.size(); index++) {
                groupCounts[index] = new int[groups.get(index).representative().candidates().size()];
            }
            SearchBudget budget = new SearchBudget(materials.copyRemaining(), materials.unlimited);
            search(groups, 0, budget, SelectionScore.ZERO, groupCounts);
            int[][] selectedCounts = bestGroupCounts;
            if (selectedCounts == null) {
                problems.add(problem(AutoBuildProblem.Type.INVALID_OPTIONS, null,
                        "gtpm.multiblock.autobuild.constraint_unsatisfied"));
                return ObjectList.of();
            }
            int[] cellAssignments = projectAssignments(cells, groups, selectedCounts);
            ObjectArrayList<SelectedCell> result = new ObjectArrayList<>(cells.size());
            for (int index = 0; index < cells.size(); index++) {
                UnresolvedCell cell = cells.get(index);
                ResolvedCandidate selected = cell.candidates().get(cellAssignments[index]);
                result.add(new SelectedCell(cell, selected));
            }
            return new ObjectImmutableList<>(result);
        }

        private int[] projectAssignments(ObjectList<UnresolvedCell> cells, ObjectList<VariableGroup> groups,
                                         int[][] selectedCounts) {
            int[] groupByCell = new int[cells.size()];
            Arrays.fill(groupByCell, -1);
            int[][] remainingCounts = copyCounts(selectedCounts);
            for (int groupIndex = 0; groupIndex < groups.size(); groupIndex++) {
                VariableGroup group = groups.get(groupIndex);
                for (int cellIndex : group.cellIndices()) {
                    if (groupByCell[cellIndex] >= 0) {
                        throw new IllegalStateException("Constraint solver assigned one cell to multiple groups");
                    }
                    groupByCell[cellIndex] = groupIndex;
                }
            }

            SearchBudget budget = new SearchBudget(materials.copyRemaining(), materials.unlimited);
            int[] assignments = new int[cells.size()];
            for (int cellIndex = 0; cellIndex < cells.size(); cellIndex++) {
                int groupIndex = groupByCell[cellIndex];
                if (groupIndex < 0) {
                    throw new IllegalStateException("Constraint solver did not assign a group for cell " + cellIndex);
                }
                UnresolvedCell cell = cells.get(cellIndex);
                int candidateIndex = -1;
                for (int candidate : orderedCandidates(cell, budget)) {
                    if (remainingCounts[groupIndex][candidate] > 0) {
                        candidateIndex = candidate;
                        break;
                    }
                }
                if (candidateIndex < 0) {
                    throw new IllegalStateException("Constraint solution could not be projected onto cell " +
                            cell.cellKey());
                }
                AppliedCandidate projected = applyCandidate(cell, cell.candidates().get(candidateIndex), 1, budget);
                if (projected == null) {
                    throw new IllegalStateException("Constraint solution became invalid while projecting cell " +
                            cell.cellKey());
                }
                remainingCounts[groupIndex][candidateIndex]--;
                assignments[cellIndex] = candidateIndex;
            }

            for (int[] groupCounts : remainingCounts) {
                for (int count : groupCounts) {
                    if (count != 0) {
                        throw new IllegalStateException("Constraint solution projection left unassigned candidates");
                    }
                }
            }
            return assignments;
        }

        private void registerLimits(ObjectList<UnresolvedCell> cells) {
            ObjectLinkedOpenHashSet<AutoBuildItemKey> encounteredKeys = new ObjectLinkedOpenHashSet<>();
            for (UnresolvedCell cell : cells) {
                for (ResolvedCandidate candidate : cell.candidates()) {
                    if (candidate.materialKey() != null) encounteredKeys.add(candidate.materialKey());
                    for (ConstraintLimit limit : candidate.limits()) {
                        limits.putIfAbsent(limit.key(), limit);
                        if (limit.layerMin() >= 0 || limit.layerMax() >= 0) {
                            layerLimits.computeIfAbsent(cell.layer(), ignored -> new Reference2ObjectOpenHashMap<>())
                                    .putIfAbsent(limit.key(), limit);
                        }
                    }
                }
            }
            orderedLimits = limits.values().stream().sorted(Comparator.comparing(ConstraintLimit::id))
                    .collect(ObjectImmutableList.toList());
            orderedLayerLimits = layerLimits.int2ObjectEntrySet().stream()
                    .flatMap(entry -> entry.getValue().values().stream()
                            .map(limit -> new LayerLimit(entry.getIntKey(), limit)))
                    .sorted(Comparator.comparingInt(LayerLimit::layer)
                            .thenComparing(layerLimit -> layerLimit.limit().id()))
                    .collect(ObjectImmutableList.toList());
            budgetKeys = new ObjectImmutableList<>(encounteredKeys);
        }

        private ObjectList<VariableGroup> groupCells(ObjectList<UnresolvedCell> cells) {
            Object2ObjectLinkedOpenHashMap<GroupKey, IntArrayList> grouped = new Object2ObjectLinkedOpenHashMap<>();
            for (int index = 0; index < cells.size(); index++) {
                UnresolvedCell cell = cells.get(index);
                ObjectList<CandidateKey> candidates = cell.candidates().stream()
                        .map(candidate -> new CandidateKey(candidate.state(), candidate.materialKey(),
                                candidate.blockInfo().hasBlockEntity(), candidate.hatch(), candidate.any(),
                                candidate.order(), candidate.tierGroups(), candidate.limits().stream()
                                        .map(ConstraintLimit::id).sorted().collect(ObjectImmutableList.toList())))
                        .collect(ObjectImmutableList.toList());
                GroupKey key = new GroupKey(cell.layer(), cell.observedState(), cell.requiredDirection(),
                        cell.relativePos().equals(BlockPos.ZERO), candidates);
                grouped.computeIfAbsent(key, ignored -> new IntArrayList()).add(index);
            }
            ObjectArrayList<VariableGroup> result = new ObjectArrayList<>(grouped.size());
            grouped.values().forEach(indices -> result.add(new VariableGroup(new IntImmutableList(indices),
                    cells.get(indices.getInt(0)))));
            result.sort(Comparator.comparingInt((VariableGroup group) -> group.hasConstraints() ? 0 : 1)
                    .thenComparingInt(group -> group.representative().candidates().size())
                    .thenComparing(Comparator.comparingInt(VariableGroup::constraintCount).reversed())
                    .thenComparingInt(group -> group.cellIndices().getInt(0)));
            return new ObjectImmutableList<>(result);
        }

        private boolean search(ObjectList<VariableGroup> groups, int groupIndex, SearchBudget budget,
                               SelectionScore score, int[][] groupCounts) {
            if (!canStillSatisfy(groups, groupIndex) || !canBeatBest(groups, groupIndex, budget, score)) {
                return false;
            }
            if (groupIndex == groups.size()) {
                if (!minimumsSatisfied()) return false;
                if (bestScore == null || score.compareTo(bestScore) < 0) {
                    bestScore = score;
                    bestGroupCounts = copyCounts(groupCounts);
                }
                return true;
            }
            SearchState stateKey = stateKey(groupIndex, budget);
            SelectionScore previous = visitedStates.get(stateKey);
            if (previous != null && previous.compareTo(score) <= 0) return false;
            visitedStates.put(stateKey, score);

            VariableGroup group = groups.get(groupIndex);
            if (!group.hasConstraints()) {
                return searchGreedyGroup(groups, groupIndex, budget, score, groupCounts);
            }
            IntList candidates = orderedCandidates(group.representative(), budget);
            return enumerateGroup(groups, groupIndex, budget, score, groupCounts, candidates, 0,
                    group.cellIndices().size());
        }

        private boolean searchGreedyGroup(ObjectList<VariableGroup> groups, int groupIndex, SearchBudget budget,
                                          SelectionScore score, int[][] groupCounts) {
            VariableGroup group = groups.get(groupIndex);
            UnresolvedCell cell = group.representative();
            ObjectArrayList<AppliedCandidate> applied = new ObjectArrayList<>(group.cellIndices().size());
            SelectionScore nextScore = score;
            for (int ignored = 0; ignored < group.cellIndices().size(); ignored++) {
                IntList ordered = orderedCandidates(cell, budget);
                if (ordered.isEmpty()) {
                    undoApplied(cell, applied, budget, groupCounts[groupIndex]);
                    return false;
                }
                int candidateIndex = ordered.getFirst();
                AppliedCandidate choice = applyCandidate(cell, cell.candidates().get(candidateIndex), 1, budget);
                if (choice == null) {
                    undoApplied(cell, applied, budget, groupCounts[groupIndex]);
                    return false;
                }
                groupCounts[groupIndex][candidateIndex]++;
                applied.add(choice);
                nextScore = nextScore.add(choice.score());
            }
            boolean found = search(groups, groupIndex + 1, budget, nextScore, groupCounts);
            undoApplied(cell, applied, budget, groupCounts[groupIndex]);
            return found;
        }

        private boolean enumerateGroup(ObjectList<VariableGroup> groups, int groupIndex, SearchBudget budget,
                                       SelectionScore score, int[][] groupCounts, IntList candidateOrder,
                                       int candidateCursor, int remainingCells) {
            if (candidateCursor == candidateOrder.size()) {
                return remainingCells == 0 && search(groups, groupIndex + 1, budget, score, groupCounts);
            }
            UnresolvedCell cell = groups.get(groupIndex).representative();
            int candidateIndex = candidateOrder.getInt(candidateCursor);
            ResolvedCandidate candidate = cell.candidates().get(candidateIndex);
            boolean found = false;
            for (int count = remainingCells; count >= 0; count--) {
                AppliedCandidate applied = applyCandidate(cell, candidate, count, budget);
                if (applied == null) continue;
                groupCounts[groupIndex][candidateIndex] = count;
                if (enumerateGroup(groups, groupIndex, budget, score.add(applied.score()), groupCounts,
                        candidateOrder, candidateCursor + 1, remainingCells - count)) {
                    found = true;
                }
                groupCounts[groupIndex][candidateIndex] = 0;
                undoCandidate(cell, applied, budget);
            }
            return found;
        }

        private IntList orderedCandidates(UnresolvedCell cell, SearchBudget budget) {
            IntList candidates = new IntArrayList();
            for (int index = 0; index < cell.candidates().size(); index++) {
                if (withinMaximums(cell.layer(), cell.candidates().get(index))) {
                    candidates.add(index);
                }
            }
            candidates.sort((IntComparator) (left, right) -> {
                var first = cell.candidates().get(left);
                var second = cell.candidates().get(right);
                int order = Integer.compare(requiredPriority(cell, first), requiredPriority(cell, second));
                if (order != 0) return order;
                order = Boolean.compare(minimizeHatches && first.hatch(), minimizeHatches && second.hatch());
                if (order != 0) return order;
                order = Boolean.compare(!exactObserved(cell, first), !exactObserved(cell, second));
                if (order != 0) return order;
                order = Integer.compare(willPlace(cell, first) ? budget.rank(first.materialKey()) : 0,
                        willPlace(cell, second) ? budget.rank(second.materialKey()) : 0);
                if (order != 0) return order;
                order = Integer.compare(canonical ? previewPriority(cell, first) : 0,
                        canonical ? previewPriority(cell, second) : 0);
                if (order != 0) return order;
                order = Integer.compare(first.order(), second.order());
                return order != 0 ? order : BuiltInRegistries.BLOCK.getKey(first.state().getBlock()).toString()
                        .compareTo(BuiltInRegistries.BLOCK.getKey(second.state().getBlock()).toString());
            });
            return candidates;
        }

        private @Nullable AppliedCandidate applyCandidate(UnresolvedCell cell, ResolvedCandidate candidate,
                                                          int count, SearchBudget budget) {
            if (count == 0) {
                return new AppliedCandidate(candidate, 0, BudgetUse.EMPTY, SelectionScore.ZERO);
            }
            if (!withinMaximums(cell.layer(), candidate, count)) return null;
            long previewPenalty = previewPenalty(cell, candidate, count);
            increment(cell.layer(), candidate, count);
            BudgetUse budgetUse = willPlace(cell, candidate) ? budget.consume(candidate.materialKey(), count) :
                    BudgetUse.EMPTY;
            SelectionScore score = new SelectionScore(
                    minimizeHatches && candidate.hatch() ? count : 0,
                    SelectionScore.saturatedMultiply(modificationCost(cell, candidate), count),
                    budgetUse.sourceCost(), previewPenalty,
                    SelectionScore.saturatedMultiply(candidate.order(), count));
            return new AppliedCandidate(candidate, count, budgetUse, score);
        }

        private void undoCandidate(UnresolvedCell cell, AppliedCandidate applied, SearchBudget budget) {
            if (applied.count() == 0) return;
            budget.restore(applied.candidate().materialKey(), applied.budgetUse());
            increment(cell.layer(), applied.candidate(), -applied.count());
        }

        private void undoApplied(UnresolvedCell cell, ObjectList<AppliedCandidate> applied, SearchBudget budget,
                                 int[] counts) {
            for (int index = applied.size() - 1; index >= 0; index--) {
                AppliedCandidate candidate = applied.get(index);
                int candidateIndex = cell.candidates().indexOf(candidate.candidate());
                counts[candidateIndex]--;
                undoCandidate(cell, candidate, budget);
            }
        }

        private long previewPenalty(UnresolvedCell cell, ResolvedCandidate candidate, int count) {
            if (!canonical) return 0;
            long penalty = 0;
            for (int offset = 0; offset < count; offset++) {
                boolean stillDesired = false;
                for (ConstraintLimit limit : candidate.limits()) {
                    if (limit.previewCount() >= 0 &&
                            globalCounts.getInt(limit.key()) + offset < limit.previewCount()) {
                        stillDesired = true;
                        break;
                    }
                }
                if (!stillDesired) penalty++;
            }
            return penalty;
        }

        private boolean canBeatBest(ObjectList<VariableGroup> groups, int groupIndex, SearchBudget budget,
                                    SelectionScore score) {
            if (bestScore == null) return true;
            SelectionScore optimistic = score;
            for (int index = groupIndex; index < groups.size(); index++) {
                VariableGroup group = groups.get(index);
                UnresolvedCell cell = group.representative();
                SelectionScore minimum = null;
                for (ResolvedCandidate candidate : cell.candidates()) {
                    if (!withinMaximums(cell.layer(), candidate)) continue;
                    SelectionScore candidateCost = optimisticCandidateScore(cell, candidate, budget);
                    if (minimum == null || candidateCost.compareTo(minimum) < 0) minimum = candidateCost;
                }
                if (minimum == null) return false;
                optimistic = optimistic.add(minimum.times(group.cellIndices().size()));
            }
            return optimistic.compareTo(bestScore) < 0;
        }

        private SelectionScore optimisticCandidateScore(UnresolvedCell cell, ResolvedCandidate candidate,
                                                        SearchBudget budget) {
            long sourceCost = willPlace(cell, candidate) && !budget.unlimited ?
                    budget.rank(candidate.materialKey()) : 0;
            return new SelectionScore(minimizeHatches && candidate.hatch() ? 1 : 0,
                    modificationCost(cell, candidate), sourceCost,
                    canonical ? previewPriority(cell, candidate) : 0, candidate.order());
        }

        private long modificationCost(UnresolvedCell cell, ResolvedCandidate candidate) {
            if (!consumeMaterials || candidate.any() || exactObserved(cell, candidate) ||
                    cell.relativePos().equals(BlockPos.ZERO)) {
                return 0;
            }
            if (candidate.state().isAir()) {
                return cell.observedState() == null || cell.observedState().isAir() ? 0 : 1;
            }
            return 1;
        }

        private int requiredPriority(UnresolvedCell cell, ResolvedCandidate candidate) {
            boolean contributes = false;
            for (ConstraintLimit limit : candidate.limits()) {
                if (limit.min() >= 0 && globalCounts.getInt(limit.key()) < limit.min() ||
                        limit.layerMin() >= 0 && layerCount(cell.layer(), limit.key()) < limit.layerMin()) {
                    contributes = true;
                }
            }
            return contributes ? 0 : 1;
        }

        private int previewPriority(UnresolvedCell cell, ResolvedCandidate candidate) {
            for (ConstraintLimit limit : candidate.limits()) {
                if (limit.previewCount() >= 0 && globalCounts.getInt(limit.key()) < limit.previewCount()) return 0;
            }
            return 1;
        }

        private boolean exactObserved(UnresolvedCell cell, ResolvedCandidate candidate) {
            return cell.observedState() != null && matchesCandidateDomain(cell, candidate, cell.observedState());
        }

        private boolean willPlace(UnresolvedCell cell, ResolvedCandidate candidate) {
            return consumeMaterials && candidate.materialKey() != null && !candidate.state().isAir() &&
                    !cell.relativePos().equals(BlockPos.ZERO) && !exactObserved(cell, candidate) &&
                    !canUpdateDirection(cell, candidate);
        }

        private boolean withinMaximums(int layer, ResolvedCandidate candidate) {
            return withinMaximums(layer, candidate, 1);
        }

        private boolean withinMaximums(int layer, ResolvedCandidate candidate, int count) {
            for (ConstraintLimit limit : candidate.limits()) {
                if (limit.max() >= 0 && (long) globalCounts.getInt(limit.key()) + count > limit.max()) return false;
                if (limit.layerMax() >= 0 &&
                        (long) layerCount(layer, limit.key()) + count > limit.layerMax())
                    return false;
            }
            return true;
        }

        private void increment(int layer, ResolvedCandidate candidate, int amount) {
            for (ConstraintLimit limit : candidate.limits()) {
                globalCounts.mergeInt(limit.key(), amount, Integer::sum);
                layerCounts.computeIfAbsent(layer, ignored -> new Reference2IntOpenHashMap<>())
                        .mergeInt(limit.key(), amount, Integer::sum);
            }
        }

        private boolean minimumsSatisfied() {
            for (ConstraintLimit limit : limits.values()) {
                if (limit.min() >= 0 && globalCounts.getInt(limit.key()) < limit.min()) {
                    return false;
                }
            }
            for (var layerEntry : layerLimits.int2ObjectEntrySet()) {
                for (ConstraintLimit limit : layerEntry.getValue().values()) {
                    if (limit.layerMin() >= 0 && layerCount(layerEntry.getIntKey(), limit.key()) < limit.layerMin()) {
                        return false;
                    }
                }
            }
            return true;
        }

        private boolean canStillSatisfy(ObjectList<VariableGroup> groups, int groupIndex) {
            for (int index = groupIndex; index < groups.size(); index++) {
                VariableGroup group = groups.get(index);
                UnresolvedCell cell = group.representative();
                long capacity = 0;
                for (ResolvedCandidate candidate : cell.candidates()) {
                    capacity += candidateCapacity(cell.layer(), candidate, group.cellIndices().size());
                    if (capacity >= group.cellIndices().size()) break;
                }
                if (capacity < group.cellIndices().size()) {
                    return false;
                }
            }
            for (ConstraintLimit limit : limits.values()) {
                if (limit.max() >= 0 && globalCounts.getInt(limit.key()) > limit.max()) return false;
                if (limit.min() >= 0 && globalCounts.getInt(limit.key()) +
                        remainingContributors(groups, groupIndex, limit.key(), null) < limit.min()) {
                    return false;
                }
            }
            for (var layerEntry : layerLimits.int2ObjectEntrySet()) {
                int layer = layerEntry.getIntKey();
                for (ConstraintLimit limit : layerEntry.getValue().values()) {
                    if (limit.layerMax() >= 0 && layerCount(layer, limit.key()) > limit.layerMax()) return false;
                    if (limit.layerMin() >= 0 && layerCount(layer, limit.key()) +
                            remainingContributors(groups, groupIndex, limit.key(), layer) < limit.layerMin()) {
                        return false;
                    }
                }
            }
            return true;
        }

        private int candidateCapacity(int layer, ResolvedCandidate candidate, int maximum) {
            int capacity = maximum;
            for (ConstraintLimit limit : candidate.limits()) {
                if (limit.max() >= 0) {
                    capacity = Math.min(capacity, limit.max() - globalCounts.getInt(limit.key()));
                }
                if (limit.layerMax() >= 0) {
                    capacity = Math.min(capacity, limit.layerMax() - layerCount(layer, limit.key()));
                }
            }
            return Math.max(0, capacity);
        }

        private int remainingContributors(ObjectList<VariableGroup> groups, int groupIndex, Object key,
                                          @Nullable Integer requiredLayer) {
            int remaining = 0;
            for (int index = groupIndex; index < groups.size(); index++) {
                VariableGroup group = groups.get(index);
                UnresolvedCell cell = group.representative();
                if (requiredLayer != null && cell.layer() != requiredLayer) continue;
                boolean contributes = cell.candidates().stream()
                        .filter(candidate -> withinMaximums(cell.layer(), candidate))
                        .anyMatch(candidate -> candidate.limits().stream().anyMatch(limit -> limit.key() == key));
                if (contributes) remaining = Math.addExact(remaining, group.cellIndices().size());
            }
            return remaining;
        }

        private int layerCount(int layer, Object key) {
            var counts = layerCounts.get(layer);
            return counts == null ? 0 : counts.getInt(key);
        }

        private SearchState stateKey(int groupIndex, SearchBudget budget) {
            IntList global = new IntImmutableList(orderedLimits.stream()
                    .mapToInt(limit -> globalCounts.getInt(limit.key())).toArray());
            IntList layer = new IntImmutableList(orderedLayerLimits.stream()
                    .mapToInt(limit -> layerCount(limit.layer(), limit.limit().key())).toArray());
            return new SearchState(groupIndex, global, layer, budget.values(budgetKeys));
        }

        private static int[][] copyCounts(int[][] source) {
            int[][] copy = new int[source.length][];
            for (int index = 0; index < source.length; index++) {
                copy[index] = source[index].clone();
            }
            return copy;
        }

        private record CandidateKey(BlockState state, @Nullable AutoBuildItemKey materialKey,
                                    boolean hasBlockEntity, boolean hatch, boolean any, int order,
                                    ObjectList<String> tierGroups, ObjectList<String> constraintIds) {}

        private record GroupKey(int layer, @Nullable BlockState observedState,
                                @Nullable Direction requiredDirection, boolean controller,
                                ObjectList<CandidateKey> candidates) {}

        private record VariableGroup(IntList cellIndices, UnresolvedCell representative) {

            private boolean hasConstraints() {
                return representative.candidates().stream().anyMatch(candidate -> !candidate.limits().isEmpty());
            }

            private int constraintCount() {
                return representative.candidates().stream().mapToInt(candidate -> candidate.limits().size()).max()
                        .orElse(0);
            }
        }

        private record LayerLimit(int layer, ConstraintLimit limit) {}

        private record SearchState(int groupIndex, IntList globalCounts, IntList layerCounts,
                                   LongList materialRemaining) {}

        private record BudgetUse(LongList consumedBySource, long sourceCost) {

            private static final BudgetUse EMPTY = new BudgetUse(LongList.of(), 0);
        }

        private record AppliedCandidate(ResolvedCandidate candidate, int count, BudgetUse budgetUse,
                                        SelectionScore score) {}

        private record SelectionScore(long hatches, long modifications, long sourcePriority,
                                      long previewPenalty, long declarationOrder)
                implements Comparable<SelectionScore> {

            private static final SelectionScore ZERO = new SelectionScore(0, 0, 0, 0, 0);

            private SelectionScore add(SelectionScore other) {
                return new SelectionScore(saturatedAdd(hatches, other.hatches),
                        saturatedAdd(modifications, other.modifications),
                        saturatedAdd(sourcePriority, other.sourcePriority),
                        saturatedAdd(previewPenalty, other.previewPenalty),
                        saturatedAdd(declarationOrder, other.declarationOrder));
            }

            private SelectionScore times(int count) {
                return new SelectionScore(saturatedMultiply(hatches, count),
                        saturatedMultiply(modifications, count), saturatedMultiply(sourcePriority, count),
                        saturatedMultiply(previewPenalty, count), saturatedMultiply(declarationOrder, count));
            }

            @Override
            public int compareTo(SelectionScore other) {
                int comparison = Long.compare(hatches, other.hatches);
                if (comparison != 0) return comparison;
                comparison = Long.compare(modifications, other.modifications);
                if (comparison != 0) return comparison;
                comparison = Long.compare(sourcePriority, other.sourcePriority);
                if (comparison != 0) return comparison;
                comparison = Long.compare(previewPenalty, other.previewPenalty);
                return comparison != 0 ? comparison : Long.compare(declarationOrder, other.declarationOrder);
            }

            private static long saturatedAdd(long first, long second) {
                return first > Long.MAX_VALUE - second ? Long.MAX_VALUE : first + second;
            }

            private static long saturatedMultiply(long value, int count) {
                if (value == 0 || count == 0) return 0;
                return value > Long.MAX_VALUE / count ? Long.MAX_VALUE : value * count;
            }
        }

        private static final class SearchBudget {

            private final ObjectList<Object2LongMap<AutoBuildItemKey>> remaining;
            private final boolean unlimited;

            private SearchBudget(ObjectList<Object2LongMap<AutoBuildItemKey>> remaining, boolean unlimited) {
                this.remaining = remaining;
                this.unlimited = unlimited;
            }

            private int rank(@Nullable AutoBuildItemKey key) {
                if (key == null || unlimited) return 0;
                for (int index = 0; index < remaining.size(); index++) {
                    if (remaining.get(index).getLong(key) > 0) return index;
                }
                return missingRank();
            }

            private int missingRank() {
                return remaining.size() + 1;
            }

            private BudgetUse consume(@Nullable AutoBuildItemKey key, int count) {
                if (key == null || unlimited || count == 0) return BudgetUse.EMPTY;
                long remainingCount = count;
                long sourceCost = 0;
                LongList consumed = new LongArrayList(remaining.size());
                for (int index = 0; index < remaining.size(); index++) {
                    Object2LongMap<AutoBuildItemKey> source = remaining.get(index);
                    long available = source.getLong(key);
                    long taken = Math.min(available, remainingCount);
                    consumed.add(taken);
                    if (taken == 0) continue;
                    source.put(key, available - taken);
                    remainingCount -= taken;
                    sourceCost = Math.addExact(sourceCost, Math.multiplyExact(index, taken));
                }
                if (remainingCount > 0) {
                    sourceCost = Math.addExact(sourceCost, Math.multiplyExact(missingRank(), remainingCount));
                }
                return new BudgetUse(new LongImmutableList(consumed), sourceCost);
            }

            private void restore(@Nullable AutoBuildItemKey key, BudgetUse use) {
                if (key == null || unlimited) return;
                for (int index = 0; index < use.consumedBySource().size(); index++) {
                    long amount = use.consumedBySource().getLong(index);
                    if (amount > 0) remaining.get(index).mergeLong(key, amount, Math::addExact);
                }
            }

            private LongList values(ObjectList<AutoBuildItemKey> keys) {
                LongList values = new LongArrayList(remaining.size() * keys.size());
                for (Object2LongMap<AutoBuildItemKey> source : remaining) {
                    for (AutoBuildItemKey key : keys) {
                        values.add(source.getLong(key));
                    }
                }
                return new LongImmutableList(values);
            }
        }
    }

    private static final class MaterialAllocation {

        private final ObjectList<Object2LongMap<AutoBuildItemKey>> remaining;
        private final ObjectList<Object2LongMap<AutoBuildItemKey>> allocated;
        private final ObjectList<AutoBuildMaterialSource.Snapshot> snapshots;
        private final LongList sourceFingerprints;
        private final ObjectList<AutoBuildProblem> structureProblems;
        private final boolean unlimited;
        private final Object2ObjectMap<BlockPos, BlockState> virtualWorld = new Object2ObjectOpenHashMap<>();

        private MaterialAllocation(ObjectList<Object2LongMap<AutoBuildItemKey>> remaining,
                                   ObjectList<Object2LongMap<AutoBuildItemKey>> allocated,
                                   ObjectList<AutoBuildMaterialSource.Snapshot> snapshots,
                                   LongList sourceFingerprints,
                                   ObjectList<AutoBuildProblem> structureProblems,
                                   boolean unlimited) {
            this.remaining = remaining;
            this.allocated = allocated;
            this.snapshots = snapshots;
            this.sourceFingerprints = sourceFingerprints;
            this.structureProblems = structureProblems;
            this.unlimited = unlimited;
        }

        private static MaterialAllocation create(ObjectList<AutoBuildMaterialSource> sources,
                                                 ObjectCollection<AutoBuildItemKey> keys,
                                                 ResolutionContext context,
                                                 ObjectList<AutoBuildProblem> problems,
                                                 boolean hasBuild,
                                                 boolean requireME) {
            boolean creative = context.player() != null && context.player().isCreative();
            if (!hasBuild || context.unlimitedMaterials() || creative && !requireME) {
                return new MaterialAllocation(ObjectList.of(), ObjectList.of(), ObjectList.of(), LongList.of(),
                        ObjectList.of(), true);
            }
            ObjectArrayList<Object2LongMap<AutoBuildItemKey>> remaining = new ObjectArrayList<>();
            ObjectArrayList<Object2LongMap<AutoBuildItemKey>> allocated = new ObjectArrayList<>();
            ObjectArrayList<AutoBuildMaterialSource.Snapshot> snapshots = new ObjectArrayList<>();
            LongList sourceFingerprints = new LongArrayList();
            ObjectArrayList<AutoBuildProblem> structureProblems = new ObjectArrayList<>();
            Reference2ObjectOpenHashMap<AutoBuildMaterialSource, ObjectSet<ProblemIdentity>> reportedProblems = new Reference2ObjectOpenHashMap<>();
            for (AutoBuildMaterialSource source : sources) {
                if (creative && source.kind() != AutoBuildMaterialSource.SourceKind.ME &&
                        source.kind() != AutoBuildMaterialSource.SourceKind.UNAVAILABLE) {
                    continue;
                }
                AutoBuildMaterialSource.Snapshot snapshot = source.snapshot(keys);
                if (creative && source.kind() == AutoBuildMaterialSource.SourceKind.UNAVAILABLE &&
                        (snapshot.problem() == null || !isStrictAEProblem(snapshot.problem().type()))) {
                    continue;
                }
                snapshots.add(snapshot);
                sourceFingerprints.add(source.planFingerprint(keys, snapshot));
                AutoBuildProblem snapshotProblem = snapshot.problem();
                if (snapshotProblem != null) {
                    ProblemIdentity identity = new ProblemIdentity(snapshotProblem.type(), snapshotProblem.pos());
                    if (reportedProblems.computeIfAbsent(source, ignored -> new ObjectOpenHashSet<>()).add(identity)) {
                        if (requireME && isStrictAEProblem(snapshotProblem.type()) &&
                                (source.kind() == AutoBuildMaterialSource.SourceKind.ME ||
                                        source.kind() == AutoBuildMaterialSource.SourceKind.UNAVAILABLE)) {
                            problems.add(snapshotProblem);
                        } else {
                            structureProblems.add(snapshotProblem);
                        }
                    }
                }
                remaining.add(new Object2LongLinkedOpenHashMap<>(snapshot.available()));
                allocated.add(new Object2LongLinkedOpenHashMap<>());
            }
            return new MaterialAllocation(remaining, allocated, new ObjectImmutableList<>(snapshots),
                    new LongImmutableList(sourceFingerprints), new ObjectImmutableList<>(structureProblems), creative);
        }

        private record ProblemIdentity(AutoBuildProblem.Type type, @Nullable BlockPos pos) {}

        private @Nullable PlannedCell commitStructure(ObjectList<PlannedCell> cells) {
            if (unlimited) return null;
            ObjectList<Object2LongMap<AutoBuildItemKey>> working = copyRemaining();
            ObjectArrayList<Object2LongMap<AutoBuildItemKey>> stagedAllocations = new ObjectArrayList<>(
                    allocated.size());
            for (int index = 0; index < allocated.size(); index++) {
                stagedAllocations.add(new Object2LongLinkedOpenHashMap<>());
            }
            for (PlannedCell cell : cells) {
                if (cell.action() != CellAction.PLACE || cell.materialKey() == null) continue;
                AutoBuildItemKey key = cell.materialKey();
                boolean found = false;
                for (int index = 0; index < working.size(); index++) {
                    Object2LongMap<AutoBuildItemKey> source = working.get(index);
                    long available = source.getLong(key);
                    if (available <= 0) continue;
                    source.put(key, available - 1);
                    stagedAllocations.get(index).mergeLong(key, 1L, Math::addExact);
                    found = true;
                    break;
                }
                if (!found) return cell;
            }
            for (int index = 0; index < remaining.size(); index++) {
                remaining.get(index).clear();
                remaining.get(index).putAll(working.get(index));
                Object2LongMap<AutoBuildItemKey> target = allocated.get(index);
                stagedAllocations.get(index).object2LongEntrySet()
                        .forEach(entry -> target.mergeLong(entry.getKey(), entry.getLongValue(), Math::addExact));
            }
            return null;
        }

        private ObjectList<AutoBuildProblem> structureProblems() {
            return structureProblems;
        }

        private ObjectList<AutoBuildMaterialSource.Snapshot> snapshots() {
            return snapshots;
        }

        private LongList sourceFingerprints() {
            return sourceFingerprints;
        }

        private Object2ObjectMap<AutoBuildItemKey, ResolvedAutoBuildSnapshot.MaterialAllocation> resolveAllocations(
                                                                                                                    ObjectList<ResolvedStructurePlan> demolitions,
                                                                                                                    ObjectList<ResolvedStructurePlan> builds) {
            Object2LongMap<AutoBuildItemKey> required = new Object2LongLinkedOpenHashMap<>();
            for (ResolvedStructurePlan plan : concat(demolitions, builds)) {
                plan.materials().object2LongEntrySet()
                        .forEach(entry -> required.mergeLong(entry.getKey(), entry.getLongValue(), Math::addExact));
            }
            Object2ObjectLinkedOpenHashMap<AutoBuildItemKey, ResolvedAutoBuildSnapshot.MaterialAllocation> result = new Object2ObjectLinkedOpenHashMap<>();
            required.object2LongEntrySet().forEach(entry -> {
                AutoBuildItemKey key = entry.getKey();
                long count = entry.getLongValue();
                LongList bySource = new LongArrayList(allocated.size());
                long assigned = 0;
                for (Object2LongMap<AutoBuildItemKey> source : allocated) {
                    long value = source.getLong(key);
                    bySource.add(value);
                    assigned = Math.addExact(assigned, value);
                }
                long missing = unlimited ? 0 : count - assigned;
                result.put(key, new ResolvedAutoBuildSnapshot.MaterialAllocation(count, bySource, missing,
                        unlimited));
            });
            return Object2ObjectMaps.unmodifiable(result);
        }

        private ObjectList<Object2LongMap<AutoBuildItemKey>> copyRemaining() {
            ObjectArrayList<Object2LongMap<AutoBuildItemKey>> copy = new ObjectArrayList<>(remaining.size());
            for (Object2LongMap<AutoBuildItemKey> source : remaining) {
                copy.add(new Object2LongLinkedOpenHashMap<>(source));
            }
            return copy;
        }

        private ObjectList<UnresolvedCell> applyVirtualWorld(ObjectList<UnresolvedCell> cells) {
            return cells.stream()
                    .map(cell -> virtualWorld.containsKey(cell.relativePos()) ?
                            cell.withObservedState(virtualWorld.get(cell.relativePos())) : cell)
                    .collect(ObjectImmutableList.toList());
        }

        private void applyPlan(ResolvedStructurePlan plan) {
            if (!plan.valid()) return;
            for (PlannedCell cell : plan.cells()) {
                switch (cell.action()) {
                    case PLACE, UPDATE_DIRECTION, CONTROLLER -> virtualWorld.put(cell.relativePos(), cell.blockState());
                    case KEEP -> {
                        BlockState observed = cell.observedState();
                        if (observed == null && cell.worldPos() != null) {
                            throw new IllegalStateException("KEEP plan cell has no observed state: " +
                                    cell.cellKey());
                        }
                        virtualWorld.put(cell.relativePos(), observed == null ? cell.blockState() : observed);
                    }
                    case CLEAR_FOR_AIR, DEMOLISH_CANDIDATE -> virtualWorld.put(cell.relativePos(),
                            Blocks.AIR.defaultBlockState());
                    case IGNORE_ANY -> {}
                }
            }
        }
    }
}
