package com.gregtechceu.gtceu.common.item.terminal.menu;

import com.gregtechceu.gtceu.GTCEu;
import com.gregtechceu.gtceu.common.data.GTMenuTypes;
import com.gregtechceu.gtceu.common.item.terminal.network.CPacketTerminalAction;
import com.gregtechceu.gtceu.common.item.terminal.network.SPacketTerminalState;
import com.gregtechceu.gtceu.common.item.terminal.profile.TerminalProfileUpdate;

import com.lowdragmc.lowdraglib2.gui.holder.ModularUIContainerMenu;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.UUID;

/** Owns strict sequence/revision handshakes for one controller-bound terminal menu. */
public final class TerminalBoundMenu extends ModularUIContainerMenu {

    private final TerminalBoundMenuHolder holder;
    private long nextClientSequence;
    private long pendingSequence = -1;
    private int refreshTicker;

    TerminalBoundMenu(int containerId, Inventory inventory, TerminalBoundMenuHolder holder) {
        super(GTMenuTypes.TERMINAL_BOUND.get(), containerId, inventory, holder);
        this.holder = holder;
        nextClientSequence = holder.state().acknowledgedSequence() + 1;
    }

    public UUID sessionId() {
        return holder.sessionId();
    }

    /** Returns the last server-authoritative state acknowledged by this installed menu. */
    TerminalMenuState authoritativeState() {
        return holder.state();
    }

    boolean hasPendingAction() {
        return pendingSequence >= 0;
    }

    /** Sends one profile patch only when the previous action has received an authoritative acknowledgement. */
    public boolean requestUpdate(TerminalProfileUpdate update) {
        if (!inventory.player.level().isClientSide || inventory.player.containerMenu != this || pendingSequence >= 0 ||
                holder.state().executing()) {
            return false;
        }
        pendingSequence = nextClientSequence++;
        PacketDistributor.sendToServer(CPacketTerminalAction.update(containerId, holder.sessionId(), pendingSequence,
                holder.state().revision(), update));
        holder.updateClientInteractivity();
        return true;
    }

    /** Sends an execution request without any client-owned plan, target or configuration. */
    public boolean requestExecute() {
        if (!inventory.player.level().isClientSide || inventory.player.containerMenu != this || pendingSequence >= 0 ||
                holder.state().executing()) {
            return false;
        }
        pendingSequence = nextClientSequence++;
        PacketDistributor.sendToServer(CPacketTerminalAction.execute(containerId, holder.sessionId(), pendingSequence,
                holder.state().revision()));
        holder.updateClientInteractivity();
        return true;
    }

    /** Applies one already-routed C2S action to the exact installed server menu. */
    public void receiveAction(ServerPlayer player, CPacketTerminalAction action) {
        if (inventory.player != player || player.containerMenu != this || action.containerId() != containerId ||
                !holder.sessionId().equals(action.sessionId())) {
            return;
        }
        if (action.actionType() == CPacketTerminalAction.Type.UPDATE) {
            TerminalProfileUpdate update = action.update();
            if (update == null) {
                throw new IllegalArgumentException("Terminal profile action has no typed update");
            }
            holder.applyUpdate(player, action.sequence(), action.expectedRevision(), update);
        } else {
            holder.execute(player, action.sequence(), action.expectedRevision());
        }
        if (player.containerMenu == this) {
            sendState(player);
        }
    }

    /** Applies one session-bound S2C acknowledgement and unlocks the next client action. */
    public boolean applyClientState(Player player, int containerId, UUID sessionId, TerminalMenuState state) {
        if (!player.level().isClientSide || player.containerMenu != this || this.containerId != containerId ||
                !holder.sessionId().equals(sessionId) || !holder.applyClientState(player, state)) {
            return false;
        }
        if (pendingSequence >= 0 && state.acknowledgedSequence() >= pendingSequence) {
            pendingSequence = -1;
        }
        nextClientSequence = Math.max(nextClientSequence, state.acknowledgedSequence() + 1);
        holder.updateClientInteractivity();
        return true;
    }

    @Override
    public void broadcastChanges() {
        super.broadcastChanges();
        if (!(inventory.player instanceof ServerPlayer player) || player.containerMenu != this) {
            return;
        }
        if (!holder.isStillValid(player)) {
            player.closeContainer();
            return;
        }
        refreshTicker++;
        if (refreshTicker >= 20) {
            refreshTicker = 0;
            try {
                if (holder.refresh(player) && player.containerMenu == this) {
                    sendState(player);
                }
            } catch (RuntimeException exception) {
                GTCEu.LOGGER.error("Closed terminal menu after refresh failed", exception);
                player.closeContainer();
            }
        }
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        holder.close(player);
    }

    private void sendState(ServerPlayer player) {
        PacketDistributor.sendToPlayer(player,
                new SPacketTerminalState(containerId, holder.sessionId(), holder.state()));
    }
}
