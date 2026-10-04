package com.gregtechceu.gtceu.common.item.behavior;

import com.gregtechceu.gtceu.api.item.component.IInteractionItem;
import com.gregtechceu.gtceu.api.machine.MetaMachine;
import com.gregtechceu.gtceu.api.machine.multiblock.MultiblockControllerMachine;
import com.gregtechceu.gtceu.common.item.terminal.menu.TerminalBoundMenuType;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

import org.jspecify.annotations.NullMarked;

@NullMarked
public class TerminalBehavior implements IInteractionItem {

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        if (player != null && player.isShiftKeyDown()) {
            Level level = context.getLevel();
            BlockPos blockPos = context.getClickedPos();
            if (MetaMachine.getMachine(level, blockPos) instanceof MultiblockControllerMachine controller) {
                if (!level.isClientSide) {
                    if (player instanceof ServerPlayer serverPlayer) {
                        TerminalBoundMenuType.open(controller, serverPlayer, context.getHand(),
                                context.getItemInHand());
                    }
                }
                return InteractionResult.sidedSuccess(level.isClientSide);
            }
        }
        return InteractionResult.PASS;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(ItemStack item, Level level, Player player,
                                                  InteractionHand usedHand) {
        return InteractionResultHolder.pass(item);
    }
}
