package com.gregtechceu.gtceu.api.misc.virtualregistry.entries;

import com.gregtechceu.gtceu.api.misc.virtualregistry.EntryTypes;
import com.gregtechceu.gtceu.api.misc.virtualregistry.VirtualEntry;
import com.gregtechceu.gtceu.api.sync_system.annotations.SaveField;
import com.gregtechceu.gtceu.api.sync_system.annotations.SyncToClient;
import com.gregtechceu.gtceu.api.transfer.item.CustomItemStackHandler;

import net.minecraft.MethodsReturnNonnullByDefault;

import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class VirtualItemStorage extends VirtualEntry {

    protected static final int DEFAULT_SLOT_AMOUNT = 1;

    @NotNull
    @Getter
    @SaveField
    @SyncToClient
    private final CustomItemStackHandler handler;

    public VirtualItemStorage() {
        this(DEFAULT_SLOT_AMOUNT);
    }

    public VirtualItemStorage(int slots) {
        handler = new CustomItemStackHandler(slots);
    }

    @Override
    public EntryTypes<? extends VirtualEntry> getType() {
        return EntryTypes.ENDER_ITEM;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof VirtualItemStorage other)) return false;
        return other.handler == this.handler;
    }

    @Override
    public void copyFrom(VirtualEntry source) {
        super.copyFrom(source);
        VirtualItemStorage storage = (VirtualItemStorage) source;
        if (handler.getSlots() != storage.handler.getSlots()) handler.setSize(storage.handler.getSlots());
        for (int i = 0; i < handler.getSlots(); i++) {
            handler.setStackInSlot(i, storage.handler.getStackInSlot(i).copy());
        }
    }

    @Override
    public boolean canRemove() {
        return super.canRemove() && isEmpty();
    }

    public boolean isEmpty() {
        for (int i = 0; i < handler.getSlots(); i++) {
            if (!handler.getStackInSlot(i).isEmpty()) return false;
        }
        return true;
    }
}
