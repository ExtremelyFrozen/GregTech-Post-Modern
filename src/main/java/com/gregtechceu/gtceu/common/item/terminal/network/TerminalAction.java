package com.gregtechceu.gtceu.common.item.terminal.network;

import com.gregtechceu.gtceu.common.item.terminal.profile.TerminalProfileUpdate;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

import org.jspecify.annotations.Nullable;

import java.util.UUID;

/** Bounded action carried by the owning held-item ModularUI RPC; a null update requests execution. */
public record TerminalAction(UUID sessionId, long sequence, long expectedRevision,
                             @Nullable TerminalProfileUpdate update) {

    public static final StreamCodec<RegistryFriendlyByteBuf, TerminalAction> STREAM_CODEC = StreamCodec.of(
            (buffer, action) -> {
                buffer.writeUUID(action.sessionId);
                buffer.writeVarLong(action.sequence);
                buffer.writeVarLong(action.expectedRevision);
                buffer.writeBoolean(action.update != null);
                if (action.update != null) TerminalProfileUpdate.STREAM_CODEC.encode(buffer, action.update);
            }, buffer -> new TerminalAction(buffer.readUUID(), buffer.readVarLong(), buffer.readVarLong(),
                    buffer.readBoolean() ? TerminalProfileUpdate.STREAM_CODEC.decode(buffer) : null));

    public TerminalAction {
        if (sequence <= 0 || expectedRevision < 0) {
            throw new IllegalArgumentException("Terminal action sequence and revision are invalid");
        }
    }
}
