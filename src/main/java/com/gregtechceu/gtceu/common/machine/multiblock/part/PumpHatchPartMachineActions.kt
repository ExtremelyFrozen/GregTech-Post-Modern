package com.gregtechceu.gtceu.common.machine.multiblock.part

import com.gregtechceu.gtceu.GTCEu
import com.gregtechceu.gtceu.api.machine.feature.FancyActionMachine
import com.gregtechceu.gtceu.api.sync_system.SyncActionContext
import com.gregtechceu.gtceu.api.sync_system.SyncActionData
import com.gregtechceu.gtceu.api.sync_system.rpc.RpcAction
import com.gregtechceu.gtceu.api.sync_system.rpc.RpcRegistry

import net.minecraft.server.level.ServerPlayer

import org.jetbrains.annotations.ApiStatus

/** Exposes the pump hatch operation that its Kotlin action handler may invoke. */
@ApiStatus.Internal
interface PumpHatchFluidSlotActionTarget : FancyActionMachine {

	/** Executes one validated fluid-container click against the pump hatch's fixed tank. */
	fun clickPumpHatchFluidSlot(player: ServerPlayer, shiftDown: Boolean)
}

/** Owns the wire protocol and server handler for pump hatch fluid-slot clicks. */
object PumpHatchPartMachineActions {

	private val ACTION = GTCEu.id("click_pump_hatch_fluid_slot")

	/** Creates a request using the registered method parameter codecs. */
	@JvmStatic
	fun createClickPumpHatchFluidSlotAction(shiftDown: Boolean): SyncActionData = RpcRegistry.create(ACTION, if (shiftDown) 1 else 0, shiftDown)

	@RpcAction("click_pump_hatch_fluid_slot")
	fun apply(context: SyncActionContext, shiftDown: Boolean): Boolean {
		val target = context.holder as? PumpHatchFluidSlotActionTarget ?: return false
		target.clickPumpHatchFluidSlot(context.player, shiftDown)
		return true
	}
}
