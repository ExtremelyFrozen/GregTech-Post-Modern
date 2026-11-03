package com.gregtechceu.gtceu.integration.ae2.machine

import com.gregtechceu.gtceu.GTCEu
import com.gregtechceu.gtceu.api.machine.feature.FancyActionMachine
import com.gregtechceu.gtceu.api.sync_system.SyncActionContext
import com.gregtechceu.gtceu.api.sync_system.SyncActionData
import com.gregtechceu.gtceu.api.sync_system.rpc.RPCAction
import com.gregtechceu.gtceu.api.sync_system.rpc.RPCRegistry

import net.minecraft.server.level.ServerPlayer

import org.jetbrains.annotations.ApiStatus

/**
 * Exposes only the authoritative Pattern Buffer operations used by its holder-scoped LDLib2 actions.
 */
@ApiStatus.Internal
interface MEPatternBufferActionTarget : FancyActionMachine {

	/** Returns whether the exact machine definition owns the Pattern Buffer action protocol. */
	fun supportsMEPatternBufferActions(): Boolean

	/** Replaces the terminal group name without trimming or otherwise normalizing it. */
	fun setMEPatternBufferName(name: String)

	/** Attempts to return every buffered crafting input to the connected ME network. */
	fun refundMEPatternBufferContents()

	/** Returns the number of independently addressable shared fluid tanks. */
	fun getMEPatternBufferShareTankCount(): Int

	/** Executes one validated fluid-container interaction against a shared tank. */
	fun clickMEPatternBufferShareTank(player: ServerPlayer, tankIndex: Int, shiftDown: Boolean)
}

/** Owns the Pattern Buffer name, refund, and shared-tank wire protocols. */
object MEPatternBufferActions {

	@JvmStatic
	fun createSetNameAction(name: String): SyncActionData = RPCRegistry.create(GTCEu.id("set_me_pattern_buffer_name"), name.hashCode(), name)

	@JvmStatic
	fun createRefundAllAction(): SyncActionData = RPCRegistry.create(GTCEu.id("refund_me_pattern_buffer"), 0)

	@JvmStatic
	fun createClickShareTankAction(tankIndex: Int, shiftDown: Boolean): SyncActionData = RPCRegistry.create(GTCEu.id("click_me_pattern_buffer_share_tank"), tankIndex * 2 + if (shiftDown) 1 else 0, tankIndex, shiftDown)

	@RPCAction("set_me_pattern_buffer_name")
	fun setName(context: SyncActionContext, name: String): Boolean {
		val target = context.holder as? MEPatternBufferActionTarget ?: return false
		if (!target.supportsMEPatternBufferActions()) return false
		target.setMEPatternBufferName(name)
		return true
	}

	@RPCAction("refund_me_pattern_buffer")
	fun refundAll(context: SyncActionContext): Boolean {
		val target = context.holder as? MEPatternBufferActionTarget ?: return false
		if (!target.supportsMEPatternBufferActions()) return false
		target.refundMEPatternBufferContents()
		return true
	}

	@RPCAction("click_me_pattern_buffer_share_tank")
	fun clickShareTank(context: SyncActionContext, tankIndex: Int, shiftDown: Boolean): Boolean {
		val target = context.holder as? MEPatternBufferActionTarget ?: return false
		if (!target.supportsMEPatternBufferActions() || tankIndex !in 0 until target.getMEPatternBufferShareTankCount()) return false
		target.clickMEPatternBufferShareTank(context.player, tankIndex, shiftDown)
		return true
	}
}
