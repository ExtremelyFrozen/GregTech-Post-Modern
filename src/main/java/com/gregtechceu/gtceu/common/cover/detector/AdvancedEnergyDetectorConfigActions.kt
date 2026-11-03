package com.gregtechceu.gtceu.common.cover.detector

import com.gregtechceu.gtceu.GTCEu
import com.gregtechceu.gtceu.api.sync_system.SyncActionContext
import com.gregtechceu.gtceu.api.sync_system.SyncActionData
import com.gregtechceu.gtceu.api.sync_system.rpc.RpcAction
import com.gregtechceu.gtceu.api.sync_system.rpc.RpcRegistry

/** Owns the wire protocol and server handler for advanced energy detector configuration actions. */
object AdvancedEnergyDetectorConfigActions {

	private val ACTION = GTCEu.id("set_advanced_energy_detector_config")

	/** Creates a request using the registered method parameter codecs. */
	@JvmStatic
	fun createSetConfigAction(min: Long, max: Long, usePercent: Boolean, inverted: Boolean): SyncActionData = RpcRegistry.create(ACTION, 0, min, max, usePercent, inverted)

	@RpcAction("set_advanced_energy_detector_config")
	fun apply(context: SyncActionContext, min: Long, max: Long, usePercent: Boolean, inverted: Boolean): Boolean {
		val cover = context.holder as? AdvancedEnergyDetectorCover ?: return false
		require(min >= 0 && max >= 0) { "Advanced energy detector bounds must be non-negative." }
		cover.setUsePercent(usePercent)
		cover.setMinValue(min)
		cover.setMaxValue(max)
		cover.setInverted(inverted)
		return true
	}
}
