package com.gregtechceu.gtceu.api.multiblock.index;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongConsumer;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;

/** Main-thread subscriptions for one attempt, shared by its candidate branches until completion or cancellation. */
public final class StructureReadTracker implements AutoCloseable {

    private final Level world;
    private final Long2ObjectOpenHashMap<LevelChunk> chunks = new Long2ObjectOpenHashMap<>();
    private final Long2ObjectOpenHashMap<LongOpenHashSet> positions = new Long2ObjectOpenHashMap<>();
    private boolean changed;
    private boolean closed;

    public StructureReadTracker(Level world) {
        this.world = world;
    }

    public void read(BlockPos position) {
        long key = ChunkPos.asLong(position.getX() >> 4, position.getZ() >> 4);
        if (!positions.computeIfAbsent(key, ignored -> new LongOpenHashSet()).add(position.asLong())) return;
        LevelChunk chunk;
        if (chunks.containsKey(key)) {
            chunk = chunks.get(key);
        } else {
            chunk = world.getChunkSource().getChunkNow(position.getX() >> 4, position.getZ() >> 4);
            chunks.put(key, chunk);
        }
        if (!closed && chunk instanceof ChunkStructureAccess access) {
            access.gtceu$createStructureIndex().watch(ChunkStructureIndex.localPosition(position), this);
        }
    }

    public void changed() {
        changed = true;
    }

    public boolean isCurrent() {
        if (changed) return false;
        for (var entry : chunks.long2ObjectEntrySet()) {
            long key = entry.getLongKey();
            if (world.getChunkSource().getChunkNow(ChunkPos.getX(key), ChunkPos.getZ(key)) != entry.getValue())
                return false;
        }
        return true;
    }

    public boolean contains(long position) {
        long key = ChunkPos.asLong(BlockPos.getX(position) >> 4, BlockPos.getZ(position) >> 4);
        LongOpenHashSet chunkPositions = positions.get(key);
        return chunkPositions != null && chunkPositions.contains(position);
    }

    public void forEach(LongConsumer consumer) {
        for (LongOpenHashSet chunkPositions : positions.values()) chunkPositions.forEach(consumer);
    }

    @Override
    public void close() {
        if (closed) return;
        closed = true;
        for (var entry : chunks.long2ObjectEntrySet()) {
            if (!(entry.getValue() instanceof ChunkStructureAccess access)) continue;
            ChunkStructureIndex index = access.gtceu$getStructureIndex();
            var iterator = positions.get(entry.getLongKey()).iterator();
            while (iterator.hasNext()) index.unwatch(ChunkStructureIndex.localPosition(iterator.nextLong()), this);
        }
    }
}
