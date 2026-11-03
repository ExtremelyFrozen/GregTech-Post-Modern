package com.gregtechceu.gtceu.common.machine.multiblock.electric

import com.gregtechceu.gtceu.GTCEu
import com.gregtechceu.gtceu.api.sync_system.SyncActionContext
import com.gregtechceu.gtceu.api.sync_system.SyncActionData
import com.gregtechceu.gtceu.api.sync_system.rpc.RPCAction
import com.gregtechceu.gtceu.api.sync_system.rpc.RPCRegistry

import net.minecraft.core.BlockPos

import org.jetbrains.annotations.ApiStatus

import java.util.Optional
import java.util.UUID

/**
 * Current or requested raw target selection for one Central Monitor group.
 *
 * @property targetPos raw position of the selected grid component, or `null` when the target is cleared.
 * @property dataSlot zero-based data-item slot associated with [targetPos].
 */
@JvmRecord
data class CentralMonitorGroupTargetState(val targetPos: BlockPos?, val dataSlot: Int)

/** Exposes only the Central Monitor target operation that its server action handler may validate and apply. */
@ApiStatus.Internal
interface CentralMonitorGroupTargetActionHost {

	/** Returns the incarnation that identifies this exact placed Central Monitor to an opened client page. */
	fun getCentralMonitorActionIncarnation(): UUID

	/** Checks the complete compare-and-set request against the current formed grid and group state. */
	fun canSetCentralMonitorGroupTarget(groupIdentity: UUID, expected: CentralMonitorGroupTargetState, requested: CentralMonitorGroupTargetState): Boolean

	/** Applies the complete compare-and-set request if every current-state check still succeeds. */
	fun setCentralMonitorGroupTarget(groupIdentity: UUID, expected: CentralMonitorGroupTargetState, requested: CentralMonitorGroupTargetState): Boolean
}

/** Owns the strict wire protocol and server handler for Central Monitor group target changes. */
object CentralMonitorGroupTargetActions {

	private val ACTION = GTCEu.id("set_central_monitor_group_target")

	@JvmStatic
	fun createSetGroupTargetAction(holderIncarnation: UUID, groupIdentity: UUID, expected: CentralMonitorGroupTargetState, requested: CentralMonitorGroupTargetState, sequence: Int): SyncActionData =
		RPCRegistry.create(ACTION, sequence, holderIncarnation, groupIdentity, Optional.ofNullable(expected.targetPos), expected.dataSlot, Optional.ofNullable(requested.targetPos), requested.dataSlot)

	@RPCAction("set_central_monitor_group_target")
	fun apply(context: SyncActionContext, holderIncarnation: UUID, groupIdentity: UUID, expectedPosition: Optional<BlockPos>, expectedSlot: Int, requestedPosition: Optional<BlockPos>, requestedSlot: Int): Boolean {
		val target = context.holder as? CentralMonitorGroupTargetActionHost ?: return false
		if (context.sequence() < 0 || target.getCentralMonitorActionIncarnation() != holderIncarnation) return false
		require(expectedSlot >= 0 && requestedSlot >= 0) { "Central Monitor target slots must be non-negative." }
		val expected = CentralMonitorGroupTargetState(expectedPosition.orElse(null), expectedSlot)
		val requested = CentralMonitorGroupTargetState(requestedPosition.orElse(null), requestedSlot)
		require(requested.targetPos != null || requestedSlot == 0) { "A cleared monitor target must use slot zero." }
		require(expected != requested) { "Central Monitor target action must change the target state." }
		if (!target.canSetCentralMonitorGroupTarget(groupIdentity, expected, requested)) return false
		check(target.setCentralMonitorGroupTarget(groupIdentity, expected, requested)) { "Central Monitor target changed during execution." }
		return true
	}
}
