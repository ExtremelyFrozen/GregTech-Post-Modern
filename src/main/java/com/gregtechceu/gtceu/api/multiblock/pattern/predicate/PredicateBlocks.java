package com.gregtechceu.gtceu.api.multiblock.pattern.predicate;

import com.gregtechceu.gtceu.api.block.MetaMachineBlock;
import com.gregtechceu.gtceu.api.multiblock.MultiblockBlockInfo;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

import org.apache.commons.lang3.ArrayUtils;

import java.util.Arrays;
import java.util.List;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class PredicateBlocks extends PredicateRule {

    public Block[] blocks;

    public PredicateBlocks(Block... blocks) {
        this.blocks = blocks;
        buildPredicate();
    }

    @Override
    public PredicateRule buildPredicate() {
        List<Block> filteredBlocks = new ObjectArrayList<>(blocks.length);
        for (Block block : blocks) {
            if (block != null && block != Blocks.AIR) {
                filteredBlocks.add(block);
            }
        }
        if (filteredBlocks.isEmpty()) {
            throw new IllegalArgumentException("Empty predicate: " + Arrays.toString(blocks));
        }
        blocks = filteredBlocks.toArray(new Block[0]);
        var block = blocks[0];
        if (block instanceof MetaMachineBlock) {
            blockInfo = () -> MultiblockBlockInfo.fromBlock(block);
        } else {
            var info = MultiblockBlockInfo.fromBlock(block);
            blockInfo = () -> info;
        }
        predicate = state -> ArrayUtils.contains(blocks, state.getBlockState().getBlock());
        candidates = () -> blocks;
        return this;
    }
}
