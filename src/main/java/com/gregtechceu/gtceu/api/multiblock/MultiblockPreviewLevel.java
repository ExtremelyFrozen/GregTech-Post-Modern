package com.gregtechceu.gtceu.api.multiblock;

import com.gregtechceu.gtceu.api.machine.MetaMachine;

import com.lowdragmc.lowdraglib2.utils.virtuallevel.TrackedDummyWorld;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongList;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.function.Predicate;

@NullMarked
public class MultiblockPreviewLevel extends TrackedDummyWorld {

    private @Nullable Long2ObjectMap<BlockState> renderStates;
    private @Nullable Long2ObjectMap<BlockEntity> renderEntities;

    public MultiblockPreviewLevel() {
        super();
    }

    public MultiblockPreviewLevel(Level level) {
        super(level);
        // Keep the source registries, but read and write only the preview's own chunks and block entities.
        proxyWorld.clear();
    }

    public Level getLevel() {
        Level proxy = proxyWorld.get();
        return proxy == null ? this : proxy;
    }

    public void setInnerBlockEntity(BlockEntity blockEntity) {
        blockEntity.setLevel(this);
        setBlockEntity(blockEntity);
    }

    /** Copies client-visible machine appearance without sharing live entities or their inventories. */
    public void copyMachineAppearance(Level source, BlockPos sourcePos, BlockPos previewPos) {
        if (source.getBlockEntity(sourcePos) instanceof MetaMachine original &&
                getBlockEntity(previewPos) instanceof MetaMachine preview &&
                original.getDefinition() == preview.getDefinition()) {
            preview.setPaintingColor(original.getPaintingColor());
            preview.setRenderState(original.getRenderState());
        }
    }

    public void setRenderFilter(Predicate<BlockPos> renderFilter) {
        setBlockFilter(renderFilter);
    }

    /** Builds the read-only render view after all block initializers have finished. */
    public void setRenderBlocks(LongList positions) {
        setBlockFilter(null);
        var states = new Long2ObjectOpenHashMap<BlockState>(positions.size());
        states.defaultReturnValue(Blocks.AIR.defaultBlockState());
        var entities = new Long2ObjectOpenHashMap<BlockEntity>();
        var pos = new BlockPos.MutableBlockPos();
        for (int i = 0; i < positions.size(); i++) {
            long packed = positions.getLong(i);
            pos.set(packed);
            states.put(packed, super.getBlockState(pos));
            var entity = super.getBlockEntity(pos);
            if (entity != null) entities.put(packed, entity);
        }
        renderStates = states;
        renderEntities = entities;
    }

    @Override
    public void setBlockFilter(@Nullable Predicate<BlockPos> filter) {
        renderStates = null;
        renderEntities = null;
        super.setBlockFilter(filter);
    }

    @Override
    public BlockState getBlockState(BlockPos pos) {
        return renderStates == null ? super.getBlockState(pos) : renderStates.get(pos.asLong());
    }

    @Override
    public @Nullable BlockEntity getBlockEntity(BlockPos pos) {
        return renderEntities == null ? super.getBlockEntity(pos) : renderEntities.get(pos.asLong());
    }

    @Override
    public FluidState getFluidState(BlockPos pos) {
        return getBlockState(pos).getFluidState();
    }
}
