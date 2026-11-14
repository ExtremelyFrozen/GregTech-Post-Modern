package com.gregtechceu.gtceu.common.network.packets;

import com.gregtechceu.gtceu.GTCEu;
import com.gregtechceu.gtceu.api.sync_system.SyncFieldData;
import com.gregtechceu.gtceu.api.sync_system.managed.SyncMenu;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Synchronizes annotated fields through the matching menu opening in either direction. */
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
            if (managed == null) return;
            var storage = managed.getSyncDataHolder();
            if (player instanceof ServerPlayer) {
                if (menu.stillValid(player)) storage.applyServerNetworkUpdate(player.registryAccess(), fields);
            } else {
                storage.applyClientNetworkUpdate(player.registryAccess(), fields, fullSync);
            }
        }
    }

    @Override
    public Type<PacketMenuSync> type() {
        return TYPE;
    }
}
