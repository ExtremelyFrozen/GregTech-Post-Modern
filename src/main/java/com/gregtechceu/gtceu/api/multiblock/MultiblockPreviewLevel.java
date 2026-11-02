package com.gregtechceu.gtceu.api.multiblock;

import com.gregtechceu.gtceu.api.machine.MetaMachine;

import com.lowdragmc.lowdraglib2.utils.virtuallevel.TrackedDummyWorld;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.function.Predicate;

public class MultiblockPreviewLevel extends TrackedDummyWorld {

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
}
