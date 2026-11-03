package com.gregtechceu.gtceu.api.machine.fancyconfigurator

import com.gregtechceu.gtceu.GTCEu
import com.gregtechceu.gtceu.api.capability.IControllable
import com.gregtechceu.gtceu.api.machine.feature.FancyActionMachine
import com.gregtechceu.gtceu.api.sync_system.SyncActionContext
import com.gregtechceu.gtceu.api.sync_system.SyncActionData
import com.gregtechceu.gtceu.api.sync_system.rpc.RpcAction
import com.gregtechceu.gtceu.api.sync_system.rpc.RpcRegistry

/** Owns the GT action protocol for the LDLib2 working-enabled configurator. */
object WorkingEnabledConfiguratorActions {

	private val ACTION = GTCEu.id("set_working_enabled")

	/** Creates a request using the registered method parameter codecs. */
	@JvmStatic
	fun createSetWorkingEnabledAction(workingEnabled: Boolean): SyncActionData = RpcRegistry.create(ACTION, if (workingEnabled) 1 else 0, workingEnabled)

	@RpcAction("set_working_enabled")
	fun apply(context: SyncActionContext, workingEnabled: Boolean): Boolean {
		val holder = context.holder
		if (holder !is IControllable || holder !is FancyActionMachine) return false
		holder.setWorkingEnabled(workingEnabled)
		return true
	}
}
