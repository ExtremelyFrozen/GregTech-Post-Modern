package com.gregtechceu.gtceu.api.multiblock.structurepredicate;

import com.gregtechceu.gtceu.api.block.MetaMachineBlock;
import com.gregtechceu.gtceu.api.machine.MetaMachine;
import com.gregtechceu.gtceu.api.machine.multiblock.PartAbility;
import com.gregtechceu.gtceu.api.multiblock.MultiblockBlockInfo;
import com.gregtechceu.gtceu.api.multiblock.MultiblockState;
import com.gregtechceu.gtceu.common.machine.multiblock.part.RotorHolderPartMachine;

import net.minecraft.world.level.block.Block;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.ObjectImmutableList;
import it.unimi.dsi.fastutil.objects.ObjectList;
import org.jetbrains.annotations.Unmodifiable;

/**
 * Matches turbine rotor holders and checks the rotor front clearance.
 */
public record RotorHolderPredicate(int tier) implements StructurePredicate {

    public static final MapCodec<RotorHolderPredicate> CODEC = RecordCodecBuilder.mapCodec(instance -> instance
            .group(Codec.INT.fieldOf("tier").forGetter(RotorHolderPredicate::tier))
            .apply(instance, RotorHolderPredicate::new));

    @Override
    public StructurePredicateType<?> type() {
        return StructurePredicateType.ROTOR_HOLDER;
    }

    @Override
    public boolean test(MultiblockState multiblockState, boolean mutateCount) {
        return MetaMachine.getMachine(multiblockState.getWorld(),
                multiblockState.getPos()) instanceof RotorHolderPartMachine rotorHolder &&
                rotorHolder.getTier() == tier &&
                multiblockState.getWorld()
                        .getBlockState(multiblockState.getPos().relative(rotorHolder.self().getFrontFacing()))
                        .isAir();
    }

    @Override
    public @Unmodifiable ObjectList<MultiblockBlockInfo> candidates() {
        return blockCandidates().stream().map(MultiblockBlockInfo::fromBlock).collect(ObjectImmutableList.toList());
    }

    @Override
    public @Unmodifiable ObjectList<Block> blockCandidates() {
        return PartAbility.ROTOR_HOLDER.getAllBlocks().stream()
                .filter(block -> block instanceof MetaMachineBlock machineBlock &&
                        machineBlock.getDefinition().getTier() == tier)
                .collect(ObjectImmutableList.toList());
    }
}
