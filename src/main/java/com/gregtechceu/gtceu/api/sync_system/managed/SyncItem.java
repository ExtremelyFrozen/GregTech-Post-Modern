package com.gregtechceu.gtceu.api.sync_system.managed;

import net.minecraft.core.RegistryAccess;
import net.minecraft.world.item.ItemStack;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Creates annotated state for an individual item stack. Items and their behavior components can implement this
 * contract without sharing mutable state on an Item singleton. Create, mutate and save holders on the owning game
 * thread; a menu retains its holder until it closes.
 */
@NullMarked
public interface SyncItem {

    /**
     * Creates fresh state before its saved fields are loaded. Returns null when this stack has no managed state.
     * The supplied stack remains the persistence target even if the player later changes their held item.
     */
    @Nullable
    ItemSyncHolder createSyncHolder(ItemStack stack, RegistryAccess registries);
}
