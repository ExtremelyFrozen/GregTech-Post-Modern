package com.gregtechceu.gtceu.common.machine.storage

import com.gregtechceu.gtceu.GTCEu
import com.gregtechceu.gtceu.api.sync_system.SyncActionContext
import com.gregtechceu.gtceu.api.sync_system.SyncActionData
import com.gregtechceu.gtceu.api.sync_system.rpc.RPCAction
import com.gregtechceu.gtceu.api.sync_system.rpc.RPCRegistry

import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.item.ItemStack

import org.jetbrains.annotations.ApiStatus

/** Exposes the four quantum chest UI operations that their Kotlin action handlers may invoke. */
@ApiStatus.Internal
interface QuantumChestActionTarget {

	/** Imports a validated requested item count from the opening player's cursor. */
	fun clickQuantumChestImportSlot(player: ServerPlayer, requestedItem: ItemStack, rightClick: Boolean)

	/** Exports one validated stack of stored items to the opening player. */
	fun exportQuantumChestItem(player: ServerPlayer)

	/** Applies one validated phantom locked-item selection or rejects an incompatible selection. */
	fun setQuantumChestLockedItem(item: ItemStack)

	/** Applies one validated lock-toggle command. */
	fun setQuantumChestLocked(locked: Boolean)
}

/** Owns the wire protocol and server handlers for the quantum chest UI state. */
object QuantumChestMachineActions {

	@JvmStatic
	fun createClickQuantumChestImportSlotAction(carried: ItemStack, rightClick: Boolean): SyncActionData {
		val item = carried.copy()
		val sequence = ItemStack.hashItemAndComponents(item) * 31 + item.count
		return RPCRegistry.create(GTCEu.id("click_quantum_chest_import_slot"), sequence * 31 + if (rightClick) 1 else 0, item, rightClick)
	}

	@JvmStatic
	fun createExportQuantumChestItemAction(): SyncActionData = RPCRegistry.create(GTCEu.id("export_quantum_chest_item"), 0)

	@JvmStatic
	fun createSetQuantumChestLockedItemAction(item: ItemStack): SyncActionData {
		val locked = if (item.isEmpty) ItemStack.EMPTY else item.copyWithCount(1)
		return RPCRegistry.create(GTCEu.id("set_quantum_chest_locked_item"), ItemStack.hashItemAndComponents(locked), locked)
	}

	@JvmStatic
	fun createSetQuantumChestLockedAction(locked: Boolean): SyncActionData = RPCRegistry.create(GTCEu.id("set_quantum_chest_locked"), if (locked) 1 else 0, locked)

	@RPCAction("click_quantum_chest_import_slot")
	fun clickImportSlot(context: SyncActionContext, item: ItemStack, rightClick: Boolean): Boolean {
		val target = context.holder as? QuantumChestActionTarget ?: return false
		target.clickQuantumChestImportSlot(context.player, item, rightClick)
		return true
	}

	@RPCAction("export_quantum_chest_item")
	fun exportItem(context: SyncActionContext): Boolean {
		val target = context.holder as? QuantumChestActionTarget ?: return false
		target.exportQuantumChestItem(context.player)
		return true
	}

	@RPCAction("set_quantum_chest_locked_item")
	fun setLockedItem(context: SyncActionContext, item: ItemStack): Boolean {
		val target = context.holder as? QuantumChestActionTarget ?: return false
		target.setQuantumChestLockedItem(item)
		return true
	}

	@RPCAction("set_quantum_chest_locked")
	fun setLocked(context: SyncActionContext, locked: Boolean): Boolean {
		val target = context.holder as? QuantumChestActionTarget ?: return false
		target.setQuantumChestLocked(locked)
		return true
	}
}
