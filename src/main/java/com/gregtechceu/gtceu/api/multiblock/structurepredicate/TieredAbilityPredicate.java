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
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.ints.IntLists;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.jetbrains.annotations.Unmodifiable;

import java.util.Arrays;
import java.util.List;

import static com.gregtechceu.gtceu.api.multiblock.structurepredicate.Util.oneOrMore;

/**
 * Matches blocks for one {@link PartAbility} constrained to explicit voltage tiers.
 *
 * <p>
 * JSON structure predicates need this when an old Java pattern used
 * {@code PatternPredicates.ability(ability, tiers...)} instead of all registered ability blocks.
 */
public record TieredAbilityPredicate(PartAbility ability, IntList tiers) implements StructurePredicate {

    /** Codec collection boundary; runtime storage and access remain primitive. */
    public TieredAbilityPredicate(PartAbility ability, List<Integer> tiers) {
        this(ability, new IntArrayList(tiers));
    }

    @Override
    public PatternObservation observeCandidate(BlockState state) {
        boolean matched = state.getBlock() instanceof MetaMachineBlock machine &&
                tiers.contains(machine.getDefinition().getTier()) && ability.isApplicable(machine);
        var facts = new ObjectArrayList<String>();
        if (matched) {
            facts.add("part");
            facts.add("part." + ability.getName());
        }
        return PatternObservation.of(matched, facts);
    }

    private static final Codec<PartAbility> ABILITY_CODEC = Codec.STRING.comapFlatMap(
            name -> PartAbility.byName(name)
                    .map(DataResult::success)
                    .orElseGet(() -> DataResult.error(() -> "Unknown part ability: " + name)),
            PartAbility::getName);

    public static final MapCodec<TieredAbilityPredicate> CODEC = RecordCodecBuilder.mapCodec(instance -> instance
            .group(
                    ABILITY_CODEC.fieldOf("ability").forGetter(TieredAbilityPredicate::ability),
                    oneOrMore(Codec.INT).fieldOf("tiers").forGetter(TieredAbilityPredicate::tiers))
            .apply(instance, TieredAbilityPredicate::new));

    public TieredAbilityPredicate {
        tiers = IntLists.unmodifiable(new IntArrayList(tiers));
        if (tiers.isEmpty()) {
            throw new IllegalArgumentException("Tiered ability predicate requires at least one tier");
        }
    }

    @Override
    public StructurePredicateType<?> type() {
        return StructurePredicateType.TIERED_ABILITY;
    }

    @Override
    public @Unmodifiable List<MultiblockBlockInfo> candidates() {
        return blockCandidates().stream().map(MultiblockBlockInfo::new).toList();
    }

    @Override
    public @Unmodifiable List<Block> blockCandidates() {
        int[] tierArray = tiers.toIntArray();
        return List.copyOf(ability.getBlocks(tierArray));
    }

    @Override
    public boolean test(MultiblockState multiblockState, boolean mutateCount) {
        return blockCandidates().contains(multiblockState.getBlockState().getBlock());
    }

    @Override
    public String toString() {
        return "TieredAbilityPredicate[ability=" + ability.getName() + ", tiers=" + Arrays.toString(
                tiers.toIntArray()) + ']';
    }
}
