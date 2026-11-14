package com.gregtechceu.gtceu.api.multiblock.scheduling

import net.minecraft.server.MinecraftServer

import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap

/** Server-owned calculation lanes. Their tasks may only retain detached input and result data. */
class FormationExecutors private constructor() : AutoCloseable {

	val formation = FormationLane("gtpm-formation")

	override fun close() {
		formation.close()
	}

	companion object {
		private val servers = Reference2ObjectOpenHashMap<MinecraftServer, FormationExecutors>()

		@JvmStatic
		fun get(server: MinecraftServer): FormationExecutors = servers.computeIfAbsent(server) { FormationExecutors() }

		@JvmStatic
		fun release(server: MinecraftServer) {
			servers.remove(server)?.close()
		}
	}
}
