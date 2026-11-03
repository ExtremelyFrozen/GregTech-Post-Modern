package com.gregtechceu.gtceu.common.machine.storage

import com.gregtechceu.gtceu.GTCEu
import com.gregtechceu.gtceu.api.sync_system.SyncActionContext
import com.gregtechceu.gtceu.api.sync_system.SyncActionData
import com.gregtechceu.gtceu.api.sync_system.rpc.RpcAction
import com.gregtechceu.gtceu.api.sync_system.rpc.RpcRegistry

import net.minecraft.world.item.ItemStack

import org.jetbrains.annotations.ApiStatus

/** Exposes only the creative chest mutation that its Kotlin server action handler may invoke. */
@ApiStatus.Internal
interface CreativeChestItemActionTarget {

	/** Applies one validated phantom item selection, normalizing its stored count. */
	fun setCreativeChestItem(item: ItemStack)
}

/** Owns the wire protocol and server handler for the creative chest phantom item slot. */
object CreativeChestMachineActions {

	private val ACTION = GTCEu.id("set_creative_chest_item")

	@JvmStatic
	fun createSetItemAction(item: ItemStack): SyncActionData {
		val storedItem = if (item.isEmpty) ItemStack.EMPTY else item.copyWithCount(1)
		return RpcRegistry.create(ACTION, ItemStack.hashItemAndComponents(storedItem), storedItem)
	}

	@RpcAction("set_creative_chest_item")
	fun apply(context: SyncActionContext, item: ItemStack): Boolean {
		val target = context.holder as? CreativeChestItemActionTarget ?: return false
		target.setCreativeChestItem(item)
		return true
	}
}
