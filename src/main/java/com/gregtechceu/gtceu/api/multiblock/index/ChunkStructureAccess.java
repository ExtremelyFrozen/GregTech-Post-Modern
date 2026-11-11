package com.gregtechceu.gtceu.api.multiblock.index;

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
}
