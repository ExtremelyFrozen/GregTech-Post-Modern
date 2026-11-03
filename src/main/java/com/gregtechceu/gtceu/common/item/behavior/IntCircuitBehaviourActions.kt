package com.gregtechceu.gtceu.common.item.behavior

import com.gregtechceu.gtceu.GTCEu
import com.gregtechceu.gtceu.api.sync_system.SyncActionContext
import com.gregtechceu.gtceu.api.sync_system.SyncActionData
import com.gregtechceu.gtceu.api.sync_system.rpc.RPCAction
import com.gregtechceu.gtceu.api.sync_system.rpc.RPCRegistry

import net.minecraft.world.item.ItemStack

/** Owns the wire protocol and server handler for programmed circuit configuration actions. */
object IntCircuitBehaviourActions {

	private val ACTION = GTCEu.id("set_circuit_configuration")

	/** Creates a request using the registered method parameter codecs. */
	@JvmStatic
	fun createSetCircuitConfigurationAction(configuration: Int): SyncActionData = RPCRegistry.create(ACTION, configuration, configuration)

	@RPCAction("set_circuit_configuration")
	fun apply(context: SyncActionContext, configuration: Int): Boolean {
		val stack = context.holder as? ItemStack ?: return false
		val openedStack = context.openedStack ?: return false
		if (!IntCircuitBehaviour.isIntegratedCircuit(stack) || !IntCircuitBehaviour.isIntegratedCircuit(openedStack)) return false
		require(configuration in 0..IntCircuitBehaviour.CIRCUIT_MAX) { "Circuit configuration is out of range: $configuration" }
		IntCircuitBehaviour.setCircuitConfiguration(stack, configuration)
		return true
	}
}
