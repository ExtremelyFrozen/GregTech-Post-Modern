package com.gregtechceu.gtceu.integration.ae2.machine

import com.gregtechceu.gtceu.GTCEu
import com.gregtechceu.gtceu.api.machine.MetaMachine
import com.gregtechceu.gtceu.api.machine.feature.FancyActionMachine
import com.gregtechceu.gtceu.api.sync_system.SyncActionContext
import com.gregtechceu.gtceu.api.sync_system.SyncActionData
import com.gregtechceu.gtceu.api.sync_system.rpc.RPCAction
import com.gregtechceu.gtceu.api.sync_system.rpc.RPCRegistry
import com.gregtechceu.gtceu.common.machine.owner.MachineOwner
import com.gregtechceu.gtceu.common.network.packets.SPacketMEOutputWaitingListSessionToClient

import com.lowdragmc.lowdraglib2.gui.holder.ModularUIContainerMenu

import net.minecraft.core.BlockPos
import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.codec.StreamCodec
import net.minecraft.resources.ResourceLocation
import net.neoforged.neoforge.network.PacketDistributor

import java.util.Optional
import java.util.UUID

/**
 * Persistent identity of one ME output machine used by menu actions and waiting-list publications.
 *
 * @property pos output machine position captured by the contextual page.
 * @property machineDefinitionId exact machine definition captured by the contextual page.
 * @property incarnation persistent identity that changes when a machine at the same position is replaced.
 */
@JvmRecord
data class MEOutputWaitingListTarget(val pos: BlockPos, val machineDefinitionId: ResourceLocation, val incarnation: UUID) {
	private constructor(buffer: RegistryFriendlyByteBuf) : this(
		buffer.readBlockPos(),
		buffer.readResourceLocation(),
		buffer.readUUID(),
	)

	private fun encode(buffer: RegistryFriendlyByteBuf) {
		buffer.writeBlockPos(pos)
		buffer.writeResourceLocation(machineDefinitionId)
		buffer.writeUUID(incarnation)
	}

	companion object {

		/** Encodes the exact output bus identity shared by the action and packet protocols. */
		@JvmField
		val STREAM_CODEC: StreamCodec<RegistryFriendlyByteBuf, MEOutputWaitingListTarget> =
			StreamCodec.ofMember(MEOutputWaitingListTarget::encode, ::MEOutputWaitingListTarget)
	}
}

/** Owns the client request that registers one opening and sends its full waiting-list state. */
object MEOutputWaitingListActions {

	private val ACTION = GTCEu.id("request_me_output_waiting_list_full")

	@JvmStatic
	fun createRequestFullAction(target: MEOutputWaitingListTarget, openingId: UUID, requestSequence: Int): SyncActionData = RPCRegistry.create(ACTION, requestSequence, target, openingId, Optional.empty<UUID>())

	@JvmStatic
	fun createRequestFullAction(target: MEOutputWaitingListTarget, openingId: UUID, requestSequence: Int, menuSessionId: UUID): SyncActionData = RPCRegistry.create(ACTION, requestSequence, target, openingId, Optional.of(menuSessionId))

	@RPCAction("request_me_output_waiting_list_full")
	fun requestFull(context: SyncActionContext, target: MEOutputWaitingListTarget, openingId: UUID, menuSession: Optional<UUID>): Boolean {
		val root = context.holder
		if (root !is MetaMachine || root !is FancyActionMachine || context.sequence() < 0) return false
		if (!MachineOwner.canOpenOwnerMachine(context.player, root)) return false
		val receiver = MEOutputWaitingListRoute.resolveSessionForAction(context.player, root, target) ?: return false
		if (!receiver.canRequestFull(context.player)) return false
		val menuSessionId = menuSession.orElse(null)
		if (menuSessionId == null || !receiver.consumeWaitingListMenuSessionChallenge(openingId, context.sequence(), menuSessionId)) {
			if (menuSessionId != null) {
				GTCEu.LOGGER.warn("ME output waiting-list rejected an expired or mismatched menu challenge from {}", context.player.gameProfile.name)
			}
			sendMenuSessionChallenge(context, receiver, target, openingId)
			return true
		}
		if (!receiver.requestFull(context.player, openingId, context.sequence())) {
			GTCEu.LOGGER.warn("ME output waiting-list full request lost its active menu for {}", context.player.gameProfile.name)
		}
		return true
	}

	private fun sendMenuSessionChallenge(context: SyncActionContext, receiver: MEOutputWaitingListSessionReceiver, target: MEOutputWaitingListTarget, openingId: UUID) {
		val menu = context.player.containerMenu as? ModularUIContainerMenu
		if (menu == null) {
			GTCEu.LOGGER.warn(
				"ME output waiting-list could not challenge a missing LDLib2 menu for {}",
				context.player.gameProfile.name,
			)
			return
		}
		val menuSessionId = receiver.issueWaitingListMenuSessionChallenge(openingId, context.sequence())
		try {
			PacketDistributor.sendToPlayer(
				context.player,
				SPacketMEOutputWaitingListSessionToClient(
					menu.containerId,
					target,
					openingId,
					context.sequence(),
					menuSessionId,
				),
			)
		} catch (exception: RuntimeException) {
			GTCEu.LOGGER.error(
				"ME output waiting-list failed to challenge menu {} for {}",
				menu.containerId,
				context.player.gameProfile.name,
				exception,
			)
		}
	}
}
