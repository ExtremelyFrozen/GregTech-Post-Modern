package com.gregtechceu.gtceu.data.pattern

enum class StructureDefinitionType(val directoryName: String, val fileExtension: String) {
	BINARY_ZSTD("binary", ".bin.zst"),
	JSON("json", ".json"),
	;

	fun matchesFileName(fileName: String): Boolean = fileName.endsWith(fileExtension)

	fun stripFileExtension(fileName: String): String {
		require(matchesFileName(fileName)) { "File '$fileName' does not match extension '$fileExtension'" }
		return fileName.substring(0, fileName.length - fileExtension.length)
	}

	companion object {
		fun fromDirectoryName(directoryName: String): StructureDefinitionType? {
			for (value in StructureDefinitionType.entries) {
				if (value.directoryName == directoryName) {
					return value
				}
			}
			return null
		}
	}
}
