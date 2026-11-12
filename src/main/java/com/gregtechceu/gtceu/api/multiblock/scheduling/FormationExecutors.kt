package com.gregtechceu.gtceu.api.multiblock.scheduling

import net.minecraft.server.MinecraftServer

import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap

import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/** Server-owned calculation lanes. Their tasks may only retain detached input and result data. */
class FormationExecutors private constructor() : AutoCloseable {

	val formation = FormationLane("gtpm-formation")
	val diagnosis: ExecutorService = Executors.newSingleThreadExecutor { task -> Thread(task, "gtpm-structure-diagnosis").also { it.isDaemon = true } }

	override fun close() {
		formation.close()
		diagnosis.shutdownNow()
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
