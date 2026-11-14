package com.gregtechceu.gtceu.api.multiblock.structurepredicate

import java.util.Optional

/** A selected preview branch retains the actual source key and all nested count requirements. */
@JvmRecord
data class StructurePreviewConstraint(val key: StructurePredicate, val minCount: Optional<Int>, val maxCount: Optional<Int>, val minCountByLayer: Optional<Int>, val maxCountByLayer: Optional<Int>, val previewCount: Optional<Int>)
