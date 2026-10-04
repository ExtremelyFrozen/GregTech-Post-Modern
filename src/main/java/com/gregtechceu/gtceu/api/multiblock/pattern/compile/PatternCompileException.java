package com.gregtechceu.gtceu.api.multiblock.pattern.compile;

/** Raised when a pattern definition cannot be compiled into a deterministic runtime plan. */
public final class PatternCompileException extends IllegalArgumentException {

    public PatternCompileException(String message) {
        super(message);
    }
}
