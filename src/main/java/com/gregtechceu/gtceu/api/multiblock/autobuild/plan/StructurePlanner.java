package com.gregtechceu.gtceu.api.multiblock.autobuild.plan;

import com.gregtechceu.gtceu.api.machine.MultiblockMachineDefinition;
import com.gregtechceu.gtceu.api.multiblock.autobuild.BuildProblem;
import com.gregtechceu.gtceu.api.multiblock.autobuild.MaterialKey;
import com.gregtechceu.gtceu.api.multiblock.autobuild.batch.BuildPolicy;
import com.gregtechceu.gtceu.api.multiblock.autobuild.batch.StructureOperation;
import com.gregtechceu.gtceu.api.multiblock.pattern.match.MultiBlockPattern;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;

import it.unimi.dsi.fastutil.objects.Object2LongLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2LongMap;
import it.unimi.dsi.fastutil.objects.Object2LongMaps;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectImmutableList;
import it.unimi.dsi.fastutil.objects.ObjectList;
import org.jspecify.annotations.Nullable;

import java.util.Objects;

import static com.gregtechceu.gtceu.api.multiblock.autobuild.plan.BatchPlanner.*;
import static com.gregtechceu.gtceu.api.multiblock.autobuild.plan.CandidateExpansion.*;

public final class StructurePlanner {

    public static StructurePlan resolveStructure(MultiblockMachineDefinition definition,
                                                 PendingStructure pending,
                                                 BuildPolicy sharedOptions,
                                                 ResolutionContext context,
                                                 MaterialAllocation materials) {
        ObjectArrayList<BuildProblem> problems = new ObjectArrayList<>(pending.problems());
        // World access failures block execution without discarding the requested preview geometry.
        if (problems.stream().anyMatch(BuildProblem::preventsPreview)) {
            return new StructurePlan(pending.options().structureName(), pending.options().mode(),
                    pending.repetitions(), pending.options().flipMode(), ObjectList.of(), Object2LongMaps.emptyMap(),
                    problems);
        }
        boolean buildMode = pending.options().mode() == StructureOperation.BUILD;
        CandidateSearch solver = new CandidateSearch(
                sharedOptions.noHatchMode() && buildMode, materials, buildMode, context.canonical());
        ObjectList<UnresolvedCell> observedCells = materials.applyVirtualWorld(pending.cells());
        int worldProblems = problems.size();
        ObjectList<SelectedCell> selected = solver.solve(observedCells, pending.requirements(), problems);
        if (problems.size() != worldProblems) {
            return new StructurePlan(pending.options().structureName(), pending.options().mode(),
                    pending.repetitions(), pending.options().flipMode(), ObjectList.of(), Object2LongMaps.emptyMap(),
                    problems);
        }

        ObjectArrayList<PlannedCell> planned = new ObjectArrayList<>(selected.size());
        Object2LongMap<MaterialKey> materialCounts = new Object2LongLinkedOpenHashMap<>();
        for (int index = 0; index < selected.size(); index++) {
            SelectedCell selectedCell = selected.get(index);
            UnresolvedCell cell = selectedCell.cell();
            ResolvedCandidate candidate = selectedCell.candidate();
            BlockState observed = cell.observedState();
            CellAction action = resolveAction(definition, pending.options().mode(), cell, candidate,
                    observed, context);
            BlockState targetState = candidate.state();
            if (observed != null && (buildMode || action == CellAction.CONTROLLER)) {
                if (action == CellAction.KEEP || action == CellAction.CONTROLLER) {
                    targetState = observed;
                } else if (action == CellAction.UPDATE_DIRECTION && cell.requiredDirection() != null) {
                    targetState = MultiBlockPattern.applyDirectionalState(observed, cell.requiredDirection());
                }
            }
            ObjectList<MaterialKey> candidateKeys = cell.candidates().stream().map(ResolvedCandidate::materialKey)
                    .filter(Objects::nonNull).distinct().collect(ObjectImmutableList.toList());
            ObjectList<BlockState> stateCandidates = cell.candidates().stream()
                    .filter(candidateState -> !candidateState.any())
                    .map(ResolvedCandidate::state).distinct().collect(ObjectImmutableList.toList());
            MaterialKey materialKey = candidate.materialKey();
            if (action == CellAction.PLACE && materialKey != null) {
                materialCounts.mergeLong(materialKey, 1L, Math::addExact);
            }
            planned.add(new PlannedCell(cell.structureName(), cell.cellKey(), cell.relativePos(), cell.worldPos(),
                    targetState, observed, candidate.blockInfo().withState(targetState), action, cell.predicate(),
                    stateCandidates,
                    cell.requiredDirection(), candidateKeys, materialKey, candidate.tierGroups(),
                    candidate.limits().stream().map(ConstraintLimit::id).collect(ObjectImmutableList.toList()),
                    observedCells.get(index).candidates().stream().anyMatch(ResolvedCandidate::hatch)));
        }
        PlannedCell missingCell = buildMode && worldProblems == 0 ? materials.commitStructure(planned) : null;
        if (missingCell != null) {
            problems.add(problem(missingCell.worldPos(),
                    "gtpm.multiblock.autobuild.missing_material", position(missingCell.worldPos())));
        }
        StructurePlan result = new StructurePlan(pending.options().structureName(),
                pending.options().mode(),
                pending.repetitions(), pending.options().flipMode(), planned, materialCounts, problems);
        materials.applyPlan(result);
        return result;
    }

    public static CellAction resolveAction(MultiblockMachineDefinition definition, StructureOperation mode,
                                           UnresolvedCell cell, ResolvedCandidate candidate,
                                           @Nullable BlockState observed, ResolutionContext context) {
        if (candidate.any()) return CellAction.IGNORE_ANY;
        boolean controllerCell = cell.relativePos().equals(BlockPos.ZERO) &&
                candidate.state().is(definition.getBlock());
        if (controllerCell) return CellAction.CONTROLLER;
        if (mode == StructureOperation.DEMOLISH) {
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

    public static boolean matchesCandidateState(UnresolvedCell cell, BlockState observed) {
        for (ResolvedCandidate candidate : cell.candidates()) {
            if (candidate.any() || candidate.state().isAir()) continue;
            if (observed.equals(candidate.state())) {
                return true;
            }
        }
        return false;
    }

    public static boolean matchesCandidateDomain(UnresolvedCell cell, ResolvedCandidate candidate,
                                                 BlockState observed) {
        return observed.equals(candidate.state()) &&
                (cell.requiredDirection() == null ||
                        MultiBlockPattern.matchesDirectionalState(observed, cell.requiredDirection()));
    }

    public static boolean canUpdateDirection(UnresolvedCell cell, ResolvedCandidate candidate) {
        BlockState observed = cell.observedState();
        Direction requiredDirection = cell.requiredDirection();
        if (observed == null || requiredDirection == null || !observed.is(candidate.state().getBlock())) {
            return false;
        }
        BlockState corrected = MultiBlockPattern.applyDirectionalState(observed, requiredDirection);
        return !observed.equals(corrected) && corrected.equals(candidate.state());
    }
}
