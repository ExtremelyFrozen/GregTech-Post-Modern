package com.gregtechceu.gtceu.api.multiblock.pattern.layout

import com.gregtechceu.gtceu.api.multiblock.pattern.predicate.PatternPredicate

import it.unimi.dsi.fastutil.objects.ObjectList

/** Coordinates are relative to the controller in definition axes, before rotation/mirroring. */
@JvmRecord
data class PatternLayoutCell(
	val path: String,
	val repeatPath: String,
	val depth: Int,
	val height: Int,
	val width: Int,
	val predicate: PatternPredicate,
	val token: String,
	val scopes: ObjectList<String>,
	val unit: Int,
	val repetition: Int,
	val inner: Int,
	val row: Int,
	val column: Int,
	val declarationPath: String,
)
