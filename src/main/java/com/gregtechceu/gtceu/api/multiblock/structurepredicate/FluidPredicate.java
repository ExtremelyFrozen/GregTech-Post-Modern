package com.gregtechceu.gtceu.api.multiblock.structurepredicate;

import com.gregtechceu.gtceu.api.multiblock.MultiblockBlockInfo;
import com.gregtechceu.gtceu.api.multiblock.MultiblockState;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.ObjectImmutableList;
import it.unimi.dsi.fastutil.objects.ObjectList;
import org.jetbrains.annotations.Unmodifiable;

import static com.gregtechceu.gtceu.api.multiblock.structurepredicate.Util.oneOrMore;

public record FluidPredicate(ObjectList<Fluid> fluids) implements StructurePredicate {

    public static final MapCodec<FluidPredicate> CODEC = RecordCodecBuilder.mapCodec(instance -> instance
            .group(oneOrMore(BuiltInRegistries.FLUID.byNameCodec()).fieldOf("fluids").forGetter(FluidPredicate::fluids))
            .apply(instance, FluidPredicate::new));

    @Override
    public StructurePredicateType<?> type() {
        return StructurePredicateType.FLUIDS;
    }

    @Override
    public ObjectList<MultiblockBlockInfo> candidates() {
        return fluids.stream()
                .map(FluidPredicate::blockInfoFromFluid)
                .collect(ObjectImmutableList.toList());
    }

    @Override
    public @Unmodifiable ObjectList<Block> blockCandidates() {
        return fluids.stream()
                .map(FluidPredicate::blockStateFromFluid)
                .map(BlockState::getBlock)
                .distinct()
                .collect(ObjectImmutableList.toList());
    }

    @Override
    public boolean test(MultiblockState multiblockState, boolean mutateCount) {
        return fluids.contains(multiblockState.getBlockState().getFluidState().getType());
    }

    static BlockState blockStateFromFluid(Fluid fluid) {
        return fluid.defaultFluidState().createLegacyBlock();
    }

    static MultiblockBlockInfo blockInfoFromFluid(Fluid fluid) {
        return new MultiblockBlockInfo(blockStateFromFluid(fluid));
    }
}
