package com.gregtechceu.gtceu.common.capability;

import com.gregtechceu.gtceu.api.placeholder.Placeholder;
import com.gregtechceu.gtceu.api.placeholder.PlaceholderData;
import com.gregtechceu.gtceu.api.sync_system.SyncFieldData;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.UUIDUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import com.mojang.serialization.Codec;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import org.jspecify.annotations.NullMarked;

import java.util.UUID;

@NullMarked
public class PlaceholderSavedData extends SavedData {

    public static final Codec<Object2ObjectOpenHashMap<String, Object2ObjectOpenHashMap<UUID, SyncFieldData>>> CODEC = Codec
            .unboundedMap(Codec.STRING,
                    Codec.unboundedMap(UUIDUtil.STRING_CODEC, SyncFieldData.CODEC)
                            .xmap(Object2ObjectOpenHashMap::new, entries -> entries))
            .xmap(Object2ObjectOpenHashMap::new, entries -> entries);

    private final Object2ObjectOpenHashMap<String, Object2ObjectOpenHashMap<UUID, SyncFieldData>> data;

    public static PlaceholderSavedData getOrCreate(ServerLevel level) {
        return level.getDataStorage()
                .computeIfAbsent(
                        new SavedData.Factory<>(PlaceholderSavedData::new,
                                PlaceholderSavedData::new),
                        "gtceu_placeholder_data");
    }

    public PlaceholderSavedData() {
        this.data = new Object2ObjectOpenHashMap<>();
    }

    public PlaceholderSavedData(CompoundTag tag, HolderLookup.Provider provider) {
        this.data = CODEC.parse(provider.createSerializationContext(NbtOps.INSTANCE), tag.getCompound("data"))
                .getOrThrow();
    }

    public PlaceholderData getPlaceholderData(Placeholder placeholder, UUID uuid) {
        String placeholderName = placeholder.getName();
        Object2ObjectOpenHashMap<UUID, SyncFieldData> entries = data.get(placeholderName);
        SyncFieldData fields = entries == null ? SyncFieldData.EMPTY : entries.getOrDefault(uuid, SyncFieldData.EMPTY);
        return new PlaceholderData(fields, updated -> {
            if (updated.isEmpty()) {
                Object2ObjectOpenHashMap<UUID, SyncFieldData> currentEntries = data.get(placeholderName);
                if (currentEntries != null) {
                    currentEntries.remove(uuid);
                    if (currentEntries.isEmpty()) {
                        data.remove(placeholderName);
                    }
                }
            } else {
                data.computeIfAbsent(placeholderName, ignored -> new Object2ObjectOpenHashMap<>()).put(uuid, updated);
            }
            setDirty();
        });
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider provider) {
        tag.put("data", CODEC.encodeStart(provider.createSerializationContext(NbtOps.INSTANCE), data).getOrThrow());
        return tag;
    }
}
