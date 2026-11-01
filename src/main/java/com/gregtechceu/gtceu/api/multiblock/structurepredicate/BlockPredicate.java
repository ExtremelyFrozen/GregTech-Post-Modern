package com.gregtechceu.gtceu.api.multiblock.structurepredicate;

import com.gregtechceu.gtceu.api.multiblock.MultiblockBlockInfo;
import com.gregtechceu.gtceu.api.multiblock.MultiblockState;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.common.util.Lazy;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.ObjectImmutableList;
import it.unimi.dsi.fastutil.objects.ObjectList;
import org.jetbrains.annotations.Unmodifiable;
import org.jspecify.annotations.Nullable;

import java.util.Objects;
import java.util.function.Supplier;

import static com.gregtechceu.gtceu.api.multiblock.structurepredicate.Util.oneOrMore;

public final class BlockPredicate implements StructurePredicate {

    public static final MapCodec<BlockPredicate> CODEC = RecordCodecBuilder.mapCodec(instance -> instance
            .group(oneOrMore(ResourceLocation.CODEC).fieldOf("blocks").forGetter(BlockPredicate::blockIds))
            .apply(instance, BlockPredicate::new));

    private final Lazy<ObjectList<ResourceLocation>> blockIds;
    private final Lazy<ObjectList<Block>> blocks;

    public BlockPredicate(ObjectList<ResourceLocation> blockIds) {
        this(() -> blockIds);
    }

    public BlockPredicate(Supplier<ObjectList<ResourceLocation>> blockIds) {
        this.blockIds = Lazy.of(blockIds);
        this.blocks = Lazy.of(() -> blockIds().stream()
                .map(BlockPredicate::resolveBlock)
                .collect(ObjectImmutableList.toList()));
    }

    @Override
    public StructurePredicateType<?> type() {
        return StructurePredicateType.BLOCKS;
    }

    @Override
    public ObjectList<MultiblockBlockInfo> candidates() {
        return blocks().stream().map(MultiblockBlockInfo::new).collect(ObjectImmutableList.toList());
    }

    @Override
    public @Unmodifiable ObjectList<Block> blockCandidates() {
        return blocks();
    }

    @Override
    public boolean test(MultiblockState multiblockState, boolean mutateCount) {
        return blocks().contains(multiblockState.getBlockState().getBlock());
    }

    private static Block resolveBlock(ResourceLocation id) {
        Block block = BuiltInRegistries.BLOCK.get(id);
        if (block == null || !id.equals(BuiltInRegistries.BLOCK.getKey(block))) {
            throw new IllegalStateException("Unknown block id in structure block predicate: " + id);
        }
        return block;
    }

    @Override
    public boolean hasAir() {
        return blocks().contains(Blocks.AIR);
    }

    public ObjectList<ResourceLocation> blockIds() {
        return blockIds.get();
    }

    public ObjectList<Block> blocks() {
        return blocks.get();
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (BlockPredicate) obj;
        return Objects.equals(this.blockIds(), that.blockIds());
    }

    @Override
    public int hashCode() {
        return Objects.hash(blockIds());
    }

    @Override
    public String toString() {
        return "BlockPredicate[" +
                "blockIds=" + blockIds() + ']';
    }
}
