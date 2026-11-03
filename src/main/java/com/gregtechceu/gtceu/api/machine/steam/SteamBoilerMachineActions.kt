package com.gregtechceu.gtceu.api.machine.steam

import com.gregtechceu.gtceu.GTCEu
import com.gregtechceu.gtceu.api.sync_system.SyncActionContext
import com.gregtechceu.gtceu.api.sync_system.SyncActionData
import com.gregtechceu.gtceu.api.sync_system.rpc.RpcAction
import com.gregtechceu.gtceu.api.sync_system.rpc.RpcRegistry

import net.minecraft.server.level.ServerPlayer

import org.jetbrains.annotations.ApiStatus

/**
 * Exposes only the steam boiler fluid-slot operation that its Kotlin server action handler may invoke.
 *
 * This narrow contract keeps the wire protocol independent from the concrete boiler hierarchy while requiring
 * every accepted holder to opt in explicitly.
 */
@ApiStatus.Internal
interface SteamBoilerFluidSlotActionTarget {

	/**
	 * Executes one validated fluid-container click against [fluidSlot], preserving the concrete boiler's tank
	 * permissions and [shiftDown] cursor semantics.
	 */
	fun clickSteamBoilerFluidSlot(player: ServerPlayer, fluidSlot: Int, shiftDown: Boolean)
}

/** Owns the wire protocol and server handler for steam boiler fluid-slot clicks. */
object SteamBoilerMachineActions {

	private val ACTION = GTCEu.id("click_steam_boiler_fluid_slot")

	/** Creates a request using the registered method parameter codecs. */
	@JvmStatic
	fun createClickSteamBoilerFluidSlotAction(fluidSlot: Int, shiftDown: Boolean): SyncActionData = RpcRegistry.create(ACTION, fluidSlot, fluidSlot, shiftDown)

	@RpcAction("click_steam_boiler_fluid_slot")
	fun apply(context: SyncActionContext, fluidSlot: Int, shiftDown: Boolean): Boolean {
		val target = context.holder as? SteamBoilerFluidSlotActionTarget ?: return false
		require(fluidSlot in 0..2) { "Steam boiler fluid slot is out of range: $fluidSlot" }
		target.clickSteamBoilerFluidSlot(context.player, fluidSlot, shiftDown)
		return true
	}
}
