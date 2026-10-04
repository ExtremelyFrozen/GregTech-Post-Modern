package com.gregtechceu.gtceu.api.multiblock.pattern.model;

/** Orientation capabilities declared by a pattern resource. */
public record PatternOrientation(boolean allowMirror, boolean allowExtendedFacing) {

    public static PatternOrientation defaults() {
        return new PatternOrientation(false, false);
    }
}
