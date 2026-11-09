package com.gregtechceu.gtceu.api.multiblock.autobuild.batch;

import net.minecraft.util.StringRepresentable;

import java.util.Locale;

/**
 * Selects whether one named structure is constructed or removed.
 */
public enum StructureOperation implements StringRepresentable {

    BUILD,
    DEMOLISH;

    public static final EnumCodec<StructureOperation> CODEC = StringRepresentable.fromEnum(StructureOperation::values);

    @Override
    public String getSerializedName() {
        return name().toLowerCase(Locale.ROOT);
    }
}
