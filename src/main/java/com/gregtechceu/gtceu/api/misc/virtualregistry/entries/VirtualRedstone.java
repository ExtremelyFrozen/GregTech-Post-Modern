package com.gregtechceu.gtceu.api.misc.virtualregistry.entries;

import com.gregtechceu.gtceu.api.misc.virtualregistry.EntryTypes;
import com.gregtechceu.gtceu.api.misc.virtualregistry.VirtualEntry;
import com.gregtechceu.gtceu.api.sync_system.annotations.SaveField;
import com.gregtechceu.gtceu.api.sync_system.annotations.SyncToClient;

import it.unimi.dsi.fastutil.objects.Object2ShortMap;
import it.unimi.dsi.fastutil.objects.Object2ShortOpenHashMap;
import lombok.Getter;

import java.util.UUID;

public class VirtualRedstone extends VirtualEntry {

    @Getter
    @SaveField
    @SyncToClient
    private final Object2ShortMap<UUID> members = new Object2ShortOpenHashMap<>();

    public VirtualRedstone() {}

    public int getSignal() {
        return members.values().intStream().max().orElse(0);
    }

    public void addMember(UUID uuid) {
        members.put(uuid, (short) 0);
    }

    public void setSignal(UUID uuid, int signal) {
        if (!members.containsKey(uuid)) return;
        members.put(uuid, (short) signal);
    }

    public void removeMember(UUID uuid) {
        members.removeShort(uuid);
    }

    @Override
    public EntryTypes<? extends VirtualEntry> getType() {
        return EntryTypes.ENDER_REDSTONE;
    }

    @Override
    public void copyFrom(VirtualEntry source) {
        super.copyFrom(source);
        VirtualRedstone redstone = (VirtualRedstone) source;
        members.clear();
        members.putAll(redstone.members);
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof VirtualRedstone other)) return false;
        return other.members == this.members;
    }

    @Override
    public boolean canRemove() {
        return super.canRemove() && members.isEmpty();
    }
}
