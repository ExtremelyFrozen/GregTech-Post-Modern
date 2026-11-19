package com.gregtechceu.gtceu.common.machine.multiblock.primitive;

import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.blockentity.BlockEntityCreationInfo;
import com.gregtechceu.gtceu.api.capability.IWorkable;
import com.gregtechceu.gtceu.api.item.ComponentItem;
import com.gregtechceu.gtceu.api.machine.multiblock.WorkableMultiblockMachine;
import com.gregtechceu.gtceu.api.machine.trait.RecipeLogic;
import com.gregtechceu.gtceu.api.machine.trait.WorkLogic;
import com.gregtechceu.gtceu.common.data.GTBlocks;
import com.gregtechceu.gtceu.common.item.behavior.LighterBehavior;
import com.gregtechceu.gtceu.utils.ExtendedUseOnContext;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.common.ItemAbilities;

import it.unimi.dsi.fastutil.longs.Long2BooleanMap;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;

import java.util.*;

public class CharcoalPileIgniterMachine extends WorkableMultiblockMachine implements IWorkable {

    private final Collection<BlockPos> logPos = new ObjectOpenHashSet<>();

    private boolean hasAir = false;

    public CharcoalPileIgniterMachine(BlockEntityCreationInfo info) {
        super(info, new CharcoalRecipeLogic());
    }

    @Override
    public void formStructure(String structureName) {
        super.formStructure(structureName);
        if (DEFAULT_STRUCTURE.equals(structureName)) {
            hasAir = false;
            if (getMultiblockState(structureName).getFacts().containsKey("logPos")) {
                Long2BooleanMap logPositions = getMultiblockState(structureName).getFacts().get("logPos");
                for (var entry : logPositions.long2BooleanEntrySet()) {
                    if (entry.getBooleanValue()) {
                        logPos.add(BlockPos.of(entry.getLongKey()));
                    } else {
                        hasAir = true;
                    }
                }
            }
            this.getRecipeLogic().setDuration(Math.max(1, (int) Math.sqrt(logPos.size() * 240_000)));
        }
    }

    @Override
    public CharcoalRecipeLogic getRecipeLogic() {
        return (CharcoalRecipeLogic) super.getRecipeLogic();
    }

    @Override
    public boolean isActive() {
        return getWorkLogic().isWorking();
    }

    @Override
    public boolean isWorkingEnabled() {
        return true;
    }

    @Override
    public void setWorkingEnabled(boolean isWorkingAllowed) {}

    @Override
    @OnlyIn(Dist.CLIENT)
    public void clientTick() {
        super.clientTick();
        if (isActive()) {
            var pos = this.getBlockPos();
            var facing = Direction.UP;
            float xPos = facing.getStepX() * 0.76F + pos.getX() + 0.25F + GTValues.RNG.nextFloat() / 2.0F;
            float yPos = facing.getStepY() * 0.76F + pos.getY() + 0.25F;
            float zPos = facing.getStepZ() * 0.76F + pos.getZ() + 0.25F + GTValues.RNG.nextFloat() / 2.0F;

            float ySpd = facing.getStepY() * 0.1F + 0.01F * GTValues.RNG.nextFloat();
            float horSpd = 0.03F * GTValues.RNG.nextFloat();
            float horSpd2 = 0.03F * GTValues.RNG.nextFloat();

            if (GTValues.RNG.nextFloat() < 0.1F) {
                getLevel().playLocalSound(xPos, yPos, zPos, SoundEvents.CAMPFIRE_CRACKLE, SoundSource.BLOCKS, 1.0F,
                        1.0F, false);
            }
            for (float xi = xPos - 1; xi <= xPos + 1; xi++) {
                for (float zi = zPos - 1; zi <= zPos + 1; zi++) {
                    if (GTValues.RNG.nextFloat() < .9F)
                        continue;
                    getLevel().addParticle(ParticleTypes.LARGE_SMOKE, xi, yPos, zi, horSpd, ySpd, horSpd2);
                }
            }
        }
    }

    private void convertLogBlocks() {
        Level level = getLevel();
        for (BlockPos pos : logPos) {
            level.setBlockAndUpdate(pos, GTBlocks.BRITTLE_CHARCOAL.getDefaultState());
        }
        logPos.clear();
    }

    @Override
    public InteractionResult onUse(ExtendedUseOnContext context) {
        var stack = context.getItemInHand();
        var player = context.getPlayer();
        var hand = context.getHand();

        if (!isFormed() || hasAir) {
            return super.onUseWithItem(context);
        }
        if (!stack.canPerformAction(ItemAbilities.FIRESTARTER_LIGHT)) {
            return InteractionResult.PASS;
        }

        if (getLevel().isClientSide && !isActive()) {
            return InteractionResult.SUCCESS;
        } else if (!isActive()) {
            boolean shouldActivate = false;
            if (stack.getItem() instanceof ComponentItem compItem) {
                for (var component : compItem.getComponents()) {
                    if (component instanceof LighterBehavior lighter && lighter.consumeFuel(player, stack)) {
                        shouldActivate = true;
                        break;
                    }
                }
            } else if (stack.isDamageableItem()) {
                stack.hurtAndBreak(1, player, LivingEntity.getSlotForHand(hand));
                shouldActivate = true;
            } else {
                stack.shrink(1);
                shouldActivate = true;
            }

            if (shouldActivate) {
                getRecipeLogic().setStatus(WorkLogic.Status.WORKING);

                getLevel().playSound(null, getBlockPos(),
                        stack.is(Items.FIRE_CHARGE) ? SoundEvents.FIRECHARGE_USE : SoundEvents.FLINTANDSTEEL_USE,
                        SoundSource.BLOCKS, 1.0f, 1.0f);
                return InteractionResult.CONSUME;
            }
        }
        return super.onUse(context);
    }

    public static class CharcoalRecipeLogic extends RecipeLogic {

        public CharcoalRecipeLogic() {
            super();
        }

        @Override
        public CharcoalPileIgniterMachine getMachine() {
            return (CharcoalPileIgniterMachine) super.getMachine();
        }

        @Override
        public void serverTick() {
            super.serverTick();
            if (isWorking() && duration > 0) {
                if (++progress >= duration) {
                    progress = 0;
                    duration = 0;
                    getMachine().convertLogBlocks();
                    setStatus(Status.IDLE);
                }
            }
        }

        public void setDuration(int max) {
            this.duration = max;
        }
    }
}
