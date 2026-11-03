package com.gregtechceu.gtceu.integration.ae2.machine

import com.gregtechceu.gtceu.GTCEu
import com.gregtechceu.gtceu.api.machine.feature.FancyActionMachine
import com.gregtechceu.gtceu.api.sync_system.SyncActionContext
import com.gregtechceu.gtceu.api.sync_system.SyncActionData
import com.gregtechceu.gtceu.api.sync_system.rpc.RPCAction
import com.gregtechceu.gtceu.api.sync_system.rpc.RPCRegistry

import net.neoforged.neoforge.fluids.FluidStack

import org.jetbrains.annotations.ApiStatus

/**
 * Exposes only the authoritative ME fluid configuration operations used by holder-scoped actions.
 */
@ApiStatus.Internal
interface MEFluidConfigActionTarget : FancyActionMachine {

	/** Returns whether the exact machine definition owns the ME fluid configuration protocol. */
	fun supportsMEFluidConfigActions(): Boolean

	/** Returns the fixed number of addressable configuration slots. */
	fun getMEFluidConfigSlotCount(): Int

	/** Returns whether auto-pull currently owns all configuration slots. */
	fun isMEFluidConfigAutoPull(): Boolean

	/** Returns whether this target is the stocking hatch variant. */
	fun isMEFluidStocking(): Boolean

	/** Validates a complete set or clear operation before any authoritative mutation occurs. */
	fun canSetMEFluidConfig(slot: Int, fluid: FluidStack): Boolean

	/** Applies one previously validated set or clear operation. */
	fun setMEFluidConfig(slot: Int, fluid: FluidStack)

	/** Validates that [slot] still owns [expectedFluid] before changing its positive target amount. */
	fun canSetMEFluidConfigAmount(slot: Int, expectedFluid: FluidStack, amount: Int): Boolean

	/** Applies one amount update after revalidating the expected fluid identity. */
	fun setMEFluidConfigAmount(slot: Int, expectedFluid: FluidStack, amount: Int)

	/** Applies one previously validated stocking auto-pull state. */
	fun setMEFluidAutoPull(autoPull: Boolean)
}

/** Owns the ME fluid configuration wire protocol and its fail-fast server handlers. */
object MEFluidConfigActions {

	@JvmStatic
	fun createSetConfigAction(slot: Int, fluid: FluidStack): SyncActionData {
		val copied = fluid.copy()
		return RPCRegistry.create(GTCEu.id("set_me_fluid_config"), slot * 31 + FluidStack.hashFluidAndComponents(copied) * 31 + copied.amount, slot, copied)
	}

	@JvmStatic
	fun createSetAmountAction(slot: Int, expectedFluid: FluidStack, amount: Int): SyncActionData {
		val normalized = expectedFluid.copyWithAmount(1)
		return RPCRegistry.create(GTCEu.id("set_me_fluid_config_amount"), slot * 31 + FluidStack.hashFluidAndComponents(normalized) * 31 + amount, slot, normalized, amount)
	}

	@JvmStatic
	fun createSetAutoPullAction(autoPull: Boolean): SyncActionData = RPCRegistry.create(GTCEu.id("set_me_fluid_auto_pull"), if (autoPull) 1 else 0, autoPull)

	@RPCAction("set_me_fluid_config")
	fun setConfig(context: SyncActionContext, slot: Int, fluid: FluidStack): Boolean {
		val target = context.holder as? MEFluidConfigActionTarget ?: return false
		if (!target.supportsMEFluidConfigActions() || slot !in 0 until target.getMEFluidConfigSlotCount()) return false
		if (target.isMEFluidConfigAutoPull() || !target.canSetMEFluidConfig(slot, fluid)) return false
		target.setMEFluidConfig(slot, fluid)
		return true
	}

	@RPCAction("set_me_fluid_config_amount")
	fun setAmount(context: SyncActionContext, slot: Int, expectedFluid: FluidStack, amount: Int): Boolean {
		val target = context.holder as? MEFluidConfigActionTarget ?: return false
		if (!target.supportsMEFluidConfigActions() || slot !in 0 until target.getMEFluidConfigSlotCount()) return false
		require(amount > 0 && !expectedFluid.isEmpty) { "ME fluid amount request is invalid." }
		val normalized = expectedFluid.copyWithAmount(1)
		if (!target.canSetMEFluidConfigAmount(slot, normalized, amount)) return false
		target.setMEFluidConfigAmount(slot, normalized, amount)
		return true
	}

	@RPCAction("set_me_fluid_auto_pull")
	fun setAutoPull(context: SyncActionContext, autoPull: Boolean): Boolean {
		val target = context.holder as? MEFluidConfigActionTarget ?: return false
		if (!target.supportsMEFluidConfigActions() || !target.isMEFluidStocking()) return false
		target.setMEFluidAutoPull(autoPull)
		return true
	}
}
