package com.gregtechceu.gtceu.common.cover.voiding

import com.gregtechceu.gtceu.GTCEu
import com.gregtechceu.gtceu.api.sync_system.SyncActionContext
import com.gregtechceu.gtceu.api.sync_system.SyncActionData
import com.gregtechceu.gtceu.api.sync_system.rpc.RPCAction
import com.gregtechceu.gtceu.api.sync_system.rpc.RPCRegistry

import org.jetbrains.annotations.ApiStatus

/** Exposes only the working-enabled mutation that the fluid voiding action handler may invoke. */
@ApiStatus.Internal
interface FluidVoidingWorkingEnabledActionTarget {

	/** Applies one validated working-enabled state through the cover's normal setter. */
	fun setWorkingEnabled(isWorkingAllowed: Boolean)
}

/** Owns the wire protocol and server handler for fluid voiding cover working-enabled actions. */
object FluidVoidingCoverConfigActions {

	private val ACTION = GTCEu.id("set_fluid_voiding_cover_config")

	/** Creates a request using the registered method parameter codecs. */
	@JvmStatic
	fun createSetWorkingEnabledAction(workingEnabled: Boolean): SyncActionData = RPCRegistry.create(ACTION, if (workingEnabled) 1 else 0, workingEnabled)

	@RPCAction("set_fluid_voiding_cover_config")
	fun apply(context: SyncActionContext, workingEnabled: Boolean): Boolean {
		val cover = context.holder as? FluidVoidingCover ?: return false
		cover.setWorkingEnabled(workingEnabled)
		return true
	}
}
