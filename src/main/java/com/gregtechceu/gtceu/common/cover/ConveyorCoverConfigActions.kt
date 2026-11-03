package com.gregtechceu.gtceu.common.cover

import com.gregtechceu.gtceu.GTCEu
import com.gregtechceu.gtceu.api.capability.recipe.IO
import com.gregtechceu.gtceu.api.sync_system.SyncActionContext
import com.gregtechceu.gtceu.api.sync_system.SyncActionData
import com.gregtechceu.gtceu.api.sync_system.rpc.RPCAction
import com.gregtechceu.gtceu.api.sync_system.rpc.RPCRegistry
import com.gregtechceu.gtceu.common.cover.data.DistributionMode
import com.gregtechceu.gtceu.common.cover.data.ManualIOMode

import org.jetbrains.annotations.ApiStatus

/** Exposes only the ordered conveyor mutations required by its Kotlin action handler. */
@ApiStatus.Internal
interface ConveyorCoverConfigActionTarget {

	/** Applies the validated positive item transfer rate before the remaining configuration. */
	fun setTransferRate(transferRate: Int)

	/** Applies the validated import or export direction after the transfer rate. */
	fun setIo(io: IO)

	/** Applies the validated distribution policy after the transfer direction. */
	fun setDistributionMode(distributionMode: DistributionMode)

	/** Applies the validated manual item I/O policy after the distribution policy. */
	fun setManualIOMode(manualIOMode: ManualIOMode)
}

/** Owns the wire protocol and server handler used by the conveyor configuration UI. */
object ConveyorCoverConfigActions {

	private val ACTION = GTCEu.id("set_conveyor_cover_config")

	/** Creates a request using the registered method parameter codecs. */
	@JvmStatic
	fun createSetConfigAction(transferRate: Int, io: IO, distributionMode: DistributionMode, manualIOMode: ManualIOMode): SyncActionData = RPCRegistry.create(ACTION, 0, transferRate, io, distributionMode, manualIOMode)

	@RPCAction("set_conveyor_cover_config")
	fun apply(context: SyncActionContext, transferRate: Int, io: IO, distributionMode: DistributionMode, manualIOMode: ManualIOMode): Boolean {
		val target = context.holder as? ConveyorCover ?: return false
		require(transferRate > 0) { "Conveyor transfer rate must be positive: $transferRate" }
		require(io == IO.IN || io == IO.OUT) { "Conveyor IO mode must be import or export: $io" }
		target.setTransferRate(transferRate)
		target.setIo(io)
		target.setDistributionMode(distributionMode)
		target.setManualIOMode(manualIOMode)
		return true
	}
}
