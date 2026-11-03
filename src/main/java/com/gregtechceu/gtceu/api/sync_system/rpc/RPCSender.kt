package com.gregtechceu.gtceu.api.sync_system.rpc

import net.minecraft.server.level.ServerPlayer

/**
 * Identifies the source of a received RPC. A null player denotes the server; a client request carries
 * its authenticated server player. This value is injected by the transport and is never decoded from the wire.
 */
class RPCSender private constructor(private val player: ServerPlayer?) {
	fun isServer(): Boolean = player == null

	fun isRemote(): Boolean = player != null

	fun asPlayer(): ServerPlayer? = player

	companion object {
		@JvmStatic
		fun ofServer(): RPCSender = SERVER

		@JvmStatic
		fun ofRemote(player: ServerPlayer): RPCSender = RPCSender(player)

		private val SERVER = RPCSender(null)
	}
}
