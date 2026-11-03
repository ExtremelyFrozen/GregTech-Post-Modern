package com.gregtechceu.gtceu.common.cover

import com.gregtechceu.gtceu.GTCEu
import com.gregtechceu.gtceu.api.sync_system.SyncActionContext
import com.gregtechceu.gtceu.api.sync_system.SyncActionData
import com.gregtechceu.gtceu.api.sync_system.rpc.RpcAction
import com.gregtechceu.gtceu.api.sync_system.rpc.RpcRegistry
import com.gregtechceu.gtceu.common.cover.data.FilterMode
import com.gregtechceu.gtceu.common.cover.data.ManualIOMode

import org.jetbrains.annotations.ApiStatus

/** Exposes only the ordered fluid-filter mutations required by its Kotlin action handler. */
@ApiStatus.Internal
interface FluidFilterCoverConfigActionTarget {

	/** Applies the validated filter direction before the manual-flow policy. */
	fun setFilterMode(filterMode: FilterMode)

	/** Applies the validated manual-flow policy after the filter direction. */
	fun setAllowFlow(manualIOMode: ManualIOMode)
}

/** Owns the wire protocol and server handler used by the fluid filter cover UI. */
object FluidFilterCoverConfigActions {

	private val ACTION = GTCEu.id("set_fluid_filter_cover_config")

	/** Creates a request using the registered method parameter codecs. */
	@JvmStatic
	fun createSetConfigAction(filterMode: FilterMode, manualIOMode: ManualIOMode): SyncActionData = RpcRegistry.create(ACTION, 0, filterMode, manualIOMode)

	@RpcAction("set_fluid_filter_cover_config")
	fun apply(context: SyncActionContext, filterMode: FilterMode, manualIOMode: ManualIOMode): Boolean {
		val target = context.holder as? FluidFilterCover ?: return false
		target.setFilterMode(filterMode)
		target.setAllowFlow(manualIOMode)
		return true
	}
}
