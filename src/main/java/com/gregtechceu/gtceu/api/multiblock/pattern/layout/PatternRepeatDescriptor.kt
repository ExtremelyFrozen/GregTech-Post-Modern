package com.gregtechceu.gtceu.api.multiblock.pattern.layout

/** Stable declaration order; fixed legacy units have identical minimum and maximum values. */
@JvmRecord
data class PatternRepeatDescriptor @JvmOverloads constructor(val index: Int, val id: String, val path: String, val axis: PatternAxis, val minimum: Int, val maximum: Int, val parameter: String = "")
