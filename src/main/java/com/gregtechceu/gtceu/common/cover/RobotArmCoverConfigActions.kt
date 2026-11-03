package com.gregtechceu.gtceu.common.cover

import com.gregtechceu.gtceu.GTCEu
import com.gregtechceu.gtceu.api.sync_system.SyncActionContext
import com.gregtechceu.gtceu.api.sync_system.SyncActionData
import com.gregtechceu.gtceu.api.sync_system.rpc.RPCAction
import com.gregtechceu.gtceu.api.sync_system.rpc.RPCRegistry
import com.gregtechceu.gtceu.common.cover.data.TransferMode

/** Owns the wire action and server handler used by the robot arm configuration UI. */
object RobotArmCoverConfigActions {

	private val ACTION = GTCEu.id("set_robot_arm_cover_config")

	/** Creates a request using the registered method parameter codecs. */
	@JvmStatic
	fun createSetConfigAction(transferMode: TransferMode, transferLimit: Int): SyncActionData = RPCRegistry.create(ACTION, 0, transferMode, transferLimit)

	@RPCAction("set_robot_arm_cover_config")
	fun apply(context: SyncActionContext, transferMode: TransferMode, transferLimit: Int): Boolean {
		val cover = context.holder as? RobotArmCover ?: return false
		require(transferLimit >= 1) { "Robot arm transfer limit must be positive: $transferLimit" }
		cover.setTransferMode(transferMode)
		cover.setGlobalTransferLimit(transferLimit)
		return true
	}
}
