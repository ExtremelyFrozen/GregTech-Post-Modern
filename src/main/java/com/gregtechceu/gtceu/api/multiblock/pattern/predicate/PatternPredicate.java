package com.gregtechceu.gtceu.api.multiblock.pattern.predicate;

import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.multiblock.MultiblockBlockInfo;
import com.gregtechceu.gtceu.api.multiblock.MultiblockState;
import com.gregtechceu.gtceu.api.multiblock.structurepredicate.StructurePredicate;
import com.gregtechceu.gtceu.api.multiblock.util.RelativeDirection;

import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

public class PatternPredicate {

    public List<PredicateRule> common = new ObjectArrayList<>();
    public List<PredicateRule> limited = new ObjectArrayList<>();
    public List<StructurePredicate> structurePatternPredicates = new ObjectArrayList<>();
    public List<String> facts = new ObjectArrayList<>();
    public Function<MultiblockState, Direction> direction = o -> null;
    public Direction fixedDirection;
    public RelativeDirection relativeDirection;

    public PatternPredicate() {}

    public PatternPredicate(Predicate<MultiblockState> predicate, Supplier<MultiblockBlockInfo> blockInfo,
                            @Nullable Supplier<Block[]> candidates) {
        this();
        common.add(new PredicateRule(predicate, blockInfo, candidates));
    }

    public PatternPredicate(Predicate<MultiblockState> predicate, Supplier<MultiblockBlockInfo[]> candidates) {
        this(predicate, () -> {
            MultiblockBlockInfo[] infos = candidates.get();
            return infos.length == 0 ? MultiblockBlockInfo.EMPTY : infos[0];
        }, () -> Arrays.stream(candidates.get())
                .map(MultiblockBlockInfo::getBlockState)
                .map(BlockState::getBlock)
                .toArray(Block[]::new));
    }

    public PatternPredicate(PredicateRule simplePredicate) {
        this();
        if (simplePredicate.minCount != -1 || simplePredicate.maxCount != -1) {
            limited.add(simplePredicate);
        } else {
            common.add(simplePredicate);
        }
    }

    public PatternPredicate(StructurePredicate structurePredicate) {
        this();
        structurePatternPredicates.add(structurePredicate);
    }

    public PatternPredicate(PatternPredicate predicate) {
        common.addAll(predicate.common);
        limited.addAll(predicate.limited);
        structurePatternPredicates.addAll(predicate.structurePatternPredicates);
        facts.addAll(predicate.facts);
        this.direction = predicate.direction;
        this.fixedDirection = predicate.fixedDirection;
        this.relativeDirection = predicate.relativeDirection;
    }

    public PatternPredicate sort() {
        limited.sort(Comparator.comparingInt(a -> a.minCount));
        return this;
    }

    public PatternPredicate setDirection(Direction direction) {
        this.fixedDirection = direction;
        this.relativeDirection = null;
        this.direction = state -> direction;
        return this;
    }

    public PatternPredicate setRelativeDirection(RelativeDirection direction) {
        this.fixedDirection = null;
        this.relativeDirection = direction;
        return this;
    }

    public Direction getDirection(MultiblockState state, Direction frontFacing, Direction upwardsFacing,
                                  boolean isFlipped) {
        if (relativeDirection != null) {
            return relativeDirection.getRelative(frontFacing, upwardsFacing, isFlipped);
        }
        if (fixedDirection != null) {
            return fixedDirection;
        }
        return direction.apply(state);
    }

    public Direction getPreviewDirection() {
        if (relativeDirection != null) {
            return relativeDirection.global;
        }
        return fixedDirection;
    }

    /**
     * Add tooltips for candidates. They are shown in JEI Pages.
     */
    public PatternPredicate addTooltips(Component... tips) {
        if (tips.length > 0) {
            List<Component> tooltips = Arrays.stream(tips).toList();
            common.forEach(predicate -> {
                if (predicate.candidates == null) return;
                if (predicate.toolTips == null) {
                    predicate.toolTips = new ObjectArrayList<>();
                }
                predicate.toolTips.addAll(tooltips);
            });
            limited.forEach(predicate -> {
                if (predicate.candidates == null) return;
                if (predicate.toolTips == null) {
                    predicate.toolTips = new ObjectArrayList<>();
                }
                predicate.toolTips.addAll(tooltips);
            });
        }
        return this;
    }

    /**
     * Set the minimum number of candidate blocks.
     */
    public PatternPredicate setMinGlobalLimited(int min) {
        limited.addAll(common);
        common.clear();
        for (PredicateRule predicate : limited) {
            predicate.minCount = min;
        }
        return this;
    }

    public PatternPredicate setMinGlobalLimited(int min, int previewCount) {
        return this.setMinGlobalLimited(min).setPreviewCount(previewCount);
    }

    /**
     * Set the maximum number of candidate blocks.
     */
    public PatternPredicate setMaxGlobalLimited(int max) {
        limited.addAll(common);
        common.clear();
        for (PredicateRule predicate : limited) {
            predicate.maxCount = max;
        }
        return this;
    }

    public PatternPredicate setMaxGlobalLimited(int max, int previewCount) {
        return this.setMaxGlobalLimited(max).setPreviewCount(previewCount);
    }

    /**
     * Set the minimum number of candidate blocks for each aisle layer.
     */
    public PatternPredicate setMinLayerLimited(int min) {
        limited.addAll(common);
        common.clear();
        for (PredicateRule predicate : limited) {
            predicate.minLayerCount = min;
        }
        return this;
    }

    public PatternPredicate setMinLayerLimited(int min, int previewCount) {
        return this.setMinLayerLimited(min).setPreviewCount(previewCount);
    }

    /**
     * Set the maximum number of candidate blocks for each aisle layer.
     */
    public PatternPredicate setMaxLayerLimited(int max) {
        limited.addAll(common);
        common.clear();
        for (PredicateRule predicate : limited) {
            predicate.maxLayerCount = max;
        }
        return this;
    }

    public PatternPredicate setMaxLayerLimited(int max, int previewCount) {
        return this.setMaxLayerLimited(max).setPreviewCount(previewCount);
    }

    /**
     * Sets the Minimum and Maximum limit to the passed value
     *
     * @param limit The Maximum and Minimum limit
     */
    public PatternPredicate setExactLimit(int limit) {
        return this.setMinGlobalLimited(limit).setMaxGlobalLimited(limit);
    }

    /**
     * Set the number of it appears in JEI pages. It only affects JEI preview. (The specific number)
     */
    public PatternPredicate setPreviewCount(int count) {
        common.forEach(predicate -> predicate.previewCount = count);
        limited.forEach(predicate -> predicate.previewCount = count);
        return this;
    }

    /**
     * Set renderMask.
     */
    public PatternPredicate disableRenderFormed() {
        common.forEach(predicate -> predicate.disableRenderFormed = true);
        limited.forEach(predicate -> predicate.disableRenderFormed = true);
        return this;
    }

    /**
     * Set io.
     */
    public PatternPredicate setIO(IO io) {
        common.forEach(predicate -> predicate.io = io);
        limited.forEach(predicate -> predicate.io = io);
        return this;
    }

    public PatternPredicate setSlotName(String slotName) {
        common.forEach(predicate -> predicate.slotName = slotName);
        limited.forEach(predicate -> predicate.slotName = slotName);
        return this;
    }

    public boolean test(MultiblockState blockWorldState) {
        blockWorldState.io = IO.BOTH;
        boolean flag = false;
        for (PredicateRule predicate : limited) {
            if (predicate.testLimited(blockWorldState)) {
                flag = true;
            }
        }
        flag = flag || common.stream().anyMatch(predicate -> predicate.test(blockWorldState));
        flag = flag || structurePatternPredicates.stream().anyMatch(predicate -> predicate.test(blockWorldState, true));
        if (flag) {
            blockWorldState.setError(null);
            facts.forEach(blockWorldState.getFacts()::addFact);
        }
        return flag;
    }

    public PatternPredicate withFacts(List<String> additionalFacts) {
        facts.addAll(additionalFacts);
        return this;
    }

    public PatternPredicate or(PatternPredicate other) {
        if (other != null) {
            PatternPredicate newPredicate = new PatternPredicate(this);
            newPredicate.common.addAll(other.common);
            newPredicate.limited.addAll(other.limited);
            newPredicate.structurePatternPredicates.addAll(other.structurePatternPredicates);
            return newPredicate;
        }
        return this;
    }

    public boolean isAny() {
        return this.common.size() == 1 && this.limited.isEmpty() && this.structurePatternPredicates.isEmpty() &&
                this.common.getFirst() == PredicateRule.ANY ||
                this.common.isEmpty() && this.limited.isEmpty() && this.structurePatternPredicates.size() == 1 &&
                        this.structurePatternPredicates.getFirst().isAny();
    }

    public boolean addCache() {
        return common.stream().anyMatch(PredicateRule::addCache) ||
                limited.stream().anyMatch(PredicateRule::addCache) ||
                structurePatternPredicates.stream().anyMatch(StructurePredicate::addCache);
    }

    public boolean isAir() {
        return this.common.size() == 1 && this.limited.isEmpty() && this.structurePatternPredicates.isEmpty() &&
                this.common.getFirst() == PredicateRule.AIR ||
                this.common.isEmpty() && this.limited.isEmpty() && this.structurePatternPredicates.size() == 1 &&
                        this.structurePatternPredicates.getFirst().isAir();
    }

    public boolean isSingle() {
        return !isAny() && !isAir() &&
                this.common.size() + this.limited.size() + this.structurePatternPredicates.size() == 1;
    }

    public boolean hasAir() {
        return this.common.contains(PredicateRule.AIR) ||
                this.structurePatternPredicates.stream().anyMatch(StructurePredicate::hasAir);
    }
}
