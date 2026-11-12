package com.gregtechceu.gtceu.api.multiblock.index;

import com.gregtechceu.gtceu.api.multiblock.MultiblockState;
import com.gregtechceu.gtceu.api.multiblock.MultiblockWorldSavedData;

import net.minecraft.core.BlockPos;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;

/** Position-to-structure subscriptions owned by one loaded server chunk. All access is on the server thread. */
public final class ChunkStructureIndex {

    private final Int2ObjectOpenHashMap<ReferenceOpenHashSet<MultiblockState>> positions = new Int2ObjectOpenHashMap<>();

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
    }

    public void changed(BlockPos pos, MultiblockWorldSavedData data) {
        var structures = positions.get(localPosition(pos));
        if (structures == null) return;
        for (MultiblockState state : structures) {
            data.queueChangedPosition(state, pos.asLong());
        }
    }
}
