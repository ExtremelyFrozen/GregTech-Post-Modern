package com.gregtechceu.gtceu.common.machine.multiblock.part

import com.gregtechceu.gtceu.GTCEu
import com.gregtechceu.gtceu.api.machine.feature.FancyActionMachine
import com.gregtechceu.gtceu.api.sync_system.SyncActionContext
import com.gregtechceu.gtceu.api.sync_system.SyncActionData
import com.gregtechceu.gtceu.api.sync_system.rpc.RPCAction
import com.gregtechceu.gtceu.api.sync_system.rpc.RPCRegistry

import net.minecraft.server.level.ServerPlayer
import net.neoforged.neoforge.fluids.FluidStack

import org.jetbrains.annotations.ApiStatus

/**
 * Exposes only standalone fluid hatch operations that its Kotlin server action handlers may invoke.
 */
@ApiStatus.Internal
interface FluidHatchFluidSlotActionTarget : FancyActionMachine {

	/** Returns whether this definition uses the base fluid hatch LDLib2 page and action protocol. */
	fun supportsFluidHatchActions(): Boolean

	/** Returns whether this hatch exposes its single-tank locked-fluid controls. */
	fun supportsFluidHatchLocking(): Boolean

	/** Returns the number of independently addressable tanks. */
	fun getFluidHatchTankCount(): Int

	/** Executes one validated fluid-container click against [tankIndex]. */
	fun clickFluidHatchSlot(player: ServerPlayer, tankIndex: Int, shiftDown: Boolean)

	/** Applies one validated phantom locked-fluid selection. */
	fun setFluidHatchLockedFluid(fluid: FluidStack)

	/** Applies one validated lock-toggle command. */
	fun setFluidHatchLocked(locked: Boolean)
}

/** Owns the wire protocol and server handlers for the standalone fluid hatch page. */
object FluidHatchPartMachineActions {

	@JvmStatic
	fun createClickFluidSlotAction(tankIndex: Int, shiftDown: Boolean): SyncActionData = RPCRegistry.create(GTCEu.id("click_fluid_hatch_fluid_slot"), tankIndex * 2 + if (shiftDown) 1 else 0, tankIndex, shiftDown)

	@JvmStatic
	fun createSetLockedFluidAction(fluid: FluidStack): SyncActionData {
		val lockedFluid = if (fluid.isEmpty) FluidStack.EMPTY else fluid.copyWithAmount(1)
		return RPCRegistry.create(GTCEu.id("set_fluid_hatch_locked_fluid"), FluidStack.hashFluidAndComponents(lockedFluid) * 31 + lockedFluid.amount, lockedFluid)
	}

	@JvmStatic
	fun createSetLockedAction(locked: Boolean): SyncActionData = RPCRegistry.create(GTCEu.id("set_fluid_hatch_locked"), if (locked) 1 else 0, locked)

	@RPCAction("click_fluid_hatch_fluid_slot")
	fun clickFluidSlot(context: SyncActionContext, tankIndex: Int, shiftDown: Boolean): Boolean {
		val target = context.holder as? FluidHatchFluidSlotActionTarget ?: return false
		if (!target.supportsFluidHatchActions() || tankIndex !in 0 until target.getFluidHatchTankCount()) return false
		target.clickFluidHatchSlot(context.player, tankIndex, shiftDown)
		return true
	}

	@RPCAction("set_fluid_hatch_locked_fluid")
	fun setLockedFluid(context: SyncActionContext, fluid: FluidStack): Boolean {
		val target = context.holder as? FluidHatchFluidSlotActionTarget ?: return false
		if (!target.supportsFluidHatchActions() || !target.supportsFluidHatchLocking()) return false
		target.setFluidHatchLockedFluid(fluid)
		return true
	}

	@RPCAction("set_fluid_hatch_locked")
	fun setLocked(context: SyncActionContext, locked: Boolean): Boolean {
		val target = context.holder as? FluidHatchFluidSlotActionTarget ?: return false
		if (!target.supportsFluidHatchActions() || !target.supportsFluidHatchLocking()) return false
		target.setFluidHatchLocked(locked)
		return true
	}
}
