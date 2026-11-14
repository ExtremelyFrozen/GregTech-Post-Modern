package com.gregtechceu.gtceu.api.sync_system.managed

import com.gregtechceu.gtceu.GTCEu
import com.gregtechceu.gtceu.api.registry.GTRegistries
import com.gregtechceu.gtceu.api.sync_system.ContextualFieldCodec
import com.gregtechceu.gtceu.api.sync_system.FieldCodecs
import com.gregtechceu.gtceu.api.sync_system.SyncDataHolder
import com.gregtechceu.gtceu.api.sync_system.SyncFieldData
import com.gregtechceu.gtceu.api.sync_system.rpc.RPCMethodMeta
import com.gregtechceu.gtceu.api.sync_system.rpc.RPCSender

import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.level.ChunkPos
import net.neoforged.neoforge.network.PacketDistributor
import net.neoforged.neoforge.network.connection.ConnectionType

import com.mojang.serialization.Codec
import io.netty.buffer.Unpooled
import org.jetbrains.annotations.Nullable

/**
 * Represents a class with fields that have sync annotations. <br></br>
 * Differs from [ISyncAnnotated] in that more control is provided over syncing. <br></br>
 * An [ISyncManaged] class manages the sync status of itself and its fields,
 * while a [ISyncAnnotated] must be managed by a field in an [ISyncManaged] class.
 *
 *
 * A field of type `T` can be marked with sync annotations if:
 * <ul>
 * <li>`T` is primitive</li>
 * <li>`T` has a [com.gregtechceu.gtceu.api.sync_system.FieldCodecs] codec registered</li>
 * <li>`T` is an [ISyncManaged] or [ISyncAnnotated] class</li>
 * </ul>
 *
 * @see com.gregtechceu.gtceu.api.sync_system.SyncDataHolder
 * @see ISyncAnnotated
 */
interface ISyncManaged {
	fun getSyncDataHolder(): SyncDataHolder

	/** Root storage follows the same parent chain as dirty propagation and field synchronization. */
	fun getRootStorage(): SyncDataHolder = getParentSyncObject()?.getRootStorage() ?: getSyncDataHolder()

	/** Network roots supply the transport; addon roots override this factory without a central type list. */
	fun createRPCPacket(data: ByteArray): CustomPacketPayload = requireNotNull(getParentSyncObject()) { "Managed object has no RPC transport" }.createRPCPacket(data)

	fun getServerLevel(): ServerLevel = requireNotNull(getParentSyncObject()).getServerLevel()

	fun getTrackingPos(): ChunkPos = requireNotNull(getParentSyncObject()).getTrackingPos()

	private fun parseArgs2Bytes(methodName: String, args: Array<out Any>): ByteArray {
		val index = getRootStorage().getManaged().indexOfFirst { it === this }
		require(index >= 0) { "No such RPC managed object: $methodName" }
		val method = RPCMethodMeta.getMethods(javaClass).getValue(methodName)
		val buffer = RegistryFriendlyByteBuf(Unpooled.buffer(), GTRegistries.builtinRegistry(), ConnectionType.NEOFORGE)
		return try {
			buffer.writeVarInt(index)
			buffer.writeUtf(methodName)
			method.serializeArgs(buffer, args)
			ByteArray(buffer.readableBytes()).also { buffer.readBytes(it) }
		} finally {
			buffer.release()
		}
	}

	/** Invokes a server method on this object through its owning root. */
	fun rpcToServer(methodName: String, vararg args: Any) = PacketDistributor.sendToServer(createRPCPacket(parseArgs2Bytes(methodName, args)))

	/** Sends a client method to one player on the server game thread. */
	fun rpcToPlayer(player: ServerPlayer, methodName: String, vararg args: Any) = PacketDistributor.sendToPlayer(player, createRPCPacket(parseArgs2Bytes(methodName, args)))

	/** Sends a client method to every player tracking this object's block. */
	fun rpcToTracking(methodName: String, vararg args: Any) = PacketDistributor.sendToPlayersTrackingChunk(getServerLevel(), getTrackingPos(), createRPCPacket(parseArgs2Bytes(methodName, args)))

	fun handleRPCPacket(sender: RPCSender, data: ByteArray) {
		val buffer = RegistryFriendlyByteBuf(Unpooled.wrappedBuffer(data), GTRegistries.builtinRegistry(), ConnectionType.NEOFORGE)
		try {
			val managed = getRootStorage().getManaged().getOrNull(buffer.readVarInt()) ?: return
			val name = buffer.readUtf()
			RPCMethodMeta.getMethods(managed.javaClass).getValue(name).invoke(managed, sender, buffer)
		} catch (exception: Exception) {
			GTCEu.LOGGER.error("RPC on {} from {} failed", this, sender, exception)
		} finally {
			buffer.release()
		}
	}

	/**
	 * Gets the parent sync object of this sync object
	 *
	 * @return The parent sync object, can only return null if this object does not have a parent sync object and both
	 * [scheduleRenderUpdate] and [markAsChanged] are overriden
	 */
	@Nullable
	fun getParentSyncObject(): ISyncManaged?

	/**
	 * Function called when a synced field requests a rerender
	 */
	fun scheduleRenderUpdate() {
		getParentSyncObject()?.scheduleRenderUpdate()
	}

	/**
	 * Function called to notify the server that this object has been updated and must be synced to clients
	 */
	fun markAsChanged() {
		getParentSyncObject()?.markAsChanged()
	}

	companion object {
		val CODEC: ContextualFieldCodec<ISyncManaged> = object : ContextualFieldCodec<ISyncManaged> {
			override fun codec(context: ContextualFieldCodec.Context<ISyncManaged>): Codec<ISyncManaged> = SyncFieldData.CODEC.xmap(
				{ data ->
					val value = context.currentValue ?: FieldCodecs.create(requireNotNull(context.type.classValue)) as ISyncManaged
					value.getSyncDataHolder().deserializeFieldData(context.lookup, data, context.isClientSync, true, context.isClientFullSyncUpdate, false)
					value
				},
				{ value ->
					if (context.isClientSync && context.readOnly) {
						value.getSyncDataHolder().getClientData(context.lookup)
					} else {
						value.getSyncDataHolder().serializeToFieldData(context.lookup, context.isClientSync, context.isClientFullSyncUpdate)
					}
				},
			)
		}

		init {
			FieldCodecs.registerContextual(ISyncManaged::class.java, CODEC)
		}
	}
}
