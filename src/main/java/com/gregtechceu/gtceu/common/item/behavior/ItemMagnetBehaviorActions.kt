package com.gregtechceu.gtceu.common.item.behavior

import com.gregtechceu.gtceu.GTCEu
import com.gregtechceu.gtceu.api.item.IComponentItem
import com.gregtechceu.gtceu.api.sync_system.SyncActionContext
import com.gregtechceu.gtceu.api.sync_system.SyncActionData
import com.gregtechceu.gtceu.api.sync_system.rpc.RPCAction
import com.gregtechceu.gtceu.api.sync_system.rpc.RPCRegistry
import com.gregtechceu.gtceu.common.data.GTDataComponents
import com.gregtechceu.gtceu.common.item.behavior.ItemMagnetBehavior.Filter
import com.gregtechceu.gtceu.common.item.behavior.ItemMagnetBehavior.MagnetComponent

import net.minecraft.world.item.ItemStack

/** Owns the wire protocol and server handler for held-item magnet filter selection. */
object ItemMagnetBehaviorActions {

	private val ACTION = GTCEu.id("set_magnet_filter")

	@JvmStatic
	fun createSetMagnetFilterAction(stack: ItemStack, filter: Filter): SyncActionData {
		check(stack.has(GTDataComponents.MAGNET)) { "Cannot configure an item without a magnet component." }
		return RPCRegistry.create(ACTION, filter.ordinal, filter)
	}

	@RPCAction("set_magnet_filter")
	fun apply(context: SyncActionContext, filter: Filter): Boolean {
		val stack = context.holder as? ItemStack ?: return false
		val openedStack = context.openedStack ?: return false
		if (!isMagnet(stack) || !isMagnet(openedStack)) return false
		val current = checkNotNull(stack[GTDataComponents.MAGNET]) { "Magnet holder has no magnet component." }
		stack.set(GTDataComponents.MAGNET, MagnetComponent(current.active(), filter))
		return true
	}

	private fun isMagnet(stack: ItemStack): Boolean {
		val item = stack.item as? IComponentItem ?: return false
		return item.components.any { component -> component is ItemMagnetBehavior }
	}
}
