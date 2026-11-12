package com.gregtechceu.gtceu.api.multiblock;

import com.gregtechceu.gtceu.GTCEu;
import com.gregtechceu.gtceu.api.machine.multiblock.MultiblockControllerMachine;
import com.gregtechceu.gtceu.api.multiblock.index.ChunkStructureAccess;
import com.gregtechceu.gtceu.api.multiblock.index.ChunkStructureIndex;
import com.gregtechceu.gtceu.api.multiblock.index.StructureSearchIndex;
import com.gregtechceu.gtceu.api.multiblock.pattern.match.MultiBlockPattern;
import com.gregtechceu.gtceu.api.multiblock.pattern.match.PatternCheckSession;
import com.gregtechceu.gtceu.api.multiblock.scheduling.FormationCheckQueue;
import com.gregtechceu.gtceu.data.pattern.StructurePatternRegistry;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.saveddata.SavedData;

import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Reference2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.Reference2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.Nullable;

import java.util.Collections;
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
    private final Reference2ObjectOpenHashMap<MultiblockState, Long2ObjectOpenHashMap<IntOpenHashSet>> indexedChunks = new Reference2ObjectOpenHashMap<>();
    private final Reference2ObjectLinkedOpenHashMap<MultiblockState, LongOpenHashSet> changedPositions = new Reference2ObjectLinkedOpenHashMap<>();
    private final ReferenceOpenHashSet<MultiblockControllerMachine> controllers = new ReferenceOpenHashSet<>();
    private final StructureSearchIndex searchRegions = new StructureSearchIndex();
    private final Reference2ObjectOpenHashMap<MultiblockState, PendingPatternCheck> scheduledChecks = new Reference2ObjectOpenHashMap<>();
    private final Reference2IntOpenHashMap<MultiblockState> retryDelays = new Reference2IntOpenHashMap<>();
    private long patternGeneration = StructurePatternRegistry.generation();
    private final FormationCheckQueue<PendingPatternCheck> pendingChecks = new FormationCheckQueue<>();
    private @Nullable MultiblockState invalidatingCheck;

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
        if (indexedPositions.get(state) == state.cache) return;
        removeMapping(state);
        this.mapping.put(StructureMappingKey.from(state), state);
        LongOpenHashSet positions = state.cache;
        positions.add(state.controllerPos.asLong());
        indexedPositions.put(state, positions);
        Long2ObjectOpenHashMap<IntOpenHashSet> chunks = new Long2ObjectOpenHashMap<>();
        indexedChunks.put(state, chunks);
        var iterator = positions.iterator();
        while (iterator.hasNext()) {
            long position = iterator.nextLong();
            long key = ChunkPos.asLong(BlockPos.getX(position) >> 4, BlockPos.getZ(position) >> 4);
            chunks.computeIfAbsent(key, ignored -> new IntOpenHashSet())
                    .add(ChunkStructureIndex.localPosition(position));
        }
        for (var entry : chunks.long2ObjectEntrySet()) {
            long key = entry.getLongKey();
            ChunkPos chunkPos = new ChunkPos(key);
            chunkPosMapping.computeIfAbsent(chunkPos, ignored -> new ReferenceOpenHashSet<>()).add(state);
            var chunk = state.world.getChunkSource().getChunkNow(ChunkPos.getX(key), ChunkPos.getZ(key));
            if (chunk != null) {
                ChunkStructureIndex index = ((ChunkStructureAccess) chunk).gtceu$createStructureIndex();
                var localPositions = entry.getValue().iterator();
                while (localPositions.hasNext()) index.add(localPositions.nextInt(), state);
            }
        }
    }

    public void removeMapping(MultiblockState state) {
        this.mapping.remove(StructureMappingKey.from(state), state);
        searchRegions.remove(state);
        indexedPositions.remove(state);
        Long2ObjectOpenHashMap<IntOpenHashSet> chunks = indexedChunks.remove(state);
        if (chunks == null) return;
        for (var entry : chunks.long2ObjectEntrySet()) {
            long key = entry.getLongKey();
            var chunk = state.world.getChunkSource().getChunkNow(ChunkPos.getX(key), ChunkPos.getZ(key));
            if (chunk != null) {
                var index = ((ChunkStructureAccess) chunk).gtceu$getStructureIndex();
                if (index != null) {
                    var localPositions = entry.getValue().iterator();
                    while (localPositions.hasNext()) index.remove(localPositions.nextInt(), state);
                }
            }
            ChunkPos chunkPos = new ChunkPos(key);
            Set<MultiblockState> structures = chunkPosMapping.get(chunkPos);
            if (structures != null) {
                structures.remove(state);
                if (structures.isEmpty()) chunkPosMapping.remove(chunkPos);
            }
        }
    }

    public void queueChangedPosition(MultiblockState state, long position) {
        LongOpenHashSet positions = changedPositions.computeIfAbsent(state, k -> new LongOpenHashSet());
        positions.add(position);
    }

    public void onChunkPositionChanged(BlockPos position) {
        searchRegions.changed(position, this);
    }

    public void onStructureInvalidated(MultiblockState state, MultiblockControllerMachine controller) {
        removeMapping(state);
        if (state != invalidatingCheck && state.world.getBlockEntity(state.controllerPos) == controller &&
                !controller.isRemoved()) {
            addAsyncLogic(controller);
            requestPatternCheck(state);
        }
    }

    /** Synchronous construction and automatic checks publish through the same registration boundary. */
    public void onStructureFormed(MultiblockState state, MultiblockControllerMachine controller) {
        PendingPatternCheck pending = scheduledChecks.remove(state);
        if (pending != null) pendingChecks.remove(pending);
        changedPositions.remove(state);
        retryDelays.removeInt(state);
        addMapping(state);
        searchRegions.remove(state);
        if (!hasPendingStructureSearch(controller)) controllers.remove(controller);
        if (!controller.getPattern(state.getStructureName()).isEventDriven())
            schedulePatternCheck(state, state.world.getGameTime() + 200);
    }

    public void queuePatternCheck(MultiblockState state) {
        if (!scheduledChecks.containsKey(state)) {
            PendingPatternCheck pending = new PendingPatternCheck(state, retryDelays.getOrDefault(state, 20));
            schedulePatternCheck(pending, state.world.getGameTime(), false);
        }
    }

    public void requestPatternCheck(MultiblockState state) {
        requestPatternCheck(state, new LongOpenHashSet());
    }

    public void requestPatternCheck(MultiblockState state, LongOpenHashSet positions) {
        PendingPatternCheck pending = scheduledChecks.get(state);
        if (pending == null) pending = new PendingPatternCheck(state, 20);
        pending.session = null;
        pending.delay = 20;
        pending.changes.addAll(positions);
        if (positions.isEmpty()) pending.fullCheck = true;
        retryDelays.removeInt(state);
        schedulePatternCheck(pending, state.world.getGameTime(), true);
    }

    public void rebuildChunkIndex(LevelChunk chunk) {
        Set<MultiblockState> structures = chunkPosMapping.get(chunk.getPos());
        if (structures == null) return;
        ChunkStructureIndex index = ((ChunkStructureAccess) chunk).gtceu$createStructureIndex();
        for (MultiblockState state : structures) {
            IntOpenHashSet positions = indexedChunks.get(state).get(chunk.getPos().toLong());
            var iterator = positions.iterator();
            while (iterator.hasNext()) index.add(iterator.nextInt(), state);
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
            boolean hasChanges = !changedPositions.isEmpty();
            boolean hasDueCheck = pendingChecks.hasDue(level.getGameTime());
            if (!hasChanges && !hasDueCheck) return;

            if (hasChanges && (changeNext || !hasDueCheck)) {
                MultiblockState state = changedPositions.firstKey();
                LongOpenHashSet positions = changedPositions.remove(state);
                if (positions != null && (indexedPositions.containsKey(state) || searchRegions.contains(state)))
                    state.onBlockStateChanged(positions);
                changeNext = false;
            } else {
                PendingPatternCheck pending = pendingChecks.poll(level.getGameTime());
                if (pending != null) {
                    scheduledChecks.remove(pending.state());
                    checkPattern(pending, level, deadline);
                }
                changeNext = true;
            }
        }
    }

    private void checkPattern(PendingPatternCheck pending, ServerLevel level, long deadline) {
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
            schedulePatternCheck(pending, level.getGameTime() + 1, !pending.changes.isEmpty());
            return;
        }
        try {
            String structureName = state.getStructureName();
            MultiBlockPattern pattern = controller.getPattern(structureName);
            long generation = StructurePatternRegistry.generation();
            if (pending.session != null && !pending.session.isSourceCurrent()) {
                pending.fullCheck = true;
                pending.session = null;
            }
            if (pending.session == null || pending.pattern != pattern || pending.generation != generation) {
                pending.pattern = pattern;
                pending.generation = generation;
                pending.session = pending.fullCheck ? pattern.beginCheck(state) :
                        pattern.beginCheck(state, pending.changes);
            }
            PatternCheckSession session = pending.session;
            if (!session.advance(deadline)) {
                schedulePatternCheck(pending, level.getGameTime(), !pending.changes.isEmpty());
                return;
            }
            if (!session.getState().isMatchCurrent() || session.getState().lastController != state.getController()) {
                requestPatternCheck(state, pending.changes);
                return;
            }
            boolean matched = controller.validatePatternResult(session.getState(), session.matched());
            if (generation != StructurePatternRegistry.generation() ||
                    controller.getPattern(structureName) != pattern ||
                    !session.isSourceCurrent() || !session.getState().isMatchCurrent()) {
                requestPatternCheck(state);
                return;
            }
            if (!matched) {
                if (controller.isStructureFormed(structureName)) {
                    invalidatingCheck = state;
                    try {
                        controller.invalidateStructure(structureName);
                    } finally {
                        invalidatingCheck = null;
                    }
                }
                state.applyMatch(session.getState());
                addMapping(state);
                searchRegions.add(state, pattern.getSearchRadius());
                if (!pattern.isEventDriven()) scheduleRetry(pending, level.getGameTime());
                return;
            }
            boolean refresh = !controller.isStructureFormed(structureName) || session.requiresRefresh();
            state.applyMatch(session.getState());
            searchRegions.remove(state);
            retryDelays.removeInt(state);
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
        pending.delay = Math.min(200, delay * 2);
        pending.session = null;
        pending.fullCheck = true;
        pending.changes.clear();
        retryDelays.put(state, pending.delay);
        schedulePatternCheck(pending, currentTick + delay, false);
    }

    private void schedulePatternCheck(PendingPatternCheck pending, long dueTick, boolean priority) {
        PendingPatternCheck current = scheduledChecks.get(pending.state());
        if (current != null && current != pending && current.dueTick() <= dueTick) return;
        if (current != null && current != pending) {
            pendingChecks.remove(current);
            pending.changes.addAll(current.changes);
        }
        pendingChecks.schedule(pending, dueTick, priority);
        scheduledChecks.put(pending.state(), pending);
    }

    private void schedulePatternCheck(MultiblockState state, long dueTick) {
        PendingPatternCheck pending = scheduledChecks.get(state);
        if (pending != null && pending.dueTick() <= dueTick) return;
        if (pending == null) pending = new PendingPatternCheck(state, retryDelays.getOrDefault(state, 20));
        pending.fullCheck = true;
        schedulePatternCheck(pending, dueTick, !pending.changes.isEmpty());
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
            PendingPatternCheck pending = scheduledChecks.remove(state);
            if (pending != null) pendingChecks.remove(pending);
            retryDelays.removeInt(state);
            changedPositions.remove(state);
            searchRegions.remove(state);
            removeMapping(state);
        }
    }

    public static boolean isThreadService() {
        return false;
    }

    public void releaseExecutorService() {
        controllers.clear();
        changedPositions.clear();
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

    private static final class PendingPatternCheck extends FormationCheckQueue.Entry {

        private final MultiblockState state;
        private int delay;
        private boolean fullCheck;
        private PatternCheckSession session;
        private MultiBlockPattern pattern;
        private long generation;
        private final LongOpenHashSet changes = new LongOpenHashSet();

        private PendingPatternCheck(MultiblockState state, int delay) {
            this.state = state;
            this.delay = delay;
        }

        private MultiblockState state() {
            return state;
        }

        private int delay() {
            return delay;
        }
    }
}
