package com.gregtechceu.gtceu.api.multiblock.autobuild.plan;

import com.gregtechceu.gtceu.api.multiblock.autobuild.BuildProblem;
import com.gregtechceu.gtceu.api.multiblock.autobuild.MaterialKey;
import com.gregtechceu.gtceu.api.multiblock.pattern.constraint.PropertyBindings;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.state.BlockState;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntImmutableList;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongImmutableList;
import it.unimi.dsi.fastutil.longs.LongList;
import it.unimi.dsi.fastutil.objects.Object2IntLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2LongMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectImmutableList;
import it.unimi.dsi.fastutil.objects.ObjectLinkedOpenHashSet;
import it.unimi.dsi.fastutil.objects.ObjectList;
import it.unimi.dsi.fastutil.objects.Reference2IntMap;
import it.unimi.dsi.fastutil.objects.Reference2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import it.unimi.dsi.fastutil.objects.ReferenceSet;
import it.unimi.dsi.fastutil.objects.ReferenceSets;
import org.jspecify.annotations.Nullable;

import java.util.Arrays;
import java.util.Comparator;

import static com.gregtechceu.gtceu.api.multiblock.autobuild.plan.BatchPlanner.*;
import static com.gregtechceu.gtceu.api.multiblock.autobuild.plan.CandidateExpansion.*;
import static com.gregtechceu.gtceu.api.multiblock.autobuild.plan.StructurePlanner.*;

public final class CandidateSearch {

    public final boolean noHatchMode;
    public final MaterialAllocation materials;
    public final boolean consumeMaterials;
    public final boolean canonical;
    public final PropertyBindings properties = new PropertyBindings();
    public final Reference2IntMap<Object> globalCounts = new Reference2IntOpenHashMap<>();
    public final Int2ObjectMap<Reference2IntMap<Object>> layerCounts = new Int2ObjectOpenHashMap<>();
    public final Reference2ObjectOpenHashMap<Object, ConstraintLimit> limits = new Reference2ObjectOpenHashMap<>();
    public final Int2ObjectMap<Reference2ObjectOpenHashMap<Object, ConstraintLimit>> layerLimits = new Int2ObjectOpenHashMap<>();
    public final Object2ObjectMap<SearchState, SelectionScore> visitedStates = new Object2ObjectOpenHashMap<>();
    public ObjectList<ConstraintLimit> orderedLimits = ObjectList.of();
    public ObjectList<LayerLimit> orderedLayerLimits = ObjectList.of();
    public ObjectList<MaterialKey> budgetKeys = ObjectList.of();
    @Nullable
    public SelectionScore bestScore;
    public int @Nullable [][] bestGroupCounts;

    public CandidateSearch(boolean noHatchMode, MaterialAllocation materials,
                           boolean consumeMaterials, boolean canonical) {
        this.noHatchMode = noHatchMode;
        this.materials = materials;
        this.consumeMaterials = consumeMaterials;
        this.canonical = canonical;
    }

    public ObjectList<SelectedCell> solve(ObjectList<UnresolvedCell> cells,
                                          ObjectList<ConstraintLimit> requirements,
                                          ObjectList<BuildProblem> problems) {
        ReferenceSet<Object> excludedLimits = noHatchMode ? new ReferenceOpenHashSet<>() : ReferenceSets.emptySet();
        if (noHatchMode) cells = filterHatches(cells, excludedLimits);
        registerLimits(cells, requirements, excludedLimits);
        ObjectList<VariableGroup> groups = groupCells(cells);
        int[][] groupCounts = new int[groups.size()][];
        for (int index = 0; index < groups.size(); index++) {
            groupCounts[index] = new int[groups.get(index).representative().candidates().size()];
        }
        SearchBudget budget = new SearchBudget(materials.copyRemaining(), materials.unlimited);
        search(groups, 0, budget, SelectionScore.ZERO, groupCounts);
        int[][] selectedCounts = bestGroupCounts;
        if (selectedCounts == null) {
            problems.add(problem(BuildProblem.Type.INVALID_OPTIONS, null,
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

    private static ObjectList<UnresolvedCell> filterHatches(ObjectList<UnresolvedCell> cells,
                                                            ReferenceSet<Object> excludedLimits) {
        return cells.stream().map(cell -> {
            ObjectList<ResolvedCandidate> candidates = new ObjectArrayList<>();
            for (ResolvedCandidate candidate : cell.candidates()) {
                if (candidate.hatch()) {
                    for (ConstraintLimit limit : candidate.limits()) excludedLimits.add(limit.key());
                } else {
                    candidates.add(candidate);
                }
            }
            // Skip positions with no non-hatch candidates without clearing occupied blocks.
            if (candidates.isEmpty()) candidates.add(ResolvedCandidate.wildcard());
            return cell.withCandidates(candidates);
        }).collect(ObjectImmutableList.toList());
    }

    public int[] projectAssignments(ObjectList<UnresolvedCell> cells, ObjectList<VariableGroup> groups,
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
            applyCandidate(cell, cell.candidates().get(candidateIndex), 1, budget);
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

    public void registerLimits(ObjectList<UnresolvedCell> cells, ObjectList<ConstraintLimit> requirements,
                               ReferenceSet<Object> excludedLimits) {
        ObjectLinkedOpenHashSet<MaterialKey> encounteredKeys = new ObjectLinkedOpenHashSet<>();
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
        // Shared casing constraints remain; requirements supplied only by removed hatches do not.
        for (ConstraintLimit requirement : requirements) {
            if (!excludedLimits.contains(requirement.key()) || limits.containsKey(requirement.key())) {
                limits.put(requirement.key(), requirement);
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

    public ObjectList<VariableGroup> groupCells(ObjectList<UnresolvedCell> cells) {
        Object2ObjectLinkedOpenHashMap<GroupKey, IntArrayList> grouped = new Object2ObjectLinkedOpenHashMap<>();
        for (int index = 0; index < cells.size(); index++) {
            UnresolvedCell cell = cells.get(index);
            ObjectList<CandidateKey> candidates = cell.candidates().stream()
                    .map(candidate -> new CandidateKey(candidate.state(), candidate.materialKey(),
                            candidate.blockInfo().hasBlockEntity(), candidate.hatch(), candidate.any(),
                            candidate.order(), candidate.tierGroups(), candidate.limits().stream()
                                    .map(ConstraintLimit::id).sorted().collect(ObjectImmutableList.toList()),
                            candidate.observation().properties()))
                    .collect(ObjectImmutableList.toList());
            // Layer identity matters only where a rule actually constrains a layer's count.
            GroupKey key = new GroupKey(layerLimits.containsKey(cell.layer()) ? cell.layer() : -1,
                    cell.observedState(), cell.requiredDirection(),
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

    public boolean search(ObjectList<VariableGroup> groups, int groupIndex, SearchBudget budget,
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
        SearchState stateKey = stateKey(groupIndex, -1, 0, budget);
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

    public boolean searchGreedyGroup(ObjectList<VariableGroup> groups, int groupIndex, SearchBudget budget,
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
            groupCounts[groupIndex][candidateIndex]++;
            applied.add(choice);
            nextScore = nextScore.add(choice.score());
        }
        boolean found = search(groups, groupIndex + 1, budget, nextScore, groupCounts);
        undoApplied(cell, applied, budget, groupCounts[groupIndex]);
        return found;
    }

    public boolean enumerateGroup(ObjectList<VariableGroup> groups, int groupIndex, SearchBudget budget,
                                  SelectionScore score, int[][] groupCounts, IntList candidateOrder,
                                  int candidateCursor, int remainingCells) {
        if (remainingCells == 0) {
            return search(groups, groupIndex + 1, budget, score, groupCounts);
        }
        if (candidateCursor == candidateOrder.size()) {
            return false;
        }
        UnresolvedCell cell = groups.get(groupIndex).representative();
        if (availableCells(cell, candidateOrder, candidateCursor, remainingCells, null) < remainingCells) return false;
        for (ConstraintLimit limit : orderedLimits) {
            int needed = limit.min() - globalCounts.getInt(limit.key());
            if (needed > 0 && availableCells(cell, candidateOrder, candidateCursor, remainingCells, limit.key()) +
                    remainingContributors(groups, groupIndex + 1, limit.key(), null) < needed)
                return false;
        }
        for (LayerLimit layerLimit : orderedLayerLimits) {
            ConstraintLimit limit = layerLimit.limit();
            int needed = limit.layerMin() - layerCount(layerLimit.layer(), limit.key());
            if (needed <= 0) continue;
            int available = layerLimit.layer() == cell.layer() ?
                    availableCells(cell, candidateOrder, candidateCursor, remainingCells, limit.key()) : 0;
            if (available + remainingContributors(groups, groupIndex + 1, limit.key(), layerLimit.layer()) < needed)
                return false;
        }
        if (bestScore != null) {
            SelectionScore minimum = null;
            for (int cursor = candidateCursor; cursor < candidateOrder.size(); cursor++) {
                ResolvedCandidate candidate = cell.candidates().get(candidateOrder.getInt(cursor));
                if (!withinMaximums(cell.layer(), candidate) ||
                        !properties.accepts(candidate.observation().properties()))
                    continue;
                SelectionScore cost = optimisticCandidateScore(cell, candidate, budget);
                if (minimum == null || cost.compareTo(minimum) < 0) minimum = cost;
            }
            if (minimum == null || !canBeatBest(groups, groupIndex + 1, budget,
                    score.add(minimum.times(remainingCells))))
                return false;
        }
        SearchState stateKey = stateKey(groupIndex, candidateCursor, remainingCells, budget);
        SelectionScore previous = visitedStates.get(stateKey);
        if (previous != null && previous.compareTo(score) <= 0) return false;
        visitedStates.put(stateKey, score);

        int candidateIndex = candidateOrder.getInt(candidateCursor);
        ResolvedCandidate candidate = cell.candidates().get(candidateIndex);
        int maximum = properties.accepts(candidate.observation().properties()) ?
                candidateCapacity(cell.layer(), candidate, remainingCells) : 0;
        int minimum = candidateCursor + 1 == candidateOrder.size() ? remainingCells : 0;
        boolean found = false;
        for (int count = maximum; count >= minimum; count--) {
            AppliedCandidate applied = applyCandidate(cell, candidate, count, budget);
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

    public int availableCells(UnresolvedCell cell, IntList candidateOrder, int candidateCursor,
                              int remainingCells, @Nullable Object key) {
        int available = 0;
        for (int cursor = candidateCursor; cursor < candidateOrder.size(); cursor++) {
            ResolvedCandidate candidate = cell.candidates().get(candidateOrder.getInt(cursor));
            if (!properties.accepts(candidate.observation().properties()) || key != null &&
                    candidate.limits().stream().noneMatch(limit -> limit.key() == key))
                continue;
            available += candidateCapacity(cell.layer(), candidate, remainingCells - available);
            if (available == remainingCells) break;
        }
        return available;
    }

    public IntList orderedCandidates(UnresolvedCell cell, SearchBudget budget) {
        IntList candidates = new IntArrayList();
        for (int index = 0; index < cell.candidates().size(); index++) {
            ResolvedCandidate candidate = cell.candidates().get(index);
            if (withinMaximums(cell.layer(), candidate) &&
                    properties.accepts(candidate.observation().properties())) {
                candidates.add(index);
            }
        }
        if (budget.unlimited) {
            // Equivalent effects with unlimited materials are dominated by the earliest declared candidate.
            Object2IntLinkedOpenHashMap<CandidateEffect> representatives = new Object2IntLinkedOpenHashMap<>();
            representatives.defaultReturnValue(-1);
            for (int index : candidates) {
                ResolvedCandidate candidate = cell.candidates().get(index);
                CandidateEffect effect = new CandidateEffect(candidate.limits(), candidate.observation().properties(),
                        modificationCost(cell, candidate), candidate.any());
                int previous = representatives.getInt(effect);
                if (previous < 0 || candidate.order() < cell.candidates().get(previous).order()) {
                    representatives.put(effect, index);
                }
            }
            candidates = new IntArrayList(representatives.values());
        }
        candidates.sort((left, right) -> {
            var first = cell.candidates().get(left);
            var second = cell.candidates().get(right);
            int order = Integer.compare(requiredPriority(cell, first), requiredPriority(cell, second));
            if (order != 0) return order;
            order = Boolean.compare(!exactObserved(cell, first), !exactObserved(cell, second));
            if (order != 0) return order;
            order = Integer.compare(willPlace(cell, first) ? budget.rank(first.materialKey()) : 0,
                    willPlace(cell, second) ? budget.rank(second.materialKey()) : 0);
            if (order != 0) return order;
            order = Integer.compare(canonical ? previewPriority(first) : 0,
                    canonical ? previewPriority(second) : 0);
            if (order != 0) return order;
            order = Integer.compare(first.order(), second.order());
            return order != 0 ? order : BuiltInRegistries.BLOCK.getKey(first.state().getBlock()).toString()
                    .compareTo(BuiltInRegistries.BLOCK.getKey(second.state().getBlock()).toString());
        });
        return candidates;
    }

    public AppliedCandidate applyCandidate(UnresolvedCell cell, ResolvedCandidate candidate,
                                           int count, SearchBudget budget) {
        if (count == 0) {
            return new AppliedCandidate(candidate, 0, BudgetUse.EMPTY, SelectionScore.ZERO);
        }
        long previewPenalty = previewPenalty(candidate, count);
        increment(cell.layer(), candidate, count);
        properties.add(candidate.observation().properties(), count);
        BudgetUse budgetUse = willPlace(cell, candidate) ? budget.consume(candidate.materialKey(), count) :
                BudgetUse.EMPTY;
        SelectionScore score = new SelectionScore(
                SelectionScore.saturatedMultiply(modificationCost(cell, candidate), count),
                budgetUse.sourceCost(), previewPenalty,
                SelectionScore.saturatedMultiply(candidate.order(), count));
        return new AppliedCandidate(candidate, count, budgetUse, score);
    }

    public void undoCandidate(UnresolvedCell cell, AppliedCandidate applied, SearchBudget budget) {
        if (applied.count() == 0) return;
        budget.restore(applied.candidate().materialKey(), applied.budgetUse());
        increment(cell.layer(), applied.candidate(), -applied.count());
        properties.add(applied.candidate().observation().properties(), -applied.count());
    }

    public void undoApplied(UnresolvedCell cell, ObjectList<AppliedCandidate> applied, SearchBudget budget,
                            int[] counts) {
        for (int index = applied.size() - 1; index >= 0; index--) {
            AppliedCandidate candidate = applied.get(index);
            int candidateIndex = cell.candidates().indexOf(candidate.candidate());
            counts[candidateIndex]--;
            undoCandidate(cell, candidate, budget);
        }
    }

    public long previewPenalty(ResolvedCandidate candidate, int count) {
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

    public boolean canBeatBest(ObjectList<VariableGroup> groups, int groupIndex, SearchBudget budget,
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

    public SelectionScore optimisticCandidateScore(UnresolvedCell cell, ResolvedCandidate candidate,
                                                   SearchBudget budget) {
        long sourceCost = willPlace(cell, candidate) && !budget.unlimited ?
                budget.rank(candidate.materialKey()) : 0;
        return new SelectionScore(modificationCost(cell, candidate), sourceCost,
                canonical ? previewPriority(candidate) : 0, candidate.order());
    }

    public long modificationCost(UnresolvedCell cell, ResolvedCandidate candidate) {
        if (!consumeMaterials || candidate.any() || exactObserved(cell, candidate) ||
                cell.relativePos().equals(BlockPos.ZERO)) {
            return 0;
        }
        if (candidate.state().isAir()) {
            return cell.observedState() == null || cell.observedState().isAir() ? 0 : 1;
        }
        return 1;
    }

    public int requiredPriority(UnresolvedCell cell, ResolvedCandidate candidate) {
        boolean contributes = false;
        for (ConstraintLimit limit : candidate.limits()) {
            if (limit.min() >= 0 && globalCounts.getInt(limit.key()) < limit.min() ||
                    limit.layerMin() >= 0 && layerCount(cell.layer(), limit.key()) < limit.layerMin()) {
                contributes = true;
            }
        }
        return contributes ? 0 : 1;
    }

    public int previewPriority(ResolvedCandidate candidate) {
        for (ConstraintLimit limit : candidate.limits()) {
            if (limit.previewCount() >= 0 && globalCounts.getInt(limit.key()) < limit.previewCount()) return 0;
        }
        return 1;
    }

    public boolean exactObserved(UnresolvedCell cell, ResolvedCandidate candidate) {
        return cell.observedState() != null && matchesCandidateDomain(cell, candidate, cell.observedState());
    }

    public boolean willPlace(UnresolvedCell cell, ResolvedCandidate candidate) {
        return consumeMaterials && candidate.materialKey() != null && !candidate.state().isAir() &&
                !cell.relativePos().equals(BlockPos.ZERO) && !exactObserved(cell, candidate) &&
                !canUpdateDirection(cell, candidate);
    }

    public boolean withinMaximums(int layer, ResolvedCandidate candidate) {
        for (ConstraintLimit limit : candidate.limits()) {
            if (limit.max() >= 0 && (long) globalCounts.getInt(limit.key()) + 1 > limit.max()) return false;
            if (limit.layerMax() >= 0 &&
                    (long) layerCount(layer, limit.key()) + 1 > limit.layerMax())
                return false;
        }
        return true;
    }

    public void increment(int layer, ResolvedCandidate candidate, int amount) {
        for (ConstraintLimit limit : candidate.limits()) {
            globalCounts.mergeInt(limit.key(), amount, Integer::sum);
            layerCounts.computeIfAbsent(layer, ignored -> new Reference2IntOpenHashMap<>())
                    .mergeInt(limit.key(), amount, Integer::sum);
        }
    }

    public boolean minimumsSatisfied() {
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

    public boolean canStillSatisfy(ObjectList<VariableGroup> groups, int groupIndex) {
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

    public int candidateCapacity(int layer, ResolvedCandidate candidate, int maximum) {
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

    public int remainingContributors(ObjectList<VariableGroup> groups, int groupIndex, Object key,
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

    public int layerCount(int layer, Object key) {
        var counts = layerCounts.get(layer);
        return counts == null ? 0 : counts.getInt(key);
    }

    public SearchState stateKey(int groupIndex, int candidateCursor, int remainingCells, SearchBudget budget) {
        IntList global = new IntImmutableList(orderedLimits.stream()
                .mapToInt(limit -> limit.max() >= 0 ? globalCounts.getInt(limit.key()) :
                        Math.min(globalCounts.getInt(limit.key()),
                                Math.max(0, Math.max(limit.min(), limit.previewCount()))))
                .toArray());
        IntList layer = new IntImmutableList(orderedLayerLimits.stream()
                .mapToInt(limit -> limit.limit().layerMax() >= 0 ? layerCount(limit.layer(), limit.limit().key()) :
                        Math.clamp(limit.limit().layerMin(), 0, layerCount(limit.layer(), limit.limit().key())))
                .toArray());
        return new SearchState(groupIndex, candidateCursor, remainingCells, global, layer,
                budget.values(budgetKeys), properties.values());
    }

    public static int[][] copyCounts(int[][] source) {
        int[][] copy = new int[source.length][];
        for (int index = 0; index < source.length; index++) {
            copy[index] = source[index].clone();
        }
        return copy;
    }

    private record CandidateEffect(ObjectList<ConstraintLimit> limits, Object2ObjectMap<String, Object> properties,
                                   long modifications, boolean any) {}

    private record CandidateKey(BlockState state, @Nullable MaterialKey materialKey,
                                boolean hasBlockEntity, boolean hatch, boolean any, int order,
                                ObjectList<String> tierGroups, ObjectList<String> constraintIds,
                                Object2ObjectMap<String, Object> properties) {}

    private record GroupKey(int layer, @Nullable BlockState observedState,
                            @Nullable Direction requiredDirection, boolean controller,
                            ObjectList<CandidateKey> candidates) {}

    private record VariableGroup(IntList cellIndices, UnresolvedCell representative) {

        private boolean hasConstraints() {
            return representative.candidates().stream()
                    .anyMatch(candidate -> !candidate.limits().isEmpty() ||
                            !candidate.observation().properties().isEmpty());
        }

        private int constraintCount() {
            return representative.candidates().stream().mapToInt(candidate -> candidate.limits().size()).max()
                    .orElse(0);
        }
    }

    private record LayerLimit(int layer, ConstraintLimit limit) {}

    private record SearchState(int groupIndex, int candidateCursor, int remainingCells,
                               IntList globalCounts, IntList layerCounts,
                               LongList materialRemaining, Object2ObjectMap<String, Object> properties) {}

    private record BudgetUse(LongList consumedBySource, long sourceCost) {

        private static final BudgetUse EMPTY = new BudgetUse(LongList.of(), 0);
    }

    private record AppliedCandidate(ResolvedCandidate candidate, int count, BudgetUse budgetUse,
                                    SelectionScore score) {}

    private record SelectionScore(long modifications, long sourcePriority,
                                  long previewPenalty, long declarationOrder)
            implements Comparable<SelectionScore> {

        private static final SelectionScore ZERO = new SelectionScore(0, 0, 0, 0);

        private SelectionScore add(SelectionScore other) {
            return new SelectionScore(saturatedAdd(modifications, other.modifications),
                    saturatedAdd(sourcePriority, other.sourcePriority),
                    saturatedAdd(previewPenalty, other.previewPenalty),
                    saturatedAdd(declarationOrder, other.declarationOrder));
        }

        private SelectionScore times(int count) {
            return new SelectionScore(saturatedMultiply(modifications, count), saturatedMultiply(sourcePriority, count),
                    saturatedMultiply(previewPenalty, count), saturatedMultiply(declarationOrder, count));
        }

        @Override
        public int compareTo(SelectionScore other) {
            int comparison = Long.compare(modifications, other.modifications);
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

    private record SearchBudget(ObjectList<Object2LongMap<MaterialKey>> remaining, boolean unlimited) {

        private int rank(@Nullable MaterialKey key) {
            if (key == null || unlimited) return 0;
            for (int index = 0; index < remaining.size(); index++) {
                if (remaining.get(index).getLong(key) > 0) return index;
            }
            return missingRank();
        }

        private int missingRank() {
            return remaining.size() + 1;
        }

        private BudgetUse consume(@Nullable MaterialKey key, int count) {
            if (key == null || unlimited || count == 0) return BudgetUse.EMPTY;
            long remainingCount = count;
            long sourceCost = 0;
            LongList consumed = new LongArrayList(remaining.size());
            for (int index = 0; index < remaining.size(); index++) {
                Object2LongMap<MaterialKey> source = remaining.get(index);
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

        private void restore(@Nullable MaterialKey key, BudgetUse use) {
            if (key == null || unlimited) return;
            for (int index = 0; index < use.consumedBySource().size(); index++) {
                long amount = use.consumedBySource().getLong(index);
                if (amount > 0) remaining.get(index).mergeLong(key, amount, Math::addExact);
            }
        }

        private LongList values(ObjectList<MaterialKey> keys) {
            LongList values = new LongArrayList(remaining.size() * keys.size());
            for (Object2LongMap<MaterialKey> source : remaining) {
                for (MaterialKey key : keys) {
                    values.add(source.getLong(key));
                }
            }
            return new LongImmutableList(values);
        }
    }
}
