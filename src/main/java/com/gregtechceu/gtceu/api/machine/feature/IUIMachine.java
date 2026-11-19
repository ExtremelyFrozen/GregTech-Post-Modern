package com.gregtechceu.gtceu.api.machine.feature;

import com.gregtechceu.gtceu.api.gui.factory.MachineBlockUIHolder;
import com.gregtechceu.gtceu.api.gui.factory.MachineUIHolder;
import com.gregtechceu.gtceu.api.gui.factory.UIRegistry;
import com.gregtechceu.gtceu.api.machine.MetaMachine;
import com.gregtechceu.gtceu.api.sync_system.SyncFieldData;

import com.lowdragmc.lowdraglib2.gui.holder.ModularUIContainerMenu;
import com.lowdragmc.lowdraglib2.gui.ui.UI;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Creates an LDLib2 machine UI through GTM's stable machine holder contract.
 *
 * <p>
 * This provider lets machine business code depend on {@link MachineUIHolder} instead of an LDLib2 menu holder.
 */
public interface IUIMachine extends IMachineFeature {

    /** Registry-aware block state codec shared by the common machine opening header and detached machine views. */
    StreamCodec<RegistryFriendlyByteBuf, BlockState> BLOCK_STATE_CODEC = ByteBufCodecs
            .fromCodecWithRegistries(BlockState.CODEC);

    /**
     * Opens this provider through its authoritative server-side menu path.
     *
     * <p>
     * All machines use the same registered factory. Providers that capture an opening session may override this
     * transaction while retaining the common position and block state header in their holder's opening data.
     */
    default boolean openUI(MetaMachine machine, ServerPlayer player) {
        MachineBlockUIHolder holder = new MachineBlockUIHolder(player, machine.getBlockPos(), machine.getBlockState(),
                machine.getDefinition().getId());
        return canCreateUI(player, holder) && UIRegistry.open(player, UIRegistry.MACHINE, holder);
    }

    /**
     * Applies the machine's initial synchronized fields before its UI reads them on the client thread.
     * Providers with additional opening data may override this method.
     *
     * @param windowId  server-assigned vanilla container identifier
     * @param inventory receiving player's inventory
     * @param holder    machine resolved from the common opening header
     * @param data      synchronized fields followed by any provider-owned opening data
     * @return menu using the common {@link UIRegistry#MENU} type
     */
    default ModularUIContainerMenu createClientMenu(int windowId, Inventory inventory, MachineBlockUIHolder holder,
                                                    RegistryFriendlyByteBuf data) {
        holder.getMachine().getSyncDataHolder().deserializeFieldData(inventory.player.registryAccess(),
                SyncFieldData.STREAM_CODEC.decode(data), true, true, true);
        return new ModularUIContainerMenu(UIRegistry.MENU.get(), windowId, inventory, holder);
    }

    /**
     * Returns whether this machine should use LDLib2 for the provided holder context.
     */
    default boolean canCreateUI(Player player, MachineUIHolder holder) {
        return true;
    }

    /**
     * Builds the LDLib2 machine UI tree for the provided player and opened machine identity.
     *
     * @param player player building the UI in the current runtime.
     * @param holder GTM holder that exposes the opened machine position, definition id, and current machine lookup.
     * @return non-null LDLib2 UI tree.
     */
    UI createUI(Player player, MachineUIHolder holder);
}
