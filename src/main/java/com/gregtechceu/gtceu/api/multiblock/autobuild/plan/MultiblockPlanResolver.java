package com.gregtechceu.gtceu.api.multiblock.autobuild.plan;

import com.gregtechceu.gtceu.GTCEu;
import com.gregtechceu.gtceu.api.block.MetaMachineBlock;
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
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

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
        ArrayList<Integer> repetitions = new ArrayList<>(pattern.aisleRepetitions.length);
        for (int[] limits : pattern.aisleRepetitions) {
            repetitions.add(limits[0]);
        }
        Map<ResourceLocation, List<ResourceLocation>> availableTiers = tierChoiceOptions(definition, structureName);
        LinkedHashMap<ResourceLocation, ResourceLocation> tiers = new LinkedHashMap<>();
        availableTiers.forEach((group, candidates) -> tiers.put(group, candidates.getFirst()));
        return new AutoBuildStructureOptions(structureName, repetitions, tiers, false, AutoBuildMode.BUILD);
    }

    /**
     * Returns every reliable tier group and its exact candidates in stable selection order.
     */
    public Map<ResourceLocation, List<ResourceLocation>> tierChoiceOptions(MultiblockMachineDefinition definition,
                                                                           String structureName) {
        MultiBlockPattern pattern = definition.getPattern(structureName);
        ArrayList<Integer> repetitions = new ArrayList<>(pattern.aisleRepetitions.length);
        for (int[] limits : pattern.aisleRepetitions) {
            repetitions.add(limits[0]);
        }
        AutoBuildStructureOptions unresolved = new AutoBuildStructureOptions(structureName, repetitions, Map.of(),
                false, AutoBuildMode.BUILD);
        ResolutionContext context = new ResolutionContext(null, null, null, BlockPos.ZERO, CANONICAL_FRONT,
                CANONICAL_UP, true, true);
        ArrayList<AutoBuildProblem> problems = new ArrayList<>();
        List<UnresolvedCell> cells = expandStructure(definition, pattern, unresolved, repetitions, context,
                new ConstraintIds(), problems);
        if (!problems.isEmpty()) {
            throw new IllegalStateException(problems.getFirst().message().getString());
        }
        LinkedHashMap<ResourceLocation, LinkedHashSet<ResourceLocation>> encountered = new LinkedHashMap<>();
        for (UnresolvedCell cell : cells) {
            for (ResolvedCandidate candidate : cell.candidates()) {
                for (ResourceLocation tierGroup : candidate.tierGroups()) {
                    encountered.computeIfAbsent(tierGroup, ignored -> new LinkedHashSet<>())
                            .add(AutoBuildBlockMap.blockId(candidate.state().getBlock()));
                }
            }
        }
        LinkedHashMap<ResourceLocation, List<ResourceLocation>> result = new LinkedHashMap<>();
        encountered.forEach((group, candidateIds) -> {
            ArrayList<ResourceLocation> ordered = new ArrayList<>();
            Block[] registered = AutoBuildBlockMap.categoryBlocks(group);
            if (registered != null) {
                for (Block block : registered) {
                    ResourceLocation blockId = AutoBuildBlockMap.blockId(block);
                    if (candidateIds.contains(blockId)) ordered.add(blockId);
                }
            }
            candidateIds.stream().filter(candidate -> !ordered.contains(candidate)).forEach(ordered::add);
            result.put(group, List.copyOf(ordered));
        });
        return Collections.unmodifiableMap(result);
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
        Map<String, Integer> order = structureOrder(definition);
        ArrayList<PendingStructure> pending = new ArrayList<>(request.structures().size());
        ArrayList<AutoBuildProblem> sharedProblems = new ArrayList<>();
        validateSharedSources(request, context, sharedProblems);

        Set<String> requestedBuilds = new HashSet<>();
        Set<String> requestedDemolitions = new HashSet<>();
        for (AutoBuildStructureOptions options : request.structures()) {
            (options.mode() == AutoBuildMode.BUILD ? requestedBuilds : requestedDemolitions)
                    .add(options.structureName());
        }

        ConstraintIds constraintIds = new ConstraintIds();
        for (AutoBuildStructureOptions options : request.structures()) {
            ArrayList<AutoBuildProblem> problems = new ArrayList<>();
            MultiBlockPattern pattern = null;
            List<Integer> repetitions = List.of();
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

            List<UnresolvedCell> cells = List.of();
            if (pattern != null && problems.isEmpty()) {
                cells = expandStructure(definition, pattern, options, repetitions, context, constraintIds, problems);
                validateTierChoices(options, cells, problems, context.worldPos());
            }
            pending.add(new PendingStructure(options, repetitions, cells, problems));
        }

        pending.sort(executionComparator(order));
        LinkedHashSet<AutoBuildItemKey> materialKeys = new LinkedHashSet<>();
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

        ArrayList<ResolvedStructurePlan> demolitionPlans = new ArrayList<>();
        ArrayList<ResolvedStructurePlan> buildPlans = new ArrayList<>();
        for (PendingStructure structure : pending) {
            ResolvedStructurePlan plan = resolveStructure(definition, structure, request.sharedOptions(), context,
                    materials);
            if (structure.options().mode() == AutoBuildMode.DEMOLISH) {
                demolitionPlans.add(plan);
            } else {
                buildPlans.add(plan);
            }
        }

        LinkedHashMap<BlockPos, MergedPlanCell> merged = mergeCells(demolitionPlans, buildPlans);
        Map<AutoBuildItemKey, ResolvedAutoBuildSnapshot.MaterialAllocation> allocations = materials
                .resolveAllocations(demolitionPlans, buildPlans);
        long fingerprint = fingerprint(demolitionPlans, buildPlans, merged, materials.sourceFingerprints(),
                allocations);
        AutoBuildPlan plan = new AutoBuildPlan(demolitionPlans, buildPlans, merged, sharedProblems, fingerprint);
        return new ResolvedAutoBuildSnapshot(plan, materials.snapshots(), allocations);
    }

    private static ResolvedAutoBuildSnapshot sharedFailureSnapshot(AutoBuildBatchRequest request,
                                                                   AutoBuildProblem problem) {
        ArrayList<ResolvedStructurePlan> demolitions = new ArrayList<>();
        ArrayList<ResolvedStructurePlan> builds = new ArrayList<>();
        for (AutoBuildStructureOptions options : request.structures()) {
            ResolvedStructurePlan plan = new ResolvedStructurePlan(options.structureName(), options.mode(),
                    options.repetitions(), options.flipMode(), List.of(), Map.of(), List.of(problem));
            (options.mode() == AutoBuildMode.DEMOLISH ? demolitions : builds).add(plan);
        }
        AutoBuildPlan plan = new AutoBuildPlan(demolitions, builds, Map.of(), List.of(problem), FINGERPRINT_OFFSET);
        return new ResolvedAutoBuildSnapshot(plan, List.of(), Map.of());
    }

    private static Map<String, Integer> structureOrder(MultiblockMachineDefinition definition) {
        LinkedHashMap<String, Integer> result = new LinkedHashMap<>();
        List<String> order = definition.getStructureOrder();
        for (int index = 0; index < order.size(); index++) {
            result.put(order.get(index), index);
        }
        return result;
    }

    private static Comparator<PendingStructure> executionComparator(Map<String, Integer> order) {
        return Comparator.<PendingStructure, AutoBuildMode>comparing(pending -> pending.options().mode(),
                Comparator.comparingInt(mode -> mode == AutoBuildMode.DEMOLISH ? 0 : 1))
                .thenComparingInt(pending -> {
                    int index = order.getOrDefault(pending.options().structureName(), Integer.MAX_VALUE);
                    return pending.options().mode() == AutoBuildMode.DEMOLISH ? -index : index;
                });
    }

    private static void validateSharedSources(AutoBuildBatchRequest request, ResolutionContext context,
                                              List<AutoBuildProblem> problems) {
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
                                             Set<String> requestedBuilds,
                                             Set<String> requestedDemolitions,
                                             List<AutoBuildProblem> problems,
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

    private static List<Integer> validateRepetitions(MultiBlockPattern pattern, AutoBuildStructureOptions options,
                                                     List<AutoBuildProblem> problems) {
        if (options.repetitions().size() != pattern.aisleRepetitions.length) {
            problems.add(problem(AutoBuildProblem.Type.INVALID_OPTIONS, null,
                    "gtpm.multiblock.autobuild.repetition_count_mismatch", options.structureName(),
                    pattern.aisleRepetitions.length, options.repetitions().size()));
            return List.of();
        }
        for (int index = 0; index < pattern.aisleRepetitions.length; index++) {
            int value = options.repetitions().get(index);
            int min = pattern.aisleRepetitions[index][0];
            int max = pattern.aisleRepetitions[index][1];
            if (value < min || value > max) {
                problems.add(problem(AutoBuildProblem.Type.INVALID_OPTIONS, null,
                        "gtpm.multiblock.autobuild.repetition_out_of_range", index, value, min, max));
            }
        }
        return problems.isEmpty() ? options.repetitions() : List.of();
    }

    private static List<UnresolvedCell> expandStructure(MultiblockMachineDefinition definition,
                                                        MultiBlockPattern pattern,
                                                        AutoBuildStructureOptions options,
                                                        List<Integer> repetitions,
                                                        ResolutionContext context,
                                                        ConstraintIds constraintIds,
                                                        List<AutoBuildProblem> problems) {
        ArrayList<UnresolvedCell> cells = new ArrayList<>();
        int expandedZ = pattern.getMinZ();
        int layer = 0;
        for (int unit = 0; unit < pattern.aisleRepetitions.length; unit++) {
            int unitStart = pattern.unitStarts[unit];
            int unitDepth = pattern.unitDepths[unit];
            for (int repetition = 0; repetition < repetitions.get(unit); repetition++) {
                for (int inner = 0; inner < unitDepth; inner++, expandedZ++, layer++) {
                    for (int row = 0, y = pattern.getMinY(); row < pattern.getThumbLength(); row++, y++) {
                        for (int column = 0, x = pattern.getMinX(); column < pattern.getPalmLength(); column++, x++) {
                            PatternPredicate predicate = pattern.getPredicate(unitStart + inner, row, column);
                            if (predicate == null) continue;
                            PatternCellKey cellKey = new PatternCellKey(unit, repetition, inner, row, column);
                            BlockPos relativePos = pattern.getActualRelativeOffset(x, y, expandedZ,
                                    context.frontFacing(), context.upwardsFacing(), options.flipMode());
                            BlockPos worldPos = context.canonical() ? null : context.origin().offset(relativePos);
                            BlockState observed = null;
                            if (!context.canonical()) {
                                Level level = context.level();
                                if (level == null || worldPos == null || !level.isLoaded(worldPos)) {
                                    problems.add(problem(AutoBuildProblem.Type.UNLOADED, worldPos,
                                            "gtpm.multiblock.autobuild.unloaded", position(worldPos)));
                                    return List.of();
                                }
                                observed = level.getBlockState(worldPos);
                            }
                            List<ResolvedCandidate> candidates = resolveCandidates(definition, predicate,
                                    context.frontFacing(), context.upwardsFacing(), options.flipMode(), constraintIds,
                                    problems, worldPos);
                            Direction requiredDirection = resolveDirection(predicate, context.frontFacing(),
                                    context.upwardsFacing(), options.flipMode());
                            cells.add(new UnresolvedCell(options.structureName(), cellKey, relativePos, worldPos,
                                    observed, layer, predicate, candidates, requiredDirection));
                        }
                    }
                }
            }
        }
        return cells;
    }

    private static List<ResolvedCandidate> resolveCandidates(MultiblockMachineDefinition definition,
                                                             PatternPredicate predicate,
                                                             Direction frontFacing,
                                                             Direction upwardsFacing,
                                                             boolean flipped,
                                                             ConstraintIds constraintIds,
                                                             List<AutoBuildProblem> problems,
                                                             @Nullable BlockPos pos) {
        if (predicate.isAny()) {
            return List.of(ResolvedCandidate.wildcard());
        }
        LinkedHashMap<BlockState, CandidateAccumulator> candidates = new LinkedHashMap<>();
        CandidateOrder order = new CandidateOrder();
        for (PredicateRule simple : predicate.limited) {
            addSimpleCandidates(candidates, simple, order, constraintIds);
        }
        for (PredicateRule simple : predicate.common) {
            addSimpleCandidates(candidates, simple, order, constraintIds);
        }
        for (StructurePredicate structurePredicate : predicate.structurePatternPredicates) {
            List<StructureChoice> choices = structureChoices(structurePredicate, definition, constraintIds);
            for (StructureChoice choice : choices) {
                List<MultiblockBlockInfo> stableCandidates = choice.tierGroups().isEmpty() ?
                        List.copyOf(choice.candidates()) : choice.candidates().stream()
                                .sorted(Comparator
                                        .comparing((MultiblockBlockInfo info) -> AutoBuildBlockMap
                                                .blockId(info.getBlockState().getBlock()).toString())
                                        .thenComparing(info -> info.getBlockState().toString())
                                        .thenComparing(info -> info.getItemStackForm().toString()))
                                .toList();
                for (MultiblockBlockInfo blockInfo : stableCandidates) {
                    addCandidate(candidates, blockInfo, choice.limits(), choice.hatch(), choice.tierGroups(),
                            order.next());
                }
            }
        }
        if (predicate.hasAir() && candidates.keySet().stream().noneMatch(BlockState::isAir)) {
            addCandidate(candidates, MultiblockBlockInfo.EMPTY, List.of(), false, List.of(), order.next());
        }

        Direction direction = resolveDirection(predicate, frontFacing, upwardsFacing, flipped);
        ArrayList<ResolvedCandidate> result = new ArrayList<>(candidates.size());
        for (CandidateAccumulator candidate : candidates.values()) {
            BlockState state = candidate.blockInfo().blockState();
            if (direction != null && !state.isAir()) {
                state = MultiBlockPattern.applyDirectionalState(state, direction);
                if (!MultiBlockPattern.matchesDirectionalState(state, direction)) continue;
            }
            PlannedBlockInfo blockInfo = candidate.blockInfo().withState(state);
            AutoBuildItemKey materialKey = materialKey(blockInfo.itemStack());
            List<ResourceLocation> tierGroups = mergeTierGroups(candidate.tierGroups(),
                    AutoBuildBlockMap.categoryIds(state.getBlock()));
            result.add(new ResolvedCandidate(state, blockInfo, List.copyOf(candidate.limits()), candidate.hatch(),
                    tierGroups, candidate.order(), materialKey, false));
        }
        if (result.isEmpty()) {
            problems.add(problem(AutoBuildProblem.Type.UNSUPPORTED, pos,
                    "gtpm.multiblock.autobuild.no_candidate", position(pos)));
        }
        return List.copyOf(result);
    }

    private static void addSimpleCandidates(Map<BlockState, CandidateAccumulator> result, PredicateRule simple,
                                            CandidateOrder order, ConstraintIds constraintIds) {
        ConstraintLimit limit = ConstraintLimit.fromSimple(simple, constraintIds.id(simple));
        if (simple == PredicateRule.AIR) {
            addCandidate(result, MultiblockBlockInfo.EMPTY, List.of(limit), false, List.of(), order.next());
            return;
        }
        if (simple == PredicateRule.ANY) return;
        MultiblockBlockInfo representative = simple.blockInfo.get();
        if (simple.candidates == null) {
            if (representative != null) {
                addCandidate(result, representative, List.of(limit), isHatch(representative.getBlockState().getBlock()),
                        List.of(), order.next());
            }
            return;
        }
        Block[] blocks = simple.candidates.get();
        if (blocks == null) return;
        for (Block block : blocks) {
            if (block == null) continue;
            MultiblockBlockInfo info = representative != null && representative.getBlockState().is(block) ?
                    representative : MultiblockBlockInfo.fromBlock(block);
            addCandidate(result, info, List.of(limit), isHatch(block), List.of(), order.next());
        }
    }

    private static List<StructureChoice> structureChoices(StructurePredicate predicate,
                                                          MultiblockMachineDefinition definition,
                                                          ConstraintIds constraintIds) {
        if (predicate instanceof RestrictedPredicate restricted) {
            ConstraintLimit limit = ConstraintLimit.fromStructure(new StructurePreviewConstraint(restricted,
                    restricted.minCount(), restricted.maxCount(), restricted.minCountByLayer(),
                    restricted.maxCountByLayer(), restricted.previewCount()), constraintIds.id(restricted));
            return structureChoices(restricted.predicate(), definition, constraintIds).stream()
                    .map(choice -> choice.withLimit(limit))
                    .toList();
        }
        if (predicate instanceof ConcatenatedPredicate concatenated) {
            return concatenated.predicates().stream()
                    .flatMap(child -> structureChoices(child, definition, constraintIds).stream())
                    .toList();
        }
        ResourceLocation abilityGroup = abilityGroup(predicate);
        boolean hatch = predicate instanceof AbilityPredicate || predicate instanceof TieredAbilityPredicate;
        ArrayList<StructureChoice> result = new ArrayList<>();
        for (StructurePreviewChoice choice : predicate.previewChoices(definition)) {
            List<ConstraintLimit> limits = choice.constraints().stream()
                    .map(constraint -> ConstraintLimit.fromStructure(constraint, constraintIds.id(constraint.key())))
                    .toList();
            result.add(new StructureChoice(choice.candidates(), limits, hatch,
                    abilityGroup == null ? List.of() : List.of(abilityGroup)));
        }
        if (abilityGroup != null) {
            Block[] blocks = abilityBlocks(predicate).stream()
                    .distinct()
                    .sorted(Comparator
                            .comparingInt((Block block) -> block instanceof MetaMachineBlock machineBlock ?
                                    machineBlock.getDefinition().getTier() : Integer.MAX_VALUE)
                            .thenComparing(block -> AutoBuildBlockMap.blockId(block).toString()))
                    .toArray(Block[]::new);
            if (blocks.length > 0) {
                AutoBuildBlockMap.registerCategory(abilityGroup, blocks);
            }
        }
        return result;
    }

    private static Collection<Block> abilityBlocks(StructurePredicate predicate) {
        if (predicate instanceof AbilityPredicate abilities && abilities.abilities().size() == 1) {
            return abilities.abilities().getFirst().getAllBlocks();
        }
        if (predicate instanceof TieredAbilityPredicate tiered) {
            return tiered.ability().getAllBlocks();
        }
        return List.of();
    }

    private static @Nullable ResourceLocation abilityGroup(StructurePredicate predicate) {
        String ability = predicate instanceof AbilityPredicate abilities && abilities.abilities().size() == 1 ?
                abilities.abilities().getFirst().getName() :
                predicate instanceof TieredAbilityPredicate tiered ? tiered.ability().getName() : null;
        if (ability == null) return null;
        String path = ability.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9/._-]", "_");
        return ResourceLocation.fromNamespaceAndPath(GTCEu.MOD_ID, "ability/" + path);
    }

    private static void addCandidate(Map<BlockState, CandidateAccumulator> result, MultiblockBlockInfo blockInfo,
                                     List<ConstraintLimit> limits, boolean hatch,
                                     List<ResourceLocation> tierGroups, int order) {
        PlannedBlockInfo planned = PlannedBlockInfo.from(blockInfo);
        CandidateAccumulator existing = result.get(planned.blockState());
        if (existing == null) {
            result.put(planned.blockState(), new CandidateAccumulator(planned, new ArrayList<>(limits), hatch,
                    List.copyOf(tierGroups), order));
            return;
        }
        for (ConstraintLimit limit : limits) {
            if (existing.limits().stream().noneMatch(current -> current.key() == limit.key())) {
                existing.limits().add(limit);
            }
        }
        List<ResourceLocation> mergedGroups = mergeTierGroups(existing.tierGroups(), tierGroups);
        if (hatch && !existing.hatch() || !mergedGroups.equals(existing.tierGroups())) {
            result.put(planned.blockState(), new CandidateAccumulator(existing.blockInfo(), existing.limits(),
                    existing.hatch() || hatch, mergedGroups,
                    existing.order()));
        }
    }

    private static List<ResourceLocation> mergeTierGroups(List<ResourceLocation> first,
                                                          List<ResourceLocation> second) {
        LinkedHashSet<ResourceLocation> result = new LinkedHashSet<>(first);
        result.addAll(second);
        return List.copyOf(result);
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

    private static void validateTierChoices(AutoBuildStructureOptions options, List<UnresolvedCell> cells,
                                            List<AutoBuildProblem> problems, @Nullable BlockPos pos) {
        LinkedHashMap<ResourceLocation, ResourceLocation> selections = new LinkedHashMap<>();
        for (var entry : options.tierChoices().entrySet()) {
            ResourceLocation group = AutoBuildBlockMap.canonicalCategory(entry.getKey());
            ResourceLocation previous = selections.putIfAbsent(group, entry.getValue());
            if (previous != null && !previous.equals(entry.getValue())) {
                problems.add(problem(AutoBuildProblem.Type.INVALID_TIER_SELECTION, pos,
                        "gtpm.multiblock.autobuild.tier_selection_conflict", group.toString()));
            }
        }

        LinkedHashMap<ResourceLocation, LinkedHashSet<ResourceLocation>> available = new LinkedHashMap<>();
        for (UnresolvedCell cell : cells) {
            for (ResolvedCandidate candidate : cell.candidates()) {
                for (ResourceLocation tierGroup : candidate.tierGroups()) {
                    available.computeIfAbsent(tierGroup, ignored -> new LinkedHashSet<>())
                            .add(AutoBuildBlockMap.blockId(candidate.state().getBlock()));
                }
            }
        }
        available.forEach((group, candidates) -> selections.putIfAbsent(group, candidates.getFirst()));
        for (var entry : selections.entrySet()) {
            Set<ResourceLocation> candidates = available.get(entry.getKey());
            if (candidates == null || !candidates.contains(entry.getValue())) {
                problems.add(problem(AutoBuildProblem.Type.INVALID_TIER_SELECTION, pos,
                        "gtpm.multiblock.autobuild.tier_candidate_invalid", entry.getValue().toString(),
                        entry.getKey().toString(), options.structureName()));
            }
        }
        if (!problems.isEmpty()) return;

        for (int index = 0; index < cells.size(); index++) {
            UnresolvedCell cell = cells.get(index);
            List<ResolvedCandidate> filtered = cell.candidates().stream().filter(candidate -> {
                ResourceLocation blockId = AutoBuildBlockMap.blockId(candidate.state().getBlock());
                for (ResourceLocation tierGroup : candidate.tierGroups()) {
                    ResourceLocation selected = selections.get(tierGroup);
                    if (selected != null && !selected.equals(blockId)) return false;
                }
                return true;
            }).toList();
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
        ArrayList<AutoBuildProblem> problems = new ArrayList<>(pending.problems());
        if (!problems.isEmpty()) {
            return new ResolvedStructurePlan(pending.options().structureName(), pending.options().mode(),
                    pending.repetitions(), pending.options().flipMode(), List.of(), Map.of(), problems);
        }
        boolean buildMode = pending.options().mode() == AutoBuildMode.BUILD;
        ConstraintCandidateSolver solver = new ConstraintCandidateSolver(
                sharedOptions.noHatchMode() && buildMode, materials, buildMode, context.canonical());
        List<UnresolvedCell> observedCells = materials.applyVirtualWorld(pending.cells());
        List<SelectedCell> selected = solver.solve(observedCells, problems);
        if (!problems.isEmpty()) {
            return new ResolvedStructurePlan(pending.options().structureName(), pending.options().mode(),
                    pending.repetitions(), pending.options().flipMode(), List.of(), Map.of(), problems);
        }

        ArrayList<PlannedCell> planned = new ArrayList<>(selected.size());
        LinkedHashMap<AutoBuildItemKey, Long> materialCounts = new LinkedHashMap<>();
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
            List<AutoBuildItemKey> candidateKeys = cell.candidates().stream().map(ResolvedCandidate::materialKey)
                    .filter(Objects::nonNull).distinct().toList();
            List<BlockState> stateCandidates = cell.candidates().stream()
                    .filter(candidateState -> !candidateState.any())
                    .map(ResolvedCandidate::state).distinct().toList();
            AutoBuildItemKey materialKey = candidate.materialKey();
            if (action == CellAction.PLACE && materialKey != null) {
                materialCounts.merge(materialKey, 1L, Math::addExact);
            }
            planned.add(new PlannedCell(cell.structureName(), cell.cellKey(), cell.relativePos(), cell.worldPos(),
                    targetState, observed, candidate.blockInfo().withState(targetState), action, cell.predicate(),
                    stateCandidates,
                    cell.requiredDirection(), candidateKeys, materialKey, candidate.tierGroups(),
                    candidate.limits().stream().map(ConstraintLimit::id).toList()));
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

    private static LinkedHashMap<BlockPos, MergedPlanCell> mergeCells(List<ResolvedStructurePlan> demolitions,
                                                                      List<ResolvedStructurePlan> builds) {
        LinkedHashMap<BlockPos, ArrayList<PlannedCell>> grouped = new LinkedHashMap<>();
        for (ResolvedStructurePlan plan : concat(demolitions, builds)) {
            for (PlannedCell cell : plan.cells()) {
                grouped.computeIfAbsent(cell.relativePos(), ignored -> new ArrayList<>()).add(cell);
            }
        }
        LinkedHashMap<BlockPos, MergedPlanCell> result = new LinkedHashMap<>();
        grouped.forEach((pos, contributors) -> {
            ArrayList<PlannedCell> ordered = new ArrayList<>(contributors.size());
            contributors.stream().filter(cell -> cell.action() == CellAction.IGNORE_ANY).forEach(ordered::add);
            contributors.stream().filter(cell -> cell.action() != CellAction.IGNORE_ANY).forEach(ordered::add);
            MergeState state = compatible(ordered) ? MergeState.COMPATIBLE :
                    MergeState.CONFLICT;
            result.put(pos, new MergedPlanCell(pos, ordered.getFirst().worldPos(), ordered, state));
        });
        return result;
    }

    private static List<ResolvedStructurePlan> concat(List<ResolvedStructurePlan> first,
                                                      List<ResolvedStructurePlan> second) {
        ArrayList<ResolvedStructurePlan> result = new ArrayList<>(first.size() + second.size());
        result.addAll(first);
        result.addAll(second);
        return result;
    }

    private static boolean compatible(List<PlannedCell> contributors) {
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
                                    List<ResolvedStructurePlan> demolitions,
                                    List<ResolvedStructurePlan> builds,
                                    Map<BlockPos, MergedPlanCell> merged,
                                    List<Long> sourceFingerprints,
                                    Map<AutoBuildItemKey, ResolvedAutoBuildSnapshot.MaterialAllocation> allocations) {
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
        for (Map.Entry<AutoBuildItemKey, ResolvedAutoBuildSnapshot.MaterialAllocation> entry : allocations.entrySet()) {
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

    private record PendingStructure(AutoBuildStructureOptions options, List<Integer> repetitions,
                                    List<UnresolvedCell> cells, List<AutoBuildProblem> problems) {}

    private record UnresolvedCell(String structureName, PatternCellKey cellKey, BlockPos relativePos,
                                  @Nullable BlockPos worldPos, @Nullable BlockState observedState, int layer,
                                  PatternPredicate predicate,
                                  List<ResolvedCandidate> candidates, @Nullable Direction requiredDirection) {

        private UnresolvedCell withCandidates(List<ResolvedCandidate> replacement) {
            return new UnresolvedCell(structureName, cellKey, relativePos, worldPos, observedState, layer, predicate,
                    List.copyOf(replacement), requiredDirection);
        }

        private UnresolvedCell withObservedState(@Nullable BlockState observed) {
            return new UnresolvedCell(structureName, cellKey, relativePos, worldPos, observed, layer, predicate,
                    candidates, requiredDirection);
        }
    }

    private record ResolvedCandidate(BlockState state, PlannedBlockInfo blockInfo, List<ConstraintLimit> limits,
                                     boolean hatch, List<ResourceLocation> tierGroups, int order,
                                     @Nullable AutoBuildItemKey materialKey, boolean any) {

        private static ResolvedCandidate wildcard() {
            return new ResolvedCandidate(Blocks.AIR.defaultBlockState(),
                    PlannedBlockInfo.from(Blocks.AIR.defaultBlockState()),
                    List.of(), false, List.of(), 0, null, true);
        }
    }

    private record CandidateAccumulator(PlannedBlockInfo blockInfo, List<ConstraintLimit> limits, boolean hatch,
                                        List<ResourceLocation> tierGroups, int order) {}

    private record StructureChoice(List<MultiblockBlockInfo> candidates, List<ConstraintLimit> limits,
                                   boolean hatch, List<ResourceLocation> tierGroups) {

        private StructureChoice withLimit(ConstraintLimit limit) {
            ArrayList<ConstraintLimit> result = new ArrayList<>(limits.size() + 1);
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

        private final IdentityHashMap<Object, String> ids = new IdentityHashMap<>();

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
        private final IdentityHashMap<Object, Integer> globalCounts = new IdentityHashMap<>();
        private final Map<Integer, IdentityHashMap<Object, Integer>> layerCounts = new HashMap<>();
        private final IdentityHashMap<Object, ConstraintLimit> limits = new IdentityHashMap<>();
        private final Map<Integer, IdentityHashMap<Object, ConstraintLimit>> layerLimits = new HashMap<>();
        private final Map<SearchState, SelectionScore> visitedStates = new HashMap<>();
        private List<ConstraintLimit> orderedLimits = List.of();
        private List<LayerLimit> orderedLayerLimits = List.of();
        private List<AutoBuildItemKey> budgetKeys = List.of();
        private @Nullable SelectionScore bestScore;
        private @Nullable int[][] bestGroupCounts;

        private ConstraintCandidateSolver(boolean minimizeHatches, MaterialAllocation materials,
                                          boolean consumeMaterials, boolean canonical) {
            this.minimizeHatches = minimizeHatches;
            this.materials = materials;
            this.consumeMaterials = consumeMaterials;
            this.canonical = canonical;
        }

        private List<SelectedCell> solve(List<UnresolvedCell> cells, List<AutoBuildProblem> problems) {
            registerLimits(cells);
            List<VariableGroup> groups = groupCells(cells);
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
                return List.of();
            }
            int[] cellAssignments = projectAssignments(cells, groups, selectedCounts);
            ArrayList<SelectedCell> result = new ArrayList<>(cells.size());
            for (int index = 0; index < cells.size(); index++) {
                UnresolvedCell cell = cells.get(index);
                ResolvedCandidate selected = cell.candidates().get(cellAssignments[index]);
                result.add(new SelectedCell(cell, selected));
            }
            return List.copyOf(result);
        }

        private int[] projectAssignments(List<UnresolvedCell> cells, List<VariableGroup> groups,
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

        private void registerLimits(List<UnresolvedCell> cells) {
            LinkedHashSet<AutoBuildItemKey> encounteredKeys = new LinkedHashSet<>();
            for (UnresolvedCell cell : cells) {
                for (ResolvedCandidate candidate : cell.candidates()) {
                    if (candidate.materialKey() != null) encounteredKeys.add(candidate.materialKey());
                    for (ConstraintLimit limit : candidate.limits()) {
                        limits.putIfAbsent(limit.key(), limit);
                        if (limit.layerMin() >= 0 || limit.layerMax() >= 0) {
                            layerLimits.computeIfAbsent(cell.layer(), ignored -> new IdentityHashMap<>())
                                    .putIfAbsent(limit.key(), limit);
                        }
                    }
                }
            }
            orderedLimits = limits.values().stream().sorted(Comparator.comparing(ConstraintLimit::id)).toList();
            orderedLayerLimits = layerLimits.entrySet().stream()
                    .flatMap(entry -> entry.getValue().values().stream()
                            .map(limit -> new LayerLimit(entry.getKey(), limit)))
                    .sorted(Comparator.comparingInt(LayerLimit::layer)
                            .thenComparing(layerLimit -> layerLimit.limit().id()))
                    .toList();
            budgetKeys = List.copyOf(encounteredKeys);
        }

        private List<VariableGroup> groupCells(List<UnresolvedCell> cells) {
            LinkedHashMap<GroupKey, ArrayList<Integer>> grouped = new LinkedHashMap<>();
            for (int index = 0; index < cells.size(); index++) {
                UnresolvedCell cell = cells.get(index);
                List<CandidateKey> candidates = cell.candidates().stream()
                        .map(candidate -> new CandidateKey(candidate.state(), candidate.materialKey(),
                                candidate.blockInfo().hasBlockEntity(), candidate.hatch(), candidate.any(),
                                candidate.order(), candidate.tierGroups(), candidate.limits().stream()
                                        .map(ConstraintLimit::id).sorted().toList()))
                        .toList();
                GroupKey key = new GroupKey(cell.layer(), cell.observedState(), cell.requiredDirection(),
                        cell.relativePos().equals(BlockPos.ZERO), candidates);
                grouped.computeIfAbsent(key, ignored -> new ArrayList<>()).add(index);
            }
            ArrayList<VariableGroup> result = new ArrayList<>(grouped.size());
            grouped.values().forEach(indices -> result.add(new VariableGroup(List.copyOf(indices),
                    cells.get(indices.getFirst()))));
            result.sort(Comparator.comparingInt((VariableGroup group) -> group.hasConstraints() ? 0 : 1)
                    .thenComparingInt(group -> group.representative().candidates().size())
                    .thenComparing(Comparator.comparingInt(VariableGroup::constraintCount).reversed())
                    .thenComparingInt(group -> group.cellIndices().getFirst()));
            return List.copyOf(result);
        }

        private boolean search(List<VariableGroup> groups, int groupIndex, SearchBudget budget,
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
            List<Integer> candidates = orderedCandidates(group.representative(), budget);
            return enumerateGroup(groups, groupIndex, budget, score, groupCounts, candidates, 0,
                    group.cellIndices().size());
        }

        private boolean searchGreedyGroup(List<VariableGroup> groups, int groupIndex, SearchBudget budget,
                                          SelectionScore score, int[][] groupCounts) {
            VariableGroup group = groups.get(groupIndex);
            UnresolvedCell cell = group.representative();
            ArrayList<AppliedCandidate> applied = new ArrayList<>(group.cellIndices().size());
            SelectionScore nextScore = score;
            for (int ignored = 0; ignored < group.cellIndices().size(); ignored++) {
                List<Integer> ordered = orderedCandidates(cell, budget);
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

        private boolean enumerateGroup(List<VariableGroup> groups, int groupIndex, SearchBudget budget,
                                       SelectionScore score, int[][] groupCounts, List<Integer> candidateOrder,
                                       int candidateCursor, int remainingCells) {
            if (candidateCursor == candidateOrder.size()) {
                return remainingCells == 0 && search(groups, groupIndex + 1, budget, score, groupCounts);
            }
            UnresolvedCell cell = groups.get(groupIndex).representative();
            int candidateIndex = candidateOrder.get(candidateCursor);
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

        private List<Integer> orderedCandidates(UnresolvedCell cell, SearchBudget budget) {
            ArrayList<Integer> candidates = new ArrayList<>();
            for (int index = 0; index < cell.candidates().size(); index++) {
                if (withinMaximums(cell.layer(), cell.candidates().get(index))) {
                    candidates.add(index);
                }
            }
            candidates.sort(Comparator
                    .comparingInt((Integer index) -> requiredPriority(cell, cell.candidates().get(index)))
                    .thenComparingInt(index -> minimizeHatches && cell.candidates().get(index).hatch() ? 1 : 0)
                    .thenComparingInt(index -> exactObserved(cell, cell.candidates().get(index)) ? 0 : 1)
                    .thenComparingInt(index -> willPlace(cell, cell.candidates().get(index)) ?
                            budget.rank(cell.candidates().get(index).materialKey()) : 0)
                    .thenComparingInt(index -> canonical ?
                            previewPriority(cell, cell.candidates().get(index)) : 0)
                    .thenComparingInt(index -> cell.candidates().get(index).order())
                    .thenComparing(index -> AutoBuildBlockMap.blockId(
                            cell.candidates().get(index).state().getBlock()).toString()));
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

        private void undoApplied(UnresolvedCell cell, List<AppliedCandidate> applied, SearchBudget budget,
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
                            globalCount(limit.key()) + offset < limit.previewCount()) {
                        stillDesired = true;
                        break;
                    }
                }
                if (!stillDesired) penalty++;
            }
            return penalty;
        }

        private boolean canBeatBest(List<VariableGroup> groups, int groupIndex, SearchBudget budget,
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
                if (limit.min() >= 0 && globalCount(limit.key()) < limit.min() ||
                        limit.layerMin() >= 0 && layerCount(cell.layer(), limit.key()) < limit.layerMin()) {
                    contributes = true;
                }
            }
            return contributes ? 0 : 1;
        }

        private int previewPriority(UnresolvedCell cell, ResolvedCandidate candidate) {
            for (ConstraintLimit limit : candidate.limits()) {
                if (limit.previewCount() >= 0 && globalCount(limit.key()) < limit.previewCount()) return 0;
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
                if (limit.max() >= 0 && (long) globalCount(limit.key()) + count > limit.max()) return false;
                if (limit.layerMax() >= 0 &&
                        (long) layerCount(layer, limit.key()) + count > limit.layerMax())
                    return false;
            }
            return true;
        }

        private void increment(int layer, ResolvedCandidate candidate, int amount) {
            for (ConstraintLimit limit : candidate.limits()) {
                globalCounts.merge(limit.key(), amount, Integer::sum);
                layerCounts.computeIfAbsent(layer, ignored -> new IdentityHashMap<>())
                        .merge(limit.key(), amount, Integer::sum);
            }
        }

        private boolean minimumsSatisfied() {
            for (ConstraintLimit limit : limits.values()) {
                if (limit.min() >= 0 && globalCount(limit.key()) < limit.min()) {
                    return false;
                }
            }
            for (var layerEntry : layerLimits.entrySet()) {
                for (ConstraintLimit limit : layerEntry.getValue().values()) {
                    if (limit.layerMin() >= 0 && layerCount(layerEntry.getKey(), limit.key()) < limit.layerMin()) {
                        return false;
                    }
                }
            }
            return true;
        }

        private boolean canStillSatisfy(List<VariableGroup> groups, int groupIndex) {
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
                if (limit.max() >= 0 && globalCount(limit.key()) > limit.max()) return false;
                if (limit.min() >= 0 && globalCount(limit.key()) +
                        remainingContributors(groups, groupIndex, limit.key(), null) < limit.min()) {
                    return false;
                }
            }
            for (var layerEntry : layerLimits.entrySet()) {
                int layer = layerEntry.getKey();
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
                    capacity = Math.min(capacity, limit.max() - globalCount(limit.key()));
                }
                if (limit.layerMax() >= 0) {
                    capacity = Math.min(capacity, limit.layerMax() - layerCount(layer, limit.key()));
                }
            }
            return Math.max(0, capacity);
        }

        private int remainingContributors(List<VariableGroup> groups, int groupIndex, Object key,
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

        private int globalCount(Object key) {
            return globalCounts.getOrDefault(key, 0);
        }

        private int layerCount(int layer, Object key) {
            return layerCounts.getOrDefault(layer, new IdentityHashMap<>()).getOrDefault(key, 0);
        }

        private SearchState stateKey(int groupIndex, SearchBudget budget) {
            List<Integer> global = orderedLimits.stream().map(limit -> globalCount(limit.key())).toList();
            List<Integer> layer = orderedLayerLimits.stream()
                    .map(limit -> layerCount(limit.layer(), limit.limit().key()))
                    .toList();
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
                                    List<ResourceLocation> tierGroups, List<String> constraintIds) {}

        private record GroupKey(int layer, @Nullable BlockState observedState,
                                @Nullable Direction requiredDirection, boolean controller,
                                List<CandidateKey> candidates) {}

        private record VariableGroup(List<Integer> cellIndices, UnresolvedCell representative) {

            private boolean hasConstraints() {
                return representative.candidates().stream().anyMatch(candidate -> !candidate.limits().isEmpty());
            }

            private int constraintCount() {
                return representative.candidates().stream().mapToInt(candidate -> candidate.limits().size()).max()
                        .orElse(0);
            }
        }

        private record LayerLimit(int layer, ConstraintLimit limit) {}

        private record SearchState(int groupIndex, List<Integer> globalCounts, List<Integer> layerCounts,
                                   List<Long> materialRemaining) {}

        private record BudgetUse(List<Long> consumedBySource, long sourceCost) {

            private static final BudgetUse EMPTY = new BudgetUse(List.of(), 0);
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

            private final List<Map<AutoBuildItemKey, Long>> remaining;
            private final boolean unlimited;

            private SearchBudget(List<Map<AutoBuildItemKey, Long>> remaining, boolean unlimited) {
                this.remaining = remaining;
                this.unlimited = unlimited;
            }

            private int rank(@Nullable AutoBuildItemKey key) {
                if (key == null || unlimited) return 0;
                for (int index = 0; index < remaining.size(); index++) {
                    if (remaining.get(index).getOrDefault(key, 0L) > 0) return index;
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
                ArrayList<Long> consumed = new ArrayList<>(remaining.size());
                for (int index = 0; index < remaining.size(); index++) {
                    Map<AutoBuildItemKey, Long> source = remaining.get(index);
                    long available = source.getOrDefault(key, 0L);
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
                return new BudgetUse(List.copyOf(consumed), sourceCost);
            }

            private void restore(@Nullable AutoBuildItemKey key, BudgetUse use) {
                if (key == null || unlimited) return;
                for (int index = 0; index < use.consumedBySource().size(); index++) {
                    long amount = use.consumedBySource().get(index);
                    if (amount > 0) remaining.get(index).merge(key, amount, Math::addExact);
                }
            }

            private List<Long> values(List<AutoBuildItemKey> keys) {
                ArrayList<Long> values = new ArrayList<>(remaining.size() * keys.size());
                for (Map<AutoBuildItemKey, Long> source : remaining) {
                    for (AutoBuildItemKey key : keys) {
                        values.add(source.getOrDefault(key, 0L));
                    }
                }
                return List.copyOf(values);
            }
        }
    }

    private static final class MaterialAllocation {

        private final List<Map<AutoBuildItemKey, Long>> remaining;
        private final List<Map<AutoBuildItemKey, Long>> allocated;
        private final List<AutoBuildMaterialSource.Snapshot> snapshots;
        private final List<Long> sourceFingerprints;
        private final List<AutoBuildProblem> structureProblems;
        private final boolean unlimited;
        private final Map<BlockPos, BlockState> virtualWorld = new HashMap<>();

        private MaterialAllocation(List<Map<AutoBuildItemKey, Long>> remaining,
                                   List<Map<AutoBuildItemKey, Long>> allocated,
                                   List<AutoBuildMaterialSource.Snapshot> snapshots,
                                   List<Long> sourceFingerprints,
                                   List<AutoBuildProblem> structureProblems,
                                   boolean unlimited) {
            this.remaining = remaining;
            this.allocated = allocated;
            this.snapshots = snapshots;
            this.sourceFingerprints = sourceFingerprints;
            this.structureProblems = structureProblems;
            this.unlimited = unlimited;
        }

        private static MaterialAllocation create(List<AutoBuildMaterialSource> sources,
                                                 Collection<AutoBuildItemKey> keys,
                                                 ResolutionContext context,
                                                 List<AutoBuildProblem> problems,
                                                 boolean hasBuild,
                                                 boolean requireME) {
            boolean creative = context.player() != null && context.player().isCreative();
            if (!hasBuild || context.unlimitedMaterials() || creative && !requireME) {
                return new MaterialAllocation(List.of(), List.of(), List.of(), List.of(), List.of(), true);
            }
            ArrayList<Map<AutoBuildItemKey, Long>> remaining = new ArrayList<>();
            ArrayList<Map<AutoBuildItemKey, Long>> allocated = new ArrayList<>();
            ArrayList<AutoBuildMaterialSource.Snapshot> snapshots = new ArrayList<>();
            ArrayList<Long> sourceFingerprints = new ArrayList<>();
            ArrayList<AutoBuildProblem> structureProblems = new ArrayList<>();
            IdentityHashMap<AutoBuildMaterialSource, Set<ProblemIdentity>> reportedProblems = new IdentityHashMap<>();
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
                    if (reportedProblems.computeIfAbsent(source, ignored -> new HashSet<>()).add(identity)) {
                        if (requireME && isStrictAEProblem(snapshotProblem.type()) &&
                                (source.kind() == AutoBuildMaterialSource.SourceKind.ME ||
                                        source.kind() == AutoBuildMaterialSource.SourceKind.UNAVAILABLE)) {
                            problems.add(snapshotProblem);
                        } else {
                            structureProblems.add(snapshotProblem);
                        }
                    }
                }
                remaining.add(new LinkedHashMap<>(snapshot.available()));
                allocated.add(new LinkedHashMap<>());
            }
            return new MaterialAllocation(remaining, allocated, List.copyOf(snapshots),
                    List.copyOf(sourceFingerprints), List.copyOf(structureProblems), creative);
        }

        private record ProblemIdentity(AutoBuildProblem.Type type, @Nullable BlockPos pos) {}

        private @Nullable PlannedCell commitStructure(List<PlannedCell> cells) {
            if (unlimited) return null;
            List<Map<AutoBuildItemKey, Long>> working = copyRemaining();
            ArrayList<Map<AutoBuildItemKey, Long>> stagedAllocations = new ArrayList<>(allocated.size());
            for (int index = 0; index < allocated.size(); index++) {
                stagedAllocations.add(new LinkedHashMap<>());
            }
            for (PlannedCell cell : cells) {
                if (cell.action() != CellAction.PLACE || cell.materialKey() == null) continue;
                AutoBuildItemKey key = cell.materialKey();
                boolean found = false;
                for (int index = 0; index < working.size(); index++) {
                    Map<AutoBuildItemKey, Long> source = working.get(index);
                    long available = source.getOrDefault(key, 0L);
                    if (available <= 0) continue;
                    source.put(key, available - 1);
                    stagedAllocations.get(index).merge(key, 1L, Math::addExact);
                    found = true;
                    break;
                }
                if (!found) return cell;
            }
            for (int index = 0; index < remaining.size(); index++) {
                remaining.get(index).clear();
                remaining.get(index).putAll(working.get(index));
                Map<AutoBuildItemKey, Long> target = allocated.get(index);
                stagedAllocations.get(index).forEach((key, count) -> target.merge(key, count, Math::addExact));
            }
            return null;
        }

        private List<AutoBuildProblem> structureProblems() {
            return structureProblems;
        }

        private List<AutoBuildMaterialSource.Snapshot> snapshots() {
            return snapshots;
        }

        private List<Long> sourceFingerprints() {
            return sourceFingerprints;
        }

        private Map<AutoBuildItemKey, ResolvedAutoBuildSnapshot.MaterialAllocation> resolveAllocations(
                                                                                                       List<ResolvedStructurePlan> demolitions,
                                                                                                       List<ResolvedStructurePlan> builds) {
            LinkedHashMap<AutoBuildItemKey, Long> required = new LinkedHashMap<>();
            for (ResolvedStructurePlan plan : concat(demolitions, builds)) {
                plan.materials().forEach((key, count) -> required.merge(key, count, Math::addExact));
            }
            LinkedHashMap<AutoBuildItemKey, ResolvedAutoBuildSnapshot.MaterialAllocation> result = new LinkedHashMap<>();
            required.forEach((key, count) -> {
                ArrayList<Long> bySource = new ArrayList<>(allocated.size());
                long assigned = 0;
                for (Map<AutoBuildItemKey, Long> source : allocated) {
                    long value = source.getOrDefault(key, 0L);
                    bySource.add(value);
                    assigned = Math.addExact(assigned, value);
                }
                long missing = unlimited ? 0 : count - assigned;
                result.put(key, new ResolvedAutoBuildSnapshot.MaterialAllocation(count, bySource, missing,
                        unlimited));
            });
            return Collections.unmodifiableMap(result);
        }

        private List<Map<AutoBuildItemKey, Long>> copyRemaining() {
            ArrayList<Map<AutoBuildItemKey, Long>> copy = new ArrayList<>(remaining.size());
            for (Map<AutoBuildItemKey, Long> source : remaining) {
                copy.add(new LinkedHashMap<>(source));
            }
            return copy;
        }

        private List<UnresolvedCell> applyVirtualWorld(List<UnresolvedCell> cells) {
            return cells.stream()
                    .map(cell -> virtualWorld.containsKey(cell.relativePos()) ?
                            cell.withObservedState(virtualWorld.get(cell.relativePos())) : cell)
                    .toList();
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
