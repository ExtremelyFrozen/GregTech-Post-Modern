package com.gregtechceu.gtceu.common.machine.multiblock.electric

import com.gregtechceu.gtceu.GTCEu
import com.gregtechceu.gtceu.api.item.IComponentItem
import com.gregtechceu.gtceu.api.sync_system.SyncActionContext
import com.gregtechceu.gtceu.api.sync_system.SyncActionData
import com.gregtechceu.gtceu.api.sync_system.rpc.RpcAction
import com.gregtechceu.gtceu.api.sync_system.rpc.RpcRegistry
import com.gregtechceu.gtceu.common.data.GTDataComponents
import com.gregtechceu.gtceu.common.item.datacomponents.TextLineList
import com.gregtechceu.gtceu.common.item.modules.TextModuleBehaviour

import net.minecraft.network.chat.Component
import net.minecraft.world.item.ItemStack

import org.jetbrains.annotations.ApiStatus

import java.util.UUID

/**
 * Exposes the compare-and-set operation required to change one Central Monitor text module configuration.
 *
 * The expected module snapshot retains editable text and every other stable component while excluding only values
 * derived by the server tick. This prevents one editor from overwriting a concurrent configuration change.
 */
@ApiStatus.Internal
interface CentralMonitorTextModuleActionHost {

	/** Returns the incarnation that identifies this exact placed Central Monitor to an opened client page. */
	fun getCentralMonitorActionIncarnation(): UUID

	/** Checks the complete request against the current group, slot, revision, module snapshot, and requested configuration. */
	fun canSetCentralMonitorTextModuleConfiguration(groupIdentity: UUID, moduleSlotIncarnation: UUID, expectedConfigurationRevision: Long, expectedModule: ItemStack, requestedConfiguration: TextLineList): Boolean

	/** Applies the text configuration if every current-state check still succeeds. */
	fun setCentralMonitorTextModuleConfiguration(groupIdentity: UUID, moduleSlotIncarnation: UUID, expectedConfigurationRevision: Long, expectedModule: ItemStack, requestedConfiguration: TextLineList): Boolean

	/** Publishes the authoritative groups again after a parsed action no longer matches current state. */
	fun resyncCentralMonitorTextModuleState()
}

/** Owns the strict wire protocol and server handler for Central Monitor text module configuration changes. */
object CentralMonitorTextModuleActions {

	private val ACTION = GTCEu.id("set_central_monitor_text_module_configuration")

	private const val MIN_TEXT_SCALE = 0.0001f
	private const val MAX_TEXT_SCALE = 1000.0f

	@JvmStatic
	fun createSetTextModuleConfigurationAction(holderIncarnation: UUID, groupIdentity: UUID, moduleSlotIncarnation: UUID, expectedConfigurationRevision: Long, expectedModule: ItemStack, requestedConfiguration: TextLineList, sequence: Int): SyncActionData =
		RpcRegistry.create(ACTION, sequence, holderIncarnation, groupIdentity, moduleSlotIncarnation, expectedConfigurationRevision, captureExpectedModule(expectedModule), requestedConfiguration.lines.map { it.string }, requestedConfiguration.scale)

	@RpcAction("set_central_monitor_text_module_configuration")
	fun apply(context: SyncActionContext, holderIncarnation: UUID, groupIdentity: UUID, moduleSlotIncarnation: UUID, expectedConfigurationRevision: Long, expectedModule: ItemStack, lines: List<String>, scale: Float): Boolean {
		val target = context.holder as? CentralMonitorTextModuleActionHost ?: return false
		if (context.sequence() < 0) return false
		require(expectedConfigurationRevision in 0 until Long.MAX_VALUE) { "Text module revision must be non-negative and incrementable." }
		require(hasValidStackCount(expectedModule) && isTextModule(expectedModule)) { "Text module action requires a valid module snapshot." }
		require(!expectedModule.has(GTDataComponents.TEXT_LINE_LIST.get()) && !expectedModule.has(GTDataComponents.PLACEHOLDER_UUID.get())) { "Text module snapshot contains derived values." }
		val requestedConfiguration = createConfiguration(lines, scale)
		require(expectedModule[GTDataComponents.FORMAT_STRING_LIST.get()] != requestedConfiguration) { "Text module action must change its configuration." }
		if (target.getCentralMonitorActionIncarnation() != holderIncarnation || !target.canSetCentralMonitorTextModuleConfiguration(groupIdentity, moduleSlotIncarnation, expectedConfigurationRevision, expectedModule, requestedConfiguration)) {
			target.resyncCentralMonitorTextModuleState()
			return false
		}
		val applied = target.setCentralMonitorTextModuleConfiguration(groupIdentity, moduleSlotIncarnation, expectedConfigurationRevision, expectedModule, requestedConfiguration)
		if (!applied) target.resyncCentralMonitorTextModuleState()
		check(applied) { "Central Monitor text module changed during execution." }
		return true
	}

	@JvmStatic
	fun createConfiguration(lines: List<String>, scale: Float): TextLineList {
		require(isValidScale(scale)) {
			"Central Monitor text module scale must be finite and between $MIN_TEXT_SCALE and $MAX_TEXT_SCALE: $scale"
		}
		return TextLineList(lines.map(Component::literal), scale)
	}

	@JvmStatic
	fun captureExpectedModule(module: ItemStack): ItemStack {
		require(hasValidStackCount(module)) {
			"Central Monitor text module snapshot must contain a valid non-empty item stack."
		}
		return module.copy().also { snapshot ->
			snapshot.remove(GTDataComponents.TEXT_LINE_LIST.get())
			snapshot.remove(GTDataComponents.PLACEHOLDER_UUID.get())
		}
	}

	@JvmStatic
	fun matchesExpectedModule(current: ItemStack, expected: ItemStack): Boolean {
		if (!hasValidStackCount(current) || !hasValidStackCount(expected)) return false
		return ItemStack.matches(captureExpectedModule(current), expected)
	}

	@JvmStatic
	fun isTextModule(stack: ItemStack): Boolean {
		val componentItem = stack.item as? IComponentItem ?: return false
		return componentItem.components.any { component -> component is TextModuleBehaviour }
	}

	@JvmStatic
	fun isValidScale(scale: Float): Boolean = scale.isFinite() && scale in MIN_TEXT_SCALE..MAX_TEXT_SCALE

	private fun hasValidStackCount(stack: ItemStack): Boolean = !stack.isEmpty && stack.count <= stack.maxStackSize
}
