package com.gregtechceu.gtceu.common.network.packets;

import com.gregtechceu.gtceu.GTCEu;
import com.gregtechceu.gtceu.api.sync_system.SyncFieldData;
import com.gregtechceu.gtceu.api.sync_system.managed.SyncMenu;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Delivers changed annotated fields to the matching client menu. */
public record PacketMenuSync(int containerId, boolean fullSync, SyncFieldData fields) implements CustomPacketPayload {

    public static final Type<PacketMenuSync> TYPE = new Type<>(GTCEu.id("menu_sync"));
    public static final StreamCodec<RegistryFriendlyByteBuf, PacketMenuSync> CODEC = StreamCodec.ofMember(
            PacketMenuSync::encode,
            buffer -> new PacketMenuSync(buffer.readVarInt(), buffer.readBoolean(),
                    SyncFieldData.STREAM_CODEC.decode(buffer)));

    private void encode(RegistryFriendlyByteBuf buffer) {
        buffer.writeVarInt(containerId);
        buffer.writeBoolean(fullSync);
        SyncFieldData.STREAM_CODEC.encode(buffer, fields);
    }

    public void execute(IPayloadContext context) {
        var player = context.player();
        var menu = player.containerMenu;
        if (menu.containerId == containerId && menu instanceof SyncMenu synced) {
            var managed = synced.getSyncManaged();
            if (managed != null)
                managed.getSyncDataHolder().applyClientNetworkUpdate(player.registryAccess(), fields, fullSync);
        }
    }

    @Override
    public Type<PacketMenuSync> type() {
        return TYPE;
    }
}
