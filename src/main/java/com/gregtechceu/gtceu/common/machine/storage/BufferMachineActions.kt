package com.gregtechceu.gtceu.common.machine.storage

import com.gregtechceu.gtceu.GTCEu
import com.gregtechceu.gtceu.api.machine.feature.FancyActionMachine
import com.gregtechceu.gtceu.api.sync_system.SyncActionContext
import com.gregtechceu.gtceu.api.sync_system.SyncActionData
import com.gregtechceu.gtceu.api.sync_system.rpc.RPCAction
import com.gregtechceu.gtceu.api.sync_system.rpc.RPCRegistry

import net.minecraft.server.level.ServerPlayer

import org.jetbrains.annotations.ApiStatus

/** Exposes the buffer fluid-slot operation that its Kotlin action handler may invoke. */
@ApiStatus.Internal
interface BufferFluidSlotActionTarget : FancyActionMachine {

	/** Executes one validated fluid-container click against [tankIndex]. */
	fun clickBufferFluidSlot(player: ServerPlayer, tankIndex: Int, shiftDown: Boolean)
}

/** Owns the wire protocol and server handler for buffer fluid-slot clicks. */
object BufferMachineActions {

	private val ACTION = GTCEu.id("click_buffer_fluid_slot")

	/** Creates a request using the registered method parameter codecs. */
	@JvmStatic
	fun createClickFluidSlotAction(tankIndex: Int, shiftDown: Boolean): SyncActionData = RPCRegistry.create(ACTION, tankIndex * 2 + if (shiftDown) 1 else 0, tankIndex, shiftDown)

	@RPCAction("click_buffer_fluid_slot")
	fun apply(context: SyncActionContext, tankIndex: Int, shiftDown: Boolean): Boolean {
		val target = context.holder as? BufferFluidSlotActionTarget ?: return false
		require(tankIndex >= 0) { "Buffer tank index must be non-negative: $tankIndex" }
		target.clickBufferFluidSlot(context.player, tankIndex, shiftDown)
		return true
	}
}
