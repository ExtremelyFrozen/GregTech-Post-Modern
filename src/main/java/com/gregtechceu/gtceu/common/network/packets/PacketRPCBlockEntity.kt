package com.gregtechceu.gtceu.common.network.packets

import com.gregtechceu.gtceu.GTCEu
import com.gregtechceu.gtceu.api.sync_system.managed.ISyncManaged
import com.gregtechceu.gtceu.api.sync_system.rpc.RPCSender

import net.minecraft.core.BlockPos
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.level.block.entity.BlockEntity
import net.neoforged.neoforge.network.handling.IPayloadContext

/** LDLib2-style bidirectional block holder transport; nested receivers belong to its root storage. */
class PacketRPCBlockEntity(private val pos: BlockPos, private val blockEntityType: ResourceLocation, private val data: ByteArray) : CustomPacketPayload {
	constructor(block: BlockEntity, data: ByteArray) : this(block.blockPos, requireNotNull(BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(block.type)), data)

	private constructor(buffer: RegistryFriendlyByteBuf) : this(buffer.readBlockPos(), buffer.readResourceLocation(), buffer.readByteArray())

	private fun encode(buffer: RegistryFriendlyByteBuf) {
		buffer.writeBlockPos(pos)
		buffer.writeResourceLocation(blockEntityType)
		buffer.writeByteArray(data)
	}

	fun execute(context: IPayloadContext) {
		val player = context.player()
		val level = player.level()
		if (!level.isLoaded(pos)) return
		val block = level.getBlockEntity(pos) ?: return
		if (BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(block.type) != blockEntityType || block !is ISyncManaged) return
		block.handleRPCPacket(if (player is ServerPlayer) RPCSender.ofRemote(player) else RPCSender.ofServer(), data)
	}

	override fun type(): Type<PacketRPCBlockEntity> = TYPE

	companion object {
		@JvmField val TYPE = Type<PacketRPCBlockEntity>(GTCEu.id("rpc_block_entity"))

		@JvmField val CODEC: StreamCodec<RegistryFriendlyByteBuf, PacketRPCBlockEntity> = StreamCodec.ofMember(PacketRPCBlockEntity::encode, ::PacketRPCBlockEntity)
	}
}
