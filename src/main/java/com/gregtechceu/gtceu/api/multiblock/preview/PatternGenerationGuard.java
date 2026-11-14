package com.gregtechceu.gtceu.api.multiblock.preview;

import java.util.function.IntFunction;
import java.util.function.LongSupplier;

/**
 * Resolves generation-sensitive preview data without accepting a value produced across a pattern reload.
 */
public final class PatternGenerationGuard {

    private static final int MAX_ATTEMPTS = 8;

    private PatternGenerationGuard() {}

    /**
     * Repeats resolution when the generation changes between the two observations around one attempt.
     *
     * @param generation current published pattern generation
     * @param resolver   resolver receiving its zero-based attempt index
     */
    public static <T> Resolution<T> resolve(LongSupplier generation, IntFunction<T> resolver) {
        for (int attempt = 0; attempt < MAX_ATTEMPTS; attempt++) {
            long before = generation.getAsLong();
            T value = resolver.apply(attempt);
            long after = generation.getAsLong();
            if (before == after) {
                return new Resolution<>(after, value);
            }
        }
        throw new IllegalStateException("Pattern generation did not stabilize while resolving a preview");
    }

    /** One value and the generation that remained stable throughout its resolution. */
    public record Resolution<T>(long generation, T value) {}
}
