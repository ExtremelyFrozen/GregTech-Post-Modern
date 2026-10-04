package com.gregtechceu.gtceu.data.pattern

import com.gregtechceu.gtceu.api.machine.multiblock.MultiblockControllerMachine
import net.minecraft.resources.ResourceLocation

/** Identifies one named structure pattern belonging to a multiblock machine. */
@JvmRecord
data class StructurePatternKey(
    val machineId: ResourceLocation,
    val structureName: String,
) {
    init {
        require(structureName.isNotBlank()) {
            "Structure pattern name must not be blank for $machineId"
        }
        require('/' !in structureName && '\\' !in structureName) {
            "Structure pattern name must be one path segment: $structureName"
        }
    }

    fun isDefaultStructure(): Boolean = DEFAULT_STRUCTURE_NAME == structureName

    fun resourceId(): ResourceLocation = if (isDefaultStructure()) {
        machineId
    } else {
        machineId.withPath { path -> "$path/$structureName" }
    }

    override fun toString(): String = "$machineId#$structureName"

    companion object {
        @JvmField
        val DEFAULT_STRUCTURE_NAME: String = MultiblockControllerMachine.DEFAULT_STRUCTURE

        @JvmStatic
        fun main(machineId: ResourceLocation): StructurePatternKey =
            StructurePatternKey(machineId, DEFAULT_STRUCTURE_NAME)
    }
}
