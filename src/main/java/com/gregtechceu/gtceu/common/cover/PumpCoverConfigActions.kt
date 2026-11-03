package com.gregtechceu.gtceu.common.cover

import com.gregtechceu.gtceu.GTCEu
import com.gregtechceu.gtceu.api.capability.recipe.IO
import com.gregtechceu.gtceu.api.sync_system.SyncActionContext
import com.gregtechceu.gtceu.api.sync_system.SyncActionData
import com.gregtechceu.gtceu.api.sync_system.rpc.RpcAction
import com.gregtechceu.gtceu.api.sync_system.rpc.RpcRegistry
import com.gregtechceu.gtceu.common.cover.data.BucketMode
import com.gregtechceu.gtceu.common.cover.data.ManualIOMode

import org.jetbrains.annotations.ApiStatus

/** Exposes only the pump configuration state and ordered mutations required by its Kotlin action handler. */
@ApiStatus.Internal
interface PumpCoverConfigActionTarget {

	/** Returns the target pump's maximum milli-bucket transfer rate for bucket-mode permission checks. */
	fun getMaxFluidTransferRate(): Int

	/** Applies the validated non-negative milli-bucket transfer rate before the remaining configuration. */
	fun setTransferRate(transferRate: Int)

	/** Applies the validated import or export direction after the transfer rate. */
	fun setIo(io: IO)

	/** Applies the validated bucket display mode after the transfer direction. */
	fun setBucketMode(bucketMode: BucketMode)

	/** Applies the validated manual I/O mode after the other pump settings. */
	fun setManualIOMode(manualIOMode: ManualIOMode)
}

/** Owns the wire protocol and server handler used by the pump configuration UI. */
object PumpCoverConfigActions {

	private val ACTION = GTCEu.id("set_pump_cover_config")

	/** Creates a request using the registered method parameter codecs. */
	@JvmStatic
	fun createSetConfigAction(transferRate: Int, io: IO, bucketMode: BucketMode, manualIOMode: ManualIOMode): SyncActionData = RpcRegistry.create(ACTION, 0, transferRate, io, bucketMode, manualIOMode)

	@RpcAction("set_pump_cover_config")
	fun apply(context: SyncActionContext, transferRate: Int, io: IO, bucketMode: BucketMode, manualIOMode: ManualIOMode): Boolean {
		val target = context.holder as? PumpCoverConfigActionTarget ?: return false
		require(transferRate >= 0) { "Pump transfer rate must be non-negative: $transferRate" }
		require(io == IO.IN || io == IO.OUT) { "Pump IO mode must be import or export: $io" }
		if (bucketMode.multiplier > target.getMaxFluidTransferRate()) return false
		target.setTransferRate(transferRate)
		target.setIo(io)
		target.setBucketMode(bucketMode)
		target.setManualIOMode(manualIOMode)
		return true
	}
}
