package com.gregtechceu.gtceu.common.cover.detector

import com.gregtechceu.gtceu.GTCEu
import com.gregtechceu.gtceu.api.sync_system.SyncActionContext
import com.gregtechceu.gtceu.api.sync_system.SyncActionData
import com.gregtechceu.gtceu.api.sync_system.rpc.RpcAction
import com.gregtechceu.gtceu.api.sync_system.rpc.RpcRegistry

/** Owns the wire protocol and server handler for advanced fluid detector configuration actions. */
object AdvancedFluidDetectorConfigActions {

	private val ACTION = GTCEu.id("set_advanced_fluid_detector_config")

	/** Creates a request using the registered method parameter codecs. */
	@JvmStatic
	fun createSetConfigAction(min: Int, max: Int, latched: Boolean, inverted: Boolean): SyncActionData = RpcRegistry.create(ACTION, 0, min, max, latched, inverted)

	@RpcAction("set_advanced_fluid_detector_config")
	fun apply(context: SyncActionContext, min: Int, max: Int, latched: Boolean, inverted: Boolean): Boolean {
		val cover = context.holder as? AdvancedFluidDetectorCover ?: return false
		require(min >= 0 && max >= 0) { "Advanced detector bounds must be non-negative." }
		cover.setMinValue(min)
		cover.setMaxValue(max)
		cover.setLatched(latched)
		cover.setInverted(inverted)
		return true
	}
}
