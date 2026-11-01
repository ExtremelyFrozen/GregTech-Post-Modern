package com.gregtechceu.gtceu.common.item.terminal.network;

import com.gregtechceu.gtceu.common.item.terminal.menu.TerminalMenuState;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

import java.util.UUID;

/** Authoritative state synchronized by the generic held-item UI, with its opening-specific identity. */
public record TerminalStateUpdate(UUID sessionId, TerminalMenuState state) {

    public static final StreamCodec<RegistryFriendlyByteBuf, TerminalStateUpdate> STREAM_CODEC = StreamCodec.of(
            (buffer, update) -> {
                buffer.writeUUID(update.sessionId);
                TerminalMenuState.STREAM_CODEC.encode(buffer, update.state);
            }, buffer -> new TerminalStateUpdate(buffer.readUUID(), TerminalMenuState.STREAM_CODEC.decode(buffer)));
}
