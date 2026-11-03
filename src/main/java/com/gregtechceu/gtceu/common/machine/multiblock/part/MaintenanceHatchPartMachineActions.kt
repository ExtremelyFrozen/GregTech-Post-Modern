package com.gregtechceu.gtceu.common.machine.multiblock.part

import com.gregtechceu.gtceu.GTCEu
import com.gregtechceu.gtceu.api.sync_system.SyncActionContext
import com.gregtechceu.gtceu.api.sync_system.SyncActionData
import com.gregtechceu.gtceu.api.sync_system.rpc.RpcAction
import com.gregtechceu.gtceu.api.sync_system.rpc.RpcRegistry

import net.minecraft.server.level.ServerPlayer

import org.jetbrains.annotations.ApiStatus

/**
 * Exposes only the maintenance hatch operations that its Kotlin UI action handlers may invoke.
 *
 * This contract keeps holder authorization and the action protocol independent from the concrete hatch while
 * preserving the existing Java implementation of duration adjustment and player-driven repairs.
 */
@ApiStatus.Internal
interface MaintenanceHatchActionTarget {

	/** Returns whether this holder exposes the configurable duration controls. */
	fun supportsMaintenanceDurationAdjustment(): Boolean

	/** Applies one validated `-1` or `1` duration step through the holder's existing clamp logic. */
	fun adjustMaintenanceDuration(direction: Int)

	/** Runs the existing creative, duct-tape, and tool repair sequence for [player]. */
	fun fixMaintenanceProblemsForAction(player: ServerPlayer)
}

/** Owns the wire protocols and server handlers for maintenance hatch duration and repair controls. */
object MaintenanceHatchPartMachineActions {

	@JvmStatic
	fun createAdjustMaintenanceDurationAction(direction: Int): SyncActionData = RpcRegistry.create(GTCEu.id("adjust_maintenance_duration_multiplier"), if (direction > 0) 1 else 0, direction)

	@JvmStatic
	fun createFixMaintenanceProblemsAction(): SyncActionData = RpcRegistry.create(GTCEu.id("fix_maintenance_problems"), 0)

	@RpcAction("adjust_maintenance_duration_multiplier")
	fun adjustDuration(context: SyncActionContext, direction: Int): Boolean {
		val target = context.holder as? MaintenanceHatchActionTarget ?: return false
		if (!target.supportsMaintenanceDurationAdjustment()) return false
		require(direction == -1 || direction == 1) { "Maintenance duration direction must be -1 or 1: $direction" }
		target.adjustMaintenanceDuration(direction)
		return true
	}

	@RpcAction("fix_maintenance_problems")
	fun fixProblems(context: SyncActionContext): Boolean {
		val target = context.holder as? MaintenanceHatchActionTarget ?: return false
		target.fixMaintenanceProblemsForAction(context.player)
		return true
	}
}
