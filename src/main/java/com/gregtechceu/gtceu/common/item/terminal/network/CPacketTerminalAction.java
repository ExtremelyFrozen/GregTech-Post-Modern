package com.gregtechceu.gtceu.common.item.terminal.network;

import com.gregtechceu.gtceu.GTCEu;
import com.gregtechceu.gtceu.common.item.terminal.menu.TerminalBoundMenu;
import com.gregtechceu.gtceu.common.item.terminal.profile.TerminalProfileUpdate;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import org.jspecify.annotations.Nullable;

import java.util.UUID;

/** One strictly ordered profile-update or execute action for an installed terminal menu. */
public record CPacketTerminalAction(int containerId, UUID sessionId, long sequence, long expectedRevision,
                                    Type actionType,
                                    @Nullable TerminalProfileUpdate update)
        implements CustomPacketPayload {

    public static final ResourceLocation ID = GTCEu.id("terminal_action_to_server");
    public static final CustomPacketPayload.Type<CPacketTerminalAction> TYPE = new CustomPacketPayload.Type<>(ID);
    public static final StreamCodec<RegistryFriendlyByteBuf, CPacketTerminalAction> CODEC = StreamCodec
            .of(CPacketTerminalAction::encode, CPacketTerminalAction::decode);

    public CPacketTerminalAction {
        if (containerId < 0 || sequence <= 0 || expectedRevision < 0) {
            throw new IllegalArgumentException("Terminal action identity is invalid or unbounded");
        }
        if (actionType == Type.UPDATE && update == null || actionType == Type.EXECUTE && update != null) {
            throw new IllegalArgumentException("Terminal action payload does not match its type");
        }
    }

    public static CPacketTerminalAction update(int containerId, UUID sessionId, long sequence,
                                               long expectedRevision, TerminalProfileUpdate update) {
        return new CPacketTerminalAction(containerId, sessionId, sequence, expectedRevision, Type.UPDATE,
                update);
    }

    public static CPacketTerminalAction execute(int containerId, UUID sessionId, long sequence,
                                                long expectedRevision) {
        return new CPacketTerminalAction(containerId, sessionId, sequence, expectedRevision, Type.EXECUTE, null);
    }

    public void execute(IPayloadContext context) {
        if (context.player() instanceof ServerPlayer player && player.containerMenu instanceof TerminalBoundMenu menu &&
                menu.containerId == containerId && menu.sessionId().equals(sessionId)) {
            try {
                menu.receiveAction(player, this);
            } catch (RuntimeException exception) {
                GTCEu.LOGGER.error("Closed terminal menu after an invalid action failed", exception);
                player.closeContainer();
            }
        } else {
            GTCEu.LOGGER.warn("Rejected terminal action without its matching installed menu");
        }
    }

    private static CPacketTerminalAction decode(RegistryFriendlyByteBuf buffer) {
        int containerId = buffer.readVarInt();
        UUID sessionId = buffer.readUUID();
        long sequence = buffer.readVarLong();
        long revision = buffer.readVarLong();
        Type actionType = buffer.readEnum(Type.class);
        TerminalProfileUpdate update = actionType == Type.UPDATE ? TerminalProfileUpdate.STREAM_CODEC.decode(buffer) :
                null;
        return new CPacketTerminalAction(containerId, sessionId, sequence, revision, actionType, update);
    }

    private static void encode(RegistryFriendlyByteBuf buffer, CPacketTerminalAction value) {
        buffer.writeVarInt(value.containerId);
        buffer.writeUUID(value.sessionId);
        buffer.writeVarLong(value.sequence);
        buffer.writeVarLong(value.expectedRevision);
        buffer.writeEnum(value.actionType);
        if (value.actionType == Type.UPDATE) {
            TerminalProfileUpdate.STREAM_CODEC.encode(buffer, value.update);
        }
    }

    @Override
    public CustomPacketPayload.Type<CPacketTerminalAction> type() {
        return TYPE;
    }

    public enum Type {
        UPDATE,
        EXECUTE
    }
}
