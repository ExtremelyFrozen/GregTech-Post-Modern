package com.gregtechceu.gtceu.api.multiblock.autobuild.batch;

import com.mojang.serialization.Codec;

/**
 * Selects whether one named structure is constructed or removed.
 */
public enum StructureOperation {

    BUILD,
    DEMOLISH;

    public static final Codec<StructureOperation> CODEC = Codec.STRING.xmap(StructureOperation::valueOf,
            StructureOperation::name);
}
