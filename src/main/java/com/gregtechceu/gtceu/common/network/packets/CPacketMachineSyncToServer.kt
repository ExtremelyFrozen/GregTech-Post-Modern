package com.gregtechceu.gtceu.common.network.packets

import com.gregtechceu.gtceu.GTCEu
import com.gregtechceu.gtceu.api.machine.MetaMachine
import com.gregtechceu.gtceu.api.machine.trait.MachineTrait
import com.gregtechceu.gtceu.api.sync_system.SyncFieldData
import com.gregtechceu.gtceu.api.sync_system.managed.ISyncManaged
import com.gregtechceu.gtceu.api.sync_system.managed.ManagedSyncBlockEntity
import com.gregtechceu.gtceu.common.machine.owner.MachineOwner

import net.minecraft.core.BlockPos
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.entity.BlockEntity
import net.neoforged.neoforge.network.handling.IPayloadContext

open class CPacketMachineSyncToServer private constructor(private val pos: BlockPos, private val blockEntityTypeId: ResourceLocation, private val data: SyncFieldData, private val traitTarget: MachineTraitTarget?) : CustomPacketPayload {

	constructor(pos: BlockPos, blockEntityTypeId: ResourceLocation, data: SyncFieldData) : this(
		pos,
		blockEntityTypeId,
		data,
		null,
	)

	constructor(buffer: RegistryFriendlyByteBuf) : this(
		buffer.readBlockPos(),
		buffer.readResourceLocation(),
		SyncFieldData.STREAM_CODEC.decode(buffer),
		if (buffer.readBoolean()) MachineTraitTarget.decode(buffer) else null,
	)

	open fun encode(buffer: RegistryFriendlyByteBuf) {
		buffer.writeBlockPos(pos)
		buffer.writeResourceLocation(blockEntityTypeId)
		SyncFieldData.STREAM_CODEC.encode(buffer, data)
		buffer.writeBoolean(traitTarget != null)
		traitTarget?.encode(buffer)
	}

	open fun execute(context: IPayloadContext) {
		val player = context.player()
		if (player !is ServerPlayer) {
			GTCEu.LOGGER.warn("Sync: rejecting block entity field update without server player")
			return
		}

		if (data.isEmpty()) {
			GTCEu.LOGGER.warn(
				"Sync: rejecting block entity field update from {} because payload is empty",
				player.gameProfile.name,
			)
			return
		}

		val level: Level = player.level()
		if (!level.isLoaded(pos)) {
			GTCEu.LOGGER.warn(
				"Sync: rejecting block entity field update from {} because {} is not loaded",
				player.gameProfile.name,
				pos,
			)
			return
		}

		if (!canInteract(player, pos)) {
			GTCEu.LOGGER.warn(
				"Sync: rejecting block entity field update from {} because interaction is not allowed",
				player.gameProfile.name,
			)
			return
		}

		val blockEntity: BlockEntity? = level.getBlockEntity(pos)
		if (blockEntity !is ManagedSyncBlockEntity) {
			GTCEu.LOGGER.warn(
				"Sync: rejecting block entity field update from {} because holder at {} is invalid",
				player.gameProfile.name,
				pos,
			)
			return
		}

		val currentBlockEntityTypeId: ResourceLocation = BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(blockEntity.type)!!
		if (!currentBlockEntityTypeId.equals(blockEntityTypeId)) {
			GTCEu.LOGGER.warn(
				"Sync: rejecting block entity field update from {} because holder at {} changed",
				player.gameProfile.name,
				pos,
			)
			return
		}

		if (blockEntity is MetaMachine && !MachineOwner.canOpenOwnerMachine(player, blockEntity)) {
			GTCEu.LOGGER.warn(
				"Sync: rejecting block entity field update from {} because owner permission failed",
				player.gameProfile.name,
			)
			return
		}

		val syncTarget = resolveSyncTarget(player, blockEntity) ?: return
		val updateResult = try {
			syncTarget.getSyncDataHolder().tryApplyServerNetworkUpdate(level.registryAccess(), data)
		} catch (exception: RuntimeException) {
			GTCEu.LOGGER.error(
				"Sync: unexpected failure while applying block entity field update from {} at {} to {}",
				player.gameProfile.name,
				pos,
				syncTarget.javaClass.name,
				exception,
			)
			return
		}
		if (!updateResult.accepted) {
			GTCEu.LOGGER.warn(
				"Sync: rejecting block entity field update from {} at {} because {}",
				player.gameProfile.name,
				pos,
				updateResult.rejectionReason,
			)
			return
		}
		if (!updateResult.changed) {
			return
		}
		syncTarget.markAsChanged()
		blockEntity.setChanged()
	}

	private fun resolveSyncTarget(player: ServerPlayer, blockEntity: ManagedSyncBlockEntity): ISyncManaged? {
		val target = traitTarget ?: return blockEntity
		if (blockEntity !is MetaMachine) {
			GTCEu.LOGGER.warn(
				"Sync: rejecting machine trait field update from {} because holder at {} is not a machine",
				player.gameProfile.name,
				pos,
			)
			return null
		}
		if (blockEntity.definition.id != target.machineDefinitionId) {
			GTCEu.LOGGER.warn(
				"Sync: rejecting machine trait field update from {} because machine definition at {} changed from {} to {}",
				player.gameProfile.name,
				pos,
				target.machineDefinitionId,
				blockEntity.definition.id,
			)
			return null
		}

		val traits = blockEntity.getSyncTraits()
		if (target.index !in traits.indices) {
			GTCEu.LOGGER.warn(
				"Sync: rejecting machine trait field update from {} because trait index {} is missing at {}",
				player.gameProfile.name,
				target.index,
				pos,
			)
			return null
		}

		val trait = traits[target.index]
		if (trait.javaClass.name != target.className) {
			GTCEu.LOGGER.warn(
				"Sync: rejecting machine trait field update from {} because trait {} at index {} changed to {} at {}",
				player.gameProfile.name,
				target.className,
				target.index,
				trait.javaClass.name,
				pos,
			)
			return null
		}
		if (trait.machine !== blockEntity) {
			GTCEu.LOGGER.error(
				"Sync: rejecting machine trait field update from {} because trait {} at index {} does not belong to machine at {}",
				player.gameProfile.name,
				target.className,
				target.index,
				pos,
			)
			return null
		}
		return trait
	}

	private fun canInteract(player: ServerPlayer, pos: BlockPos): Boolean = !player.isSpectator && player.canInteractWithBlock(pos, MAX_INTERACTION_DISTANCE)

	override fun type(): Type<CPacketMachineSyncToServer> = TYPE

	companion object {
		@JvmField
		val ID: ResourceLocation = GTCEu.id("machine_sync_to_server")

		@JvmField
		val TYPE: Type<CPacketMachineSyncToServer> = Type(ID)

		@JvmField
		val CODEC: StreamCodec<RegistryFriendlyByteBuf, CPacketMachineSyncToServer> =
			StreamCodec.ofMember(CPacketMachineSyncToServer::encode, ::CPacketMachineSyncToServer)

		@JvmStatic
		fun forMachineTrait(machine: MetaMachine, trait: MachineTrait, data: SyncFieldData): CPacketMachineSyncToServer {
			val traitIndex = machine.getSyncTraits().indexOfFirst { current -> current === trait }
			return CPacketMachineSyncToServer(
				machine.blockPos,
				BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(machine.type)!!,
				data,
				MachineTraitTarget(machine.definition.id, traitIndex, trait.javaClass.name),
			)
		}

		private const val MAX_INTERACTION_DISTANCE = 8.0
	}

	private class MachineTraitTarget(val machineDefinitionId: ResourceLocation, val index: Int, val className: String) {
		fun encode(buffer: RegistryFriendlyByteBuf) {
			buffer.writeResourceLocation(machineDefinitionId)
			buffer.writeVarInt(index)
			buffer.writeUtf(className)
		}

		companion object {
			fun decode(buffer: RegistryFriendlyByteBuf): MachineTraitTarget = MachineTraitTarget(
				buffer.readResourceLocation(),
				buffer.readVarInt(),
				buffer.readUtf(),
			)
		}
	}
}
