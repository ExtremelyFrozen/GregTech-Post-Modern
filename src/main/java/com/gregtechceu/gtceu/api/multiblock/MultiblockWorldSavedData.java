package com.gregtechceu.gtceu.api.multiblock;

import com.gregtechceu.gtceu.GTCEu;
import com.gregtechceu.gtceu.api.machine.multiblock.MultiblockControllerMachine;
import com.gregtechceu.gtceu.api.multiblock.index.ChunkStructureAccess;
import com.gregtechceu.gtceu.api.multiblock.index.ChunkStructureIndex;

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
import it.unimi.dsi.fastutil.objects.ObjectHeapPriorityQueue;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import it.unimi.dsi.fastutil.objects.Reference2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
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
    private final Reference2ObjectOpenHashMap<MultiblockState, PendingPatternCheck> scheduledChecks = new Reference2ObjectOpenHashMap<>();
    private final Reference2IntOpenHashMap<MultiblockState> retryDelays = new Reference2IntOpenHashMap<>();
    private final ObjectHeapPriorityQueue<PendingPatternCheck> pendingChecks = new ObjectHeapPriorityQueue<>(
            Comparator.comparingLong(PendingPatternCheck::dueTick));

    private MultiblockWorldSavedData() {
        this.mapping = new Object2ObjectOpenHashMap<>();
        this.chunkPosMapping = new HashMap<>();
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

    public void queuePatternCheck(MultiblockState state) {
        if (!scheduledChecks.containsKey(state)) schedulePatternCheck(state, state.world.getGameTime());
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
        }
    }

    public void clearChunkIndex(LevelChunk chunk) {
        ChunkStructureIndex index = ((ChunkStructureAccess) chunk).gtceu$getStructureIndex();
        if (index != null) index.clear();
    }

    private void schedulePatternCheck(MultiblockState state, long dueTick) {
        PendingPatternCheck current = scheduledChecks.get(state);
        if (current != null && current.dueTick() <= dueTick) return;
        PendingPatternCheck pending = new PendingPatternCheck(state, dueTick, retryDelays.getOrDefault(state, 20));
        scheduledChecks.put(state, pending);
        pendingChecks.enqueue(pending);
    }

    public void tickFormationChecks(ServerLevel level, long deadline) {
        periodID++;
        if (periodID % 200 == 0) {
            for (MultiblockControllerMachine controller : controllers) {
                controller.asyncCheckPattern(periodID);
            }
        }

        boolean changeNext = true;
        while (System.nanoTime() < deadline) {
            discardStaleChecks();
            boolean hasChanges = !changedStates.isEmpty();
            boolean hasDueCheck = !pendingChecks.isEmpty() && pendingChecks.first().dueTick() <= level.getGameTime();
            if (!hasChanges && !hasDueCheck) return;

            if (hasChanges && (changeNext || !hasDueCheck)) {
                MultiblockState state = changedStates.dequeue();
                LongOpenHashSet positions = changedPositions.remove(state);
                if (positions != null && indexedPositions.containsKey(state)) state.onBlockStateChanged(positions);
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
        MultiblockControllerMachine controller = state.getController();
        if (controller == null) return;
        var lock = controller.getPatternLock();
        if (!lock.tryLock()) {
            schedulePatternCheck(state, level.getGameTime() + 1);
            return;
        }
        try {
            String structureName = state.getStructureName();
            if (!controller.checkPattern(structureName)) {
                scheduleRetry(pending, level.getGameTime());
                return;
            }
            retryDelays.remove(state);
            if (MultiblockControllerMachine.DEFAULT_STRUCTURE.equals(structureName)) {
                controller.setFlipped(state.isNeededFlip());
            }
            controller.formStructure(structureName);
            addMapping(state);
            if (!hasPendingStructureSearch(controller)) removeAsyncLogic(controller);
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
        schedulePatternCheck(state, currentTick + delay);
        retryDelays.put(state, Math.min(200, delay * 2));
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
        // Retained for binary compatibility. Formation checks run on the server tick scheduler.
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
            if (state.hasError() || !controller.isStructureFormed(structureName)) queuePatternCheck(state);
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
        }
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
    }

    public record StructureMappingKey(BlockPos controllerPos, String structureName) {

        public static StructureMappingKey from(MultiblockState state) {
            return new StructureMappingKey(state.controllerPos, state.getStructureName());
        }
    }

    private record PendingPatternCheck(MultiblockState state, long dueTick, int delay) {

        private PendingPatternCheck(MultiblockState state, long dueTick) {
            this(state, dueTick, 20);
        }
    }
}
