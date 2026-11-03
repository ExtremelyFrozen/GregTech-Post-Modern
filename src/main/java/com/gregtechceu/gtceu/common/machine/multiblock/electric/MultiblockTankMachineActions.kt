package com.gregtechceu.gtceu.common.machine.multiblock.electric

import com.gregtechceu.gtceu.GTCEu
import com.gregtechceu.gtceu.api.sync_system.SyncActionContext
import com.gregtechceu.gtceu.api.sync_system.SyncActionData
import com.gregtechceu.gtceu.api.sync_system.rpc.RPCAction
import com.gregtechceu.gtceu.api.sync_system.rpc.RPCRegistry

import net.minecraft.server.level.ServerPlayer

import org.jetbrains.annotations.ApiStatus

/** Exposes the multiblock tank operation that its Kotlin action handler may invoke. */
@ApiStatus.Internal
interface MultiblockTankFluidSlotActionTarget {

	/** Executes one validated fluid-container click against the tank's only fluid slot. */
	fun clickMultiblockTankFluidSlot(player: ServerPlayer, shiftDown: Boolean)
}

/** Owns the wire protocol and server handler for multiblock tank fluid-slot clicks. */
object MultiblockTankMachineActions {

	private val ACTION = GTCEu.id("click_multiblock_tank_fluid_slot")

	/** Creates a request using the registered method parameter codecs. */
	@JvmStatic
	fun createClickMultiblockTankFluidSlotAction(shiftDown: Boolean): SyncActionData = RPCRegistry.create(ACTION, if (shiftDown) 1 else 0, shiftDown)

	@RPCAction("click_multiblock_tank_fluid_slot")
	fun apply(context: SyncActionContext, shiftDown: Boolean): Boolean {
		val target = context.holder as? MultiblockTankFluidSlotActionTarget ?: return false
		target.clickMultiblockTankFluidSlot(context.player, shiftDown)
		return true
	}
}
