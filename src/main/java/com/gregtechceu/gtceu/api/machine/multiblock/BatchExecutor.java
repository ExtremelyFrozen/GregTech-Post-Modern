package com.gregtechceu.gtceu.api.machine.multiblock;

import com.gregtechceu.gtceu.GTCEu;
import com.gregtechceu.gtceu.api.machine.MetaMachine;
import com.gregtechceu.gtceu.api.multiblock.MultiblockState;
import com.gregtechceu.gtceu.api.multiblock.autobuild.BuildProblem;
import com.gregtechceu.gtceu.api.multiblock.autobuild.MaterialKey;
import com.gregtechceu.gtceu.api.multiblock.autobuild.MaterialRequest;
import com.gregtechceu.gtceu.api.multiblock.autobuild.MaterialSource;
import com.gregtechceu.gtceu.api.multiblock.autobuild.MaterialSource.Reservation;
import com.gregtechceu.gtceu.api.multiblock.autobuild.MaterialSource.Session;
import com.gregtechceu.gtceu.api.multiblock.autobuild.MaterialSources;
import com.gregtechceu.gtceu.api.multiblock.autobuild.batch.BatchRequest;
import com.gregtechceu.gtceu.api.multiblock.autobuild.batch.BatchResult;
import com.gregtechceu.gtceu.api.multiblock.autobuild.batch.StructureOperation;
import com.gregtechceu.gtceu.api.multiblock.autobuild.batch.StructureRequest;
import com.gregtechceu.gtceu.api.multiblock.autobuild.batch.StructureResult;
import com.gregtechceu.gtceu.api.multiblock.autobuild.plan.BatchPlan;
import com.gregtechceu.gtceu.api.multiblock.autobuild.plan.CellAction;
import com.gregtechceu.gtceu.api.multiblock.autobuild.plan.PlannedCell;
import com.gregtechceu.gtceu.api.multiblock.autobuild.plan.StructurePlan;
import com.gregtechceu.gtceu.api.multiblock.pattern.match.MultiBlockPattern;
import com.gregtechceu.gtceu.common.machine.owner.MachineOwner;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.neoforged.neoforge.common.CommonHooks;

import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectImmutableList;
import it.unimi.dsi.fastutil.objects.ObjectList;
import lombok.Getter;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Executes an already resolved batch without performing any candidate selection.
 */
@NullMarked
public final class BatchExecutor {

    private BatchExecutor() {}

    public static BatchResult execute(MultiblockControllerMachine controller, ServerPlayer player,
                                      BatchRequest request, BatchPlan plan,
                                      long expectedFingerprint) {
        BuildProblem targetProblem = validateTarget(controller, player);
        if (targetProblem != null) {
            return failAll(plan, targetProblem);
        }
        if (plan.fingerprint() != expectedFingerprint) {
            return failAll(plan, problem(controller.getBlockPos(),
                    "gtpm.multiblock.autobuild.stale_plan"));
        }
        if (!plan.sharedProblems().isEmpty()) {
            return failAll(plan, plan.sharedProblems());
        }
        Object2ObjectLinkedOpenHashMap<String, StructureRequest> optionsByName = new Object2ObjectLinkedOpenHashMap<>();
        for (StructureRequest options : request.structures()) {
            optionsByName.put(options.structureName(), options);
        }
        ObjectArrayList<StructureResult> results = new ObjectArrayList<>(plan.executionOrder().size());
        for (StructurePlan structure : plan.executionOrder()) {
            results.add(executeStructure(controller, player, request, structure));
        }
        finalizeSuccessfulBuilds(controller, results, optionsByName);
        return BatchResult.of(results);
    }

    static @Nullable BuildProblem validateTarget(MultiblockControllerMachine controller, ServerPlayer player) {
        if (player.isSpectator()) {
            return new BuildProblem(controller.getBlockPos(),
                    Component.translatable("gtpm.multiblock.autobuild.spectator_denied"));
        }
        if (!(player.level() instanceof ServerLevel level) || controller.getLevel() != level) {
            return new BuildProblem(controller.getBlockPos(),
                    Component.translatable("gtpm.multiblock.autobuild.invalid_target_level"));
        }
        BlockPos controllerPos = controller.getBlockPos();
        if (!level.isLoaded(controllerPos)) {
            return new BuildProblem(controllerPos,
                    Component.translatable("gtpm.multiblock.autobuild.controller_unloaded"));
        }
        if (MetaMachine.getMachine(level, controllerPos) != controller) {
            return new BuildProblem(controllerPos,
                    Component.translatable("gtpm.multiblock.autobuild.controller_changed"));
        }
        if (player.distanceToSqr(Vec3.atCenterOf(controllerPos)) > 64) {
            return new BuildProblem(controllerPos,
                    Component.translatable("gtpm.multiblock.autobuild.too_far"));
        }
        if (!level.mayInteract(player, controllerPos) ||
                !MachineOwner.canBreakOwnerMachine(player, controller)) {
            return new BuildProblem(controllerPos,
                    Component.translatable("gtpm.multiblock.autobuild.permission_denied",
                            controllerPos.toShortString()));
        }
        return null;
    }

    private static StructureResult executeStructure(MultiblockControllerMachine controller,
                                                    ServerPlayer player,
                                                    BatchRequest request,
                                                    StructurePlan structure) {
        if (!structure.problems().isEmpty()) {
            return failed(structure, structure.problems());
        }
        if (!(player.level() instanceof ServerLevel level)) {
            return failed(structure, problem(controller.getBlockPos(),
                    "gtpm.multiblock.autobuild.server_only"));
        }

        ObjectArrayList<MaterialSource.Session> sessions = new ObjectArrayList<>();
        ObjectList<MaterialRequest.Result> bindings = new ObjectArrayList<>();
        for (MaterialRequest.Result binding : request.materialSources()) {
            MaterialSource source = binding.source();
            MaterialSource.Session session = source.openSession();
            BuildProblem sessionProblem = session.problem();
            if (sessionProblem != null) continue;
            sessions.add(session);
            bindings.add(new MaterialRequest.Result(binding.providerId(), source, session,
                    binding.excludedContainer(), binding.materials()));
        }
        StructureWork work = assess(controller, player, level, request, structure, sessions);
        if (!work.problems().isEmpty()) {
            return failed(structure, work.problems());
        }
        if (!player.isCreative() && !reserveMaterials(player, bindings, work)) {
            return failed(structure, work.problems());
        }
        if (!approveClears(player, level, work)) {
            return failed(structure, work.problems());
        }
        try {
            if (!commitMaterials(controller.getBlockPos(), work)) {
                refundCommitted(controller.getBlockPos(), work, 0);
                return failed(structure, work.problems());
            }

            int removed = clearBlocks(player, level, work);
            if (!work.problems().isEmpty()) {
                refundCommitted(controller.getBlockPos(), work, 0);
                return new StructureResult(structure.structureName(), structure.mode(), false, 0, removed,
                        work.problems());
            }
            if (!applyStateCorrections(level, work)) {
                refundCommitted(controller.getBlockPos(), work, 0);
                return new StructureResult(structure.structureName(), structure.mode(), false, 0, removed,
                        work.problems());
            }
            int placed = placeBlocks(player, level, work);
            if (!work.problems().isEmpty()) {
                refundCommitted(controller.getBlockPos(), work, work.consumedPlacements());
                controller.invalidateStructure(structure.structureName());
                return new StructureResult(structure.structureName(), structure.mode(), false, placed, removed,
                        work.problems());
            }

            if (structure.mode() == StructureOperation.DEMOLISH) {
                if (removed > 0) controller.invalidateStructure(structure.structureName());
                return new StructureResult(structure.structureName(), structure.mode(), true, 0, removed,
                        ObjectList.of());
            }
            return new StructureResult(structure.structureName(), structure.mode(), true, placed, removed,
                    ObjectList.of());
        } catch (RuntimeException exception) {
            GTCEu.LOGGER.error("Automatic-build transaction failed for {} at {}", structure.structureName(),
                    controller.getBlockPos(), exception);
            Placement active = work.activePlacement;
            if (active != null) {
                BlockState actual = level.getBlockState(active.cell().worldPos());
                if (!actual.equals(work.stateBeforePlacement) && actual.is(active.cell().blockState().getBlock())) {
                    work.consumePlacement();
                }
            }
            try {
                refundCommitted(controller.getBlockPos(), work, work.consumedPlacements());
            } catch (RuntimeException refundException) {
                exception.addSuppressed(refundException);
                GTCEu.LOGGER.error("Automatic-build compensation failed for {} at {}", structure.structureName(),
                        controller.getBlockPos(), refundException);
            }
            controller.invalidateStructure(structure.structureName());
            throw exception;
        }
    }

    private static StructureWork assess(MultiblockControllerMachine controller, ServerPlayer player,
                                        ServerLevel level, BatchRequest request,
                                        StructurePlan structure,
                                        ObjectList<MaterialSource.Session> sessions) {
        StructureWork work = new StructureWork(player, sessions);
        for (PlannedCell cell : structure.cells()) {
            BlockPos pos = cell.worldPos();
            if (pos == null || !level.isLoaded(pos)) {
                work.problems().add(problem(pos,
                        "gtpm.multiblock.autobuild.unloaded", position(pos)));
                return work;
            }
            if (cell.action() == CellAction.CONTROLLER ||
                    cell.action() == CellAction.IGNORE_ANY) {
                continue;
            }
            BlockState current = level.getBlockState(pos);
            if (structure.mode() == StructureOperation.DEMOLISH) {
                if (cell.action() == CellAction.DEMOLISH_CANDIDATE && !current.isAir()) {
                    BuildProblem clearProblem = validateClear(controller, player, level, pos, current);
                    if (clearProblem != null) {
                        work.problems().add(clearProblem);
                        return work;
                    }
                    work.clears().add(pos);
                }
                continue;
            }
            if (cell.blockState().isAir()) {
                if (!current.isAir()) {
                    if (!request.replaceMode()) {
                        work.problems().add(problem(pos,
                                "gtpm.multiblock.autobuild.blocked", position(pos)));
                        return work;
                    }
                    BuildProblem clearProblem = validateClear(controller, player, level, pos, current);
                    if (clearProblem != null) {
                        work.problems().add(clearProblem);
                        return work;
                    }
                    work.clears().add(pos);
                }
                continue;
            }
            if (current.equals(cell.blockState())) continue;
            if (cell.action() == CellAction.UPDATE_DIRECTION &&
                    current.is(cell.blockState().getBlock())) {
                BuildProblem correctionProblem = validateStateCorrection(player, level, pos, cell);
                if (correctionProblem != null) {
                    work.problems().add(correctionProblem);
                    return work;
                }
                work.stateCorrections().add(new StateCorrection(pos, cell.blockState()));
                continue;
            }
            boolean replaceable = current.isAir() || current.canBeReplaced();
            if (!current.isAir() && !request.replaceMode()) {
                work.problems().add(problem(pos,
                        "gtpm.multiblock.autobuild.blocked", position(pos)));
                return work;
            }
            if (!replaceable) {
                BuildProblem clearProblem = validateClear(controller, player, level, pos, current);
                if (clearProblem != null) {
                    work.problems().add(clearProblem);
                    return work;
                }
                work.clears().add(pos);
            }
            MaterialKey materialKey = cell.materialKey();
            if (materialKey == null || !(materialKey.prototype().getItem() instanceof BlockItem)) {
                work.problems().add(problem(pos,
                        "gtpm.multiblock.autobuild.no_candidate", position(pos)));
                return work;
            }
            BuildProblem placementProblem = validatePlacement(player, level, pos, cell, !replaceable);
            if (placementProblem != null) {
                work.problems().add(placementProblem);
                return work;
            }
            work.placements().add(new Placement(cell));
        }
        return work;
    }

    private static boolean reserveMaterials(ServerPlayer player, ObjectList<MaterialRequest.Result> bindings,
                                            StructureWork work) {
        ObjectList<MaterialRequest.Demand> demands = new ObjectArrayList<>(work.placements().size());
        for (Placement placement : work.placements()) {
            demands.add(new MaterialRequest.Demand(placement.cell().materialKey(), 1));
        }
        ItemStack excluded = bindings.isEmpty() ? ItemStack.EMPTY : bindings.getFirst().excludedContainer();
        MaterialRequest request = new MaterialRequest(demands, excluded, bindings);
        for (MaterialRequest.Result result : MaterialSources.request(player, request)) {
            MaterialSource.Session session = result.session();
            for (MaterialRequest.Allocation allocation : result.materials().allocations()) {
                work.placements().get(allocation.demandIndex()).reservations()
                        .add(new SourcedReservation(session, allocation.reservation()));
            }
        }
        int missing = request.firstUnfulfilled();
        if (missing >= 0) {
            BlockPos pos = work.placements().get(missing).cell().worldPos();
            work.problems().add(problem(pos,
                    "gtpm.multiblock.autobuild.missing_material", position(pos)));
            return false;
        }
        return true;
    }

    private static boolean approveClears(ServerPlayer player, ServerLevel level, StructureWork work) {
        for (BlockPos pos : work.clears()) {
            BlockState state = level.getBlockState(pos);
            if (!state.isAir() && CommonHooks.fireBlockBreak(level, player.gameMode.getGameModeForPlayer(), player,
                    pos, state).isCanceled()) {
                work.problems().add(problem(pos,
                        "gtpm.multiblock.autobuild.break_event_denied", position(pos)));
                return false;
            }
        }
        return true;
    }

    private static boolean commitMaterials(BlockPos fallbackPos, StructureWork work) {
        for (Placement placement : work.placements()) {
            for (SourcedReservation sourced : placement.reservations()) {
                sourced.commitAttempted = true;
                MaterialSource.CommitResult result = sourced.reservation().commitExact();
                drainRejectedStacks(fallbackPos, work, sourced);
                if (result.problem() != null) {
                    work.problems().add(result.problem());
                    return false;
                }
                if (!work.problems().isEmpty()) return false;
                if (!result.complete()) {
                    work.problems().add(problem(placement.cell().worldPos(),
                            "gtpm.multiblock.autobuild.extraction_changed",
                            position(placement.cell().worldPos())));
                    return false;
                }
            }
        }
        return true;
    }

    private static int clearBlocks(ServerPlayer player, ServerLevel level, StructureWork work) {
        int removed = 0;
        for (BlockPos pos : work.clears()) {
            BlockState state = level.getBlockState(pos);
            if (state.isAir()) continue;
            var drops = Block.getDrops(state, level, pos, level.getBlockEntity(pos), player,
                    player.getMainHandItem());
            if (!level.destroyBlock(pos, false, player)) {
                work.problems().add(problem(pos,
                        "gtpm.multiblock.autobuild.demolition_failed", position(pos)));
                return removed;
            }
            drops.forEach(drop -> insertOrDrop(work, drop, null));
            removed++;
        }
        return removed;
    }

    private static boolean applyStateCorrections(ServerLevel level, StructureWork work) {
        for (StateCorrection correction : work.stateCorrections()) {
            if (!level.setBlock(correction.pos(), correction.state(),
                    Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE) ||
                    !level.getBlockState(correction.pos()).equals(correction.state())) {
                work.problems().add(problem(correction.pos(),
                        "gtpm.multiblock.autobuild.direction_update_failed", position(correction.pos())));
                return false;
            }
        }
        return true;
    }

    private static int placeBlocks(ServerPlayer player, ServerLevel level, StructureWork work) {
        int placed = 0;
        for (Placement placement : work.placements()) {
            PlannedCell cell = placement.cell();
            ItemStack stack = cell.materialKey().prototype();
            if (!(stack.getItem() instanceof BlockItem blockItem)) {
                work.problems().add(problem(cell.worldPos(),
                        "gtpm.multiblock.autobuild.not_placeable", position(cell.worldPos())));
                return placed;
            }
            BlockPlaceContext context = new ExactPlaceContext(level, player, stack, cell.worldPos());
            work.activePlacement = placement;
            work.stateBeforePlacement = level.getBlockState(cell.worldPos());
            InteractionResult result = blockItem.place(context);
            if (result == InteractionResult.FAIL) {
                work.activePlacement = null;
                work.problems().add(problem(cell.worldPos(),
                        "gtpm.multiblock.autobuild.place_failed", position(cell.worldPos())));
                return placed;
            }
            work.consumePlacement();
            work.activePlacement = null;
            BlockState actual = level.getBlockState(cell.worldPos());
            if (!actual.is(cell.blockState().getBlock())) {
                work.problems().add(problem(cell.worldPos(),
                        "gtpm.multiblock.autobuild.place_failed", position(cell.worldPos())));
                return placed;
            }
            if (!actual.equals(cell.blockState()) &&
                    !level.setBlock(cell.worldPos(), cell.blockState(),
                            Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE)) {
                work.problems().add(problem(cell.worldPos(),
                        "gtpm.multiblock.autobuild.place_failed", position(cell.worldPos())));
                return placed;
            }
            if (!level.getBlockState(cell.worldPos()).equals(cell.blockState())) {
                work.problems().add(problem(cell.worldPos(),
                        "gtpm.multiblock.autobuild.place_failed", position(cell.worldPos())));
                return placed;
            }
            if (cell.blockInfo().hasBlockEntity()) {
                BlockEntity blockEntity = level.getBlockEntity(cell.worldPos());
                if (blockEntity == null) {
                    work.problems().add(problem(cell.worldPos(),
                            "gtpm.multiblock.autobuild.place_failed", position(cell.worldPos())));
                    return placed;
                }
                try {
                    cell.blockInfo().initialize(blockEntity);
                    blockEntity.setChanged();
                } catch (RuntimeException exception) {
                    GTCEu.LOGGER.error("Failed to initialize automatic-build block entity at {}", cell.worldPos(),
                            exception);
                    work.problems().add(problem(cell.worldPos(),
                            "gtpm.multiblock.autobuild.place_failed", position(cell.worldPos())));
                    return placed;
                }
            }
            if (!level.getBlockState(cell.worldPos()).equals(cell.blockState())) {
                work.problems().add(problem(cell.worldPos(),
                        "gtpm.multiblock.autobuild.place_failed", position(cell.worldPos())));
                return placed;
            }
            placed++;
        }
        return placed;
    }

    private static void refundCommitted(BlockPos fallbackPos, StructureWork work,
                                        int consumedPlacements) {
        if (work.refundStarted) return;
        work.refundStarted = true;
        for (int index = consumedPlacements; index < work.placements().size(); index++) {
            Placement placement = work.placements().get(index);
            for (SourcedReservation sourced : placement.reservations()) {
                if (!sourced.commitAttempted) {
                    drainRejectedStacks(fallbackPos, work, sourced);
                    continue;
                }
                MaterialSource.RollbackResult rollback = sourced.reservation().rollback();
                long restored = rollback.restored();
                long remainder = rollback.requested() - restored;
                while (remainder-- > 0) {
                    insertOrDrop(work, sourced.reservation().key().prototype(),
                            sourced.getSourceSession());
                }
                drainRejectedStacks(fallbackPos, work, sourced);
                if (rollback.problem() != null || restored < rollback.requested()) {
                    BuildProblem rollbackProblem = rollback.problem() == null ?
                            problem(fallbackPos,
                                    "gtpm.multiblock.autobuild.refund_failed") :
                            rollback.problem();
                    GTCEu.LOGGER.error("Failed to return automatic-build material at {}: {}", fallbackPos,
                            rollbackProblem.message().getString());
                    work.problems().add(rollbackProblem);
                }
            }
        }
    }

    private static void drainRejectedStacks(BlockPos fallbackPos, StructureWork work,
                                            SourcedReservation sourced) {
        ObjectList<ItemStack> rejected = sourced.reservation().rejectedStacks();
        if (rejected.size() <= sourced.rejectedStacksDrained) return;
        for (int index = sourced.rejectedStacksDrained; index < rejected.size(); index++) {
            ItemStack stack = rejected.get(index);
            if (stack.isEmpty()) continue;
            insertOrDrop(work, stack, sourced.getSourceSession());
        }
        int newlyRejected = rejected.size() - sourced.rejectedStacksDrained;
        sourced.rejectedStacksDrained = rejected.size();
        BuildProblem problem = problem(fallbackPos,
                "gtpm.multiblock.autobuild.rejected_stacks");
        GTCEu.LOGGER.error("Material source returned {} rejected stack(s) during automatic building at {}",
                newlyRejected, fallbackPos);
        work.problems().add(problem);
    }

    private static void insertOrDrop(StructureWork work, ItemStack stack,
                                     @Nullable Session excludedSource) {
        ItemStack remainder = stack.copy();
        for (int index = work.sessions().size() - 1; index >= 0; index--) {
            MaterialSource.Session session = work.sessions().get(index);
            if (session == excludedSource) continue;
            if (remainder.isEmpty()) return;
            remainder = session.insert(remainder, false);
        }
        if (!remainder.isEmpty()) {
            ServerPlayer player = work.player;
            ItemEntity dropped = new ItemEntity(player.serverLevel(), player.getX(), player.getY(), player.getZ(),
                    remainder, 0, 0, 0);
            dropped.setDefaultPickUpDelay();
            player.serverLevel().addFreshEntity(dropped);
        }
    }

    private static @Nullable BuildProblem validateClear(MultiblockControllerMachine controller,
                                                        ServerPlayer player, ServerLevel level,
                                                        BlockPos pos, BlockState state) {
        if (pos.equals(controller.getBlockPos())) {
            return problem(pos,
                    "gtpm.multiblock.autobuild.controller_protected");
        }
        if (!level.isLoaded(pos)) {
            return problem(pos, "gtpm.multiblock.autobuild.unloaded",
                    position(pos));
        }
        if (!level.mayInteract(player, pos) ||
                !player.mayUseItemAt(pos.relative(Direction.UP), Direction.UP, player.getMainHandItem())) {
            return problem(pos,
                    "gtpm.multiblock.autobuild.permission_denied", position(pos));
        }
        if (state.hasBlockEntity() && level.getBlockEntity(pos) instanceof MetaMachine machine &&
                !MachineOwner.canBreakOwnerMachine(player, machine)) {
            return problem(pos,
                    "gtpm.multiblock.autobuild.permission_denied", position(pos));
        }
        if (state.getDestroySpeed(level, pos) < 0) {
            return problem(pos,
                    "gtpm.multiblock.autobuild.unbreakable", position(pos));
        }
        return null;
    }

    private static @Nullable BuildProblem validateStateCorrection(ServerPlayer player, ServerLevel level,
                                                                  BlockPos pos, PlannedCell cell) {
        ItemStack stack = cell.materialKey() == null ? player.getMainHandItem() : cell.materialKey().prototype();
        if (!level.mayInteract(player, pos) ||
                !player.mayUseItemAt(pos.relative(Direction.UP), Direction.UP, stack)) {
            return problem(pos,
                    "gtpm.multiblock.autobuild.permission_denied", position(pos));
        }
        if (level.getBlockEntity(pos) instanceof MetaMachine machine &&
                !MachineOwner.canBreakOwnerMachine(player, machine)) {
            return problem(pos,
                    "gtpm.multiblock.autobuild.permission_denied", position(pos));
        }
        if (!cell.blockState().canSurvive(level, pos) ||
                !level.isUnobstructed(cell.blockState(), pos, CollisionContext.of(player))) {
            return problem(pos,
                    "gtpm.multiblock.autobuild.not_placeable", position(pos));
        }
        return null;
    }

    private static @Nullable BuildProblem validatePlacement(ServerPlayer player, ServerLevel level,
                                                            BlockPos pos, PlannedCell cell, boolean willClear) {
        ItemStack stack = cell.materialKey().prototype();
        if (!(stack.getItem() instanceof BlockItem blockItem) ||
                !blockItem.getBlock().isEnabled(level.enabledFeatures()) ||
                !cell.blockState().is(blockItem.getBlock())) {
            return problem(pos, "gtpm.multiblock.autobuild.not_placeable",
                    position(pos));
        }
        if (!level.mayInteract(player, pos) ||
                !player.mayUseItemAt(pos.relative(Direction.UP), Direction.UP, stack)) {
            return problem(pos,
                    "gtpm.multiblock.autobuild.permission_denied", position(pos));
        }
        BlockPlaceContext context = new ExactPlaceContext(level, player, stack, pos);
        if (!willClear && !context.canPlace()) {
            return problem(pos,
                    "gtpm.multiblock.autobuild.not_placeable", position(pos));
        }
        BlockPlaceContext updated = blockItem.updatePlacementContext(context);
        if (updated == null) {
            return problem(pos,
                    "gtpm.multiblock.autobuild.not_placeable", position(pos));
        }
        BlockState placementState = blockItem.getBlock().getStateForPlacement(updated);
        if (placementState == null || !cell.blockState().canSurvive(level, pos) ||
                !level.isUnobstructed(cell.blockState(), pos, CollisionContext.of(player))) {
            return problem(pos,
                    "gtpm.multiblock.autobuild.not_placeable", position(pos));
        }
        return null;
    }

    private static boolean commitExactStructure(MultiblockControllerMachine controller,
                                                StructureRequest options) {
        MultiBlockPattern pattern = controller.getPattern(options.structureName());
        if (options.repetitions().size() != pattern.getRepeatDescriptors().size()) return false;
        int[] repetitions = options.repetitions().toIntArray();
        var lock = controller.getPatternLock();
        lock.lock();
        try {
            MultiblockState candidate = new MultiblockState(controller.getLevel(), controller.getBlockPos(),
                    options.structureName());
            candidate.lastController = controller;
            boolean matched = pattern.checkPatternAtExact(candidate, controller.getBlockPos(),
                    controller.getFrontFacing(),
                    controller.getUpwardsFacing(), options.flipMode(), true, repetitions, options.choices());
            return controller.commitStructure(candidate, matched);
        } finally {
            lock.unlock();
        }
    }

    private static void finalizeSuccessfulBuilds(MultiblockControllerMachine controller,
                                                 ObjectList<StructureResult> results,
                                                 Object2ObjectMap<String, StructureRequest> optionsByName) {
        for (int index = 0; index < results.size(); index++) {
            StructureResult result = results.get(index);
            if (!result.success() || result.mode() != StructureOperation.BUILD) continue;
            StructureRequest options = optionsByName.get(result.structureName());
            if (!commitExactStructure(controller, options)) {
                ObjectArrayList<BuildProblem> problems = new ObjectArrayList<>(result.problems());
                problems.add(problem(controller.getBlockPos(),
                        "gtpm.multiblock.autobuild.structure_check_failed", result.structureName()));
                results.set(index, new StructureResult(result.structureName(), result.mode(), false,
                        result.placed(), result.removed(), problems));
            }
        }
    }

    private static BatchResult failAll(BatchPlan plan, BuildProblem problem) {
        return failAll(plan, ObjectList.of(problem));
    }

    private static BatchResult failAll(BatchPlan plan, ObjectList<BuildProblem> problems) {
        return BatchResult.of(plan.executionOrder().stream()
                .map(structure -> new StructureResult(structure.structureName(), structure.mode(), false,
                        0, 0, problems))
                .collect(ObjectImmutableList.toList()));
    }

    private static StructureResult failed(StructurePlan structure, BuildProblem problem) {
        return failed(structure, ObjectList.of(problem));
    }

    private static StructureResult failed(StructurePlan structure,
                                          ObjectList<BuildProblem> problems) {
        return new StructureResult(structure.structureName(), structure.mode(), false, 0, 0, problems);
    }

    private static BuildProblem problem(@Nullable BlockPos pos,
                                        String translationKey, Object... args) {
        return new BuildProblem(pos, Component.translatable(translationKey, args));
    }

    private static String position(@Nullable BlockPos pos) {
        return pos == null ? "?" : pos.toShortString();
    }

    private static final class StructureWork {

        private final ServerPlayer player;
        private final ObjectList<MaterialSource.Session> sessions;
        private final ObjectList<BlockPos> clears = new ObjectArrayList<>();
        private final ObjectList<StateCorrection> stateCorrections = new ObjectArrayList<>();
        private final ObjectList<Placement> placements = new ObjectArrayList<>();
        private final ObjectList<BuildProblem> problems = new ObjectArrayList<>();
        private int consumedPlacements;
        private boolean refundStarted;
        private @Nullable Placement activePlacement;
        private @Nullable BlockState stateBeforePlacement;

        private StructureWork(ServerPlayer player, ObjectList<MaterialSource.Session> sessions) {
            this.player = player;
            this.sessions = new ObjectImmutableList<>(sessions);
        }

        private ObjectList<MaterialSource.Session> sessions() {
            return sessions;
        }

        private ObjectList<BlockPos> clears() {
            return clears;
        }

        private ObjectList<StateCorrection> stateCorrections() {
            return stateCorrections;
        }

        private ObjectList<Placement> placements() {
            return placements;
        }

        private ObjectList<BuildProblem> problems() {
            return problems;
        }

        private void consumePlacement() {
            consumedPlacements++;
        }

        private int consumedPlacements() {
            return consumedPlacements;
        }
    }

    private record StateCorrection(BlockPos pos, BlockState state) {}

    private static final class Placement {

        private final PlannedCell cell;
        private final ObjectList<SourcedReservation> reservations = new ObjectArrayList<>();

        private Placement(PlannedCell cell) {
            this.cell = cell;
        }

        private PlannedCell cell() {
            return cell;
        }

        private ObjectList<SourcedReservation> reservations() {
            return reservations;
        }
    }

    private static final class SourcedReservation {

        @Getter
        private final MaterialSource.Session sourceSession;
        private final Reservation reservation;
        private boolean commitAttempted;
        private int rejectedStacksDrained;

        private SourcedReservation(MaterialSource.Session sourceSession, Reservation reservation) {
            this.sourceSession = sourceSession;
            this.reservation = reservation;
        }

        private Reservation reservation() {
            return reservation;
        }
    }

    private static final class ExactPlaceContext extends BlockPlaceContext {

        private ExactPlaceContext(ServerLevel level, ServerPlayer player, ItemStack stack, BlockPos pos) {
            super(level, player, InteractionHand.MAIN_HAND, stack,
                    new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false));
            replaceClicked = true;
        }
    }
}
