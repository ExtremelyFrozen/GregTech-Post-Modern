package com.gregtechceu.gtceu.api.multiblock;

import com.gregtechceu.gtceu.GTCEu;
import com.gregtechceu.gtceu.api.machine.multiblock.MultiblockControllerMachine;
import com.gregtechceu.gtceu.api.multiblock.index.ChunkStructureAccess;
import com.gregtechceu.gtceu.api.multiblock.index.ChunkStructureIndex;
import com.gregtechceu.gtceu.api.multiblock.index.StructureSearchIndex;
import com.gregtechceu.gtceu.api.multiblock.pattern.match.MultiBlockPattern;
import com.gregtechceu.gtceu.api.multiblock.pattern.match.PatternCheckSession;
import com.gregtechceu.gtceu.data.pattern.StructurePatternRegistry;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.saveddata.SavedData;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayFIFOQueue;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectHeapPriorityQueue;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import it.unimi.dsi.fastutil.objects.Reference2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.Comparator;
import java.util.Map;
import java.util.Set;

public class MultiblockWorldSavedData extends SavedData {

    public static MultiblockWorldSavedData getOrCreate(ServerLevel serverLevel) {
        return serverLevel.getDataStorage().computeIfAbsent(
                new Factory<>(MultiblockWorldSavedData::new, MultiblockWorldSavedData::new),
                "gtceu_multiblock");
    }

    /**
     * Store all formed multiblocks' structure info
     */
    public final Map<StructureMappingKey, MultiblockState> mapping;
    /**
     * Chunk pos mapping.
     */
    public final Map<ChunkPos, Set<MultiblockState>> chunkPosMapping;
    private final Reference2ObjectOpenHashMap<MultiblockState, LongOpenHashSet> indexedPositions = new Reference2ObjectOpenHashMap<>();
    private final Reference2ObjectOpenHashMap<MultiblockState, ObjectOpenHashSet<ChunkPos>> indexedChunks = new Reference2ObjectOpenHashMap<>();
    private final Reference2ObjectOpenHashMap<MultiblockState, LongOpenHashSet> changedPositions = new Reference2ObjectOpenHashMap<>();
    private final ObjectArrayFIFOQueue<MultiblockState> changedStates = new ObjectArrayFIFOQueue<>();
    private final ReferenceOpenHashSet<MultiblockControllerMachine> controllers = new ReferenceOpenHashSet<>();
    private final StructureSearchIndex searchRegions = new StructureSearchIndex();
    private final Reference2ObjectOpenHashMap<MultiblockState, PendingPatternCheck> scheduledChecks = new Reference2ObjectOpenHashMap<>();
    private final Reference2IntOpenHashMap<MultiblockState> retryDelays = new Reference2IntOpenHashMap<>();
    private long queueOrder;
    private long patternGeneration = StructurePatternRegistry.generation();
    private final ObjectHeapPriorityQueue<PendingPatternCheck> pendingChecks = new ObjectHeapPriorityQueue<>(
            Comparator.comparingLong(PendingPatternCheck::dueTick).thenComparingLong(check -> check.queueOrder));

    private MultiblockWorldSavedData() {
        this.mapping = new Object2ObjectOpenHashMap<>();
        this.chunkPosMapping = new Object2ObjectOpenHashMap<>();
    }

    private MultiblockWorldSavedData(CompoundTag tag, HolderLookup.Provider provider) {
        this();
    }

    public Set<MultiblockState> getControllersInChunk(ChunkPos chunkPos) {
        return chunkPosMapping.getOrDefault(chunkPos, Collections.emptySet());
    }

    public void addMapping(MultiblockState state) {
        removeMapping(state);
        this.mapping.put(StructureMappingKey.from(state), state);
        LongOpenHashSet positions = new LongOpenHashSet(state.cache);
        positions.add(state.controllerPos.asLong());
        indexedPositions.put(state, positions);
        ObjectOpenHashSet<ChunkPos> chunks = new ObjectOpenHashSet<>();
        indexedChunks.put(state, chunks);
        var iterator = positions.iterator();
        while (iterator.hasNext()) {
            BlockPos blockPos = BlockPos.of(iterator.nextLong());
            ChunkPos chunkPos = new ChunkPos(blockPos);
            chunks.add(chunkPos);
            chunkPosMapping.computeIfAbsent(chunkPos, ignored -> new ReferenceOpenHashSet<>()).add(state);
            var chunk = state.world.getChunkSource().getChunkNow(blockPos.getX() >> 4, blockPos.getZ() >> 4);
            if (chunk != null) {
                ((ChunkStructureAccess) chunk).gtceu$createStructureIndex()
                        .add(ChunkStructureIndex.localPosition(blockPos), state);
            }
        }
    }

    public void removeMapping(MultiblockState state) {
        this.mapping.remove(StructureMappingKey.from(state));
        searchRegions.remove(state);
        LongOpenHashSet positions = indexedPositions.remove(state);
        if (positions != null) {
            var iterator = positions.iterator();
            while (iterator.hasNext()) {
                BlockPos blockPos = BlockPos.of(iterator.nextLong());
                var chunk = state.world.getChunkSource().getChunkNow(blockPos.getX() >> 4, blockPos.getZ() >> 4);
                if (chunk != null) {
                    var index = ((ChunkStructureAccess) chunk).gtceu$getStructureIndex();
                    if (index != null) index.remove(ChunkStructureIndex.localPosition(blockPos), state);
                }
            }
        }
        ObjectOpenHashSet<ChunkPos> chunks = indexedChunks.remove(state);
        if (chunks == null) return;
        for (ChunkPos chunk : chunks) {
            Set<MultiblockState> structures = chunkPosMapping.get(chunk);
            if (structures == null) continue;
            structures.remove(state);
            if (structures.isEmpty()) chunkPosMapping.remove(chunk);
        }
    }

    public void queueChangedPosition(MultiblockState state, long position) {
        LongOpenHashSet positions = changedPositions.get(state);
        if (positions == null) {
            positions = new LongOpenHashSet();
            changedPositions.put(state, positions);
            changedStates.enqueue(state);
        }
        positions.add(position);
    }

    public void onChunkPositionChanged(BlockPos position) {
        searchRegions.changed(position, this);
    }

    public void queuePatternCheck(MultiblockState state) {
        if (!scheduledChecks.containsKey(state)) schedulePatternCheck(state, state.world.getGameTime());
    }

    public void requestPatternCheck(MultiblockState state) {
        requestPatternCheck(state, new LongOpenHashSet());
    }

    public void requestPatternCheck(MultiblockState state, LongOpenHashSet positions) {
        PendingPatternCheck previous = scheduledChecks.get(state);
        LongOpenHashSet changes = new LongOpenHashSet(positions);
        if (previous != null) changes.addAll(previous.changes);
        scheduledChecks.remove(state);
        retryDelays.removeInt(state);
        schedulePatternCheck(state, state.world.getGameTime());
        scheduledChecks.get(state).changes.addAll(changes);
    }

    public void rebuildChunkIndex(LevelChunk chunk) {
        Set<MultiblockState> structures = chunkPosMapping.get(chunk.getPos());
        if (structures == null) return;
        ChunkStructureIndex index = ((ChunkStructureAccess) chunk).gtceu$createStructureIndex();
        for (MultiblockState state : structures) {
            LongOpenHashSet positions = indexedPositions.get(state);
            if (positions == null) continue;
            var iterator = positions.iterator();
            while (iterator.hasNext()) {
                BlockPos pos = BlockPos.of(iterator.nextLong());
                if ((pos.getX() >> 4) == chunk.getPos().x && (pos.getZ() >> 4) == chunk.getPos().z) {
                    index.add(ChunkStructureIndex.localPosition(pos), state);
                }
            }
            requestPatternCheck(state);
        }
    }

    public void clearChunkIndex(LevelChunk chunk) {
        ChunkStructureIndex index = ((ChunkStructureAccess) chunk).gtceu$getStructureIndex();
        if (index != null) index.clear();
        for (MultiblockState state : getControllersInChunk(chunk.getPos())) {
            state.setError(MultiblockState.UNLOAD_ERROR);
            requestPatternCheck(state);
        }
    }

    private void schedulePatternCheck(MultiblockState state, long dueTick) {
        PendingPatternCheck current = scheduledChecks.get(state);
        if (current != null && current.dueTick() <= dueTick) return;
        PendingPatternCheck pending = new PendingPatternCheck(state, dueTick, retryDelays.getOrDefault(state, 20));
        scheduledChecks.put(state, pending);
        pending.queueOrder = queueOrder++;
        pendingChecks.enqueue(pending);
    }

    public void tickFormationChecks(ServerLevel level, long deadline) {
        long generation = StructurePatternRegistry.generation();
        if (generation != patternGeneration) {
            patternGeneration = generation;
            for (MultiblockState state : mapping.values()) requestPatternCheck(state);
            for (MultiblockControllerMachine controller : controllers) {
                for (String structureName : controller.getDefinition().getStructureNames()) {
                    MultiblockState state = controller.getMultiblockState(structureName);
                    if (!controller.isStructureFormed(structureName)) {
                        searchRegions.add(state, controller.getPattern(structureName).getSearchRadius());
                    }
                    requestPatternCheck(state);
                }
            }
        }
        periodID++;

        boolean changeNext = true;
        while (System.nanoTime() < deadline) {
            discardStaleChecks();
            boolean hasChanges = !changedStates.isEmpty();
            boolean hasDueCheck = !pendingChecks.isEmpty() && pendingChecks.first().dueTick() <= level.getGameTime();
            if (!hasChanges && !hasDueCheck) return;

            if (hasChanges && (changeNext || !hasDueCheck)) {
                MultiblockState state = changedStates.dequeue();
                LongOpenHashSet positions = changedPositions.remove(state);
                if (positions != null && (indexedPositions.containsKey(state) || searchRegions.contains(state)))
                    state.onBlockStateChanged(positions);
                changeNext = false;
            } else {
                PendingPatternCheck pending = pendingChecks.dequeue();
                scheduledChecks.remove(pending.state());
                checkPattern(pending, level);
                changeNext = true;
            }
        }
    }

    private void discardStaleChecks() {
        while (!pendingChecks.isEmpty() &&
                scheduledChecks.get(pendingChecks.first().state()) != pendingChecks.first()) {
            pendingChecks.dequeue();
        }
    }

    private void checkPattern(PendingPatternCheck pending, ServerLevel level) {
        MultiblockState state = pending.state();
        LongOpenHashSet queuedChanges = changedPositions.remove(state);
        if (queuedChanges != null) {
            pending.changes.addAll(queuedChanges);
            pending.session = null;
        }
        MultiblockControllerMachine controller = state.getController();
        if (controller == null) return;
        var lock = controller.getPatternLock();
        if (!lock.tryLock()) {
            schedulePatternCheck(state, level.getGameTime() + 1);
            return;
        }
        try {
            String structureName = state.getStructureName();
            MultiBlockPattern pattern = controller.getPattern(structureName);
            long generation = StructurePatternRegistry.generation();
            if (pending.session == null || pending.pattern != pattern || pending.generation != generation ||
                    !pending.session.isSourceCurrent()) {
                pending.pattern = pattern;
                pending.generation = generation;
                pending.session = pattern.beginCheck(state, pending.changes);
            }
            PatternCheckSession session = pending.session;
            if (!session.advance()) {
                pending.dueTick = level.getGameTime();
                pending.queueOrder = queueOrder++;
                scheduledChecks.put(state, pending);
                pendingChecks.enqueue(pending);
                return;
            }
            if (!session.getState().isMatchCurrent() || session.getState().lastController != state.getController()) {
                requestPatternCheck(state, pending.changes);
                return;
            }
            boolean matched = controller.validatePatternResult(session.getState(), session.matched());
            if (!matched) {
                if (controller.isStructureFormed(structureName)) controller.invalidateStructure(structureName);
                state.applyMatch(session.getState());
                addMapping(state);
                searchRegions.add(state, pattern.getSearchRadius());
                if (!pattern.isEventDriven()) scheduleRetry(pending, level.getGameTime());
                return;
            }
            boolean refresh = session.requiresRefresh();
            state.applyMatch(session.getState());
            searchRegions.remove(state);
            retryDelays.remove(state);
            if (MultiblockControllerMachine.DEFAULT_STRUCTURE.equals(structureName)) {
                controller.setFlipped(state.isNeededFlip());
            }
            if (refresh) {
                controller.formStructure(structureName);
                addMapping(state);
            }
            if (!hasPendingStructureSearch(controller)) {
                controllers.remove(controller);
            }
            if (!pattern.isEventDriven()) schedulePatternCheck(state, level.getGameTime() + 200);
        } catch (RuntimeException exception) {
            GTCEu.LOGGER.error("Error while assembling multiblock {} at {}", controller, state.controllerPos,
                    exception);
            scheduleRetry(pending, level.getGameTime());
        } finally {
            lock.unlock();
        }
    }

    private void scheduleRetry(PendingPatternCheck pending, long currentTick) {
        MultiblockState state = pending.state();
        int delay = pending.delay();
        retryDelays.put(state, Math.min(200, delay * 2));
        schedulePatternCheck(state, currentTick + delay);
    }

    private static boolean hasPendingStructureSearch(MultiblockControllerMachine controller) {
        for (String structureName : controller.getDefinition().getStructureNames()) {
            MultiblockState state = controller.getMultiblockState(structureName);
            if (state.hasError() || !controller.isStructureFormed(structureName)) return true;
        }
        return false;
    }

    @NotNull
    @Override
    public CompoundTag save(@NotNull CompoundTag compound, HolderLookup.Provider provider) {
        return compound;
    }

    @Getter
    private long periodID;

    public void createExecutorService() {
        for (MultiblockControllerMachine controller : controllers) controller.asyncCheckPattern(periodID);
    }

    /**
     * add a async logic runnable
     *
     * @param controller controller
     */
    public void addAsyncLogic(MultiblockControllerMachine controller) {
        controllers.add(controller);
        for (String structureName : controller.getDefinition().getStructureNames()) {
            MultiblockState state = controller.getMultiblockState(structureName);
            state.lastController = controller;
            if (state.hasError() || !controller.isStructureFormed(structureName)) {
                searchRegions.add(state, controller.getPattern(structureName).getSearchRadius());
                queuePatternCheck(state);
            }
        }
    }

    /**
     * remove async controller
     *
     * @param controller controller
     */
    public void removeAsyncLogic(MultiblockControllerMachine controller) {
        controllers.remove(controller);
        for (String structureName : controller.getDefinition().getStructureNames()) {
            MultiblockState state = controller.getMultiblockState(structureName);
            scheduledChecks.remove(state);
            retryDelays.remove(state);
            changedPositions.remove(state);
            searchRegions.remove(state);
            removeMapping(state);
        }
        ObjectArrayList<PendingPatternCheck> retained = new ObjectArrayList<>(scheduledChecks.values());
        pendingChecks.clear();
        for (PendingPatternCheck pending : retained) pendingChecks.enqueue(pending);
    }

    public static boolean isThreadService() {
        return false;
    }

    public void releaseExecutorService() {
        controllers.clear();
        changedPositions.clear();
        changedStates.clear();
        scheduledChecks.clear();
        pendingChecks.clear();
        retryDelays.clear();
        searchRegions.clear();
        mapping.clear();
        chunkPosMapping.clear();
        indexedPositions.clear();
        indexedChunks.clear();
    }

    public record StructureMappingKey(BlockPos controllerPos, String structureName) {

        public static StructureMappingKey from(MultiblockState state) {
            return new StructureMappingKey(state.controllerPos, state.getStructureName());
        }
    }

    private static final class PendingPatternCheck {

        private final MultiblockState state;
        private long dueTick;
        private final int delay;
        private long queueOrder;
        private PatternCheckSession session;
        private MultiBlockPattern pattern;
        private long generation;
        private final LongOpenHashSet changes = new LongOpenHashSet();

        private PendingPatternCheck(MultiblockState state, long dueTick, int delay) {
            this.state = state;
            this.dueTick = dueTick;
            this.delay = delay;
        }

        private MultiblockState state() {
            return state;
        }

        private long dueTick() {
            return dueTick;
        }

        private int delay() {
            return delay;
        }
    }
}
