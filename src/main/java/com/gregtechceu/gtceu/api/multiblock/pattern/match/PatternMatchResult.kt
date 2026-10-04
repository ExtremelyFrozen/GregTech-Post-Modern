package com.gregtechceu.gtceu.api.multiblock.pattern.match

import com.gregtechceu.gtceu.api.multiblock.pattern.model.PatternFacts
import org.jspecify.annotations.NullMarked

/** Immutable result shared by machine matching, preview and auto-build. */
@NullMarked
@JvmRecord
data class PatternMatchResult(
    val matched: Boolean,
    val facts: PatternFacts,
    val diagnostics: List<PatternDiagnostic>,
) {
    companion object {
        @JvmStatic
        fun success(facts: PatternFacts): PatternMatchResult = PatternMatchResult(true, facts, emptyList())
    }
}
