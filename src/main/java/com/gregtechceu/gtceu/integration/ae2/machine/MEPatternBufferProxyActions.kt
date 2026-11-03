package com.gregtechceu.gtceu.integration.ae2.machine

import com.gregtechceu.gtceu.GTCEu
import com.gregtechceu.gtceu.api.machine.feature.FancyActionMachine
import com.gregtechceu.gtceu.api.sync_system.SyncActionContext
import com.gregtechceu.gtceu.api.sync_system.SyncActionData
import com.gregtechceu.gtceu.api.sync_system.rpc.RpcAction
import com.gregtechceu.gtceu.api.sync_system.rpc.RpcRegistry
import com.gregtechceu.gtceu.common.item.behavior.IntCircuitBehaviour

import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.codec.StreamCodec
import net.minecraft.server.level.ServerPlayer

import org.jetbrains.annotations.ApiStatus

import java.util.UUID

/**
 * Immutable identity of the Proxy instance and linked Pattern Buffer captured by one menu opening.
 *
 * @property proxyIncarnation persistent Proxy identity that rejects packets queued for a replacement Proxy.
 * @property bufferPos linked Pattern Buffer position captured when the page opened.
 * @property linkRevision monotonically changing link generation captured when the page opened.
 */
@JvmRecord
data class MEPatternBufferProxyOpeningIdentity(val proxyIncarnation: UUID, val bufferPos: BlockPos, val linkRevision: Long) {
	private constructor(buffer: RegistryFriendlyByteBuf) : this(
		buffer.readUUID(),
		buffer.readBlockPos(),
		buffer.readLong(),
	)

	private fun encode(buffer: RegistryFriendlyByteBuf) {
		buffer.writeUUID(proxyIncarnation)
		buffer.writeBlockPos(bufferPos)
		buffer.writeLong(linkRevision)
	}

	companion object {

		/** Encodes the immutable Proxy and linked-buffer generation captured by the server opening. */
		@JvmField
		val STREAM_CODEC: StreamCodec<RegistryFriendlyByteBuf, MEPatternBufferProxyOpeningIdentity> =
			StreamCodec.ofMember(MEPatternBufferProxyOpeningIdentity::encode, ::MEPatternBufferProxyOpeningIdentity)
	}
}

/** Exposes only Proxy operations that revalidate an immutable opening before touching its linked buffer. */
@ApiStatus.Internal
interface MEPatternBufferProxyActionTarget : FancyActionMachine {

	/** Returns whether the player may still use the exact Proxy and linked-buffer opening. */
	fun canExecuteMEPatternBufferProxyAction(player: ServerPlayer, opening: MEPatternBufferProxyOpeningIdentity): Boolean

	/** Replaces the linked buffer name after revalidating [opening]. */
	fun setLinkedMEPatternBufferName(opening: MEPatternBufferProxyOpeningIdentity, name: String)

	/** Refunds the linked buffer contents after revalidating [opening]. */
	fun refundLinkedMEPatternBufferContents(opening: MEPatternBufferProxyOpeningIdentity)

	/** Returns the linked buffer tank count after revalidating [opening]. */
	fun getLinkedMEPatternBufferShareTankCount(opening: MEPatternBufferProxyOpeningIdentity): Int

	/** Executes one linked shared-tank interaction after revalidating [opening]. */
	fun clickLinkedMEPatternBufferShareTank(player: ServerPlayer, opening: MEPatternBufferProxyOpeningIdentity, tankIndex: Int, shiftDown: Boolean)

	/** Dispatches one allowlisted circuit action against the revalidated linked buffer. */
	fun configureLinkedMEPatternBufferCircuit(player: ServerPlayer, opening: MEPatternBufferProxyOpeningIdentity, configuration: Int)

	/** Dispatches one allowlisted cover action against the revalidated linked buffer. */
	fun configureLinkedMEPatternBufferCover(player: ServerPlayer, opening: MEPatternBufferProxyOpeningIdentity, side: Direction, operation: MEPatternBufferProxyCoverOperation)
}

/** Exact cover commands that a Proxy directional page may forward to its linked buffer. */
enum class MEPatternBufferProxyCoverOperation {
	PLACE,
	REMOVE,
	OPEN,
}

/** Owns the opening-bound wire protocol for every interactive Pattern Buffer Proxy control. */
object MEPatternBufferProxyActions {

	@JvmStatic
	fun createSetNameAction(opening: MEPatternBufferProxyOpeningIdentity, name: String): SyncActionData = RpcRegistry.create(GTCEu.id("set_me_pattern_buffer_proxy_name"), name.hashCode(), opening, name)

	@JvmStatic
	fun createRefundAllAction(opening: MEPatternBufferProxyOpeningIdentity): SyncActionData = RpcRegistry.create(GTCEu.id("refund_me_pattern_buffer_proxy"), 0, opening)

	@JvmStatic
	fun createClickShareTankAction(opening: MEPatternBufferProxyOpeningIdentity, tankIndex: Int, shiftDown: Boolean): SyncActionData = RpcRegistry.create(GTCEu.id("click_me_pattern_buffer_proxy_share_tank"), tankIndex * 2 + if (shiftDown) 1 else 0, opening, tankIndex, shiftDown)

	@JvmStatic
	fun createSetCircuitConfigurationAction(opening: MEPatternBufferProxyOpeningIdentity, configuration: Int): SyncActionData = RpcRegistry.create(GTCEu.id("set_me_pattern_buffer_proxy_circuit"), configuration, opening, configuration)

	@JvmStatic
	fun createPlaceCoverAction(opening: MEPatternBufferProxyOpeningIdentity, side: Direction): SyncActionData = createCoverAction(opening, side, MEPatternBufferProxyCoverOperation.PLACE)

	@JvmStatic
	fun createRemoveCoverAction(opening: MEPatternBufferProxyOpeningIdentity, side: Direction): SyncActionData = createCoverAction(opening, side, MEPatternBufferProxyCoverOperation.REMOVE)

	@JvmStatic
	fun createOpenCoverAction(opening: MEPatternBufferProxyOpeningIdentity, side: Direction): SyncActionData = createCoverAction(opening, side, MEPatternBufferProxyCoverOperation.OPEN)

	private fun createCoverAction(opening: MEPatternBufferProxyOpeningIdentity, side: Direction, operation: MEPatternBufferProxyCoverOperation): SyncActionData = RpcRegistry.create(GTCEu.id("configure_me_pattern_buffer_proxy_cover"), side.get3DDataValue(), opening, side, operation)

	@RpcAction("set_me_pattern_buffer_proxy_name")
	fun setName(context: SyncActionContext, opening: MEPatternBufferProxyOpeningIdentity, name: String): Boolean {
		val target = context.holder as? MEPatternBufferProxyActionTarget ?: return false
		if (!target.canExecuteMEPatternBufferProxyAction(context.player, opening)) return false
		target.setLinkedMEPatternBufferName(opening, name)
		return true
	}

	@RpcAction("refund_me_pattern_buffer_proxy")
	fun refundAll(context: SyncActionContext, opening: MEPatternBufferProxyOpeningIdentity): Boolean {
		val target = context.holder as? MEPatternBufferProxyActionTarget ?: return false
		if (!target.canExecuteMEPatternBufferProxyAction(context.player, opening)) return false
		target.refundLinkedMEPatternBufferContents(opening)
		return true
	}

	@RpcAction("click_me_pattern_buffer_proxy_share_tank")
	fun clickShareTank(context: SyncActionContext, opening: MEPatternBufferProxyOpeningIdentity, tankIndex: Int, shiftDown: Boolean): Boolean {
		val target = context.holder as? MEPatternBufferProxyActionTarget ?: return false
		if (!target.canExecuteMEPatternBufferProxyAction(context.player, opening)) return false
		if (tankIndex !in 0 until target.getLinkedMEPatternBufferShareTankCount(opening)) return false
		target.clickLinkedMEPatternBufferShareTank(context.player, opening, tankIndex, shiftDown)
		return true
	}

	@RpcAction("set_me_pattern_buffer_proxy_circuit")
	fun setCircuit(context: SyncActionContext, opening: MEPatternBufferProxyOpeningIdentity, configuration: Int): Boolean {
		val target = context.holder as? MEPatternBufferProxyActionTarget ?: return false
		if (!target.canExecuteMEPatternBufferProxyAction(context.player, opening)) return false
		require(configuration in -1..IntCircuitBehaviour.CIRCUIT_MAX) { "Proxy circuit configuration is out of range: $configuration" }
		target.configureLinkedMEPatternBufferCircuit(context.player, opening, configuration)
		return true
	}

	@RpcAction("configure_me_pattern_buffer_proxy_cover")
	fun configureCover(context: SyncActionContext, opening: MEPatternBufferProxyOpeningIdentity, side: Direction, operation: MEPatternBufferProxyCoverOperation): Boolean {
		val target = context.holder as? MEPatternBufferProxyActionTarget ?: return false
		if (!target.canExecuteMEPatternBufferProxyAction(context.player, opening)) return false
		target.configureLinkedMEPatternBufferCover(context.player, opening, side, operation)
		return true
	}
}
