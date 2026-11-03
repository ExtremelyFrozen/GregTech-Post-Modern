package com.gregtechceu.gtceu.common.machine.multiblock.electric

import com.gregtechceu.gtceu.GTCEu
import com.gregtechceu.gtceu.api.sync_system.SyncActionContext
import com.gregtechceu.gtceu.api.sync_system.SyncActionData
import com.gregtechceu.gtceu.api.sync_system.rpc.RPCAction
import com.gregtechceu.gtceu.api.sync_system.rpc.RPCRegistry

import org.jetbrains.annotations.ApiStatus

/** Exposes only the two idle-only Large Miner mode mutations used by its UI action handlers. */
@ApiStatus.Internal
interface LargeMinerModeActionTarget {

	/** Returns whether live mining currently blocks both mode changes. */
	fun isLargeMinerModeChangeBlocked(): Boolean

	/** Toggles silk touch through the existing Large Miner logic restriction. */
	fun toggleLargeMinerSilkTouch()

	/** Toggles chunk mode through the existing Large Miner logic and its single area reset. */
	fun toggleLargeMinerChunkMode()
}

/** Owns the empty-payload GT actions for the two clickable Large Miner display modes. */
object LargeMinerMachineActions {

	@JvmStatic
	fun createToggleSilkTouchAction(): SyncActionData = RPCRegistry.create(GTCEu.id("toggle_large_miner_silk_touch"), 0)

	@JvmStatic
	fun createToggleChunkModeAction(): SyncActionData = RPCRegistry.create(GTCEu.id("toggle_large_miner_chunk_mode"), 0)

	@RPCAction("toggle_large_miner_silk_touch")
	fun toggleSilkTouch(context: SyncActionContext): Boolean {
		val target = context.holder as? LargeMinerMachine ?: return false
		if (target.isLargeMinerModeChangeBlocked()) return false
		target.toggleLargeMinerSilkTouch()
		return true
	}

	@RPCAction("toggle_large_miner_chunk_mode")
	fun toggleChunkMode(context: SyncActionContext): Boolean {
		val target = context.holder as? LargeMinerMachine ?: return false
		if (target.isLargeMinerModeChangeBlocked()) return false
		target.toggleLargeMinerChunkMode()
		return true
	}
}
