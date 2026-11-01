package com.gregtechceu.gtceu.common.item.terminal.network;

import com.gregtechceu.gtceu.common.item.terminal.profile.TerminalChange;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

import org.jspecify.annotations.Nullable;

import java.util.UUID;

/** Action carried by the owning held-item ModularUI RPC; a null update requests execution. */
public record TerminalAction(UUID sessionId, long sequence, long expectedRevision,
                             @Nullable TerminalChange update) {

    public static final StreamCodec<RegistryFriendlyByteBuf, TerminalAction> STREAM_CODEC = StreamCodec.of(
            (buffer, action) -> {
                buffer.writeUUID(action.sessionId);
                buffer.writeVarLong(action.sequence);
                buffer.writeVarLong(action.expectedRevision);
                buffer.writeBoolean(action.update != null);
                if (action.update != null) TerminalChange.STREAM_CODEC.encode(buffer, action.update);
            }, buffer -> new TerminalAction(buffer.readUUID(), buffer.readVarLong(), buffer.readVarLong(),
                    buffer.readBoolean() ? TerminalChange.STREAM_CODEC.decode(buffer) : null));
}
