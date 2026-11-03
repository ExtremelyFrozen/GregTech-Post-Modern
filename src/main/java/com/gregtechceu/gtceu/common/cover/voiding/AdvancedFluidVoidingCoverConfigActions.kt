package com.gregtechceu.gtceu.common.cover.voiding

import com.gregtechceu.gtceu.GTCEu
import com.gregtechceu.gtceu.api.sync_system.SyncActionContext
import com.gregtechceu.gtceu.api.sync_system.SyncActionData
import com.gregtechceu.gtceu.api.sync_system.rpc.RpcAction
import com.gregtechceu.gtceu.api.sync_system.rpc.RpcRegistry
import com.gregtechceu.gtceu.common.cover.data.BucketMode
import com.gregtechceu.gtceu.common.cover.data.VoidingMode

import org.jetbrains.annotations.ApiStatus

/** Exposes only the ordered mutations that the advanced fluid voiding config action may invoke. */
@ApiStatus.Internal
interface AdvancedFluidVoidingCoverConfigActionTarget {

	/** Applies the validated voiding mode before mode-dependent bucket and size state. */
	fun setVoidingMode(voidingMode: VoidingMode)

	/** Applies the validated display bucket mode before storing the base-unit transfer size. */
	fun setTransferBucketMode(transferBucketMode: BucketMode)

	/** Applies the validated positive transfer size in milli-buckets. */
	fun setGlobalTransferSizeMillibuckets(transferSize: Int)
}

/** Owns the wire protocol and server handler for advanced fluid voiding cover configuration. */
object AdvancedFluidVoidingCoverConfigActions {

	private val ACTION = GTCEu.id("set_advanced_fluid_voiding_cover_config")

	/** Creates a request using the registered method parameter codecs. */
	@JvmStatic
	fun createSetConfigAction(voidingMode: VoidingMode, transferSize: Int, bucketMode: BucketMode): SyncActionData = RpcRegistry.create(ACTION, 0, voidingMode, transferSize, bucketMode)

	@RpcAction("set_advanced_fluid_voiding_cover_config")
	fun apply(context: SyncActionContext, voidingMode: VoidingMode, transferSize: Int, bucketMode: BucketMode): Boolean {
		val target = context.holder as? AdvancedFluidVoidingCover ?: return false
		require(transferSize > 0) { "Advanced fluid voiding transfer size must be positive: $transferSize" }
		target.setVoidingMode(voidingMode)
		target.setTransferBucketMode(bucketMode)
		target.setGlobalTransferSizeMillibuckets(transferSize)
		return true
	}
}
