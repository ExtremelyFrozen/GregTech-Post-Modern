package com.gregtechceu.gtceu.common.network.packets;

import com.gregtechceu.gtceu.GTCEu;
import com.gregtechceu.gtceu.api.gui.factory.MachineUIHolder;
import com.gregtechceu.gtceu.api.machine.MetaMachine;
import com.gregtechceu.gtceu.api.sync_system.SyncFieldData;
import com.gregtechceu.gtceu.api.sync_system.managed.ManagedSyncBlockEntity;

import com.lowdragmc.lowdraglib2.gui.holder.ModularUIContainerMenu;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import lombok.Getter;

public class SPacketMachineSyncToClient implements CustomPacketPayload {

    public static final ResourceLocation ID = GTCEu.id("machine_sync_to_client");
    public static final Type<SPacketMachineSyncToClient> TYPE = new Type<>(ID);
    public static final StreamCodec<RegistryFriendlyByteBuf, SPacketMachineSyncToClient> CODEC = StreamCodec
            .ofMember(SPacketMachineSyncToClient::encode, SPacketMachineSyncToClient::new);

    private final BlockPos pos;
    private final SyncFieldData data;
    @Getter
    private final boolean fullSync;

    public SPacketMachineSyncToClient(BlockPos pos, SyncFieldData data, boolean fullSync) {
        this.pos = pos;
        this.data = data;
        this.fullSync = fullSync;
    }

    public SPacketMachineSyncToClient(BlockPos pos, SyncFieldData data) {
        this(pos, data, false);
    }

    public SPacketMachineSyncToClient(RegistryFriendlyByteBuf buffer) {
        this(buffer.readBlockPos(), SyncFieldData.STREAM_CODEC.decode(buffer), buffer.readBoolean());
    }

    public void encode(RegistryFriendlyByteBuf buffer) {
        buffer.writeBlockPos(pos);
        SyncFieldData.STREAM_CODEC.encode(buffer, data);
        buffer.writeBoolean(fullSync);
    }

    public void execute(IPayloadContext context) {
        var player = context.player();
        var level = player.level();
        var world = level.getBlockEntity(pos);
        if (world instanceof ManagedSyncBlockEntity managed) {
            managed.getSyncDataHolder().applyClientNetworkUpdate(level.registryAccess(), data, fullSync);
        }
        if (player.containerMenu instanceof ModularUIContainerMenu menu &&
                menu.uiHolder instanceof MachineUIHolder holder && holder.getPos().equals(pos)) {
            MetaMachine machine = holder.getMachine();
            if (machine != world) {
                machine.getSyncDataHolder().applyClientNetworkUpdate(level.registryAccess(), data, fullSync);
            }
        }
    }

    @Override
    public Type<SPacketMachineSyncToClient> type() {
        return TYPE;
    }
}
