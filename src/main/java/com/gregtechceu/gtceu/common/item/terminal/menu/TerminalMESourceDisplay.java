package com.gregtechceu.gtceu.common.item.terminal.menu;

import com.gregtechceu.gtceu.api.multiblock.autobuild.AutoBuildMaterialSource;

import net.minecraft.core.GlobalPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

/** Client-safe display identity for the exact AE wireless terminal selected by the server. */
public record TerminalMESourceDisplay(String terminalName, String selectionIdentity, GlobalPos linkTarget) {

    private static final int MAX_TEXT = 256;

    public static final StreamCodec<RegistryFriendlyByteBuf, TerminalMESourceDisplay> STREAM_CODEC = StreamCodec
            .of(TerminalMESourceDisplay::encode, TerminalMESourceDisplay::decode);

    public TerminalMESourceDisplay {
        if (terminalName.isBlank() || terminalName.length() > MAX_TEXT ||
                selectionIdentity.isBlank() || selectionIdentity.length() > MAX_TEXT) {
            throw new IllegalArgumentException("AE terminal display identity must be present and bounded");
        }
    }

    public static TerminalMESourceDisplay from(AutoBuildMaterialSource.SourceDescriptor descriptor) {
        if (descriptor.kind() != AutoBuildMaterialSource.SourceKind.ME || descriptor.linkTarget() == null) {
            throw new IllegalArgumentException("Only a linked ME source can be displayed as a wireless terminal");
        }
        return new TerminalMESourceDisplay(descriptor.displayName(), descriptor.selectionIdentity(),
                descriptor.linkTarget());
    }

    private static void encode(RegistryFriendlyByteBuf buffer, TerminalMESourceDisplay value) {
        buffer.writeUtf(value.terminalName, MAX_TEXT);
        buffer.writeUtf(value.selectionIdentity, MAX_TEXT);
        buffer.writeGlobalPos(value.linkTarget);
    }

    private static TerminalMESourceDisplay decode(RegistryFriendlyByteBuf buffer) {
        return new TerminalMESourceDisplay(buffer.readUtf(MAX_TEXT), buffer.readUtf(MAX_TEXT),
                buffer.readGlobalPos());
    }
}
