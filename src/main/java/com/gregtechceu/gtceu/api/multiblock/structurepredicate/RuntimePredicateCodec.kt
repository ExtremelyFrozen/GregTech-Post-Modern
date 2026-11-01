package com.gregtechceu.gtceu.api.multiblock.structurepredicate

import com.gregtechceu.gtceu.GTCEu

import com.mojang.serialization.DataResult
import com.mojang.serialization.DynamicOps
import com.mojang.serialization.JavaOps
import com.mojang.serialization.MapCodec
import com.mojang.serialization.MapLike
import com.mojang.serialization.RecordBuilder

import java.util.stream.Stream

/** Java DSL definitions retain their actual predicate object through JavaOps instead of serializing it to Gson. */
internal object RuntimePredicateCodec : MapCodec<PatternPredicate>() {
	val TYPE: PredicateType<PatternPredicate> by lazy {
		PredicateType(this) { it }.also { PredicateRegistry.register(GTCEu.id("java"), it) }
	}
	override fun <T> decode(ops: DynamicOps<T>, input: MapLike<T>): DataResult<PatternPredicate> {
		val runtime = input.get("runtime") as? PatternPredicate
		return if (runtime != null) DataResult.success(runtime) else DataResult.error { "A Java predicate requires its runtime object" }
	}
	override fun <T> encode(input: PatternPredicate, ops: DynamicOps<T>, prefix: RecordBuilder<T>): RecordBuilder<T> = if (ops === JavaOps.INSTANCE) prefix.add("runtime", input as T) else prefix.withErrorsFrom(DataResult.error<T> { "Java runtime predicates cannot be serialized" })
	override fun <T> keys(ops: DynamicOps<T>): Stream<T> = Stream.of(ops.createString("runtime"))
}
