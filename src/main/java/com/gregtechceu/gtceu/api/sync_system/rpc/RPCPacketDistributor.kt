package com.gregtechceu.gtceu.api.sync_system.rpc

import com.gregtechceu.gtceu.GTCEu
import com.gregtechceu.gtceu.api.registry.GTRegistries
import com.gregtechceu.gtceu.common.network.packets.PacketRPCPacket

import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.level.ChunkPos
import net.neoforged.fml.ModList
import net.neoforged.neoforge.network.PacketDistributor
import net.neoforged.neoforge.network.connection.ConnectionType

import io.netty.buffer.Unpooled

import java.lang.annotation.ElementType
import java.lang.reflect.Modifier
import java.util.concurrent.ConcurrentHashMap

/** Global static RPC API matching LDLib2; handlers live on the actual classes declaring the operation. */
object RPCPacketDistributor {
	private val packets = ConcurrentHashMap<String, RPCMethodMeta>()

	@JvmStatic
	fun init() {
		val classes = HashSet<String>()
		for (file in ModList.get().modFiles) {
			for (annotation in file.file.scanResult.getAnnotatedBy(RPCPacket::class.java, ElementType.METHOD)) {
				val modId = annotation.annotationData()["modId"] as? String ?: ""
				if (modId.isEmpty() || ModList.get().isLoaded(modId)) classes.add(annotation.clazz().className)
			}
		}
		for (name in classes) {
			try {
				register(Class.forName(name))
			} catch (exception: ReflectiveOperationException) {
				throw IllegalStateException("Cannot discover RPC methods in $name", exception)
			}
		}
	}

	@JvmStatic
	fun register(type: Class<*>) {
		for (method in type.declaredMethods) {
			val annotation = method.getAnnotation(RPCPacket::class.java) ?: continue
			if (annotation.modId.isNotEmpty() && !ModList.get().isLoaded(annotation.modId)) continue
			require(Modifier.isStatic(method.modifiers)) { "@RPCPacket requires a static method: $method" }
			registerPacket(annotation.value, RPCMethodMeta(method))
		}
	}

	@JvmStatic
	fun registerPacket(packetID: String, handler: RPCMethodMeta) {
		require(packets.putIfAbsent(packetID, handler) == null) { "Duplicate RPC packet: $packetID" }
	}

	@JvmStatic
	fun getPacketHandler(packetID: String): RPCMethodMeta? = packets[packetID]

	@JvmStatic
	fun rpcToServer(packetID: String, vararg args: Any) = PacketDistributor.sendToServer(packet(packetID, args))

	@JvmStatic
	fun rpcToPlayer(player: ServerPlayer, packetID: String, vararg args: Any) = PacketDistributor.sendToPlayer(player, packet(packetID, args))

	@JvmStatic
	fun rpcToAllPlayers(packetID: String, vararg args: Any) = PacketDistributor.sendToAllPlayers(packet(packetID, args))

	@JvmStatic
	fun rpcToTracking(level: ServerLevel, chunkPos: ChunkPos, packetID: String, vararg args: Any) = PacketDistributor.sendToPlayersTrackingChunk(level, chunkPos, packet(packetID, args))

	private fun packet(id: String, args: Array<out Any>): PacketRPCPacket {
		val buffer = RegistryFriendlyByteBuf(Unpooled.buffer(), GTRegistries.builtinRegistry(), ConnectionType.NEOFORGE)
		return try {
			requireNotNull(packets[id]) { "Unknown RPC packet: $id" }.serializeArgs(buffer, args)
			PacketRPCPacket(id, ByteArray(buffer.readableBytes()).also { buffer.readBytes(it) })
		} finally {
			buffer.release()
		}
	}

	internal fun handle(id: String, sender: RPCSender, data: ByteArray) {
		val buffer = RegistryFriendlyByteBuf(Unpooled.wrappedBuffer(data), GTRegistries.builtinRegistry(), ConnectionType.NEOFORGE)
		try {
			requireNotNull(packets[id]) { "Unknown RPC packet: $id" }.invoke(null, sender, buffer)
		} catch (exception: Exception) {
			GTCEu.LOGGER.error("RPC packet {} from {} failed", id, sender, exception)
		} finally {
			buffer.release()
		}
	}
}
