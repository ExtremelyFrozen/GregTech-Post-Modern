package com.gregtechceu.gtceu.integration.ae2.machine

import com.gregtechceu.gtceu.GTCEu
import com.gregtechceu.gtceu.api.machine.feature.FancyActionMachine
import com.gregtechceu.gtceu.api.sync_system.SyncActionContext
import com.gregtechceu.gtceu.api.sync_system.SyncActionData
import com.gregtechceu.gtceu.api.sync_system.rpc.RPCAction
import com.gregtechceu.gtceu.api.sync_system.rpc.RPCRegistry

import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.item.ItemStack

import org.jetbrains.annotations.ApiStatus

/**
 * Exposes only the authoritative ME item configuration operations used by holder-scoped actions.
 */
@ApiStatus.Internal
interface MEItemConfigActionTarget : FancyActionMachine {

	/** Returns whether the exact machine definition owns the ME item configuration protocol. */
	fun supportsMEItemConfigActions(): Boolean

	/** Returns the fixed number of addressable configuration slots. */
	fun getMEItemConfigSlotCount(): Int

	/** Returns whether auto-pull currently owns all configuration slots. */
	fun isMEItemConfigAutoPull(): Boolean

	/** Returns whether this target is the stocking bus variant. */
	fun isMEItemStocking(): Boolean

	/** Validates a complete set or clear operation before any authoritative mutation occurs. */
	fun canSetMEItemConfig(slot: Int, item: ItemStack): Boolean

	/** Applies one previously validated set or clear operation. */
	fun setMEItemConfig(slot: Int, item: ItemStack)

	/** Validates that [slot] still owns [expectedItem] before changing its positive target amount. */
	fun canSetMEItemConfigAmount(slot: Int, expectedItem: ItemStack, amount: Int): Boolean

	/** Applies one amount update after revalidating the expected item identity. */
	fun setMEItemConfigAmount(slot: Int, expectedItem: ItemStack, amount: Int)

	/** Validates the cursor, slot, and expected stock identity before one pickup. */
	fun canPickupMEItemConfigStock(player: ServerPlayer, slot: Int, expectedItem: ItemStack): Boolean

	/** Moves one validated saturated stock amount to [player]'s empty cursor. */
	fun pickupMEItemConfigStock(player: ServerPlayer, slot: Int, expectedItem: ItemStack)

	/** Applies one previously validated stocking auto-pull state. */
	fun setMEItemAutoPull(autoPull: Boolean)
}

/** Owns the ME item configuration wire protocol and its fail-fast server handlers. */
object MEItemConfigActions {

	@JvmStatic
	fun createSetConfigAction(slot: Int, item: ItemStack): SyncActionData {
		val copied = item.copy()
		return RPCRegistry.create(GTCEu.id("set_me_item_config"), slot * 31 + ItemStack.hashItemAndComponents(copied) * 31 + copied.count, slot, copied)
	}

	@JvmStatic
	fun createSetAmountAction(slot: Int, expectedItem: ItemStack, amount: Int): SyncActionData {
		val normalized = expectedItem.copyWithCount(1)
		return RPCRegistry.create(GTCEu.id("set_me_item_config_amount"), slot * 31 + ItemStack.hashItemAndComponents(normalized) * 31 + amount, slot, normalized, amount)
	}

	@JvmStatic
	fun createSetAutoPullAction(autoPull: Boolean): SyncActionData = RPCRegistry.create(GTCEu.id("set_me_item_auto_pull"), if (autoPull) 1 else 0, autoPull)

	@RPCAction("set_me_item_config")
	fun setConfig(context: SyncActionContext, slot: Int, item: ItemStack): Boolean {
		val target = context.holder as? MEItemConfigActionTarget ?: return false
		if (!target.supportsMEItemConfigActions() || slot !in 0 until target.getMEItemConfigSlotCount()) return false
		if (target.isMEItemConfigAutoPull() || !target.canSetMEItemConfig(slot, item)) return false
		target.setMEItemConfig(slot, item)
		return true
	}

	@RPCAction("set_me_item_config_amount")
	fun setAmount(context: SyncActionContext, slot: Int, expectedItem: ItemStack, amount: Int): Boolean {
		val target = context.holder as? MEItemConfigActionTarget ?: return false
		if (!target.supportsMEItemConfigActions() || slot !in 0 until target.getMEItemConfigSlotCount()) return false
		require(amount > 0 && !expectedItem.isEmpty && expectedItem.count == 1) { "ME item amount request is invalid." }
		if (target.isMEItemStocking() || target.isMEItemConfigAutoPull()) return false
		if (!target.canSetMEItemConfigAmount(slot, expectedItem, amount)) return false
		target.setMEItemConfigAmount(slot, expectedItem, amount)
		return true
	}

	@RPCAction("set_me_item_auto_pull")
	fun setAutoPull(context: SyncActionContext, autoPull: Boolean): Boolean {
		val target = context.holder as? MEItemConfigActionTarget ?: return false
		if (!target.supportsMEItemConfigActions() || !target.isMEItemStocking()) return false
		target.setMEItemAutoPull(autoPull)
		return true
	}

	@JvmStatic
	fun createPickupStockAction(slot: Int, expectedItem: ItemStack): SyncActionData {
		val normalized = expectedItem.copyWithCount(1)
		return RPCRegistry.create(GTCEu.id("pickup_me_item_config_stock"), slot * 31 + ItemStack.hashItemAndComponents(normalized), slot, normalized)
	}

	@RPCAction("pickup_me_item_config_stock")
	fun pickupStock(context: SyncActionContext, slot: Int, expectedItem: ItemStack): Boolean {
		val target = context.holder as? MEItemConfigActionTarget ?: return false
		if (!target.supportsMEItemConfigActions() || slot !in 0 until target.getMEItemConfigSlotCount()) return false
		require(!expectedItem.isEmpty && expectedItem.count == 1) { "ME item stock pickup requires one expected item." }
		if (target.isMEItemStocking() || target.isMEItemConfigAutoPull()) return false
		if (!target.canPickupMEItemConfigStock(context.player, slot, expectedItem)) return false
		target.pickupMEItemConfigStock(context.player, slot, expectedItem)
		return true
	}
}
