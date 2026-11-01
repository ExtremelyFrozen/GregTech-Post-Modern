package com.gregtechceu.gtceu.api.multiblock.structurepredicate;

import com.gregtechceu.gtceu.api.multiblock.MultiblockBlockInfo;
import com.gregtechceu.gtceu.api.multiblock.MultiblockState;

import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.common.util.Lazy;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.ObjectCollection;
import it.unimi.dsi.fastutil.objects.ObjectImmutableList;
import it.unimi.dsi.fastutil.objects.ObjectList;
import org.jetbrains.annotations.Unmodifiable;
import org.jspecify.annotations.Nullable;

import java.util.Objects;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static com.gregtechceu.gtceu.api.multiblock.structurepredicate.Util.oneOrMore;

public final class BlockTagPredicate implements StructurePredicate {

    public static final MapCodec<BlockTagPredicate> CODEC = RecordCodecBuilder.mapCodec(instance -> instance
            .group(oneOrMore(TagKey.codec(Registries.BLOCK)).fieldOf("blockTagKeys")
                    .forGetter(BlockTagPredicate::blockTagKeys))
            .apply(instance, BlockTagPredicate::new));

    private final ObjectList<TagKey<Block>> blockTagKeys;
    private final Lazy<ObjectList<Block>> candidates;

    public BlockTagPredicate(ObjectList<TagKey<Block>> blockTagKeys) {
        this.blockTagKeys = blockTagKeys;
        this.candidates = Lazy.of(() -> unwrapTags(BuiltInRegistries.BLOCK, blockTagKeys)
                .collect(ObjectImmutableList.toList()));
    }

    @Override
    public StructurePredicateType<?> type() {
        return StructurePredicateType.BLOCK_TAGS;
    }

    @Override
    public ObjectList<MultiblockBlockInfo> candidates() {
        return candidates.get().stream().map(MultiblockBlockInfo::new).collect(ObjectImmutableList.toList());
    }

    @Override
    public @Unmodifiable ObjectList<Block> blockCandidates() {
        return candidates.get();
    }

    @Override
    public boolean test(MultiblockState multiblockState, boolean mutateCount) {
        return candidates.get().contains(multiblockState.getBlockState().getBlock());
    }

    @Override
    public boolean hasAir() {
        return candidates.get().contains(Blocks.AIR);
    }

    // FIXME: find a better way or move to utils
    static <T> Stream<T> unwrapTags(Registry<T> registry, ObjectCollection<TagKey<T>> tagKeys) {
        return tagKeys.stream()
                .flatMap(key -> StreamSupport.stream(registry.getTagOrEmpty(key).spliterator(), false)
                        .map(Holder::value));
    }

    public ObjectList<TagKey<Block>> blockTagKeys() {
        return blockTagKeys;
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (BlockTagPredicate) obj;
        return Objects.equals(this.blockTagKeys, that.blockTagKeys);
    }

    @Override
    public int hashCode() {
        return Objects.hash(blockTagKeys);
    }

    @Override
    public String toString() {
        return "BlockTagPredicate[" +
                "blockTagKeys=" + blockTagKeys + ']';
    }
}
