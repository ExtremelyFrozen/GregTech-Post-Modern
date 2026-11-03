package com.gregtechceu.gtceu.common.cover

import com.gregtechceu.gtceu.GTCEu
import com.gregtechceu.gtceu.api.sync_system.SyncActionContext
import com.gregtechceu.gtceu.api.sync_system.SyncActionData
import com.gregtechceu.gtceu.api.sync_system.rpc.RPCAction
import com.gregtechceu.gtceu.api.sync_system.rpc.RPCRegistry
import com.gregtechceu.gtceu.common.cover.data.BucketMode
import com.gregtechceu.gtceu.common.cover.data.TransferMode

import org.jetbrains.annotations.ApiStatus

/** Exposes only the ordered fluid regulator mutations that its Kotlin config action handler may invoke. */
@ApiStatus.Internal
interface FluidRegulatorCoverConfigActionTarget {

	/** Applies the validated transfer mode before values whose UI and normalization depend on that mode. */
	fun setTransferMode(transferMode: TransferMode)

	/** Applies the validated non-negative transfer limit in the regulator's base milli-bucket unit. */
	fun setGlobalTransferLimit(transferLimit: Int)

	/** Applies the validated display bucket mode after the base-unit transfer limit has been stored. */
	fun setTransferBucketMode(transferBucketMode: BucketMode)
}

/** Owns the wire action and server handler used by the fluid regulator configuration UI. */
object FluidRegulatorCoverConfigActions {

	private val ACTION = GTCEu.id("set_fluid_regulator_cover_config")

	/** Creates a request using the registered method parameter codecs. */
	@JvmStatic
	fun createSetConfigAction(transferMode: TransferMode, transferLimit: Int, transferBucketMode: BucketMode): SyncActionData = RPCRegistry.create(ACTION, 0, transferMode, transferLimit, transferBucketMode)

	@RPCAction("set_fluid_regulator_cover_config")
	fun apply(context: SyncActionContext, transferMode: TransferMode, transferLimit: Int, transferBucketMode: BucketMode): Boolean {
		val target = context.holder as? FluidRegulatorCoverConfigActionTarget ?: return false
		require(transferLimit >= 0) { "Fluid regulator transfer limit must be non-negative: $transferLimit" }
		target.setTransferMode(transferMode)
		target.setGlobalTransferLimit(transferLimit)
		target.setTransferBucketMode(transferBucketMode)
		return true
	}
}
