package com.gregtechceu.gtceu.common.machine.storage

import com.gregtechceu.gtceu.GTCEu
import com.gregtechceu.gtceu.api.sync_system.SyncActionContext
import com.gregtechceu.gtceu.api.sync_system.SyncActionData
import com.gregtechceu.gtceu.api.sync_system.rpc.RpcAction
import com.gregtechceu.gtceu.api.sync_system.rpc.RpcRegistry

import net.neoforged.neoforge.fluids.FluidStack
import net.neoforged.neoforge.fluids.FluidType

import org.jetbrains.annotations.ApiStatus

/** Exposes only the creative tank mutation that its Kotlin server action handler may invoke. */
@ApiStatus.Internal
interface CreativeTankFluidActionTarget {

	/** Applies one validated phantom fluid selection, normalizing its stored amount. */
	fun setCreativeTankFluid(fluid: FluidStack)
}

/** Owns the wire protocol and server handler for the creative tank phantom fluid slot. */
object CreativeTankMachineActions {

	private val ACTION = GTCEu.id("set_creative_tank_fluid")

	@JvmStatic
	fun createSetFluidAction(fluid: FluidStack): SyncActionData {
		val storedFluid = if (fluid.isEmpty) FluidStack.EMPTY else fluid.copyWithAmount(FluidType.BUCKET_VOLUME)
		return RpcRegistry.create(ACTION, FluidStack.hashFluidAndComponents(storedFluid) * 31 + storedFluid.amount, storedFluid)
	}

	@RpcAction("set_creative_tank_fluid")
	fun apply(context: SyncActionContext, fluid: FluidStack): Boolean {
		val target = context.holder as? CreativeTankFluidActionTarget ?: return false
		target.setCreativeTankFluid(fluid)
		return true
	}
}
