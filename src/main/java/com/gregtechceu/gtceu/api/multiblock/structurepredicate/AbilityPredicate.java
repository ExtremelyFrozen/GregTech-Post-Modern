package com.gregtechceu.gtceu.api.multiblock.structurepredicate;

import com.gregtechceu.gtceu.api.block.MetaMachineBlock;
import com.gregtechceu.gtceu.api.machine.multiblock.PartAbility;
import com.gregtechceu.gtceu.api.multiblock.MultiblockBlockInfo;
import com.gregtechceu.gtceu.api.multiblock.MultiblockState;
import com.gregtechceu.gtceu.api.multiblock.pattern.predicate.PatternObservation;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.jetbrains.annotations.Unmodifiable;

import java.util.Collection;
import java.util.List;

import static com.gregtechceu.gtceu.api.multiblock.structurepredicate.Util.oneOrMore;

public record AbilityPredicate(List<PartAbility> abilities) implements StructurePredicate {

    @Override
    public PatternObservation observeCandidate(BlockState state) {
        var facts = new ObjectArrayList<String>();
        if (state.getBlock() instanceof MetaMachineBlock) {
            for (PartAbility ability : abilities) {
                if (ability.isApplicable(state.getBlock())) facts.add("part." + ability.getName());
            }
        }
        boolean matched = !facts.isEmpty();
        if (matched) facts.add("part");
        return PatternObservation.of(matched, facts);
    }

    private static final Codec<PartAbility> ABILITY_CODEC = Codec.STRING.comapFlatMap(
            name -> PartAbility.byName(name)
                    .map(DataResult::success)
                    .orElseGet(() -> DataResult.error(() -> "Unknown part ability: " + name)),
            PartAbility::getName);

    public static final MapCodec<AbilityPredicate> CODEC = RecordCodecBuilder.mapCodec(instance -> instance
            .group(oneOrMore(ABILITY_CODEC).fieldOf("abilities").forGetter(AbilityPredicate::abilities))
            .apply(instance, AbilityPredicate::new));

    public AbilityPredicate {
        abilities = List.copyOf(abilities);
    }

    @Override
    public StructurePredicateType<?> type() {
        return StructurePredicateType.ABILITIES;
    }

    @Override
    public @Unmodifiable List<MultiblockBlockInfo> candidates() {
        return blockCandidates().stream().map(MultiblockBlockInfo::new).toList();
    }

    @Override
    public @Unmodifiable List<Block> blockCandidates() {
        return abilities.stream()
                .map(PartAbility::getAllBlocks)
                .flatMap(Collection::stream)
                .toList();
    }

    @Override
    public boolean test(MultiblockState multiblockState, boolean mutateCount) {
        Block block = multiblockState.getBlockState().getBlock();
        return abilities.stream().anyMatch(ability -> ability.isApplicable(block));
    }
}
