package com.gregtechceu.gtceu.common.machine.storage

import com.gregtechceu.gtceu.GTCEu
import com.gregtechceu.gtceu.api.sync_system.SyncActionContext
import com.gregtechceu.gtceu.api.sync_system.SyncActionData
import com.gregtechceu.gtceu.api.sync_system.rpc.RPCAction
import com.gregtechceu.gtceu.api.sync_system.rpc.RPCRegistry

import net.minecraft.server.level.ServerPlayer
import net.neoforged.neoforge.fluids.FluidStack
import net.neoforged.neoforge.fluids.FluidType

import org.jetbrains.annotations.ApiStatus

/** Exposes the three quantum tank UI operations that their Kotlin action handlers may invoke. */
@ApiStatus.Internal
interface QuantumTankActionTarget {

	/** Executes one validated fluid-container click against the quantum tank's only fluid slot. */
	fun clickQuantumTankFluidSlot(player: ServerPlayer, shiftDown: Boolean)

	/** Applies one validated phantom locked-fluid selection. */
	fun setQuantumTankLockedFluid(fluid: FluidStack)

	/** Applies one validated lock-toggle command. */
	fun setQuantumTankLocked(locked: Boolean)
}

/** Owns the wire protocol and server handlers for the quantum tank UI state. */
object QuantumTankMachineActions {

	@JvmStatic
	fun createClickQuantumTankFluidSlotAction(shiftDown: Boolean): SyncActionData = RPCRegistry.create(GTCEu.id("click_quantum_tank_fluid_slot"), if (shiftDown) 1 else 0, shiftDown)

	@JvmStatic
	fun createSetQuantumTankLockedFluidAction(fluid: FluidStack): SyncActionData {
		val locked = if (fluid.isEmpty) FluidStack.EMPTY else fluid.copyWithAmount(FluidType.BUCKET_VOLUME)
		return RPCRegistry.create(GTCEu.id("set_quantum_tank_locked_fluid"), FluidStack.hashFluidAndComponents(locked) * 31 + locked.amount, locked)
	}

	@JvmStatic
	fun createSetQuantumTankLockedAction(locked: Boolean): SyncActionData = RPCRegistry.create(GTCEu.id("set_quantum_tank_locked"), if (locked) 1 else 0, locked)

	@RPCAction("click_quantum_tank_fluid_slot")
	fun clickFluidSlot(context: SyncActionContext, shiftDown: Boolean): Boolean {
		val target = context.holder as? QuantumTankActionTarget ?: return false
		target.clickQuantumTankFluidSlot(context.player, shiftDown)
		return true
	}

	@RPCAction("set_quantum_tank_locked_fluid")
	fun setLockedFluid(context: SyncActionContext, fluid: FluidStack): Boolean {
		val target = context.holder as? QuantumTankActionTarget ?: return false
		target.setQuantumTankLockedFluid(fluid)
		return true
	}

	@RPCAction("set_quantum_tank_locked")
	fun setLocked(context: SyncActionContext, locked: Boolean): Boolean {
		val target = context.holder as? QuantumTankActionTarget ?: return false
		target.setQuantumTankLocked(locked)
		return true
	}
}
