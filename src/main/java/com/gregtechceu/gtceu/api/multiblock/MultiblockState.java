package com.gregtechceu.gtceu.api.multiblock;

import com.gregtechceu.gtceu.GTCEu;
import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.machine.multiblock.MultiblockControllerMachine;
import com.gregtechceu.gtceu.api.multiblock.error.PatternError;
import com.gregtechceu.gtceu.api.multiblock.error.PatternStringError;
import com.gregtechceu.gtceu.api.multiblock.index.StructureReadTracker;
import com.gregtechceu.gtceu.api.multiblock.pattern.match.PatternMatchCache;
import com.gregtechceu.gtceu.api.multiblock.pattern.match.PatternMatchResult;
import com.gregtechceu.gtceu.api.multiblock.pattern.model.PatternFacts;
import com.gregtechceu.gtceu.api.multiblock.structurepredicate.PatternPredicate;
import com.gregtechceu.gtceu.api.multiblock.structurepredicate.PredicateRule;
import com.gregtechceu.gtceu.api.multiblock.structurepredicate.StructurePredicate;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import it.unimi.dsi.fastutil.longs.LongConsumer;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import lombok.Getter;
import lombok.Setter;
import org.jspecify.annotations.Nullable;

import java.util.Collection;
import java.util.stream.Collectors;

public class MultiblockState {

    public final static PatternError UNLOAD_ERROR = new PatternStringError("multiblocked.pattern.error.chunk");
    public final static PatternError UNINIT_ERROR = new PatternStringError("multiblocked.pattern.error.init");

    private BlockPos pos;
    private BlockState blockState;
    private BlockEntity blockEntity;
    private boolean blockEntityInitialized;
    @Getter
    private PatternFacts facts;
    @Setter
    @Getter
    private PatternMatchResult matchResult;
    @Setter
    @Getter
    private PatternMatchCache matchCache;
    private StructureReadTracker readTracker;
    @Getter
    private long changeVersion;
    private boolean ownsReadTracker;
    @Getter
    private Object2IntOpenHashMap<PredicateRule> globalCount;
    @Getter
    private Object2IntOpenHashMap<PredicateRule> layerCount;
    @Getter
    private Object2IntOpenHashMap<StructurePredicate> structureGlobalCount;
    @Getter
    private Object2IntOpenHashMap<StructurePredicate> structureLayerCount;
    public PatternPredicate predicate;
    public IO io;
    public PatternError error;
    @Getter
    @Setter
    private boolean neededFlip = false;
    @Getter
    public final Level world;
    public final BlockPos controllerPos;
    @Getter
    public final String structureName;
    public MultiblockControllerMachine lastController;

    // persist
    public LongOpenHashSet cache;

    public MultiblockState(Level world, BlockPos controllerPos, String structureName) {
        this.world = world;
        this.controllerPos = controllerPos;
        this.structureName = structureName;
        this.error = UNINIT_ERROR;
        this.facts = new PatternFacts();
        this.readTracker = new StructureReadTracker(world);
        this.ownsReadTracker = true;
    }

    public void clean() {
        this.facts = new PatternFacts();
        this.globalCount = new Object2IntOpenHashMap<>();
        this.layerCount = new Object2IntOpenHashMap<>();
        this.structureGlobalCount = new Object2IntOpenHashMap<>();
        this.structureLayerCount = new Object2IntOpenHashMap<>();
        cache = new LongOpenHashSet();
    }

    /** Transfers a completed attempt. The source is thereafter frozen and never reused as scratch state. */
    public void applyMatch(MultiblockState source) {
        facts = source.facts;
        matchResult = source.matchResult;
        matchCache = source.matchCache;
        globalCount = source.globalCount;
        layerCount = source.layerCount;
        structureGlobalCount = source.structureGlobalCount;
        structureLayerCount = source.structureLayerCount;
        cache = source.cache;
        neededFlip = source.neededFlip;
        pos = source.pos;
        predicate = source.predicate;
        blockState = source.blockState;
        blockEntity = source.blockEntity;
        blockEntityInitialized = source.blockEntityInitialized;
        readTracker = source.readTracker;
        ownsReadTracker = false;
        setError(source.error);
    }

    public MultiblockState captureMatch() {
        MultiblockState captured = new MultiblockState(world, controllerPos, structureName);
        captured.lastController = lastController;
        captured.applyMatch(this);
        return captured;
    }

    public void inheritReadTracking(MultiblockState source) {
        readTracker = source.readTracker;
        ownsReadTracker = false;
    }

    public void beginReadTracking() {
        if (ownsReadTracker) readTracker.close();
        readTracker = new StructureReadTracker(world);
        ownsReadTracker = true;
    }

    /** Visits the read frontier without adding rejected candidates to the confirmed structure's geometry. */
    public void forEachReadPosition(LongConsumer consumer) {
        readTracker.forEach(consumer);
    }

    public boolean hasReadPosition(long position) {
        return readTracker.contains(position);
    }

    public void applyCachedMatch(MultiblockState source) {
        StructureReadTracker tracker = readTracker;
        boolean owns = ownsReadTracker;
        applyMatch(source);
        readTracker = tracker;
        ownsReadTracker = owns;
    }

    public void recordRead(BlockPos position) {
        readTracker.read(position);
    }

    public void recordChange() {
        changeVersion++;
    }

    public boolean isMatchCurrent() {
        return readTracker.isCurrent();
    }

    public void closeReadTracking() {
        readTracker.close();
    }

    public boolean update(BlockPos posIn, PatternPredicate predicate) {
        this.pos = posIn;
        this.blockState = null;
        this.blockEntity = null;
        this.blockEntityInitialized = false;
        this.predicate = predicate;
        this.error = null;
        recordRead(posIn);
        if (!world.isLoaded(posIn)) {
            error = UNLOAD_ERROR;
            return false;
        }
        return true;
    }

    public boolean updateCached(BlockPos position, PatternPredicate predicate, BlockState block,
                                @Nullable BlockEntity entity) {
        if (!update(position, predicate)) return false;
        blockState = block;
        blockEntity = entity;
        blockEntityInitialized = true;
        return true;
    }

    public MultiblockControllerMachine getController() {
        if (world.isLoaded(controllerPos)) {
            if (world.getBlockEntity(controllerPos) instanceof MultiblockControllerMachine controller) {
                return lastController = controller;
            }
        } else {
            error = UNLOAD_ERROR;
        }
        return null;
    }

    public boolean hasError() {
        return error != null;
    }

    public void setError(PatternError error) {
        this.error = error;
        if (error != null) {
            error.setWorldState(this);
        }
    }

    public BlockState getBlockState() {
        if (this.blockState == null) {
            this.blockState = this.world.getBlockState(this.pos);
        }
        if (this.blockState == null) {
            GTCEu.LOGGER.error("could not get BlockState at " + this.pos + " in MultiblockState");
        }
        return this.blockState;
    }

    @Nullable
    public BlockEntity getBlockEntity() {
        if (!getBlockState().hasBlockEntity()) {
            return null;
        }
        if (this.blockEntity == null && !this.blockEntityInitialized) {
            this.blockEntity = this.world.getBlockEntity(this.pos);
            this.blockEntityInitialized = true;
        }

        return this.blockEntity;
    }

    public BlockPos getPos() {
        return this.pos.immutable();
    }

    public BlockState getOffsetState(Direction face) {
        if (pos instanceof BlockPos.MutableBlockPos) {
            ((BlockPos.MutableBlockPos) pos).move(face);
            BlockState blockState = world.getBlockState(pos);
            ((BlockPos.MutableBlockPos) pos).move(face.getOpposite());
            return blockState;
        }
        return world.getBlockState(this.pos.relative(face));
    }

    public void addPosCache(BlockPos pos) {
        cache.add(pos.asLong());
    }

    public boolean isPosInCache(BlockPos pos) {
        return cache.contains(pos.asLong());
    }

    public Collection<BlockPos> getCache() {
        return cache.longStream().mapToObj(BlockPos::of).collect(Collectors.toSet());
    }

    public void onBlockStateChanged(BlockPos pos, BlockState state) {
        LongOpenHashSet changedPositions = new LongOpenHashSet(1);
        changedPositions.add(pos.asLong());
        onBlockStateChanged(changedPositions);
    }

    public void onBlockStateChanged(LongOpenHashSet changedPositions) {
        if (world instanceof ServerLevel serverLevel) {
            if (lastController != null && changedPositions.contains(controllerPos.asLong()) &&
                    world.getBlockEntity(controllerPos) != lastController) {
                lastController.invalidateAllStructures();
                var data = MultiblockWorldSavedData.getOrCreate(serverLevel);
                data.unregisterController(lastController);
                return;
            }
            MultiblockControllerMachine controller = getController();
            if (controller == null) return;
            MultiblockWorldSavedData.getOrCreate(serverLevel).requestPatternCheck(this, changedPositions);
        }
    }
}
