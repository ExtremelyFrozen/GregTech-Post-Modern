package com.gregtechceu.gtceu.api.multiblock.pattern.constraint

/** One fact count in a concrete structure scope, shared by every candidate that contributes to it. */
@JvmRecord
data class PatternRequirement(val fact: String, val scope: String, val minimum: Int, val maximum: Int)
