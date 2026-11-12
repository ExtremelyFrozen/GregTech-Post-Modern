package com.gregtechceu.gtceu.api.multiblock.diagnosis

import net.minecraft.network.chat.Component

import it.unimi.dsi.fastutil.objects.Object2IntMap
import it.unimi.dsi.fastutil.objects.Object2IntMaps
import it.unimi.dsi.fastutil.objects.Object2ObjectMap
import it.unimi.dsi.fastutil.objects.Object2ObjectMaps
import it.unimi.dsi.fastutil.objects.ObjectList

enum class DiagnosisMirror(key: String) {
	AUTOMATIC("gtpm.multiblock.diagnosis.automatic"),
	UNMIRRORED("gui.no"),
	MIRRORED("gui.yes"),
	;

	val label: Component = Component.translatable(key)

	fun next(): DiagnosisMirror = entries[(ordinal + 1) % entries.size]
}

/** Menu-owned parameters; an automatic selection diagnoses the confirmed structure or searches the closest candidate. */
data class DiagnosisSelection @JvmOverloads constructor(val repetitions: IntArray? = null, val choices: Object2ObjectMap<String, String> = Object2ObjectMaps.emptyMap(), val mirror: DiagnosisMirror = DiagnosisMirror.AUTOMATIC, val measurements: Object2IntMap<String> = Object2IntMaps.emptyMap())

enum class DiagnosisStatus { RUNNING, COMPLETE, STALE, FAILED, CANCELLED }

data class DiagnosisModule(val id: String, val path: String, val kind: String, val selection: String)

data class DiagnosisBasis(val front: String, val flipped: Boolean, val repetitions: IntArray, val choices: Object2ObjectMap<String, String>, val measurements: Object2IntMap<String>, val source: String, val modules: ObjectList<DiagnosisModule>)

data class DiagnosisIssue(val expected: String, val fact: String, val difference: Int, val path: String, val repeatPath: String, val count: Int, val positions: LongArray, val actual: String)

/** A bounded value report. Menu transport owns its codec rather than the diagnosis engine. */
data class DiagnosisReport(val status: DiagnosisStatus, val structure: String, val visited: Int, val attempts: Int, val incomplete: Boolean, val truncated: Boolean, val basis: DiagnosisBasis?, val issues: ObjectList<DiagnosisIssue>)
