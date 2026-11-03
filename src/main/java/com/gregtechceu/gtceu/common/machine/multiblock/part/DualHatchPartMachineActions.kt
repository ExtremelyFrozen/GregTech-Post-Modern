package com.gregtechceu.gtceu.common.machine.multiblock.part

import com.gregtechceu.gtceu.GTCEu
import com.gregtechceu.gtceu.api.machine.feature.FancyActionMachine
import com.gregtechceu.gtceu.api.sync_system.SyncActionContext
import com.gregtechceu.gtceu.api.sync_system.SyncActionData
import com.gregtechceu.gtceu.api.sync_system.rpc.RpcAction
import com.gregtechceu.gtceu.api.sync_system.rpc.RpcRegistry

import net.minecraft.server.level.ServerPlayer

import org.jetbrains.annotations.ApiStatus

/**
 * Exposes only the dual hatch operations that its Kotlin server action handler is allowed to invoke.
 */
@ApiStatus.Internal
interface DualHatchFluidSlotActionTarget : FancyActionMachine {

	/** Returns the number of independently addressable fluid tanks. */
	fun getFluidTankCount(): Int

	/** Executes one validated fluid-container click against [tankIndex]. */
	fun clickFluidSlot(player: ServerPlayer, tankIndex: Int, shiftDown: Boolean)
}

/** Owns the wire protocol and server handler for dual hatch fluid-slot clicks. */
object DualHatchPartMachineActions {

	private val ACTION = GTCEu.id("click_dual_hatch_fluid_slot")

	/** Creates a request using the registered method parameter codecs. */
	@JvmStatic
	fun createClickDualHatchFluidSlotAction(tankIndex: Int, shiftDown: Boolean): SyncActionData = RpcRegistry.create(ACTION, tankIndex * 2 + if (shiftDown) 1 else 0, tankIndex, shiftDown)

	@RpcAction("click_dual_hatch_fluid_slot")
	fun apply(context: SyncActionContext, tankIndex: Int, shiftDown: Boolean): Boolean {
		val target = context.holder as? DualHatchFluidSlotActionTarget ?: return false
		if (tankIndex !in 0 until target.getFluidTankCount()) return false
		target.clickFluidSlot(context.player, tankIndex, shiftDown)
		return true
	}
}
