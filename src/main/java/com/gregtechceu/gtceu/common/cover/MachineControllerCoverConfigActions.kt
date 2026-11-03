package com.gregtechceu.gtceu.common.cover

import com.gregtechceu.gtceu.GTCEu
import com.gregtechceu.gtceu.api.sync_system.SyncActionContext
import com.gregtechceu.gtceu.api.sync_system.SyncActionData
import com.gregtechceu.gtceu.api.sync_system.rpc.RpcAction
import com.gregtechceu.gtceu.api.sync_system.rpc.RpcRegistry
import com.gregtechceu.gtceu.common.cover.data.ControllerMode

import org.jetbrains.annotations.ApiStatus

import java.util.Optional

/** Exposes only the mode permission and ordered mutations required by the machine controller action handler. */
@ApiStatus.Internal
interface MachineControllerCoverConfigActionTarget {

	/** Returns the controller modes currently reachable from this cover. */
	fun getAllowedModes(): List<ControllerMode>

	/** Applies the validated nullable controller mode before all dependent settings. */
	fun setControllerMode(controllerMode: ControllerMode?)

	/** Applies the validated redstone threshold after the controller mode. */
	fun setMinRedstoneStrength(minRedstoneStrength: Int)

	/** Applies the validated inversion state after the redstone threshold. */
	fun setInverted(inverted: Boolean)

	/** Applies the validated power-failure behavior after the other controller settings. */
	fun setPreventPowerFail(preventPowerFail: Boolean)
}

/** Owns the wire protocol and server handler used by the machine controller cover UI. */
object MachineControllerCoverConfigActions {

	private val ACTION = GTCEu.id("set_machine_controller_cover_config")

	@JvmStatic
	fun createSetConfigAction(controllerMode: ControllerMode?, minRedstoneStrength: Int, inverted: Boolean, preventPowerFail: Boolean): SyncActionData = RpcRegistry.create(ACTION, 0, Optional.ofNullable(controllerMode), minRedstoneStrength, inverted, preventPowerFail)

	@RpcAction("set_machine_controller_cover_config")
	fun apply(context: SyncActionContext, requestedMode: Optional<ControllerMode>, minRedstoneStrength: Int, inverted: Boolean, preventPowerFail: Boolean): Boolean {
		val target = context.holder as? MachineControllerCover ?: return false
		require(minRedstoneStrength in 1..15) { "Machine controller redstone strength is out of range: $minRedstoneStrength" }
		val controllerMode = requestedMode.orElse(null)
		if (controllerMode != null && !target.getAllowedModes().contains(controllerMode)) return false
		target.setControllerMode(controllerMode)
		target.setMinRedstoneStrength(minRedstoneStrength)
		target.setInverted(inverted)
		target.setPreventPowerFail(preventPowerFail)
		return true
	}
}
