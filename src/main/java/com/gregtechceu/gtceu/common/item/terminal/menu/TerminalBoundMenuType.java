package com.gregtechceu.gtceu.common.item.terminal.menu;

import com.gregtechceu.gtceu.GTCEu;
import com.gregtechceu.gtceu.api.machine.multiblock.MultiblockControllerMachine;
import com.gregtechceu.gtceu.common.machine.owner.MachineOwner;

import com.lowdragmc.lowdraglib2.gui.holder.ModularUIContainerMenu;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import org.jspecify.annotations.Nullable;

import java.util.UUID;

/** Opens and reconstructs the exact controller-bound automatic-build terminal menu. */
public final class TerminalBoundMenuType {

    private TerminalBoundMenuType() {}

    public static boolean open(MultiblockControllerMachine controller, ServerPlayer player, InteractionHand hand,
                               ItemStack terminal) {
        if (player.isSpectator() || player.getItemInHand(hand) != terminal || controller.getLevel() != player.level() ||
                !player.level().isLoaded(controller.getBlockPos()) ||
                player.distanceToSqr(Vec3.atCenterOf(controller.getBlockPos())) > 64 ||
                !player.level().mayInteract(player, controller.getBlockPos()) ||
                !MachineOwner.canOpenOwnerMachine(player, controller) ||
                !MachineOwner.canBreakOwnerMachine(player, controller)) {
            return false;
        }
        @Nullable
        TerminalBoundMenuHolder holder = null;
        try {
            holder = new TerminalBoundMenuHolder(player, hand, terminal, controller);
            if (!holder.isStillValid(player)) {
                holder.close(player);
                return false;
            }
            if (player.openMenu(holder).isEmpty()) {
                holder.close(player);
                return false;
            }
            return true;
        } catch (RuntimeException exception) {
            if (player.containerMenu instanceof TerminalBoundMenu) {
                player.closeContainer();
            } else if (holder != null) {
                holder.close(player);
            }
            GTCEu.LOGGER.error("Failed to open automatic-build terminal at {} for {}", controller.getBlockPos(),
                    player.getGameProfile().getName(), exception);
            return false;
        }
    }

    /** Reconstructs a client menu from the exact identity written by its server holder. */
    public static ModularUIContainerMenu create(int containerId, Inventory inventory, RegistryFriendlyByteBuf data) {
        InteractionHand hand = data.readEnum(InteractionHand.class);
        BlockPos pos = data.readBlockPos();
        ResourceKey<Level> dimension = ResourceKey.create(Registries.DIMENSION, data.readResourceLocation());
        ResourceLocation definitionId = data.readResourceLocation();
        UUID sessionId = data.readUUID();
        TerminalMenuState state = TerminalMenuState.STREAM_CODEC.decode(data);
        TerminalBoundMenuHolder holder = TerminalBoundMenuHolder.client(inventory.player, hand, pos, dimension,
                definitionId, sessionId, state);
        return new TerminalBoundMenu(containerId, inventory, holder);
    }
}
