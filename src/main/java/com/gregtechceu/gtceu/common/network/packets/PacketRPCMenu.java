package com.gregtechceu.gtceu.common.network.packets;

import com.gregtechceu.gtceu.GTCEu;
import com.gregtechceu.gtceu.api.sync_system.managed.SyncMenu;
import com.gregtechceu.gtceu.api.sync_system.rpc.RPCSender;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Routes annotated RPC methods through the managed root of one menu opening. */
public record PacketRPCMenu(int containerId, byte[] data) implements CustomPacketPayload {

    public static final Type<PacketRPCMenu> TYPE = new Type<>(GTCEu.id("rpc_menu"));
    public static final StreamCodec<RegistryFriendlyByteBuf, PacketRPCMenu> CODEC = StreamCodec.ofMember(
            PacketRPCMenu::encode, buffer -> new PacketRPCMenu(buffer.readVarInt(), buffer.readByteArray()));

    private void encode(RegistryFriendlyByteBuf buffer) {
        buffer.writeVarInt(containerId);
        buffer.writeByteArray(data);
    }

    public void execute(IPayloadContext context) {
        var player = context.player();
        var menu = player.containerMenu;
        if (menu.containerId != containerId || !(menu instanceof SyncMenu synced) || !menu.stillValid(player)) return;
        var managed = synced.getSyncManaged();
        if (managed != null) {
            managed.handleRPCPacket(player instanceof ServerPlayer serverPlayer ?
                    RPCSender.ofRemote(serverPlayer) : RPCSender.ofServer(), data);
        }
    }

    @Override
    public Type<PacketRPCMenu> type() {
        return TYPE;
    }
}
