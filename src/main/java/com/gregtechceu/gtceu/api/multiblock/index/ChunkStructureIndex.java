package com.gregtechceu.gtceu.api.multiblock.index;

import com.gregtechceu.gtceu.api.multiblock.MultiblockState;
import com.gregtechceu.gtceu.api.multiblock.MultiblockWorldSavedData;

import net.minecraft.core.BlockPos;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;

/** Position-to-structure subscriptions owned by one loaded server chunk. All access is on the server thread. */
public final class ChunkStructureIndex {

    private final Int2ObjectOpenHashMap<ReferenceOpenHashSet<MultiblockState>> positions = new Int2ObjectOpenHashMap<>();
    private final Int2ObjectOpenHashMap<ReferenceOpenHashSet<StructureReadTracker>> readers = new Int2ObjectOpenHashMap<>();

    public void watch(int position, StructureReadTracker reader) {
        readers.computeIfAbsent(position, ignored -> new ReferenceOpenHashSet<>()).add(reader);
    }

    public void unwatch(int position, StructureReadTracker reader) {
        var subscriptions = readers.get(position);
        subscriptions.remove(reader);
        if (subscriptions.isEmpty()) readers.remove(position);
    }

    public static int localPosition(BlockPos pos) {
        return pos.getY() << 8 | (pos.getZ() & 15) << 4 | pos.getX() & 15;
    }

    public static int localPosition(long position) {
        return BlockPos.getY(position) << 8 | (BlockPos.getZ(position) & 15) << 4 | BlockPos.getX(position) & 15;
    }

    public void add(int position, MultiblockState state) {
        positions.computeIfAbsent(position, ignored -> new ReferenceOpenHashSet<>()).add(state);
    }

    public void remove(int position, MultiblockState state) {
        var structures = positions.get(position);
        if (structures == null) return;
        structures.remove(state);
        if (structures.isEmpty()) positions.remove(position);
    }

    public void clear() {
        positions.clear();
        for (var subscriptions : readers.values()) {
            for (var reader : subscriptions) reader.changed();
        }
    }

    public void changed(BlockPos pos, MultiblockWorldSavedData data) {
        var subscriptions = readers.get(localPosition(pos));
        if (subscriptions != null) for (var reader : subscriptions) reader.changed();
        var structures = positions.get(localPosition(pos));
        if (structures == null) return;
        for (MultiblockState state : structures) {
            data.queueChangedPosition(state, pos.asLong());
        }
    }
}
