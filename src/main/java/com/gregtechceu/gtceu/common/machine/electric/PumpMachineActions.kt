package com.gregtechceu.gtceu.common.machine.electric

import com.gregtechceu.gtceu.GTCEu
import com.gregtechceu.gtceu.api.machine.feature.FancyActionMachine
import com.gregtechceu.gtceu.api.sync_system.SyncActionContext
import com.gregtechceu.gtceu.api.sync_system.SyncActionData
import com.gregtechceu.gtceu.api.sync_system.rpc.RPCAction
import com.gregtechceu.gtceu.api.sync_system.rpc.RPCRegistry

import net.minecraft.server.level.ServerPlayer

import org.jetbrains.annotations.ApiStatus

/** Exposes the electric pump operation that its Kotlin action handler may invoke. */
@ApiStatus.Internal
interface PumpFluidSlotActionTarget : FancyActionMachine {

	/** Executes one validated fluid-container click against the pump's fixed cache tank. */
	fun clickPumpFluidSlot(player: ServerPlayer, shiftDown: Boolean)
}

/** Owns the wire protocol and server handler for electric pump fluid-slot clicks. */
object PumpMachineActions {

	private val ACTION = GTCEu.id("click_pump_machine_fluid_slot")

	/** Creates a request using the registered method parameter codecs. */
	@JvmStatic
	fun createClickPumpFluidSlotAction(shiftDown: Boolean): SyncActionData = RPCRegistry.create(ACTION, if (shiftDown) 1 else 0, shiftDown)

	@RPCAction("click_pump_machine_fluid_slot")
	fun apply(context: SyncActionContext, shiftDown: Boolean): Boolean {
		val target = context.holder as? PumpFluidSlotActionTarget ?: return false
		target.clickPumpFluidSlot(context.player, shiftDown)
		return true
	}
}
