package com.gregtechceu.gtceu.api.multiblock.index;

import net.minecraft.core.BlockPos;

import it.unimi.dsi.fastutil.longs.LongSet;
import org.jspecify.annotations.Nullable;

/**
 * Exposes the transient structure index attached to a loaded chunk. Registrations and notifications belong to
 * the server thread; keeping the index on the chunk avoids world-wide lookups for ordinary block updates.
 */
public interface ChunkStructureAccess {

    /** Returns the existing index, or null when this chunk has never contained a watched structure. */
    @Nullable
    ChunkStructureIndex gtceu$getStructureIndex();

    /** Creates the index on first registration. Called only for loaded server chunks. */
    ChunkStructureIndex gtceu$createStructureIndex();

    /** Returns the chunk's mutation revision for validating an attempt that yielded between server ticks. */
    long gtceu$getStructureRevision();

    /** Checks the bounded mutation history against an attempt's reads; an exhausted history requests a retry. */
    boolean gtceu$hasStructureChangesSince(long revision, LongSet positions);

    /** Records a server-thread dependency change, including part ownership changes without block replacement. */
    void gtceu$notifyStructureChange(BlockPos position);
}
