package com.gregtechceu.gtceu.api.sync_system.managed

import com.gregtechceu.gtceu.api.sync_system.SyncDataHolder
import com.gregtechceu.gtceu.api.sync_system.SyncFieldData
import com.gregtechceu.gtceu.common.data.GTDataComponents
import com.gregtechceu.gtceu.common.network.packets.PacketRPCMenu

import net.minecraft.core.RegistryAccess
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import net.minecraft.world.item.ItemStack

/** Stack-owned annotated state. Loading happens after construction, and only changed saved data is written back. */
abstract class ItemSyncHolder(val stack: ItemStack, val registries: RegistryAccess) : ISyncManaged {
	private val storage: SyncDataHolder by lazy { SyncDataHolder(this) }
	private var savedData = SyncFieldData.EMPTY
	var menuId: Int = -1

	override fun getSyncDataHolder(): SyncDataHolder = storage

	fun saveToStack() {
		val data = storage.serializeToItemFieldData(registries)
		if (data == savedData) return
		if (data.isEmpty()) {
			stack.remove(GTDataComponents.ITEM_DATA)
		} else {
			stack.set(GTDataComponents.ITEM_DATA, data)
		}
		savedData = data
	}

	fun loadFromStack() {
		savedData = stack.get(GTDataComponents.ITEM_DATA) ?: SyncFieldData.EMPTY
		storage.deserializeItemFieldData(registries, savedData)
	}

	override fun getParentSyncObject(): ISyncManaged? = null

	override fun createRPCPacket(data: ByteArray): CustomPacketPayload = PacketRPCMenu(menuId, data)

	override fun markAsChanged() = saveToStack()

	companion object {
		/** Creates stack state through the item's reusable provider, then loads the annotated item fields. */
		@JvmStatic
		fun create(stack: ItemStack, registries: RegistryAccess): ItemSyncHolder? = (stack.item as? SyncItem)?.createSyncHolder(stack, registries)?.also { it.loadFromStack() }
	}
}
