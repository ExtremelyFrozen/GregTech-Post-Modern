package com.gregtechceu.gtceu.api.gui.factory;

import com.gregtechceu.gtceu.api.block.MetaMachineBlock;
import com.gregtechceu.gtceu.api.machine.MetaMachine;

import com.lowdragmc.lowdraglib2.gui.factory.IContainerUIHolder;
import com.lowdragmc.lowdraglib2.gui.holder.ModularUIContainerMenu;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UI;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;

/**
 * LDLib2 block menu holder for a GTM machine block UI.
 */
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class MachineBlockUIHolder implements MachineUIHolder, MenuProvider, IContainerUIHolder {

    public final Player player;
    public final BlockPos pos;
    public final BlockState blockState;

    private final ResourceLocation machineDefinitionId;

    public MachineBlockUIHolder(Player player, BlockPos pos, BlockState blockState,
                                ResourceLocation machineDefinitionId) {
        this.player = player;
        this.pos = pos;
        this.blockState = blockState;
        this.machineDefinitionId = machineDefinitionId;
    }

    static ModularUIContainerMenu read(int windowId, Inventory inventory, RegistryFriendlyByteBuf data) {
        BlockPos pos = data.readBlockPos();
        BlockState blockState = MachineUIProvider.BLOCK_STATE_CODEC.decode(data);
        if (!(blockState.getBlock() instanceof MetaMachineBlock block)) {
            throw new IllegalArgumentException("No machine UI found for block " + blockState);
        }
        Level level = inventory.player.level();
        if (!level.isLoaded(pos)) {
            throw new IllegalStateException("Machine UI position is not loaded on the client.");
        }
        MetaMachine machine = MetaMachine.getMachine(level, pos);
        if (!(machine instanceof MachineUIProvider provider) ||
                !machine.getDefinition().getId().equals(block.getDefinition().getId())) {
            throw new IllegalStateException("Machine opening no longer resolves its announced definition.");
        }
        return provider.createClientMenu(windowId, inventory, pos, blockState, data);
    }

    @Override
    public BlockPos getPos() {
        return pos;
    }

    @Override
    public ResourceLocation getMachineDefinitionId() {
        return machineDefinitionId;
    }

    @Nullable
    @Override
    public MetaMachine getMachine() {
        Level level = player.level();
        if (!level.isLoaded(pos)) {
            return null;
        }
        MetaMachine machine = MetaMachine.getMachine(level, pos);
        if (machine == null || !machine.getDefinition().getId().equals(machineDefinitionId)) {
            return null;
        }
        return machine;
    }

    @Override
    public boolean isStillValid(Player player) {
        MetaMachine machine = getMachine();
        return machine instanceof MachineUIProvider provider && provider.canCreateUI(player, this);
    }

    @Override
    public Component getDisplayName() {
        return blockState.getBlock().getName();
    }

    @Override
    public ModularUIContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new ModularUIContainerMenu(UIRegistry.MENU.get(), containerId, inventory, this);
    }

    @Override
    public void writeClientSideData(AbstractContainerMenu menu, RegistryFriendlyByteBuf buffer) {
        buffer.writeBlockPos(pos);
        MachineUIProvider.BLOCK_STATE_CODEC.encode(buffer, blockState);
    }

    @Override
    public ModularUI createUI(Player player) {
        MetaMachine machine = getMachine();
        if (!(machine instanceof MachineUIProvider provider)) {
            throw new IllegalStateException("Machine does not expose a UI for the opened holder.");
        }
        if (!provider.canCreateUI(player, this)) {
            throw new IllegalStateException("Machine rejected the opened UI holder.");
        }
        UI ui = provider.createUI(player, this);
        if (ui == null) {
            throw new IllegalStateException("Machine UI provider returned null.");
        }
        return UIRegistry.createUI(ui, player);
    }
}
