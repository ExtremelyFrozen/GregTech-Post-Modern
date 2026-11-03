package com.gregtechceu.gtceu.api.machine.fancyconfigurator

import com.gregtechceu.gtceu.GTCEu
import com.gregtechceu.gtceu.api.machine.feature.FancyActionMachine
import com.gregtechceu.gtceu.api.machine.feature.IHasCircuitSlot
import com.gregtechceu.gtceu.api.sync_system.SyncActionContext
import com.gregtechceu.gtceu.api.sync_system.SyncActionData
import com.gregtechceu.gtceu.api.sync_system.rpc.RPCAction
import com.gregtechceu.gtceu.api.sync_system.rpc.RPCRegistry
import com.gregtechceu.gtceu.common.item.behavior.IntCircuitBehaviour

/** Owns the GT action protocol for the LDLib2 machine circuit configurator. */
object CircuitConfiguratorActions {

	private val ACTION = GTCEu.id("set_machine_circuit_configuration")

	/** Creates a request using the registered method parameter codecs. */
	@JvmStatic
	fun createSetMachineCircuitConfigurationAction(configuration: Int): SyncActionData = RPCRegistry.create(ACTION, configuration, configuration)

	@RPCAction("set_machine_circuit_configuration")
	fun apply(context: SyncActionContext, configuration: Int): Boolean {
		val holder = context.holder
		if (holder !is IHasCircuitSlot || holder !is FancyActionMachine) return false
		if (!holder.isCircuitSlotEnabled || holder.circuitInventory.slots <= 0) return false
		require(configuration in -1..IntCircuitBehaviour.CIRCUIT_MAX) { "Machine circuit configuration is out of range: $configuration" }
		CircuitConfigurator.writeMachineCircuitConfiguration(holder.circuitInventory, configuration)
		return true
	}
}
