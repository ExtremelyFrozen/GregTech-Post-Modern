package com.gregtechceu.gtceu.api.gui.factory;

import com.gregtechceu.gtceu.api.block.MetaMachineBlock;
import com.gregtechceu.gtceu.api.machine.MetaMachine;
import com.gregtechceu.gtceu.api.sync_system.SyncFieldData;

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
    private final MetaMachine machine;

    public MachineBlockUIHolder(Player player, BlockPos pos, BlockState blockState,
                                ResourceLocation machineDefinitionId) {
        this.player = player;
        this.pos = pos;
        this.blockState = blockState;
        this.machineDefinitionId = machineDefinitionId;
        var current = MetaMachine.getMachine(player.level(), pos);
        if (current == null && player.level().isClientSide()) {
            current = (MetaMachine) ((MetaMachineBlock) blockState.getBlock()).newBlockEntity(pos, blockState);
            current.setLevel(player.level());
        }
        this.machine = current;
    }

    static ModularUIContainerMenu read(int windowId, Inventory inventory, RegistryFriendlyByteBuf data) {
        BlockPos pos = data.readBlockPos();
        BlockState blockState = MachineUIProvider.BLOCK_STATE_CODEC.decode(data);
        if (!(blockState.getBlock() instanceof MetaMachineBlock block)) {
            throw new IllegalArgumentException("No machine UI found for block " + blockState);
        }
        var holder = new MachineBlockUIHolder(inventory.player, pos, blockState, block.getDefinition().getId());
        var provider = (MachineUIProvider) holder.getMachine();
        return provider.createClientMenu(windowId, inventory, holder, data);
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
        return machine;
    }

    @Override
    public boolean isStillValid(Player player) {
        MetaMachine machine = getMachine();
        return !machine.isRemoved() && machine instanceof MachineUIProvider provider &&
                provider.canCreateUI(player, this);
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
        SyncFieldData.STREAM_CODEC.encode(buffer, machine.getSyncDataHolder().getClientData(player.registryAccess()));
    }

    @Override
    public ModularUI createUI(Player player) {
        return UIRegistry.createUI(((MachineUIProvider) machine).createUI(player, this), player);
    }
}
