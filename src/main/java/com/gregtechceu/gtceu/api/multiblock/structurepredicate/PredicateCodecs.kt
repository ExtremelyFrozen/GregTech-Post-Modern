package com.gregtechceu.gtceu.api.multiblock.structurepredicate

import com.mojang.datafixers.util.Either
import com.mojang.serialization.Codec
import it.unimi.dsi.fastutil.objects.ObjectArrayList
import it.unimi.dsi.fastutil.objects.ObjectList

/** Accepts the established single-value and list forms without adding a second serialization envelope. */
internal object PredicateCodecs {
	fun <A> oneOrMore(codec: Codec<A>): Codec<ObjectList<A>> = Codec.either(codec, codec.listOf()).xmap(
		{ either -> either.map({ ObjectList.of(it) }, { ObjectArrayList(it) }) },
		{ list -> if (list.size == 1) Either.left(list.first()) else Either.right(list) },
	)
}
