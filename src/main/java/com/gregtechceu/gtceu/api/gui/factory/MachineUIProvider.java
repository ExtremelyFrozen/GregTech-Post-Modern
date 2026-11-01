package com.gregtechceu.gtceu.api.gui.factory;

import com.gregtechceu.gtceu.api.block.MetaMachineBlock;
import com.gregtechceu.gtceu.api.machine.MetaMachine;

import com.lowdragmc.lowdraglib2.gui.holder.ModularUIContainerMenu;
import com.lowdragmc.lowdraglib2.gui.ui.UI;

import net.minecraft.core.BlockPos;
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
public interface MachineUIProvider {

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
     * Reconstructs this machine's client menu after the common factory validates the announced position, machine
     * block and definition. Session-backed providers consume their remaining immutable opening data here; ordinary
     * machines have no additional payload. This runs once on the client thread and must release partially allocated
     * client views if construction fails.
     *
     * @param windowId   server-assigned vanilla container identifier
     * @param inventory  receiving player's inventory
     * @param pos        opened machine position from the server header
     * @param blockState validated machine block state from the server header
     * @param data       remaining provider-owned payload; it may be empty for ordinary machines
     * @return menu using the common {@link UIRegistry#MENU} type
     */
    default ModularUIContainerMenu createClientMenu(int windowId, Inventory inventory, BlockPos pos,
                                                    BlockState blockState, RegistryFriendlyByteBuf data) {
        MetaMachineBlock block = (MetaMachineBlock) blockState.getBlock();
        return new ModularUIContainerMenu(UIRegistry.MENU.get(), windowId, inventory,
                new MachineBlockUIHolder(inventory.player, pos, blockState, block.getDefinition().getId()));
    }

    /**
     * Returns whether this machine should use LDLib2 for the provided holder context.
     */
    boolean canCreateUI(Player player, MachineUIHolder holder);

    /**
     * Builds the LDLib2 machine UI tree for the provided player and opened machine identity.
     *
     * @param player player building the UI in the current runtime.
     * @param holder GTM holder that exposes the opened machine position, definition id, and current machine lookup.
     * @return non-null LDLib2 UI tree.
     */
    UI createUI(Player player, MachineUIHolder holder);
}
