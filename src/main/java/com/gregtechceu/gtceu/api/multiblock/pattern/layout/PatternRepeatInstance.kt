package com.gregtechceu.gtceu.api.multiblock.pattern.layout

/** One instantiated repeat, keyed by its stable path in typed repetition overrides. */
@JvmRecord
data class PatternRepeatInstance @JvmOverloads constructor(val path: String, val descriptor: PatternRepeatDescriptor, val depth: Int = 0, val height: Int = 0, val width: Int = 0)
