package com.gregtechceu.gtceu.api.multiblock.pattern.fragment

import com.gregtechceu.gtceu.api.multiblock.pattern.model.PatternFragment

import net.minecraft.resources.ResourceLocation

import org.jspecify.annotations.NullMarked

import java.util.concurrent.ConcurrentHashMap

/** Namespace-aware registry for reusable recursive fragments. */
@NullMarked
object FragmentRegistry {
	private val fragments = ConcurrentHashMap<ResourceLocation, PatternFragment>()

	@JvmStatic
	fun register(id: ResourceLocation, fragment: PatternFragment) {
		require(fragments.putIfAbsent(id, fragment) == null) {
			"Pattern fragment is already registered: $id"
		}
	}

	@JvmStatic
	fun require(id: ResourceLocation): PatternFragment = fragments[id] ?: error("Unknown pattern fragment: $id")

	@JvmStatic
	fun clearForReload() {
		fragments.clear()
	}
}
