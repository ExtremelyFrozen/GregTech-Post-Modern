package com.gregtechceu.gtceu.common.item.terminal.network;

import com.gregtechceu.gtceu.GTCEu;
import com.gregtechceu.gtceu.common.item.terminal.menu.TerminalBoundMenu;
import com.gregtechceu.gtceu.common.item.terminal.menu.TerminalMenuState;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.UUID;

/** Authoritative profile, preview and acknowledgement state for one terminal menu session. */
public record SPacketTerminalState(int containerId, UUID sessionId,
                                   TerminalMenuState state)
        implements CustomPacketPayload {

    public static final ResourceLocation ID = GTCEu.id("terminal_state_to_client");
    public static final CustomPacketPayload.Type<SPacketTerminalState> TYPE = new CustomPacketPayload.Type<>(ID);
    public static final StreamCodec<RegistryFriendlyByteBuf, SPacketTerminalState> CODEC = StreamCodec
            .of(SPacketTerminalState::encode, SPacketTerminalState::decode);

    public SPacketTerminalState {
        if (containerId < 0) {
            throw new IllegalArgumentException("Terminal state identity must be present");
        }
    }

    public void execute(IPayloadContext context) {
        Player player = context.player();
        try {
            if (!(player.containerMenu instanceof TerminalBoundMenu menu) ||
                    !menu.applyClientState(player, containerId, sessionId, state)) {
                GTCEu.LOGGER.warn("Rejected terminal state without its matching installed client menu");
            }
        } catch (RuntimeException exception) {
            GTCEu.LOGGER.error("Closed terminal menu after an invalid server state failed", exception);
            player.closeContainer();
        }
    }

    private static SPacketTerminalState decode(RegistryFriendlyByteBuf buffer) {
        return new SPacketTerminalState(buffer.readVarInt(), buffer.readUUID(),
                TerminalMenuState.STREAM_CODEC.decode(buffer));
    }

    private static void encode(RegistryFriendlyByteBuf buffer, SPacketTerminalState value) {
        buffer.writeVarInt(value.containerId);
        buffer.writeUUID(value.sessionId);
        TerminalMenuState.STREAM_CODEC.encode(buffer, value.state);
    }

    @Override
    public CustomPacketPayload.Type<SPacketTerminalState> type() {
        return TYPE;
    }
}
