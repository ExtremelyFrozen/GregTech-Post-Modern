package com.gregtechceu.gtceu.api.misc.virtualregistry;

import com.gregtechceu.gtceu.api.sync_system.SyncDataHolder;
import com.gregtechceu.gtceu.api.sync_system.annotations.SaveField;
import com.gregtechceu.gtceu.api.sync_system.managed.ISyncManaged;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

public class VirtualRegistryMap implements ISyncManaged {

    @SaveField
    private final Map<EntryTypes<?>, Map<String, VirtualEntry>> registryMap = new Object2ObjectOpenHashMap<>();
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

    public VirtualRegistryMap() {}

    public VirtualRegistryMap(Map<EntryTypes<?>, Map<String, VirtualEntry>> entries) {
        entries.forEach((type, values) -> values.forEach((name, value) -> {
            if (value.getType() != type) {
                throw new IllegalArgumentException("Virtual entry type does not match its registry: " + name);
            }
            addEntry(name, value);
        }));
    }

    @SuppressWarnings("unchecked")
    public @Nullable <T extends VirtualEntry> T getEntry(EntryTypes<T> type, String name) {
        return (T) registryMap.getOrDefault(type, Collections.emptyMap()).get(name);
    }

    public void addEntry(String name, VirtualEntry entry) {
        registryMap.computeIfAbsent(entry.getType(), k -> new Object2ObjectOpenHashMap<>()).put(name, entry);
    }

    public boolean contains(EntryTypes<?> type, String name) {
        return registryMap.containsKey(type) && registryMap.get(type).containsKey(name);
    }

    public void deleteEntry(EntryTypes<?> type, String name) {
        Map<String, VirtualEntry> entries = registryMap.get(type);
        if (entries != null) {
            entries.remove(name);
            if (entries.isEmpty()) {
                registryMap.remove(type);
            }
        }
    }

    public void clear() {
        registryMap.clear();
    }

    public Set<String> getEntryNames(EntryTypes<?> type) {
        return new ObjectOpenHashSet<>(registryMap.getOrDefault(type, Collections.emptyMap()).keySet());
    }
}
