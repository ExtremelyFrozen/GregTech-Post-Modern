package com.gregtechceu.gtceu.common.machine.multiblock.electric

import com.gregtechceu.gtceu.GTCEu
import com.gregtechceu.gtceu.api.sync_system.SyncActionContext
import com.gregtechceu.gtceu.api.sync_system.SyncActionData
import com.gregtechceu.gtceu.api.sync_system.rpc.RpcAction
import com.gregtechceu.gtceu.api.sync_system.rpc.RpcRegistry

import net.minecraft.core.BlockPos

import it.unimi.dsi.fastutil.objects.ObjectLinkedOpenHashSet
import org.jetbrains.annotations.ApiStatus

import java.util.UUID

/**
 * Exposes only the Central Monitor membership operations that server action handlers may validate and apply.
 *
 * The split validation and mutation methods keep packet decoding outside the machine while allowing the machine to
 * re-check its current structure and group collection immediately before each atomic change.
 */
@ApiStatus.Internal
@JvmSuppressWildcards
interface CentralMonitorMembershipActionTarget {

	/** Returns the incarnation that identifies this exact placed Central Monitor to an opened client page. */
	fun getCentralMonitorActionIncarnation(): UUID

	/** Returns the maximum number of positions addressable by the currently formed monitor grid. */
	fun getCentralMonitorMembershipCapacity(): Int

	/** Returns the persisted collection revision used to reject stale or replayed membership actions. */
	fun getCentralMonitorMembershipRevision(): Long

	/** Checks whether [positions] may form a new group with [groupIdentity] in the current structure. */
	fun canCreateCentralMonitorGroup(expectedRevision: Long, groupIdentity: UUID, positions: Set<BlockPos>): Boolean

	/** Creates the validated group and returns whether the current state still allowed the complete mutation. */
	fun createCentralMonitorGroup(expectedRevision: Long, groupIdentity: UUID, positions: Set<BlockPos>): Boolean

	/** Checks whether every requested position can be removed from the unique group identified by [groupIdentity]. */
	fun canRemoveCentralMonitorGroupMembers(expectedRevision: Long, groupIdentity: UUID, positions: Set<BlockPos>): Boolean

	/** Removes all validated positions atomically and returns whether the complete mutation was applied. */
	fun removeCentralMonitorGroupMembers(expectedRevision: Long, groupIdentity: UUID, positions: Set<BlockPos>): Boolean
}

/** Owns the strict wire protocol and server handlers for Central Monitor membership changes. */
object CentralMonitorMembershipActions {

	private const val MAX_MEMBERSHIP_POSITIONS = 65_536

	@JvmStatic
	fun createGroupAction(holderIncarnation: UUID, expectedRevision: Long, groupIdentity: UUID, positions: Set<BlockPos>, sequence: Int): SyncActionData = RpcRegistry.create(GTCEu.id("create_central_monitor_group"), sequence, holderIncarnation, expectedRevision, groupIdentity, positions.toList())

	@JvmStatic
	fun createRemoveGroupMembersAction(holderIncarnation: UUID, expectedRevision: Long, groupIdentity: UUID, positions: Set<BlockPos>, sequence: Int): SyncActionData =
		RpcRegistry.create(GTCEu.id("remove_central_monitor_group_members"), sequence, holderIncarnation, expectedRevision, groupIdentity, positions.toList())

	@RpcAction("create_central_monitor_group")
	fun createGroup(context: SyncActionContext, holderIncarnation: UUID, expectedRevision: Long, groupIdentity: UUID, positions: List<BlockPos>): Boolean {
		val target = context.holder as? CentralMonitorMembershipActionTarget ?: return false
		if (context.sequence() < 0 || target.getCentralMonitorActionIncarnation() != holderIncarnation) return false
		require(expectedRevision >= 0) { "Central Monitor membership revision must be non-negative." }
		require(positions.isNotEmpty() && positions.size <= MAX_MEMBERSHIP_POSITIONS) { "Invalid Central Monitor membership position count." }
		val members = ObjectLinkedOpenHashSet(positions)
		require(members.size == positions.size) { "Duplicate Central Monitor membership positions." }
		if (target.getCentralMonitorMembershipRevision() != expectedRevision || members.size > target.getCentralMonitorMembershipCapacity()) return false
		if (!target.canCreateCentralMonitorGroup(expectedRevision, groupIdentity, members)) return false
		check(target.createCentralMonitorGroup(expectedRevision, groupIdentity, members)) { "Central Monitor membership changed during execution." }
		return true
	}

	@RpcAction("remove_central_monitor_group_members")
	fun removeMembers(context: SyncActionContext, holderIncarnation: UUID, expectedRevision: Long, groupIdentity: UUID, positions: List<BlockPos>): Boolean {
		val target = context.holder as? CentralMonitorMembershipActionTarget ?: return false
		if (context.sequence() < 0 || target.getCentralMonitorActionIncarnation() != holderIncarnation) return false
		require(expectedRevision >= 0) { "Central Monitor membership revision must be non-negative." }
		require(positions.isNotEmpty() && positions.size <= MAX_MEMBERSHIP_POSITIONS) { "Invalid Central Monitor membership position count." }
		val members = ObjectLinkedOpenHashSet(positions)
		require(members.size == positions.size) { "Duplicate Central Monitor membership positions." }
		if (target.getCentralMonitorMembershipRevision() != expectedRevision || members.size > target.getCentralMonitorMembershipCapacity()) return false
		if (!target.canRemoveCentralMonitorGroupMembers(expectedRevision, groupIdentity, members)) return false
		check(target.removeCentralMonitorGroupMembers(expectedRevision, groupIdentity, members)) { "Central Monitor membership changed during execution." }
		return true
	}
}
