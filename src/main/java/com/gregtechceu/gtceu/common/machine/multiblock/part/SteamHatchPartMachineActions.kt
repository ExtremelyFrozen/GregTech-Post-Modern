package com.gregtechceu.gtceu.common.machine.multiblock.part

import com.gregtechceu.gtceu.GTCEu
import com.gregtechceu.gtceu.api.machine.feature.FancyActionMachine
import com.gregtechceu.gtceu.api.sync_system.SyncActionContext
import com.gregtechceu.gtceu.api.sync_system.SyncActionData
import com.gregtechceu.gtceu.api.sync_system.rpc.RPCAction
import com.gregtechceu.gtceu.api.sync_system.rpc.RPCRegistry

import net.minecraft.server.level.ServerPlayer

import org.jetbrains.annotations.ApiStatus

/** Exposes the steam hatch operation that its Kotlin action handler may invoke. */
@ApiStatus.Internal
interface SteamHatchFluidSlotActionTarget : FancyActionMachine {

	/** Executes one validated fluid-container click against the steam tank. */
	fun clickSteamHatchFluidSlot(player: ServerPlayer, shiftDown: Boolean)
}

/** Owns the wire protocol and server handler for steam hatch fluid-slot clicks. */
object SteamHatchPartMachineActions {

	private val ACTION = GTCEu.id("click_steam_hatch_fluid_slot")

	/** Creates a request using the registered method parameter codecs. */
	@JvmStatic
	fun createClickSteamHatchFluidSlotAction(shiftDown: Boolean): SyncActionData = RPCRegistry.create(ACTION, if (shiftDown) 1 else 0, shiftDown)

	@RPCAction("click_steam_hatch_fluid_slot")
	fun apply(context: SyncActionContext, shiftDown: Boolean): Boolean {
		val target = context.holder as? SteamHatchFluidSlotActionTarget ?: return false
		target.clickSteamHatchFluidSlot(context.player, shiftDown)
		return true
	}
}
