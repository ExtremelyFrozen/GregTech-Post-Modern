package com.gregtechceu.gtceu.data.pattern

/** Encoding source used by the structure resource index. */
enum class StructureDefinitionSource(val serializedName: String) {
	JSON("json"),
	BINARY_ZSTD("binary"),
	;

	companion object {
		@JvmStatic
		fun fromDefinitionType(type: StructureDefinitionType): StructureDefinitionSource = when (type) {
			StructureDefinitionType.JSON -> JSON
			StructureDefinitionType.BINARY_ZSTD -> BINARY_ZSTD
		}
	}
}
