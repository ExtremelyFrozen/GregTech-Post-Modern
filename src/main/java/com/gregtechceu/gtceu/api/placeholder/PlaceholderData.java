package com.gregtechceu.gtceu.api.placeholder;

import com.gregtechceu.gtceu.api.sync_system.SyncFieldData;

import com.mojang.serialization.Codec;
import lombok.Getter;

import java.util.function.Consumer;

/** Persistent script values for a placeholder. */
public final class PlaceholderData {

    @Getter
    private SyncFieldData fields;
    private final Consumer<SyncFieldData> changeConsumer;

    public PlaceholderData(SyncFieldData fields, Consumer<SyncFieldData> changeConsumer) {
        this.fields = fields;
        this.changeConsumer = changeConsumer;
    }

    public boolean contains(String key) {
        return fields.contains(SyncFieldData.key(key));
    }

    public boolean getBoolean(String key) {
        return Boolean.TRUE.equals(fields.read(SyncFieldData.key(key), Codec.BOOL));
    }

    public int getInt(String key) {
        Integer value = fields.read(SyncFieldData.key(key), Codec.INT);
        return value == null ? 0 : value;
    }

    public long getLong(String key) {
        Long value = fields.read(SyncFieldData.key(key), Codec.LONG);
        return value == null ? 0 : value;
    }

    public String getString(String key) {
        String value = fields.read(SyncFieldData.key(key), Codec.STRING);
        return value == null ? "" : value;
    }

    public void putBoolean(String key, boolean value) {
        put(key, Codec.BOOL, value);
    }

    public void putInt(String key, int value) {
        put(key, Codec.INT, value);
    }

    public void putLong(String key, long value) {
        put(key, Codec.LONG, value);
    }

    public void putString(String key, String value) {
        put(key, Codec.STRING, value);
    }

    public void remove(String key) {
        setFields(SyncFieldData.builder().putAll(fields).remove(SyncFieldData.key(key)).build());
    }

    private <T> void put(String key, Codec<T> codec, T value) {
        setFields(SyncFieldData.builder().putAll(fields).put(SyncFieldData.key(key), codec, value).build());
    }

    private void setFields(SyncFieldData fields) {
        this.fields = fields;
        changeConsumer.accept(fields);
    }
}
