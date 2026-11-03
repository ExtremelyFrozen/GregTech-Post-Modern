package com.gregtechceu.gtceu.api.sync_system.managed

import com.gregtechceu.gtceu.GTCEu
import com.gregtechceu.gtceu.api.blockentity.BlockEntityCreationInfo
import com.gregtechceu.gtceu.api.gui.factory.MachineUIHolder
import com.gregtechceu.gtceu.api.machine.MetaMachine
import com.gregtechceu.gtceu.api.sync_system.SyncFieldData
import com.gregtechceu.gtceu.common.network.packets.CPacketMachineSyncToServer
import com.gregtechceu.gtceu.common.network.packets.SPacketMachineSyncToClient

import com.lowdragmc.lowdraglib2.gui.holder.ModularUIContainerMenu

import net.minecraft.core.BlockPos
import net.minecraft.core.HolderLookup
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.NbtOps
import net.minecraft.network.Connection
import net.minecraft.network.protocol.Packet
import net.minecraft.network.protocol.game.ClientGamePacketListener
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.level.ChunkPos
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState
import net.neoforged.neoforge.network.PacketDistributor

import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet
import org.jetbrains.annotations.MustBeInvokedByOverriders
import org.jetbrains.annotations.Nullable

/**
 * A BlockEntity that manages sync and save data via the `ISyncManaged` syncdata system.
 *
 * @see ISyncManaged
 */
abstract class ManagedSyncBlockEntity :
	BlockEntity,
	ISyncManaged {
	private val savedSyncDataKey = "${GTCEu.MOD_ID}_sync_data"

	private var dirty = false

	constructor(info: BlockEntityCreationInfo) : super(info.type(), info.pos(), info.state())

	constructor(type: BlockEntityType<*>, pos: BlockPos, blockState: BlockState) : super(type, pos, blockState)

	fun isDirty(): Boolean = dirty

	fun setDirty(dirty: Boolean) {
		this.dirty = dirty
	}

	/**
	 * Saves BE data to world save.
	 */
	final override fun saveAdditional(tag: CompoundTag, registries: HolderLookup.Provider) {
		super.saveAdditional(tag, registries)
		val savedData = getSyncDataHolder().serializeToSaveFieldData(registries)
		if (!savedData.isEmpty()) {
			tag.put(
				savedSyncDataKey,
				SyncFieldData.CODEC
					.encodeStart(registries.createSerializationContext(NbtOps.INSTANCE), savedData)
					.getOrThrow(),
			)
		}
	}

	/**
	 * Loads BE data from world save.<br></br>
	 * Override this to add logic for modifying saved data before it is loaded (e.g. for cross-version
	 * compatibility).<br></br>
	 * When overriding, `super.loadAdditional(tag, registries)` must be called **AFTER** any custom logic.
	 */
	@MustBeInvokedByOverriders
	override fun loadAdditional(tag: CompoundTag, registries: HolderLookup.Provider) {
		super.loadAdditional(tag, registries)
		if (tag.contains(savedSyncDataKey)) {
			val savedData = SyncFieldData.CODEC
				.parse(registries.createSerializationContext(NbtOps.INSTANCE), tag.get(savedSyncDataKey))
				.getOrThrow()
			getSyncDataHolder().deserializeFieldData(registries, savedData, false)
		}
	}

	/**
	 * Loads BE data from client update packet
	 */
	@MustBeInvokedByOverriders
	open fun clientLoad(tag: CompoundTag, registries: HolderLookup.Provider) {}

	final override fun handleUpdateTag(tag: CompoundTag, registries: HolderLookup.Provider) {}

	final override fun onDataPacket(net: Connection, pkt: ClientboundBlockEntityDataPacket, registries: HolderLookup.Provider) {}

	/**
	 * Called to gather BE data to be sent when a client loads this BE.
	 */
	override fun getUpdateTag(registries: HolderLookup.Provider): CompoundTag = CompoundTag()

	/**
	 * Called to get an update packet which is sent to clients to notify them when a loaded BE's data changes.
	 */
	@Nullable
	override fun getUpdatePacket(): Packet<ClientGamePacketListener>? = null

	@Nullable
	override fun getParentSyncObject(): ISyncManaged? = null

	final override fun markAsChanged() {
		dirty = true
	}

	override fun setChanged() {
		level?.blockEntityChanged(blockPos)
	}

	@MustBeInvokedByOverriders
	open fun serverTick() {
		setChanged()
		val serverLevel = level as? ServerLevel
		val storage = getSyncDataHolder()
		val fullSync = storage.isFullSyncPending()
		if (serverLevel != null && storage.scanAndMarkChanges(serverLevel.registryAccess())) {
			val changes = storage.collectClientNetworkChanges(serverLevel.registryAccess(), false)
			if (!changes.isEmpty()) {
				val recipients = ReferenceOpenHashSet(serverLevel.chunkSource.chunkMap.getPlayers(ChunkPos(blockPos), false))
				for (player in serverLevel.players()) {
					val holder = (player.containerMenu as? ModularUIContainerMenu)?.uiHolder as? MachineUIHolder
					if (holder?.getMachine() === this) recipients.add(player)
				}
				val packet = SPacketMachineSyncToClient(blockPos, changes, fullSync)
				for (player in recipients) PacketDistributor.sendToPlayer(player, packet)
			}
			dirty = true
		}
		if (dirty) {
			level!!.sendBlockUpdated(blockPos, blockState, blockState, Block.UPDATE_CLIENTS)
			dirty = false
		}
	}

	open fun clientTick() {}

	open fun sendServerSyncChanges() {
		if (level == null || !level!!.isClientSide) {
			return
		}

		val registryAccess = level!!.registryAccess()
		val rootChanges = getSyncDataHolder().collectServerNetworkChanges(registryAccess)
		if (!rootChanges.isEmpty()) {
			PacketDistributor.sendToServer(CPacketMachineSyncToServer(blockPos, BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(type)!!, rootChanges))
		}

		if (this is MetaMachine) {
			for (trait in syncTraits) {
				val traitChanges = trait.getSyncDataHolder().collectServerNetworkChanges(registryAccess)
				if (!traitChanges.isEmpty()) {
					PacketDistributor.sendToServer(CPacketMachineSyncToServer.forMachineTrait(this, trait, traitChanges))
				}
			}
		}
	}
}
