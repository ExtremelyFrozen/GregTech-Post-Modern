package com.gregtechceu.gtceu.api.multiblock.pattern.match;

import com.gregtechceu.gtceu.api.multiblock.pattern.model.PatternFacts;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectLists;
import org.jspecify.annotations.NullMarked;

import java.util.List;

/** Immutable result shared by machine matching, preview and auto-build. */
@NullMarked
public record PatternMatchResult(boolean matched, PatternFacts facts, List<PatternDiagnostic> diagnostics) {

    public PatternMatchResult {
        if (facts == null || diagnostics == null)
            throw new IllegalArgumentException("Pattern match result is incomplete");
        diagnostics = ObjectLists.unmodifiable(new ObjectArrayList<>(diagnostics));
    }

    public static PatternMatchResult success(PatternFacts facts) {
        return new PatternMatchResult(true, facts, List.of());
    }
}
