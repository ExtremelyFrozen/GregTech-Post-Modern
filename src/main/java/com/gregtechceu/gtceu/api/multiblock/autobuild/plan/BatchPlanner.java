package com.gregtechceu.gtceu.api.multiblock.autobuild.plan;

import com.gregtechceu.gtceu.GTCEu;
import com.gregtechceu.gtceu.api.machine.MultiblockMachineDefinition;
import com.gregtechceu.gtceu.api.machine.multiblock.MultiblockControllerMachine;
import com.gregtechceu.gtceu.api.multiblock.autobuild.BuildProblem;
import com.gregtechceu.gtceu.api.multiblock.autobuild.MaterialKey;
import com.gregtechceu.gtceu.api.multiblock.autobuild.batch.BatchRequest;
import com.gregtechceu.gtceu.api.multiblock.autobuild.batch.BuildPolicy;
import com.gregtechceu.gtceu.api.multiblock.autobuild.batch.StructureOperation;
import com.gregtechceu.gtceu.api.multiblock.autobuild.batch.StructureRequest;
import com.gregtechceu.gtceu.api.multiblock.pattern.layout.PatternLayoutCell;
import com.gregtechceu.gtceu.api.multiblock.pattern.match.MultiBlockPattern;
import com.gregtechceu.gtceu.data.pattern.StructurePatternRegistry;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.objects.Object2IntLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2LongMaps;
import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectMaps;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectLinkedOpenHashSet;
import it.unimi.dsi.fastutil.objects.ObjectList;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import it.unimi.dsi.fastutil.objects.ObjectSet;
import org.jspecify.annotations.Nullable;

import java.util.Comparator;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static com.gregtechceu.gtceu.api.multiblock.autobuild.plan.CandidateExpansion.*;
import static com.gregtechceu.gtceu.api.multiblock.autobuild.plan.PlanAssembly.*;
import static com.gregtechceu.gtceu.api.multiblock.autobuild.plan.StructurePlanner.*;

/**
 * Resolves multiblock patterns into one deterministic session plan used by preview and execution.
 *
 * Candidate expansion, search, material allocation and assembly have separate domain owners.
 */
public final class BatchPlanner {

    public static final Direction CANONICAL_FRONT = Direction.NORTH;
    public static final Direction CANONICAL_UP = Direction.NORTH;

    private static final ConcurrentMap<CanonicalKey, BatchPlan> CANONICAL_PLANS = new ConcurrentHashMap<>();

    static {
        StructurePatternRegistry.addReloadListener(CANONICAL_PLANS::clear);
    }

    /**
     * Resolves an XEI-style plan against an empty world and unlimited representative materials.
     */
    public BatchPlan resolveCanonical(MultiblockMachineDefinition definition, BatchRequest request) {
        return CANONICAL_PLANS.computeIfAbsent(
                new CanonicalKey(definition, request.structures(), request.sharedOptions(),
                        StructurePatternRegistry.generation()),
                key -> resolve(definition, request,
                        new ResolutionContext(null, null, null, BlockPos.ZERO, CANONICAL_FRONT,
                                CANONICAL_UP, true, true))
                        .plan());
    }

    /**
     * Creates the persisted first-use defaults for one structure from its current pattern definition.
     */
    public StructureRequest defaultStructureOptions(MultiblockMachineDefinition definition,
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
        return new StructureRequest(structureName, repetitions, tiers, false, StructureOperation.BUILD, choices);
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
                                                                                          StructureRequest options) {
        var layout = definition.getPattern(options.structureName()).expandLayout(options.repetitions().toIntArray(),
                options.choices());
        return tierChoicesForPredicates(definition, layout.cells().stream().map(PatternLayoutCell::predicate).distinct()
                .collect(ObjectArrayList.toList()));
    }

    /**
     * Resolves a controller-oriented preview without requiring a player or reading material storage.
     */
    public BatchPlan resolveControllerPreview(MultiblockControllerMachine controller,
                                              BatchRequest request) {
        return resolve(controller.getDefinition(), request,
                new ResolutionContext(controller, null, controller.getLevel(), controller.getBlockPos(),
                        controller.getFrontFacing(), controller.getUpwardsFacing(), false, true))
                .plan();
    }

    /**
     * Resolves a server-authoritative plan against the controller's current world and material availability.
     */
    public BatchPlan resolveWorld(MultiblockControllerMachine controller, ServerPlayer player,
                                  BatchRequest request) {
        return resolveBatch(controller, player, request).plan();
    }

    /**
     * Resolves a server-authoritative plan and exposes the exact material availability and assignments it consumed.
     */
    public BatchResolution resolveBatch(MultiblockControllerMachine controller,
                                        ServerPlayer player,
                                        BatchRequest request) {
        if (controller.getLevel() != player.level()) {
            BuildProblem problem = problem(controller.getBlockPos(),
                    "gtpm.multiblock.autobuild.invalid_target_level");
            return sharedFailure(request, problem);
        }
        return resolve(controller.getDefinition(), request,
                new ResolutionContext(controller, player, player.level(), controller.getBlockPos(),
                        controller.getFrontFacing(), controller.getUpwardsFacing(), false, false));
    }

    private BatchResolution resolve(MultiblockMachineDefinition definition,
                                    BatchRequest request,
                                    ResolutionContext context) {
        try {
            return resolveChecked(definition, request, context);
        } catch (ArithmeticException exception) {
            GTCEu.LOGGER.error("Automatic-build material count overflow for {}", definition.getId(), exception);
            return sharedFailure(request, problem(context.worldPos(), "gtpm.multiblock.autobuild.count_overflow"));
        }
    }

    private BatchResolution resolveChecked(MultiblockMachineDefinition definition,
                                           BatchRequest request,
                                           ResolutionContext context) {
        Object2IntMap<String> order = structureOrder(definition);
        ObjectArrayList<PendingStructure> pending = new ObjectArrayList<>(request.structures().size());
        ObjectArrayList<BuildProblem> sharedProblems = new ObjectArrayList<>();

        ObjectSet<String> requestedBuilds = new ObjectOpenHashSet<>();
        ObjectSet<String> requestedDemolitions = new ObjectOpenHashSet<>();
        for (StructureRequest options : request.structures()) {
            (options.mode() == StructureOperation.BUILD ? requestedBuilds : requestedDemolitions)
                    .add(options.structureName());
        }

        ConstraintIds constraintIds = new ConstraintIds();
        for (StructureRequest options : request.structures()) {
            ObjectArrayList<BuildProblem> problems = new ObjectArrayList<>();
            MultiBlockPattern pattern = null;
            IntList repetitions = IntList.of();
            if (!order.containsKey(options.structureName())) {
                problems.add(problem(context.worldPos(),
                        "gtpm.multiblock.autobuild.unknown_structure", options.structureName()));
            } else {
                pattern = definition.getPattern(options.structureName());
                repetitions = options.repetitions();
                if (options.flipMode() && !definition.isAllowFlip()) {
                    problems.add(problem(context.worldPos(),
                            "gtpm.multiblock.autobuild.flip_not_allowed"));
                }
                validateDependencies(definition, options, requestedBuilds, requestedDemolitions, problems,
                        context.worldPos());
            }

            ObjectList<UnresolvedCell> cells = ObjectList.of();
            ObjectList<ConstraintLimit> requirements = new ObjectArrayList<>();
            if (pattern != null && problems.isEmpty()) {
                cells = expandStructure(definition, pattern, options, context, constraintIds, requirements,
                        problems);
                validateTierChoices(definition, options, cells, problems, context.worldPos());
            }
            pending.add(new PendingStructure(options, repetitions, cells, requirements, problems));
        }

        pending.sort(executionComparator(order));
        ObjectLinkedOpenHashSet<MaterialKey> materialKeys = new ObjectLinkedOpenHashSet<>();
        for (PendingStructure structure : pending) {
            if (structure.options().mode() == StructureOperation.BUILD) {
                structure.cells().stream().flatMap(cell -> cell.candidates().stream())
                        .map(ResolvedCandidate::materialKey)
                        .filter(Objects::nonNull)
                        .forEach(materialKeys::add);
            }
        }
        boolean hasBuild = pending.stream().anyMatch(
                structure -> structure.options().mode() == StructureOperation.BUILD);
        MaterialAllocation materials = MaterialAllocation.create(request.materialSources(), materialKeys, context,
                hasBuild);

        ObjectArrayList<StructurePlan> demolitionPlans = new ObjectArrayList<>();
        ObjectArrayList<StructurePlan> buildPlans = new ObjectArrayList<>();
        for (PendingStructure structure : pending) {
            StructurePlan plan = resolveStructure(definition, structure, request.sharedOptions(), context,
                    materials);
            if (structure.options().mode() == StructureOperation.DEMOLISH) {
                demolitionPlans.add(plan);
            } else {
                buildPlans.add(plan);
            }
        }

        Object2ObjectLinkedOpenHashMap<BlockPos, MergedPlanCell> merged = mergeCells(demolitionPlans, buildPlans);
        Object2ObjectMap<MaterialKey, BatchResolution.MaterialAllocation> allocations = materials
                .resolveAllocations(demolitionPlans, buildPlans);
        long fingerprint = fingerprint(demolitionPlans, buildPlans, merged, materials.sourceFingerprints(),
                allocations);
        BatchPlan plan = new BatchPlan(demolitionPlans, buildPlans, merged, sharedProblems, fingerprint);
        return new BatchResolution(plan, allocations);
    }

    private static BatchResolution sharedFailure(BatchRequest request,
                                                 BuildProblem problem) {
        ObjectArrayList<StructurePlan> demolitions = new ObjectArrayList<>();
        ObjectArrayList<StructurePlan> builds = new ObjectArrayList<>();
        for (StructureRequest options : request.structures()) {
            StructurePlan plan = new StructurePlan(options.structureName(), options.mode(),
                    options.repetitions(), options.flipMode(), ObjectList.of(), Object2LongMaps.emptyMap(),
                    ObjectList.of(problem));
            (options.mode() == StructureOperation.DEMOLISH ? demolitions : builds).add(plan);
        }
        BatchPlan plan = new BatchPlan(demolitions, builds, Object2ObjectMaps.emptyMap(),
                ObjectList.of(problem), FINGERPRINT_OFFSET);
        return new BatchResolution(plan, Object2ObjectMaps.emptyMap());
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
        return Comparator.<PendingStructure, StructureOperation>comparing(pending -> pending.options().mode(),
                Comparator.comparingInt(mode -> mode == StructureOperation.DEMOLISH ? 0 : 1))
                .thenComparingInt(pending -> {
                    int index = order.getOrDefault(pending.options().structureName(), Integer.MAX_VALUE);
                    return pending.options().mode() == StructureOperation.DEMOLISH ? -index : index;
                });
    }

    private static void validateDependencies(MultiblockMachineDefinition definition,
                                             StructureRequest options,
                                             ObjectSet<String> requestedBuilds,
                                             ObjectSet<String> requestedDemolitions,
                                             ObjectList<BuildProblem> problems,
                                             @Nullable BlockPos pos) {
        if (options.mode() != StructureOperation.BUILD) return;
        for (String dependency : definition.getRequiredStructures(options.structureName())) {
            if (!requestedBuilds.contains(dependency)) {
                problems.add(problem(pos,
                        "gtpm.multiblock.autobuild.missing_dependency", options.structureName(), dependency));
            }
            if (requestedDemolitions.contains(dependency)) {
                problems.add(problem(pos,
                        "gtpm.multiblock.autobuild.dependency_mode_conflict", dependency));
            }
        }
    }

    public static BuildProblem problem(@Nullable BlockPos pos,
                                       String translationKey, Object... args) {
        return new BuildProblem(pos, Component.translatable(translationKey, args));
    }

    public static String position(@Nullable BlockPos pos) {
        return pos == null ? "?" : pos.toShortString();
    }

    public record ResolutionContext(@Nullable MultiblockControllerMachine controller,
                                    @Nullable ServerPlayer player, @Nullable Level level, BlockPos origin,
                                    Direction frontFacing, Direction upwardsFacing, boolean canonical,
                                    boolean unlimitedMaterials) {

        @Nullable
        public BlockPos worldPos() {
            return canonical ? null : origin;
        }
    }

    private record CanonicalKey(MultiblockMachineDefinition definition, ObjectList<StructureRequest> structures,
                                BuildPolicy policy, long generation) {}

    public record PendingStructure(StructureRequest options, IntList repetitions,
                                   ObjectList<UnresolvedCell> cells, ObjectList<ConstraintLimit> requirements,
                                   ObjectList<BuildProblem> problems) {}
}
