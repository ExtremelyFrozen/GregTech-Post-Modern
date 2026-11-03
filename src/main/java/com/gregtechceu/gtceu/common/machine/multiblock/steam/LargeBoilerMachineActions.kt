package com.gregtechceu.gtceu.common.machine.multiblock.steam

import com.gregtechceu.gtceu.GTCEu
import com.gregtechceu.gtceu.api.sync_system.SyncActionContext
import com.gregtechceu.gtceu.api.sync_system.SyncActionData
import com.gregtechceu.gtceu.api.sync_system.rpc.RpcAction
import com.gregtechceu.gtceu.api.sync_system.rpc.RpcRegistry

import org.jetbrains.annotations.ApiStatus

/** Exposes only the validated throttle mutation used by the large boiler action handler. */
@ApiStatus.Internal
interface LargeBoilerThrottleActionTarget {

	/** Applies one validated decrement or increment and updates the active fuel burn time. */
	fun adjustLargeBoilerThrottle(direction: Int)
}

/** Owns the wire protocol and server handler for large boiler throttle buttons. */
object LargeBoilerMachineActions {

	private val ACTION = GTCEu.id("adjust_large_boiler_throttle")

	/** Creates a request using the registered method parameter codecs. */
	@JvmStatic
	fun createAdjustLargeBoilerThrottleAction(direction: Int): SyncActionData = RpcRegistry.create(ACTION, if (direction > 0) 1 else 0, direction)

	@RpcAction("adjust_large_boiler_throttle")
	fun apply(context: SyncActionContext, direction: Int): Boolean {
		val target = context.holder as? LargeBoilerMachine ?: return false
		require(direction == -1 || direction == 1) { "Large boiler throttle direction must be -1 or 1: $direction" }
		target.adjustLargeBoilerThrottle(direction)
		return true
	}
}
