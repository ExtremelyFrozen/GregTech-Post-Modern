package com.gregtechceu.gtceu.api.multiblock.structurepredicate;

import com.gregtechceu.gtceu.api.machine.MultiblockMachineDefinition;
import com.gregtechceu.gtceu.api.multiblock.MultiblockBlockInfo;
import com.gregtechceu.gtceu.api.multiblock.MultiblockState;
import com.gregtechceu.gtceu.api.multiblock.pattern.predicate.PatternObservation;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectList;
import it.unimi.dsi.fastutil.objects.ObjectLists;
import org.jetbrains.annotations.Unmodifiable;

import java.util.*;

public interface StructurePredicate {

    /**
     * Observes a representative state without a world or block entity. Implementations may add their own standard
     * facts and properties; a failed observation must carry neither. Candidate facts describe only the successful
     * branch and must agree with runtime semantics. This method must not change counters, caches, worlds or shared
     * matching context, and must support concurrent read-only calls after registration. The default accepts the
     * declared block candidates without adding facts; state-sensitive and semantic predicates override this contract.
     */
    default PatternObservation observeCandidate(BlockState candidateState) {
        return PatternObservation.of(blockCandidates().contains(candidateState.getBlock()));
    }

    /**
     * Supplies representative full states without creating preview block entities. State-sensitive predicates
     * override this method together with observeCandidate; compound predicates delegate to their children.
     * Returned snapshots must support concurrent read-only access and must not expose mutable registry storage.
     */
    default ObjectList<BlockState> candidateStates() {
        var states = new ObjectArrayList<BlockState>();
        for (Block block : blockCandidates()) states.add(block.defaultBlockState());
        return ObjectLists.unmodifiable(states);
    }

    Codec<StructurePredicate> CODEC = typeCodec()
            .dispatch("type", StructurePredicate::type, StructurePredicateType::codec);

    StructurePredicateType<?> type();

    private static Codec<StructurePredicateType<?>> typeCodec() {
        return ResourceLocation.CODEC.comapFlatMap(
                id -> {
                    StructurePredicateType<?> type = StructurePredicateType.byId(id);
                    return type == null ?
                            DataResult.error(() -> "Unknown structure predicate type: " + id) :
                            DataResult.success(type);
                },
                type -> {
                    ResourceLocation id = StructurePredicateType.id(type);
                    if (id == null) {
                        throw new IllegalStateException("Unregistered structure predicate type");
                    }
                    return id;
                });
    }

    @Unmodifiable
    default List<MultiblockBlockInfo> candidates() {
        return List.of();
    }

    @Unmodifiable
    default List<Block> blockCandidates() {
        return List.of();
    }

    /**
     * Expands this predicate into definition-aware choices used to build a representative preview structure.
     * Composite predicates override this method so nested count restrictions remain attached to their candidates.
     *
     * @param definition machine definition owning the pattern being previewed
     * @return ordered preview choices, or an empty list when this predicate cannot provide a representative block
     */
    @Unmodifiable
    default List<StructurePreviewChoice> previewChoices(MultiblockMachineDefinition definition) {
        var previewCandidates = new ObjectArrayList<MultiblockBlockInfo>();
        for (BlockState state : candidateStates()) previewCandidates.add(new MultiblockBlockInfo(state));
        if (!previewCandidates.isEmpty()) {
            return ObjectLists.singleton(
                    StructurePreviewChoice.unrestricted(previewCandidates).withObserver(this::observeCandidate));
        }
        if (isAny() || isAir()) {
            return ObjectLists
                    .singleton(StructurePreviewChoice.unrestricted(ObjectLists.singleton(MultiblockBlockInfo.EMPTY))
                            .withObserver(this::observeCandidate));
        }
        return ObjectLists.emptyList();
    }

    boolean test(MultiblockState multiblockState, boolean mutateCount);

    default ConcatenatedPredicate or(StructurePredicate other) {
        return ConcatenatedPredicate.concat(this, other);
    }

    default boolean isAny() {
        return this instanceof AnyPredicate;
    }

    default boolean isAir() {
        return this instanceof AirPredicate;
    }

    default boolean hasAir() {
        return false;
    }

    default boolean addCache() {
        return !isAny();
    }
}
