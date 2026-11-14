package com.gregtechceu.gtceu.api.multiblock.structurepredicate

import com.gregtechceu.gtceu.api.registry.GTRegistries

import net.minecraft.resources.ResourceLocation

/** Setup registers actual codec/compiler entries; reload compilation only reads the published registry. */
object PredicateRegistry {
	@JvmStatic
	fun register(id: ResourceLocation, type: PredicateType<*>) {
		require(find(id) == null) { "Predicate type is already registered: $id" }
		GTRegistries.register(GTRegistries.PREDICATE_TYPES, id, type)
	}

	@JvmStatic
	fun require(id: ResourceLocation): PredicateType<*> = find(id) ?: error("Unknown predicate type: $id")

	fun find(id: ResourceLocation): PredicateType<*>? = GTRegistries.lookup(GTRegistries.PREDICATE_TYPES, id)
}
