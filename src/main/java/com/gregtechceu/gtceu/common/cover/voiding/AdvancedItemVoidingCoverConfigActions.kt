package com.gregtechceu.gtceu.common.cover.voiding

import com.gregtechceu.gtceu.GTCEu
import com.gregtechceu.gtceu.api.sync_system.SyncActionContext
import com.gregtechceu.gtceu.api.sync_system.SyncActionData
import com.gregtechceu.gtceu.api.sync_system.rpc.RPCAction
import com.gregtechceu.gtceu.api.sync_system.rpc.RPCRegistry
import com.gregtechceu.gtceu.common.cover.data.VoidingMode

import org.jetbrains.annotations.ApiStatus

/** Exposes only the ordered mutations that the advanced item voiding config action may invoke. */
@ApiStatus.Internal
interface AdvancedItemVoidingCoverConfigActionTarget {

	/** Applies the validated voiding mode before the mode-dependent voiding limit. */
	fun setVoidingMode(voidingMode: VoidingMode)

	/** Applies the validated positive item voiding limit after the voiding mode. */
	fun setGlobalVoidingLimit(voidingLimit: Int)
}

/** Owns the wire protocol and server handler for advanced item voiding cover configuration. */
object AdvancedItemVoidingCoverConfigActions {

	private val ACTION = GTCEu.id("set_advanced_item_voiding_cover_config")

	/** Creates a request using the registered method parameter codecs. */
	@JvmStatic
	fun createSetConfigAction(voidingMode: VoidingMode, voidingLimit: Int): SyncActionData = RPCRegistry.create(ACTION, 0, voidingMode, voidingLimit)

	@RPCAction("set_advanced_item_voiding_cover_config")
	fun apply(context: SyncActionContext, voidingMode: VoidingMode, voidingLimit: Int): Boolean {
		val target = context.holder as? AdvancedItemVoidingCover ?: return false
		require(voidingLimit > 0) { "Advanced item voiding limit must be positive: $voidingLimit" }
		target.setVoidingMode(voidingMode)
		target.setGlobalVoidingLimit(voidingLimit)
		return true
	}
}
