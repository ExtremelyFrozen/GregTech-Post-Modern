package com.gregtechceu.gtceu.api.blockentity;

import com.gregtechceu.gtceu.api.sync_system.SyncFieldData;

import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.ItemStack;

import com.mojang.serialization.Codec;

import java.util.function.Consumer;

public final class ConfigCopyHelper {

    private ConfigCopyHelper() {}

    public static SyncFieldData withFields(SyncFieldData config, Consumer<SyncFieldData.Builder> consumer) {
        SyncFieldData.Builder builder = SyncFieldData.builder().putAll(config);
        consumer.accept(builder);
        return builder.build();
    }

    public static SyncFieldData merge(SyncFieldData first, SyncFieldData second) {
        return SyncFieldData.builder().putAll(first).putAll(second).build();
    }

    public static boolean contains(SyncFieldData config, String key) {
        return config.contains(SyncFieldData.key(key));
    }

    public static int getInt(SyncFieldData config, String key) {
        Integer value = config.read(SyncFieldData.key(key), Codec.INT);
        return value == null ? 0 : value;
    }

    public static long getLong(SyncFieldData config, String key) {
        Long value = config.read(SyncFieldData.key(key), Codec.LONG);
        return value == null ? 0L : value;
    }

    public static boolean getBoolean(SyncFieldData config, String key) {
        return Boolean.TRUE.equals(config.read(SyncFieldData.key(key), Codec.BOOL));
    }

    public static String getString(SyncFieldData config, String key) {
        String value = config.read(SyncFieldData.key(key), Codec.STRING);
        return value == null ? "" : value;
    }

    public static ItemStack getItem(HolderLookup.Provider registries, SyncFieldData config, String key) {
        ItemStack stack = config.read(SyncFieldData.key(key), ItemStack.OPTIONAL_CODEC, registries);
        return stack == null ? ItemStack.EMPTY : stack;
    }
}
