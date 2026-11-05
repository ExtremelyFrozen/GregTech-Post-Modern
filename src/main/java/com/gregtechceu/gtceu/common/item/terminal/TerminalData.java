package com.gregtechceu.gtceu.common.item.terminal;

import com.gregtechceu.gtceu.api.sync_system.annotations.ClientFieldChangeListener;
import com.gregtechceu.gtceu.api.sync_system.annotations.ItemSave;
import com.gregtechceu.gtceu.api.sync_system.annotations.SyncToClient;
import com.gregtechceu.gtceu.api.sync_system.managed.ItemSyncHolder;
import com.gregtechceu.gtceu.api.sync_system.rpc.RPCMethod;
import com.gregtechceu.gtceu.api.sync_system.rpc.RPCSender;
import com.gregtechceu.gtceu.common.item.datacomponents.TerminalControllerBinding;
import com.gregtechceu.gtceu.common.item.datacomponents.TerminalMachine;
import com.gregtechceu.gtceu.common.item.terminal.menu.TerminalMenuState;
import com.gregtechceu.gtceu.common.item.terminal.menu.TerminalSession;
import com.gregtechceu.gtceu.common.item.terminal.network.TerminalChange;

import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import lombok.Getter;
import lombok.Setter;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/** Saved terminal choices and the authoritative view for this stack's current menu opening. */
@NullMarked
public final class TerminalData extends ItemSyncHolder {

    @Getter
    @Setter
    @ItemSave
    private @Nullable TerminalControllerBinding binding;
    @Getter
    @ItemSave
    private final Object2ObjectMap<ResourceLocation, TerminalMachine> machines = new Object2ObjectLinkedOpenHashMap<>();
    @Getter
    @Setter
    @SyncToClient
    private @Nullable TerminalMenuState state;
    @Getter
    @Setter
    private @Nullable TerminalSession session;
    @Setter
    private @Nullable Runnable stateListener;

    public TerminalData(ItemStack stack, RegistryAccess registries) {
        super(stack, registries);
    }

    @ClientFieldChangeListener(fieldName = "state")
    public void onStateChanged() {
        if (stateListener != null) stateListener.run();
    }

    @RPCMethod
    public void change(RPCSender sender, long sequence, long revision, TerminalChange update) {
        if (sender.isRemote() && session != null) {
            session.applyUpdate(sender.asPlayer(), sequence, revision, update);
        }
    }

    @RPCMethod
    public void execute(RPCSender sender, long sequence, long revision, long fingerprint) {
        if (sender.isRemote() && session != null) {
            session.execute(sender.asPlayer(), sequence, revision, fingerprint);
        }
    }
}
