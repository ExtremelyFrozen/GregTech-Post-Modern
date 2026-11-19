package com.gregtechceu.gtceu.api.multiblock.autobuild.plan;

import com.gregtechceu.gtceu.api.multiblock.MultiblockBlockInfo;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.function.Consumer;

/**
 * Immutable block-entity creation description stored by a plan without caching a live block entity.
 */
public final class PlannedBlockInfo {

    private final BlockState blockState;
    private final boolean hasBlockEntity;
    private final ItemStack itemStack;
    private final Consumer<BlockEntity> postCreate;

    private PlannedBlockInfo(BlockState blockState, boolean hasBlockEntity, ItemStack itemStack,
                             Consumer<BlockEntity> postCreate) {
        this.blockState = blockState;
        this.hasBlockEntity = hasBlockEntity;
        this.itemStack = itemStack.copy();
        this.postCreate = postCreate;
    }

    public static PlannedBlockInfo from(MultiblockBlockInfo source) {
        return new PlannedBlockInfo(source.getBlockState(), source.hasBlockEntity(), source.getItemStackForm(),
                source::postEntity);
    }

    public static PlannedBlockInfo from(BlockState state) {
        return from(MultiblockBlockInfo.fromBlockState(state));
    }

    public BlockState blockState() {
        return blockState;
    }

    public boolean hasBlockEntity() {
        return hasBlockEntity;
    }

    public ItemStack itemStack() {
        return itemStack.copy();
    }

    public PlannedBlockInfo withState(BlockState state) {
        return new PlannedBlockInfo(state, hasBlockEntity, itemStack, postCreate);
    }

    /**
     * Applies the pattern-provided initializer to the newly placed world block entity.
     */
    public void initialize(BlockEntity blockEntity) {
        postCreate.accept(blockEntity);
    }

    /**
     * Creates a fresh preview descriptor so windows never share a cached block entity.
     */
    public MultiblockBlockInfo createBlockInfo() {
        return new MultiblockBlockInfo(blockState, hasBlockEntity, itemStack.copy(), postCreate);
    }
}
