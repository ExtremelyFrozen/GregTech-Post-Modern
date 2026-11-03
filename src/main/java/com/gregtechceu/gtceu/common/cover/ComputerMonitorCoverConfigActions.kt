package com.gregtechceu.gtceu.common.cover

import com.gregtechceu.gtceu.GTCEu
import com.gregtechceu.gtceu.api.sync_system.SyncActionContext
import com.gregtechceu.gtceu.api.sync_system.SyncActionData
import com.gregtechceu.gtceu.api.sync_system.rpc.RpcAction
import com.gregtechceu.gtceu.api.sync_system.rpc.RpcRegistry

import org.jetbrains.annotations.ApiStatus

/** Exposes only the ordered monitor configuration mutations required by its Kotlin action handler. */
@ApiStatus.Internal
interface ComputerMonitorCoverConfigActionTarget {

	/** Replaces the displayed format lines before the placeholder arguments are applied. */
	fun replaceFormatStringLines(lines: List<String>)

	/** Replaces the placeholder arguments after the displayed format lines. */
	fun replaceFormatStringArgs(args: List<String>)

	/** Applies the validated refresh interval after both format lists. */
	fun setUpdateInterval(updateInterval: Int)
}

/** Owns the wire protocol and server handler used by the computer monitor configuration UI. */
object ComputerMonitorCoverConfigActions {

	private val ACTION = GTCEu.id("set_computer_monitor_cover_config")

	/** Creates a request using the registered method parameter codecs. */
	@JvmStatic
	fun createSetConfigAction(lines: List<String>, args: List<String>, updateInterval: Int): SyncActionData = RpcRegistry.create(ACTION, 0, lines.toList(), args.toList(), updateInterval)

	@RpcAction("set_computer_monitor_cover_config")
	fun apply(context: SyncActionContext, lines: List<String>, args: List<String>, updateInterval: Int): Boolean {
		val target = context.holder as? ComputerMonitorCover ?: return false
		require(updateInterval in 1..1200) { "Computer monitor update interval is out of range: $updateInterval" }
		target.replaceFormatStringLines(lines)
		target.replaceFormatStringArgs(args)
		target.setUpdateInterval(updateInterval)
		return true
	}
}
