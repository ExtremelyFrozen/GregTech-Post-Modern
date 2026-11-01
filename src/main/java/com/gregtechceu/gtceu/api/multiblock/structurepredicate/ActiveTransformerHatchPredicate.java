package com.gregtechceu.gtceu.api.multiblock.structurepredicate;

import com.gregtechceu.gtceu.api.machine.MultiblockMachineDefinition;
import com.gregtechceu.gtceu.api.machine.multiblock.PartAbility;
import com.gregtechceu.gtceu.api.multiblock.MultiblockBlockInfo;
import com.gregtechceu.gtceu.api.multiblock.MultiblockState;

import net.minecraft.world.level.block.Block;

import com.mojang.serialization.MapCodec;
import it.unimi.dsi.fastutil.objects.ObjectImmutableList;
import it.unimi.dsi.fastutil.objects.ObjectList;
import org.jetbrains.annotations.Unmodifiable;

import java.util.stream.Stream;

/**
 * Matches all hatch abilities accepted by the active transformer structure.
 */
public enum ActiveTransformerHatchPredicate implements StructurePredicate {

    INSTANCE;

    public static final MapCodec<ActiveTransformerHatchPredicate> CODEC = MapCodec.unit(INSTANCE);

    private static final ObjectList<PartAbility> INPUT_ABILITIES = ObjectList.of(
            PartAbility.INPUT_ENERGY,
            PartAbility.SUBSTATION_INPUT_ENERGY,
            PartAbility.INPUT_LASER);
    private static final ObjectList<PartAbility> OUTPUT_ABILITIES = ObjectList.of(
            PartAbility.OUTPUT_ENERGY,
            PartAbility.SUBSTATION_OUTPUT_ENERGY,
            PartAbility.OUTPUT_LASER);
    private static final ObjectList<PartAbility> ABILITIES = Stream
            .concat(INPUT_ABILITIES.stream(), OUTPUT_ABILITIES.stream())
            .collect(ObjectImmutableList.toList());
    private static final ObjectList<StructurePredicate> PREVIEW_PREDICATES = ObjectList.of(
            previewPredicate(INPUT_ABILITIES),
            previewPredicate(OUTPUT_ABILITIES));

    @Override
    public StructurePredicateType<?> type() {
        return StructurePredicateType.ACTIVE_TRANSFORMER_HATCHES;
    }

    @Override
    public boolean test(MultiblockState multiblockState, boolean mutateCount) {
        Block block = multiblockState.getBlockState().getBlock();
        return ABILITIES.stream().anyMatch(ability -> ability.isApplicable(block));
    }

    @Override
    public @Unmodifiable ObjectList<MultiblockBlockInfo> candidates() {
        return blockCandidates().stream().map(MultiblockBlockInfo::new).collect(ObjectImmutableList.toList());
    }

    @Override
    public @Unmodifiable ObjectList<Block> blockCandidates() {
        return ABILITIES.stream()
                .flatMap(ability -> ability.getAllBlocks().stream())
                .collect(ObjectImmutableList.toList());
    }

    @Override
    public @Unmodifiable ObjectList<StructurePreviewChoice> previewChoices(MultiblockMachineDefinition definition) {
        return PREVIEW_PREDICATES.stream()
                .flatMap(predicate -> predicate.previewChoices(definition).stream())
                .collect(ObjectImmutableList.toList());
    }

    private static RestrictedPredicate previewPredicate(ObjectList<PartAbility> abilities) {
        return RestrictedPredicate.builder()
                .base(new AbilityPredicate(abilities))
                .previewCount(1)
                .build();
    }
}
