package com.gregtechceu.gtceu.api.multiblock.pattern.layout

/** A well-formed candidate that cannot match; it remains inspectable and is safe to cache. */
data class PatternLayoutRejection(val kind: Kind, val message: String) {
	enum class Kind { SIZE_CONSTRAINT, MODULE_OVERLAP }
}
