package com.gregtechceu.gtceu.core.mixins.multiblock;

import com.gregtechceu.gtceu.api.multiblock.index.ChunkStructureAccess;
import com.gregtechceu.gtceu.api.multiblock.index.ChunkStructureIndex;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;

import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LevelChunk.class)
public abstract class LevelChunkMixin implements ChunkStructureAccess {

    @Shadow
    @Final
    private Level level;

    @Unique
    private @Nullable ChunkStructureIndex gtceu$structureIndex;

    @Override
    public @Nullable ChunkStructureIndex gtceu$getStructureIndex() {
        return gtceu$structureIndex;
    }

    @Override
    public ChunkStructureIndex gtceu$createStructureIndex() {
        if (gtceu$structureIndex == null) gtceu$structureIndex = new ChunkStructureIndex();
        return gtceu$structureIndex;
    }

    @Inject(method = "setBlockState", at = @At("RETURN"))
    private void gtceu$blockChanged(BlockPos pos, BlockState state, boolean moving,
                                    CallbackInfoReturnable<BlockState> cir) {
        if (cir.getReturnValue() != null) gtceu$notifyStructure(pos);
    }

    @Inject(method = "setBlockEntity", at = @At("RETURN"))
    private void gtceu$blockEntityChanged(BlockEntity entity, CallbackInfo ci) {
        gtceu$notifyStructure(entity.getBlockPos());
    }

    @Inject(method = "removeBlockEntity", at = @At("RETURN"))
    private void gtceu$blockEntityRemoved(BlockPos pos, CallbackInfo ci) {
        gtceu$notifyStructure(pos);
    }

    @Unique
    private void gtceu$notifyStructure(BlockPos pos) {
        if (!level.isClientSide && gtceu$structureIndex != null) gtceu$structureIndex.changed(pos);
    }
}
