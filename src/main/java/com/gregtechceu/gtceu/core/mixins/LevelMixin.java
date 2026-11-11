package com.gregtechceu.gtceu.core.mixins;

import com.gregtechceu.gtceu.api.multiblock.MultiblockWorldSavedData;

import com.lowdragmc.lowdraglib2.async.AsyncThreadData;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunk;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Level.class)
public abstract class LevelMixin implements LevelAccessor {

    @Shadow
    @Final
    public boolean isClientSide;

    @Shadow
    @Final
    private Thread thread;

    @Inject(method = "getBlockEntity", at = @At(value = "HEAD"), cancellable = true)
    private void gtceu$getBlockEntityOffThread(BlockPos pos, CallbackInfoReturnable<BlockEntity> cir) {
        if (Thread.currentThread() == this.thread) return;
        if (this.isClientSide) return;
        if (!MultiblockWorldSavedData.isThreadService() && !AsyncThreadData.isThreadService()) return;

        int chunkX = pos.getX() >> 4, chunkZ = pos.getZ() >> 4;
        if (!this.getChunkSource().hasChunk(chunkX, chunkZ)) return;

        ChunkAccess chunk = this.getChunkSource().getChunkNow(chunkX, chunkZ);
        if (chunk instanceof LevelChunk levelChunk) {
            cir.setReturnValue(levelChunk.getBlockEntities().get(pos));
        }
    }

    @Inject(method = "getBlockState", at = @At(value = "HEAD"), cancellable = true)
    private void gtceu$getBlockStateOffThread(BlockPos pos, CallbackInfoReturnable<BlockState> cir) {
        if (Thread.currentThread() == this.thread) return;
        if (this.isClientSide) return;
        if (!MultiblockWorldSavedData.isThreadService() && !AsyncThreadData.isThreadService()) return;

        int chunkX = pos.getX() >> 4, chunkZ = pos.getZ() >> 4;
        if (!this.getChunkSource().hasChunk(chunkX, chunkZ)) return;

        ChunkAccess chunk = this.getChunkSource().getChunkNow(chunkX, chunkZ);
        if (chunk != null) {
            cir.setReturnValue(chunk.getBlockState(pos));
        }
    }
}
