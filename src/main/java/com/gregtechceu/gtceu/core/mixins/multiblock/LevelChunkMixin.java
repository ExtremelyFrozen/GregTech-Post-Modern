package com.gregtechceu.gtceu.core.mixins.multiblock;

import com.gregtechceu.gtceu.api.multiblock.MultiblockWorldSavedData;
import com.gregtechceu.gtceu.api.multiblock.index.ChunkStructureAccess;
import com.gregtechceu.gtceu.api.multiblock.index.ChunkStructureIndex;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
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
import org.spongepowered.asm.mixin.injection.Slice;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Map;

@Mixin(LevelChunk.class)
public abstract class LevelChunkMixin implements ChunkStructureAccess {

    @Shadow
    @Final
    private Level level;

    @Shadow
    public abstract Map<BlockPos, BlockEntity> getBlockEntities();

    @Unique
    private @Nullable ChunkStructureIndex gtceu$structureIndex;

    @Unique
    private @Nullable MultiblockWorldSavedData gtceu$structureData;

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
        if (cir.getReturnValue() != null) gtceu$notifyStructureChange(pos);
    }

    @Inject(method = "setBlockEntity",
            at = @At(value = "INVOKE",
                     target = "Ljava/util/Map;put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;"))
    private void gtceu$blockEntityChanged(BlockEntity entity, CallbackInfo ci) {
        if (getBlockEntities().get(entity.getBlockPos()) != entity) gtceu$notifyStructureChange(entity.getBlockPos());
    }

    @Inject(method = "removeBlockEntity",
            at = @At(value = "INVOKE", target = "Ljava/util/Map;remove(Ljava/lang/Object;)Ljava/lang/Object;"))
    private void gtceu$blockEntityRemoved(BlockPos pos, CallbackInfo ci) {
        gtceu$notifyStructureChange(pos);
    }

    @Inject(method = "getBlockEntity(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/chunk/LevelChunk$EntityCreationType;)Lnet/minecraft/world/level/block/entity/BlockEntity;",
            slice = @Slice(from = @At(value = "INVOKE",
                                      target = "Lnet/minecraft/world/level/block/entity/BlockEntity;isRemoved()Z")),
            at = @At(value = "INVOKE", target = "Ljava/util/Map;remove(Ljava/lang/Object;)Ljava/lang/Object;"))
    private void gtceu$removedBlockEntityDropped(BlockPos pos, LevelChunk.EntityCreationType type,
                                                 CallbackInfoReturnable<BlockEntity> cir) {
        gtceu$notifyStructureChange(pos);
    }

    @Inject(method = "clearAllBlockEntities", at = @At("HEAD"))
    private void gtceu$blockEntitiesCleared(CallbackInfo ci) {
        for (BlockPos pos : getBlockEntities().keySet()) gtceu$notifyStructureChange(pos);
    }

    @Override
    public void gtceu$notifyStructureChange(BlockPos pos) {
        if (!(level instanceof ServerLevel serverLevel)) return;
        var server = serverLevel.getServer();
        if (!server.isSameThread()) {
            BlockPos position = pos.immutable();
            server.execute(() -> gtceu$notifyStructureChange(position));
            return;
        }
        if (gtceu$structureData == null) gtceu$structureData = MultiblockWorldSavedData.getOrCreate(serverLevel);
        if (gtceu$structureIndex != null) gtceu$structureIndex.changed(pos, gtceu$structureData);
        gtceu$structureData.onChunkPositionChanged(pos);
    }
}
