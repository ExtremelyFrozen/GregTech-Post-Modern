package com.gregtechceu.gtceu.api.blockentity;

import com.gregtechceu.gtceu.api.sync_system.SyncFieldData;
import com.gregtechceu.gtceu.api.sync_system.managed.ISyncManaged;

import net.minecraft.core.HolderLookup;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/// An interface for machines and machine traits which have settings that can be copied using the machine memory card.
public interface ICopyable {

    /**
     * Copies runtime configuration into codec data for the machine memory card.
     *
     * <p>
     * The default path reuses the sync annotation model, so {@code @SaveField} data is serialized without exposing NBT
     * outside of the final block entity storage boundary.
     * </p>
     */
    default SyncFieldData copyConfig(HolderLookup.Provider registries) {
        if (this instanceof ISyncManaged syncManaged) {
            return syncManaged.getSyncDataHolder().serializeToSaveFieldData(registries);
        }
        return SyncFieldData.EMPTY;
    }

    /**
     * Applies a memory card configuration previously produced by {@link #copyConfig(HolderLookup.Provider)}.
     *
     * @param player player applying the configuration, used by implementations that consume items or rebuild covers
     * @param config typed configuration payload copied from the memory card
     */
    default void pasteConfig(ServerPlayer player, HolderLookup.Provider registries, SyncFieldData config) {
        if (this instanceof ISyncManaged syncManaged) {
            syncManaged.getSyncDataHolder().deserializeFieldData(registries, config, false);
        }
    }

    /// Returns a `List<ItemStack>` of items required to paste the saved config.
    default List<ItemStack> getItemsRequiredToPaste() {
        return List.of();
    }
}
