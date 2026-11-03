package com.gregtechceu.gtceu.api.sync_system

import com.gregtechceu.gtceu.api.sync_system.rpc.RPCRegistry

import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.codec.StreamCodec
import net.minecraft.resources.ResourceLocation

/**
 * Network data for one client-requested sync action.
 *
 * @property actionId the registered server action identifier.
 * @property sequence the client sequence number used to order or correlate action requests.
 * @property payload the concrete value encoded by the handler registered for [actionId].
 */
@JvmRecord
data class SyncActionData(val actionId: ResourceLocation, val sequence: Int, val payload: Array<out Any>) {
	override fun toString(): String = "SyncActionData[actionId=$actionId, sequence=$sequence, payload=$payload]"

	companion object {
		/**
		 * Encodes action data with the same registry-aware buffer style as field sync packets.
		 */
		@JvmField
		val STREAM_CODEC: StreamCodec<RegistryFriendlyByteBuf, SyncActionData> =
			object : StreamCodec<RegistryFriendlyByteBuf, SyncActionData> {
				override fun decode(buffer: RegistryFriendlyByteBuf): SyncActionData {
					val actionId = ResourceLocation.STREAM_CODEC.decode(buffer)
					val sequence = buffer.readVarInt()
					val payload = RPCRegistry.decode(actionId, buffer)
					return SyncActionData(actionId, sequence, payload)
				}

				override fun encode(buffer: RegistryFriendlyByteBuf, value: SyncActionData) {
					ResourceLocation.STREAM_CODEC.encode(buffer, value.actionId)
					buffer.writeVarInt(value.sequence)
					RPCRegistry.encode(value.actionId, buffer, value.payload)
				}
			}
	}
}
