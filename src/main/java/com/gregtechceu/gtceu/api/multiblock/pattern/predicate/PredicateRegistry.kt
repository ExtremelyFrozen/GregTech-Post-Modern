package com.gregtechceu.gtceu.api.multiblock.pattern.predicate

import net.minecraft.resources.ResourceLocation

import org.jspecify.annotations.NullMarked

import java.util.concurrent.ConcurrentHashMap

/** Explicit predicate type registry shared by Java, JSON and binary sources. */
@NullMarked
object PredicateRegistry {
	private val types = ConcurrentHashMap<ResourceLocation, PredicateType>()

	@JvmStatic
	fun register(id: ResourceLocation, type: PredicateType) {
		require(types.putIfAbsent(id, type) == null) {
			"Predicate type is already registered: $id"
		}
	}

	@JvmStatic
	fun require(id: ResourceLocation): PredicateType = types[id] ?: error("Unknown predicate type: $id")

	@JvmStatic
	fun clearForReload() {
		types.clear()
	}
}
