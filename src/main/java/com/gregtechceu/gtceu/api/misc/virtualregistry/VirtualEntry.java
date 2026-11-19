package com.gregtechceu.gtceu.api.misc.virtualregistry;

import com.gregtechceu.gtceu.api.sync_system.ContextualFieldCodec;
import com.gregtechceu.gtceu.api.sync_system.ContextualFieldCodec.Context;
import com.gregtechceu.gtceu.api.sync_system.FieldCodecs;
import com.gregtechceu.gtceu.api.sync_system.SyncDataHolder;
import com.gregtechceu.gtceu.api.sync_system.SyncFieldData;
import com.gregtechceu.gtceu.api.sync_system.annotations.SaveField;
import com.gregtechceu.gtceu.api.sync_system.annotations.SyncToClient;
import com.gregtechceu.gtceu.api.sync_system.managed.ISyncManaged;

import net.minecraft.core.HolderLookup;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import com.mojang.serialization.Codec;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;

@Getter
@Accessors(chain = true)
public abstract class VirtualEntry implements ISyncManaged {

    public static final String DEFAULT_COLOR = "FFFFFFFF";
    public static final StreamCodec<RegistryFriendlyByteBuf, VirtualEntry> STREAM_CODEC = new StreamCodec<>() {

        @Override
        public VirtualEntry decode(RegistryFriendlyByteBuf buffer) {
            return ByteBufCodecs.fromCodecWithRegistries(codec(buffer.registryAccess(), null, false, true))
                    .decode(buffer);
        }

        @Override
        public void encode(RegistryFriendlyByteBuf buffer, VirtualEntry value) {
            ByteBufCodecs.fromCodecWithRegistries(codec(buffer.registryAccess(), null, false, true)).encode(buffer,
                    value);
        }
    };

    static {
        ContextualFieldCodec<VirtualEntry> codec = VirtualEntry::codec;
        FieldCodecs.registerContextual(VirtualEntry.class, codec);
        FieldCodecs.registerStream(VirtualEntry.class, STREAM_CODEC);
    }

    @Setter
    @NotNull
    @SaveField
    @SyncToClient
    private String description = "";
    private int color = 0xFFFFFFFF;
    @SaveField
    @SyncToClient
    private String colorStr = DEFAULT_COLOR;
    private SyncDataHolder syncDataHolder;

    @Override
    public SyncDataHolder getSyncDataHolder() {
        if (syncDataHolder == null) syncDataHolder = new SyncDataHolder(this);
        return syncDataHolder;
    }

    @Override
    public @Nullable ISyncManaged getParentSyncObject() {
        return null;
    }

    public abstract EntryTypes<? extends VirtualEntry> getType();

    /** Keeps a synchronized registry entry and its shared storage alive when the entry type is unchanged. */
    public static Codec<VirtualEntry> codec(Context<VirtualEntry> context) {
        return codec(context.lookup(), context.currentValue(), context.isClientSync(),
                context.isClientFullSyncUpdate());
    }

    private static Codec<VirtualEntry> codec(HolderLookup.Provider lookup, @Nullable VirtualEntry current,
                                             boolean clientSync, boolean fullSync) {
        return EntryTypes.CODEC.dispatch("type", VirtualEntry::getType, type -> SyncFieldData.CODEC.xmap(fields -> {
            VirtualEntry entry = current != null && current.getType() == type ? current : type.createInstance();
            entry.getSyncDataHolder().deserializeFieldData(lookup, fields, clientSync, true, fullSync, false);
            entry.onStateDecoded();
            return entry;
        }, entry -> entry.getSyncDataHolder().serializeToFieldData(lookup, clientSync, fullSync)).fieldOf("data"));
    }

    /** Restores runtime values derived from fields after one complete managed state has been applied. */
    protected void onStateDecoded() {
        setColor(colorStr);
    }

    public void setColor(String color) {
        this.color = parseColor(color);
        this.colorStr = color.toUpperCase(Locale.ROOT);
    }

    public void setColor(int color) {
        this.color = color;
        this.colorStr = Integer.toHexString(color).toUpperCase(Locale.ROOT);
    }

    public static int parseColor(String colorString) {
        if (colorString.length() < 8) {
            throw new IllegalArgumentException("Invalid color string: " + colorString);
        }

        int red = Integer.parseInt(colorString.substring(0, 2), 16);
        int green = Integer.parseInt(colorString.substring(2, 4), 16);
        int blue = Integer.parseInt(colorString.substring(4, 6), 16);
        int alpha = Integer.parseInt(colorString.substring(6, 8), 16);

        return (alpha << 24) | (red << 16) | (green << 8) | blue;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof VirtualEntry other)) return false;
        return this.getType() == other.getType() && this.color == other.color &&
                this.description.equals(other.description);
    }

    /** Applies decoded state without replacing a registry-owned entry or its shared storage objects. */
    public void copyFrom(VirtualEntry source) {
        if (source.getType() != getType()) {
            throw new IllegalArgumentException("Cannot copy virtual entries of different types");
        }
        this.color = source.color;
        this.colorStr = source.colorStr;
        this.description = source.description;
    }

    /** Captures an independent channel snapshot for a queued client publication. */
    public VirtualEntry copy() {
        VirtualEntry copy = getType().createInstance();
        copy.copyFrom(this);
        return copy;
    }

    public boolean canRemove() {
        return this.description.isEmpty();
    }
}
