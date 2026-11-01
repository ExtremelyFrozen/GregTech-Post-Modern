package com.gregtechceu.gtceu.common.item.terminal.menu;

import com.gregtechceu.gtceu.api.multiblock.autobuild.MaterialSource;

import net.minecraft.core.GlobalPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

/** Client-safe display identity for the exact AE wireless terminal selected by the server. */
public record TerminalMESourceDisplay(String terminalName, String selectionIdentity, GlobalPos linkTarget) {

    public static final StreamCodec<RegistryFriendlyByteBuf, TerminalMESourceDisplay> STREAM_CODEC = StreamCodec
            .of(TerminalMESourceDisplay::encode, TerminalMESourceDisplay::decode);

    public static TerminalMESourceDisplay from(MaterialSource.SourceDescriptor descriptor) {
        if (descriptor.kind() != MaterialSource.SourceKind.ME || descriptor.linkTarget() == null) {
            throw new IllegalArgumentException("Only a linked ME source can be displayed as a wireless terminal");
        }
        return new TerminalMESourceDisplay(descriptor.displayName(), descriptor.selectionIdentity(),
                descriptor.linkTarget());
    }

    private static void encode(RegistryFriendlyByteBuf buffer, TerminalMESourceDisplay value) {
        buffer.writeUtf(value.terminalName);
        buffer.writeUtf(value.selectionIdentity);
        buffer.writeGlobalPos(value.linkTarget);
    }

    private static TerminalMESourceDisplay decode(RegistryFriendlyByteBuf buffer) {
        return new TerminalMESourceDisplay(buffer.readUtf(), buffer.readUtf(),
                buffer.readGlobalPos());
    }
}
