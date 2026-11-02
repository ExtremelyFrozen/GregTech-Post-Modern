package com.gregtechceu.gtceu.common.item.behavior;

import com.gregtechceu.gtceu.GTCEu;
import com.gregtechceu.gtceu.api.gui.factory.HeldItemUIHelper;
import com.gregtechceu.gtceu.api.gui.factory.HeldItemUIHolder;
import com.gregtechceu.gtceu.api.item.component.IItemUIFactory;
import com.gregtechceu.gtceu.api.machine.MetaMachine;
import com.gregtechceu.gtceu.api.machine.MultiblockMachineDefinition;
import com.gregtechceu.gtceu.api.machine.multiblock.MultiblockControllerMachine;
import com.gregtechceu.gtceu.api.registry.GTRegistries;
import com.gregtechceu.gtceu.common.data.GTDataComponents;
import com.gregtechceu.gtceu.common.item.datacomponents.TerminalControllerBinding;
import com.gregtechceu.gtceu.common.item.terminal.ui.TerminalScreen;
import com.gregtechceu.gtceu.common.machine.owner.MachineOwner;

import com.lowdragmc.lowdraglib2.gui.ui.UI;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.Objects;

@NullMarked
public class TerminalBehavior implements IItemUIFactory {

    @Override
    public InteractionResult onItemUseFirst(ItemStack itemStack, UseOnContext context) {
        Player player = context.getPlayer();
        if (player == null) {
            return InteractionResult.PASS;
        }
        Level level = context.getLevel();
        BlockPos blockPos = context.getClickedPos();
        if (MetaMachine.getMachine(level, blockPos) instanceof MultiblockControllerMachine controller) {
            if (player instanceof ServerPlayer serverPlayer &&
                    !bind(controller, serverPlayer, context.getHand(), itemStack)) {
                return InteractionResult.FAIL;
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        return InteractionResult.PASS;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(ItemStack item, Level level, Player player,
                                                  InteractionHand usedHand) {
        if (player instanceof ServerPlayer serverPlayer &&
                !openBound(serverPlayer, usedHand, item)) {
            return InteractionResultHolder.fail(item);
        }
        return InteractionResultHolder.sidedSuccess(item, level.isClientSide);
    }

    @Override
    public boolean canCreateUI(HeldItemUIHolder holder, Player player) {
        TerminalControllerBinding binding = holder.getOpenedStack().get(GTDataComponents.TERMINAL_CONTROLLER);
        return binding != null &&
                GTRegistries.MACHINES.get(binding.definitionId()) instanceof MultiblockMachineDefinition &&
                isUIStillValid(holder, player);
    }

    @Override
    public boolean isUIStillValid(HeldItemUIHolder holder, Player player) {
        TerminalControllerBinding binding = holder.getOpenedStack().get(GTDataComponents.TERMINAL_CONTROLLER);
        if (binding == null || !ItemStack.isSameItem(holder.getHeld(), holder.getOpenedStack()) ||
                !Objects.equals(binding, holder.getHeld().get(GTDataComponents.TERMINAL_CONTROLLER)) ||
                !binding.dimension().equals(player.level().dimension())) {
            return false;
        }
        if (player.level().isClientSide) return true;
        MultiblockControllerMachine controller = boundController(player, binding);
        return controller != null && canAccess(controller, player, holder.getHand(), holder.getHeld());
    }

    @Override
    public UI createUI(HeldItemUIHolder holder, Player player) {
        return new TerminalScreen(holder, player).createUI();
    }

    private static boolean bind(MultiblockControllerMachine controller, ServerPlayer player, InteractionHand hand,
                                ItemStack terminal) {
        if (!canAccess(controller, player, hand, terminal)) return reject(player, "access_denied");
        terminal.set(GTDataComponents.TERMINAL_CONTROLLER, new TerminalControllerBinding(controller.getBlockPos(),
                player.level().dimension(), controller.getDefinition().getId()));
        player.displayClientMessage(Component.translatable("gtpm.multiblock.autobuild.terminal.bound",
                controller.getDefinition().getBlock().getName(), controller.getBlockPos().toShortString()), true);
        return true;
    }

    private static boolean openBound(ServerPlayer player, InteractionHand hand, ItemStack terminal) {
        TerminalControllerBinding binding = terminal.get(GTDataComponents.TERMINAL_CONTROLLER);
        if (binding == null) return reject(player, "unbound");
        if (!binding.dimension().equals(player.level().dimension())) return reject(player, "wrong_dimension");
        if (!player.level().isLoaded(binding.position())) return reject(player, "unloaded");
        MultiblockControllerMachine controller = boundController(player, binding);
        if (controller == null) return reject(player, "controller_missing");
        if (player.distanceToSqr(Vec3.atCenterOf(binding.position())) > 64) return reject(player, "out_of_range");
        if (!canAccess(controller, player, hand, terminal)) return reject(player, "access_denied");
        try {
            return HeldItemUIHelper.open(player, hand) || reject(player, "open_failed");
        } catch (RuntimeException exception) {
            GTCEu.LOGGER.error("Failed to open terminal bound to {} at {} for {}", binding.definitionId(),
                    binding.position(), player.getGameProfile().getName(), exception);
            return reject(player, "open_failed");
        }
    }

    private static @Nullable MultiblockControllerMachine boundController(Player player,
                                                                         TerminalControllerBinding binding) {
        if (!player.level().isLoaded(binding.position())) return null;
        return MetaMachine.getMachine(player.level(),
                binding.position()) instanceof MultiblockControllerMachine controller &&
                binding.definitionId().equals(controller.getDefinition().getId()) ? controller : null;
    }

    private static boolean canAccess(MultiblockControllerMachine controller, Player player,
                                     InteractionHand hand, ItemStack terminal) {
        return !player.isSpectator() && player.getItemInHand(hand) == terminal &&
                controller.getLevel() == player.level() &&
                player.distanceToSqr(Vec3.atCenterOf(controller.getBlockPos())) <= 64 &&
                player.level().mayInteract(player, controller.getBlockPos()) &&
                MachineOwner.canOpenOwnerMachine(player, controller) &&
                MachineOwner.canBreakOwnerMachine(player, controller);
    }

    private static boolean reject(ServerPlayer player, String reason) {
        player.displayClientMessage(Component.translatable("gtpm.multiblock.autobuild.terminal." + reason), true);
        return false;
    }
}
