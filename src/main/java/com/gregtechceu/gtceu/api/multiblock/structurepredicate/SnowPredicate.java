package com.gregtechceu.gtceu.api.multiblock.structurepredicate;

import com.gregtechceu.gtceu.api.multiblock.MultiblockBlockInfo;
import com.gregtechceu.gtceu.api.multiblock.MultiblockState;
import com.gregtechceu.gtceu.utils.GTUtil;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import com.mojang.serialization.MapCodec;
import it.unimi.dsi.fastutil.objects.ObjectList;
import org.jetbrains.annotations.Unmodifiable;

/**
 * Serialized snow predicate for JSON multiblock patterns.
 *
 * <p>
 * This predicate replaces Java-only custom snow checks and accepts the same states as
 * {@link GTUtil#isBlockSnow(BlockState)}.
 */
public enum SnowPredicate implements StructurePredicate {

    INSTANCE;

    /**
     * JSON codec for {@code gtpm:snow}; no extra fields are required.
     */
    public static final MapCodec<SnowPredicate> CODEC = MapCodec.unit(INSTANCE);

    @Override
    public StructurePredicateType<?> type() {
        return StructurePredicateType.SNOW;
    }

    @Override
    public boolean test(MultiblockState multiblockState, boolean mutateCount) {
        return GTUtil.isBlockSnow(multiblockState.getBlockState());
    }

    @Override
    public @Unmodifiable ObjectList<MultiblockBlockInfo> candidates() {
        return ObjectList.of(MultiblockBlockInfo.fromBlockState(Blocks.SNOW_BLOCK.defaultBlockState()));
    }

    @Override
    public @Unmodifiable ObjectList<Block> blockCandidates() {
        return ObjectList.of(Blocks.SNOW_BLOCK);
    }
}
