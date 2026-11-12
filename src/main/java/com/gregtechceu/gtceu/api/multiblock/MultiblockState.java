package com.gregtechceu.gtceu.api.multiblock;

import com.gregtechceu.gtceu.GTCEu;
import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.machine.multiblock.MultiblockControllerMachine;
import com.gregtechceu.gtceu.api.multiblock.error.PatternError;
import com.gregtechceu.gtceu.api.multiblock.error.PatternStringError;
import com.gregtechceu.gtceu.api.multiblock.index.ChunkStructureAccess;
import com.gregtechceu.gtceu.api.multiblock.pattern.match.PatternMatchCache;
import com.gregtechceu.gtceu.api.multiblock.pattern.match.PatternMatchResult;
import com.gregtechceu.gtceu.api.multiblock.pattern.model.PatternFacts;
import com.gregtechceu.gtceu.api.multiblock.structurepredicate.PatternPredicate;
import com.gregtechceu.gtceu.api.multiblock.structurepredicate.PredicateRule;
import com.gregtechceu.gtceu.api.multiblock.structurepredicate.StructurePredicate;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;

import it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
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
    @Getter
    private PatternMatchResult matchResult;
    @Getter
    private PatternMatchCache matchCache;
    private Long2LongOpenHashMap readRevisions = new Long2LongOpenHashMap();
    private Long2ObjectOpenHashMap<LevelChunk> readChunks = new Long2ObjectOpenHashMap<>();
    private Long2ObjectOpenHashMap<LongOpenHashSet> readPositions = new Long2ObjectOpenHashMap<>();
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
        readRevisions = source.readRevisions;
        readChunks = source.readChunks;
        readPositions = source.readPositions;
        setError(source.error);
    }

    public void setMatchResult(PatternMatchResult result) {
        matchResult = result;
    }

    public void setMatchCache(PatternMatchCache cache) {
        matchCache = cache;
    }

    public MultiblockState captureMatch() {
        MultiblockState captured = new MultiblockState(world, controllerPos, structureName);
        captured.lastController = lastController;
        captured.applyMatch(this);
        return captured;
    }

    public void inheritReadRevisions(MultiblockState source) {
        readRevisions = source.readRevisions;
        readChunks = source.readChunks;
        readPositions = source.readPositions;
    }

    public void beginReadTracking() {
        readRevisions = new Long2LongOpenHashMap();
        readChunks = new Long2ObjectOpenHashMap<>();
        readPositions = new Long2ObjectOpenHashMap<>();
    }

    public void applyCachedMatch(MultiblockState source) {
        Long2LongOpenHashMap revisions = readRevisions;
        Long2ObjectOpenHashMap<LevelChunk> chunks = readChunks;
        Long2ObjectOpenHashMap<LongOpenHashSet> positions = readPositions;
        applyMatch(source);
        readRevisions = revisions;
        readChunks = chunks;
        readPositions = positions;
    }

    public void recordRead(BlockPos position) {
        int chunkX = position.getX() >> 4;
        int chunkZ = position.getZ() >> 4;
        long key = ChunkPos.asLong(chunkX, chunkZ);
        readPositions.computeIfAbsent(key, ignored -> new LongOpenHashSet()).add(position.asLong());
        if (readRevisions.containsKey(key)) return;
        var chunk = world.getChunkSource().getChunkNow(chunkX, chunkZ);
        if (chunk instanceof ChunkStructureAccess access) {
            readChunks.put(key, chunk);
            readRevisions.put(key, access.gtceu$getStructureRevision());
        }
    }

    public boolean isMatchCurrent() {
        for (var entry : readRevisions.long2LongEntrySet()) {
            var chunk = world.getChunkSource().getChunkNow(ChunkPos.getX(entry.getLongKey()),
                    ChunkPos.getZ(entry.getLongKey()));
            if (!(chunk instanceof ChunkStructureAccess access) || chunk != readChunks.get(entry.getLongKey()) ||
                    access.gtceu$hasStructureChangesSince(entry.getLongValue(), readPositions.get(entry.getLongKey())))
                return false;
        }
        return true;
    }

    public boolean update(BlockPos posIn, PatternPredicate predicate) {
        this.pos = posIn;
        this.blockState = null;
        this.blockEntity = null;
        this.blockEntityInitialized = false;
        this.predicate = predicate;
        this.error = null;
        if (!world.isLoaded(posIn)) {
            error = UNLOAD_ERROR;
            return false;
        }
        recordRead(posIn);
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
                data.removeAsyncLogic(lastController);
                return;
            }
            MultiblockControllerMachine controller = getController();
            if (controller == null) return;
            MultiblockWorldSavedData.getOrCreate(serverLevel).requestPatternCheck(this, changedPositions);
        }
    }
}
