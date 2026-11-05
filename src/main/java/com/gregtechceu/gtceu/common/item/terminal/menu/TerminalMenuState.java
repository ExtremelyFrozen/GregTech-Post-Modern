package com.gregtechceu.gtceu.common.item.terminal.menu;

import com.gregtechceu.gtceu.api.multiblock.preview.StructurePreview;
import com.gregtechceu.gtceu.api.sync_system.ContextualFieldCodec;
import com.gregtechceu.gtceu.api.sync_system.FieldCodecs;
import com.gregtechceu.gtceu.api.sync_system.managed.ItemSyncHolder;
import com.gregtechceu.gtceu.common.item.datacomponents.TerminalMachine;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;

import com.mojang.serialization.Codec;
import org.jspecify.annotations.Nullable;

/** Server-authoritative machine and preview state for one terminal menu revision. */
public record TerminalMenuState(ResourceLocation definitionId, long acknowledgedSequence, long revision,
                                TerminalMachine machine,
                                StructurePreview preview, boolean executing,
                                @Nullable TerminalExecutionSummary lastExecution) {

    public static final StreamCodec<RegistryFriendlyByteBuf, TerminalMenuState> STREAM_CODEC = StreamCodec
            .of(TerminalMenuState::encode, TerminalMenuState::decode);

    static {
        FieldCodecs.registerStream(TerminalMenuState.class, STREAM_CODEC);
    }

    public static final ContextualFieldCodec<TerminalMenuState> CODEC = new ContextualFieldCodec<>() {

        @Override
        public Codec<TerminalMenuState> codec(Context<TerminalMenuState> context) {
            return FieldCodecs.fromStreamCodec(STREAM_CODEC, ((ItemSyncHolder) context.holder()).getRegistries());
        }

        @Override
        public boolean isImmutable() {
            return true;
        }
    };

    static {
        FieldCodecs.registerContextual(TerminalMenuState.class, CODEC);
    }

    private static void encode(RegistryFriendlyByteBuf buffer, TerminalMenuState value) {
        buffer.writeResourceLocation(value.definitionId);
        buffer.writeVarLong(value.acknowledgedSequence);
        buffer.writeVarLong(value.revision);
        TerminalMachine.STREAM_CODEC.encode(buffer, value.machine);
        StructurePreview.STREAM_CODEC.encode(buffer, value.preview);
        buffer.writeBoolean(value.executing);
        buffer.writeBoolean(value.lastExecution != null);
        if (value.lastExecution != null) {
            TerminalExecutionSummary.STREAM_CODEC.encode(buffer, value.lastExecution);
        }
    }

    private static TerminalMenuState decode(RegistryFriendlyByteBuf buffer) {
        ResourceLocation definitionId = buffer.readResourceLocation();
        long acknowledgedSequence = buffer.readVarLong();
        long revision = buffer.readVarLong();
        TerminalMachine machine = TerminalMachine.STREAM_CODEC.decode(buffer);
        StructurePreview preview = StructurePreview.STREAM_CODEC.decode(buffer);
        boolean executing = buffer.readBoolean();
        TerminalExecutionSummary lastExecution = buffer.readBoolean() ?
                TerminalExecutionSummary.STREAM_CODEC.decode(buffer) : null;
        return new TerminalMenuState(definitionId, acknowledgedSequence, revision, machine, preview, executing,
                lastExecution);
    }
}
