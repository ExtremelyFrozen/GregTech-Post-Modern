package com.gregtechceu.gtceu.integration.ae2.utils;

import com.gregtechceu.gtceu.api.sync_system.SyncDataHolder;
import com.gregtechceu.gtceu.api.sync_system.annotations.ClientFieldChangeListener;
import com.gregtechceu.gtceu.api.sync_system.annotations.SaveField;
import com.gregtechceu.gtceu.api.sync_system.annotations.SyncToClient;
import com.gregtechceu.gtceu.api.sync_system.managed.ISyncManaged;

import appeng.api.config.Actionable;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.AEKey;
import appeng.api.storage.MEStorage;
import it.unimi.dsi.fastutil.objects.Object2LongLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2LongMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectList;
import lombok.Getter;
import lombok.Setter;
import org.jspecify.annotations.Nullable;

import java.util.Iterator;

/** Ordered ME contents shared directly by the machine and its UI. */
public class KeyStorage implements Iterable<Object2LongMap.Entry<AEKey>>, ISyncManaged {

    @SaveField
    @SyncToClient
    public final Object2LongLinkedOpenHashMap<AEKey> storage = new Object2LongLinkedOpenHashMap<>();

    @Getter
    private final SyncDataHolder syncDataHolder = new SyncDataHolder(this);
    @Getter
    private ObjectList<Entry> entries = ObjectList.of();
    @Setter
    private Runnable onContentsChanged = () -> {};

    @Override
    public @Nullable ISyncManaged getParentSyncObject() {
        return null;
    }

    public void insertInventory(MEStorage inventory, IActionSource source) {
        boolean changed = false;
        var iterator = iterator();
        while (iterator.hasNext()) {
            var entry = iterator.next();
            long amount = entry.getLongValue();
            long inserted = inventory.insert(entry.getKey(), amount, Actionable.MODULATE, source);
            if (inserted > 0) {
                if (inserted >= amount) iterator.remove();
                else entry.setValue(amount - inserted);
                changed = true;
            }
        }
        if (changed) onChanged();
    }

    public boolean put(AEKey key, long amount) {
        if (storage.put(key, amount) == amount) return false;
        onChanged();
        return true;
    }

    public boolean remove(AEKey key) {
        if (storage.removeLong(key) == 0) return false;
        onChanged();
        return true;
    }

    public void onChanged() {
        onContentsChanged.run();
    }

    @ClientFieldChangeListener(fieldName = "storage")
    private void updateEntries() {
        var contents = new ObjectArrayList<Entry>(storage.size());
        for (var entry : storage.object2LongEntrySet()) contents.add(new Entry(entry.getKey(), entry.getLongValue()));
        entries = contents;
    }

    @Override
    public Iterator<Object2LongMap.Entry<AEKey>> iterator() {
        return storage.object2LongEntrySet().iterator();
    }

    public boolean isEmpty() {
        return storage.isEmpty();
    }

    public record Entry(AEKey key, long amount) {}
}
