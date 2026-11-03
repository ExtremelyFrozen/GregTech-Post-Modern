package com.gregtechceu.gtceu.common.cover.ender

import com.gregtechceu.gtceu.GTCEu
import com.gregtechceu.gtceu.api.capability.recipe.IO
import com.gregtechceu.gtceu.api.misc.virtualregistry.VirtualEntry
import com.gregtechceu.gtceu.api.sync_system.SyncActionContext
import com.gregtechceu.gtceu.api.sync_system.SyncActionData
import com.gregtechceu.gtceu.api.sync_system.rpc.RPCAction
import com.gregtechceu.gtceu.api.sync_system.rpc.RPCRegistry
import com.gregtechceu.gtceu.common.cover.data.ManualIOMode

import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.server.level.ServerPlayer

import org.jetbrains.annotations.ApiStatus

import java.util.Locale

/** Exposes only the Ender Link mutations and owner-scoped channel access required by its action handlers. */
@ApiStatus.Internal
interface EnderLinkCoverActionTarget {

	/** Applies the normalized channel color before all other cover configuration. */
	fun setChannelName(channelColor: String)

	/** Applies the validated public or private registry scope after the channel color. */
	fun setPermission(permission: AbstractEnderLinkCover.Permissions)

	/** Applies the validated import or export direction after the registry scope. */
	fun setIo(io: IO)

	/** Applies the validated manual I/O policy after the transfer direction. */
	fun setManualIOMode(manualIOMode: ManualIOMode)

	/** Applies the working state after all transfer configuration. */
	fun setWorkingEnabled(workingEnabled: Boolean)

	/** Marks the Ender Link UI dirty after a configuration or description mutation. */
	fun markEnderLinkUIChanged()

	/** Returns channel names visible in this cover's public or owner-scoped private registry. */
	fun getEnderLinkActionChannelNames(): List<String>

	/** Finds one channel only within this cover's public or owner-scoped private registry. */
	fun findEnderLinkActionChannel(channelName: String): VirtualEntry?

	/** Replaces the description through the Java-owned virtual entry API. */
	fun setEnderLinkActionChannelDescription(entry: VirtualEntry, description: String)

	/** Reads the sortable color key through the Java-owned virtual entry API. */
	fun getEnderLinkActionChannelColor(entry: VirtualEntry): String

	/** Sends the sorted channel snapshot back to the requesting cover UI. */
	fun sendEnderLinkActionChannelList(player: ServerPlayer, pos: BlockPos, side: Direction, entries: List<VirtualEntry>)
}

/** Owns all four wire actions used by the Ender Link cover UI. */
object EnderLinkCoverActions {

	@JvmStatic
	fun createSetConfigAction(channelColor: String, permission: AbstractEnderLinkCover.Permissions, io: IO, manualIOMode: ManualIOMode, workingEnabled: Boolean): SyncActionData = RPCRegistry.create(
		GTCEu.id("set_ender_link_cover_config"),
		0,
		channelColor,
		permission,
		io,
		manualIOMode,
		workingEnabled,
	)

	@JvmStatic
	fun createSetDescriptionAction(channelName: String, description: String): SyncActionData = RPCRegistry.create(GTCEu.id("set_ender_link_channel_description"), 0, channelName, description)

	@JvmStatic
	fun createRequestChannelsAction(): SyncActionData = RPCRegistry.create(GTCEu.id("request_ender_link_channels"), 0)

	@JvmStatic
	fun createClearDescriptionAction(channelName: String): SyncActionData = RPCRegistry.create(GTCEu.id("clear_ender_link_channel_description"), 0, channelName)

	@RPCAction("set_ender_link_cover_config")
	fun setConfig(context: SyncActionContext, channelColor: String, permission: AbstractEnderLinkCover.Permissions, io: IO, manualIOMode: ManualIOMode, workingEnabled: Boolean): Boolean {
		val target = context.holder as? AbstractEnderLinkCover<*> ?: return false
		require(isValidColorInput(channelColor)) { "Ender link cover color is invalid: $channelColor" }
		require(io == IO.IN || io == IO.OUT) { "Ender link IO mode must be import or export: $io" }
		target.setChannelName(normalizeColorInput(channelColor))
		target.setPermission(permission)
		target.setIo(io)
		target.setManualIOMode(manualIOMode)
		target.setWorkingEnabled(workingEnabled)
		target.markEnderLinkUIChanged()
		return true
	}

	@RPCAction("set_ender_link_channel_description")
	fun setDescription(context: SyncActionContext, channelName: String, description: String): Boolean {
		val target = context.holder as? AbstractEnderLinkCover<*> ?: return false
		val entry = checkNotNull(target.findEnderLinkActionChannel(channelName)) { "Ender link channel is missing: $channelName" }
		target.setEnderLinkActionChannelDescription(entry, description)
		target.markEnderLinkUIChanged()
		return true
	}

	@RPCAction("request_ender_link_channels")
	fun requestChannels(context: SyncActionContext): Boolean {
		val target = context.holder as? AbstractEnderLinkCover<*> ?: return false
		sendChannelList(context, target)
		return true
	}

	@RPCAction("clear_ender_link_channel_description")
	fun clearDescription(context: SyncActionContext, channelName: String): Boolean {
		val target = context.holder as? AbstractEnderLinkCover<*> ?: return false
		require(isJavaNonBlank(channelName)) { "Ender link channel name is blank." }
		val entry = checkNotNull(target.findEnderLinkActionChannel(channelName)) { "Ender link channel is missing: $channelName" }
		target.setEnderLinkActionChannelDescription(entry, "")
		sendChannelList(context, target)
		return true
	}

	private fun sendChannelList(context: SyncActionContext, target: EnderLinkCoverActionTarget) {
		val pos = checkNotNull(context.pos) { "Ender link channel list action has no cover position." }
		val side = checkNotNull(context.side) { "Ender link channel list action has no cover side." }
		val entries = target.getEnderLinkActionChannelNames()
			.map { name -> checkNotNull(target.findEnderLinkActionChannel(name)) { "Ender link channel is missing: $name" } }
			.sortedBy { entry -> target.getEnderLinkActionChannelColor(entry) }
		target.sendEnderLinkActionChannelList(context.player, pos, side, entries)
	}

	private fun normalizeColorInput(color: String): String {
		var normalized = color
		if (normalized.length < 8) {
			normalized += "F".repeat(8 - normalized.length)
		}
		return normalized.uppercase(Locale.ROOT)
	}

	private fun isValidColorInput(color: String): Boolean = AbstractEnderLinkCover.COLOR_INPUT_PATTERN.matcher(color).matches()

	private fun isJavaNonBlank(value: String): Boolean = value.codePoints().anyMatch { codePoint -> !Character.isWhitespace(codePoint) }
}
