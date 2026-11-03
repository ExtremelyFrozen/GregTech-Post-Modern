package com.gregtechceu.gtceu.common.cover.voiding

import com.gregtechceu.gtceu.GTCEu
import com.gregtechceu.gtceu.api.sync_system.SyncActionContext
import com.gregtechceu.gtceu.api.sync_system.SyncActionData
import com.gregtechceu.gtceu.api.sync_system.rpc.RpcAction
import com.gregtechceu.gtceu.api.sync_system.rpc.RpcRegistry

/** Owns the wire protocol and server handler for item voiding cover working-enabled actions. */
object ItemVoidingCoverConfigActions {

	private val ACTION = GTCEu.id("set_item_voiding_cover_config")

	/** Creates a request using the registered method parameter codecs. */
	@JvmStatic
	fun createSetWorkingEnabledAction(workingEnabled: Boolean): SyncActionData = RpcRegistry.create(ACTION, if (workingEnabled) 1 else 0, workingEnabled)

	@RpcAction("set_item_voiding_cover_config")
	fun apply(context: SyncActionContext, workingEnabled: Boolean): Boolean {
		val cover = context.holder as? ItemVoidingCover ?: return false
		cover.setWorkingEnabled(workingEnabled)
		return true
	}
}
