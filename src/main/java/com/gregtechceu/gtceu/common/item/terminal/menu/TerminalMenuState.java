package com.gregtechceu.gtceu.common.item.terminal.menu;

import com.gregtechceu.gtceu.api.multiblock.preview.MultiblockPreviewSnapshot;
import com.gregtechceu.gtceu.common.item.datacomponents.TerminalAutoBuildProfiles;
import com.gregtechceu.gtceu.common.item.datacomponents.TerminalMachineProfile;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;

import org.jspecify.annotations.Nullable;

import java.util.Map;

/** Server-authoritative profile and preview state for one terminal menu revision. */
public record TerminalMenuState(ResourceLocation definitionId, long acknowledgedSequence, long revision,
                                TerminalMachineProfile profile,
                                MultiblockPreviewSnapshot preview, boolean executing,
                                @Nullable TerminalMESourceDisplay meSource,
                                @Nullable TerminalExecutionSummary lastExecution) {

    public static final StreamCodec<RegistryFriendlyByteBuf, TerminalMenuState> STREAM_CODEC = StreamCodec
            .of(TerminalMenuState::encode, TerminalMenuState::decode);

    public TerminalMenuState {
        if (acknowledgedSequence < 0 || revision < 0) {
            throw new IllegalArgumentException("Terminal menu sequence and revision cannot be negative");
        }
        if (!definitionId.equals(preview.definitionId())) {
            throw new IllegalArgumentException("Terminal profile and preview definitions do not match");
        }
    }

    public String fingerprint() {
        return preview.fingerprint();
    }

    private static void encode(RegistryFriendlyByteBuf buffer, TerminalMenuState value) {
        buffer.writeResourceLocation(value.definitionId);
        buffer.writeVarLong(value.acknowledgedSequence);
        buffer.writeVarLong(value.revision);
        TerminalAutoBuildProfiles.STREAM_CODEC.encode(buffer,
                new TerminalAutoBuildProfiles(Map.of(value.definitionId, value.profile)));
        MultiblockPreviewSnapshot.STREAM_CODEC.encode(buffer, value.preview);
        buffer.writeBoolean(value.executing);
        buffer.writeBoolean(value.meSource != null);
        if (value.meSource != null) {
            TerminalMESourceDisplay.STREAM_CODEC.encode(buffer, value.meSource);
        }
        buffer.writeBoolean(value.lastExecution != null);
        if (value.lastExecution != null) {
            TerminalExecutionSummary.STREAM_CODEC.encode(buffer, value.lastExecution);
        }
    }

    private static TerminalMenuState decode(RegistryFriendlyByteBuf buffer) {
        ResourceLocation definitionId = buffer.readResourceLocation();
        long acknowledgedSequence = buffer.readVarLong();
        long revision = buffer.readVarLong();
        TerminalAutoBuildProfiles profiles = TerminalAutoBuildProfiles.STREAM_CODEC.decode(buffer);
        TerminalMachineProfile profile = profiles.profiles().get(definitionId);
        if (profile == null || profiles.profiles().size() != 1) {
            throw new IllegalArgumentException("Terminal menu state must contain exactly its opened machine profile");
        }
        MultiblockPreviewSnapshot preview = MultiblockPreviewSnapshot.STREAM_CODEC.decode(buffer);
        boolean executing = buffer.readBoolean();
        TerminalMESourceDisplay meSource = buffer.readBoolean() ? TerminalMESourceDisplay.STREAM_CODEC.decode(buffer) :
                null;
        TerminalExecutionSummary lastExecution = buffer.readBoolean() ?
                TerminalExecutionSummary.STREAM_CODEC.decode(buffer) : null;
        return new TerminalMenuState(definitionId, acknowledgedSequence, revision, profile, preview, executing,
                meSource, lastExecution);
    }
}
