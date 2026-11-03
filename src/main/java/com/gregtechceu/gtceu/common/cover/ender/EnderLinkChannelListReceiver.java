package com.gregtechceu.gtceu.common.cover.ender;

import com.gregtechceu.gtceu.api.misc.virtualregistry.VirtualEntry;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

/**
 * Opening-scoped LDLib2 Ender link UI element whose channel state is synchronized only to its player.
 */
public interface EnderLinkChannelListReceiver {

    /**
     * Returns whether the response targets this opened cover UI.
     */
    boolean acceptsEnderLinkChannelList(BlockPos pos, Direction side, ResourceLocation coverDefinitionId);

    /**
     * Publishes a channel snapshot through this opening's native UI sync value after holder authorization.
     */
    void receiveEnderLinkChannelList(List<VirtualEntry> entries, HolderLookup.Provider registries);
}
