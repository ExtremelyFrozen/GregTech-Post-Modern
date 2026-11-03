package com.gregtechceu.gtceu.common.machine.multiblock.electric

import com.gregtechceu.gtceu.GTCEu
import com.gregtechceu.gtceu.api.item.IComponentItem
import com.gregtechceu.gtceu.api.sync_system.SyncActionContext
import com.gregtechceu.gtceu.api.sync_system.SyncActionData
import com.gregtechceu.gtceu.api.sync_system.rpc.RPCAction
import com.gregtechceu.gtceu.api.sync_system.rpc.RPCRegistry
import com.gregtechceu.gtceu.common.data.GTDataComponents
import com.gregtechceu.gtceu.common.item.modules.ImageModuleBehaviour

import net.minecraft.world.item.ItemStack

import org.jetbrains.annotations.ApiStatus

import java.util.UUID

/**
 * Exposes the compare-and-set operation required to change one Central Monitor image module URL.
 *
 * The handler passes both the opening module snapshot and the slot incarnation so a replaced module can never receive
 * configuration intended for the item that previously occupied the slot.
 */
@ApiStatus.Internal
interface CentralMonitorImageModuleActionHost {

	/** Returns the incarnation that identifies this exact placed Central Monitor to an opened client page. */
	fun getCentralMonitorActionIncarnation(): UUID

	/** Checks the complete image module request against the current group, slot, item, and URL state. */
	fun canSetCentralMonitorImageModuleUrl(groupIdentity: UUID, moduleSlotIncarnation: UUID, expectedModule: ItemStack, expectedUrl: String?, requestedUrl: String): Boolean

	/** Applies the image module request if every current-state check still succeeds. */
	fun setCentralMonitorImageModuleUrl(groupIdentity: UUID, moduleSlotIncarnation: UUID, expectedModule: ItemStack, expectedUrl: String?, requestedUrl: String): Boolean

	/** Publishes the authoritative groups again after a parsed action no longer matches current state. */
	fun resyncCentralMonitorImageModuleState()
}

/** Owns the strict wire protocol and server handler for Central Monitor image module URL changes. */
object CentralMonitorImageModuleActions {

	private val ACTION = GTCEu.id("set_central_monitor_image_module_url")

	private const val MAX_URL_LENGTH = 32_767

	@JvmStatic
	fun createSetImageModuleUrlAction(holderIncarnation: UUID, groupIdentity: UUID, moduleSlotIncarnation: UUID, expectedModule: ItemStack, requestedUrl: String, sequence: Int): SyncActionData =
		RPCRegistry.create(ACTION, sequence, holderIncarnation, groupIdentity, moduleSlotIncarnation, captureExpectedModule(expectedModule), requestedUrl)

	@RPCAction("set_central_monitor_image_module_url")
	fun apply(context: SyncActionContext, holderIncarnation: UUID, groupIdentity: UUID, moduleSlotIncarnation: UUID, expectedModule: ItemStack, requestedUrl: String): Boolean {
		val target = context.holder as? CentralMonitorImageModuleActionHost ?: return false
		if (context.sequence() < 0) return false
		require(hasValidStackCount(expectedModule) && isImageModule(expectedModule)) { "Image module action requires a valid module snapshot." }
		require(!expectedModule.has(GTDataComponents.TEXT_LINE_LIST.get())) { "Image module snapshot contains derived text." }
		val expectedUrl = expectedModule[GTDataComponents.IMAGE_MODULE_URL.get()]
		require(isValidUrl(requestedUrl) && (expectedUrl == null || isValidUrl(expectedUrl))) { "Image module URL is too long." }
		require(expectedUrl != requestedUrl) { "Image module action must change the URL." }
		if (target.getCentralMonitorActionIncarnation() != holderIncarnation || !target.canSetCentralMonitorImageModuleUrl(groupIdentity, moduleSlotIncarnation, expectedModule, expectedUrl, requestedUrl)) {
			target.resyncCentralMonitorImageModuleState()
			return false
		}
		val applied = target.setCentralMonitorImageModuleUrl(groupIdentity, moduleSlotIncarnation, expectedModule, expectedUrl, requestedUrl)
		if (!applied) target.resyncCentralMonitorImageModuleState()
		check(applied) { "Central Monitor image module changed during execution." }
		return true
	}

	@JvmStatic
	fun captureExpectedModule(module: ItemStack): ItemStack {
		require(hasValidStackCount(module)) {
			"Central Monitor module snapshot must contain a valid non-empty item stack."
		}
		return module.copy().also { snapshot -> snapshot.remove(GTDataComponents.TEXT_LINE_LIST.get()) }
	}

	@JvmStatic
	fun matchesExpectedModule(current: ItemStack, expected: ItemStack): Boolean {
		if (!hasValidStackCount(current) || !hasValidStackCount(expected)) return false
		return ItemStack.matches(captureExpectedModule(current), expected)
	}

	@JvmStatic
	fun isImageModule(stack: ItemStack): Boolean {
		val componentItem = stack.item as? IComponentItem ?: return false
		return componentItem.components.any { component -> component is ImageModuleBehaviour }
	}

	@JvmStatic
	fun isValidUrl(value: String): Boolean = value.length <= MAX_URL_LENGTH

	private fun hasValidStackCount(stack: ItemStack): Boolean = !stack.isEmpty && stack.count <= stack.maxStackSize
}
