package com.gregtechceu.gtceu.api.machine.multiblock;

import com.gregtechceu.gtceu.GTCEu;
import com.gregtechceu.gtceu.api.machine.MetaMachine;
import com.gregtechceu.gtceu.api.multiblock.MultiblockState;
import com.gregtechceu.gtceu.api.multiblock.MultiblockWorldSavedData;
import com.gregtechceu.gtceu.api.multiblock.autobuild.AutoBuildItemKey;
import com.gregtechceu.gtceu.api.multiblock.autobuild.AutoBuildMaterialSource;
import com.gregtechceu.gtceu.api.multiblock.autobuild.AutoBuildMaterialSource.Reservation;
import com.gregtechceu.gtceu.api.multiblock.autobuild.AutoBuildMaterialSource.SourceKind;
import com.gregtechceu.gtceu.api.multiblock.autobuild.AutoBuildProblem;
import com.gregtechceu.gtceu.api.multiblock.autobuild.batch.AutoBuildBatchRequest;
import com.gregtechceu.gtceu.api.multiblock.autobuild.batch.AutoBuildBatchResult;
import com.gregtechceu.gtceu.api.multiblock.autobuild.batch.AutoBuildMode;
import com.gregtechceu.gtceu.api.multiblock.autobuild.batch.AutoBuildStructureOptions;
import com.gregtechceu.gtceu.api.multiblock.autobuild.batch.AutoBuildStructureResult;
import com.gregtechceu.gtceu.api.multiblock.autobuild.plan.AutoBuildPlan;
import com.gregtechceu.gtceu.api.multiblock.autobuild.plan.CellAction;
import com.gregtechceu.gtceu.api.multiblock.autobuild.plan.PlannedCell;
import com.gregtechceu.gtceu.api.multiblock.autobuild.plan.ResolvedStructurePlan;
import com.gregtechceu.gtceu.api.multiblock.pattern.match.MultiBlockPattern;
import com.gregtechceu.gtceu.common.machine.owner.MachineOwner;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
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

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Executes an already resolved batch without performing any candidate selection.
 */
@NullMarked
public final class MultiblockBatchExecutor {

    private MultiblockBatchExecutor() {}

    public static AutoBuildBatchResult execute(MultiblockControllerMachine controller, ServerPlayer player,
                                               AutoBuildBatchRequest request, AutoBuildPlan plan,
                                               long expectedFingerprint) {
        AutoBuildProblem targetProblem = validateTarget(controller, player);
        if (targetProblem != null) {
            return failAll(plan, targetProblem);
        }
        if (plan.fingerprint() != expectedFingerprint) {
            return failAll(plan, problem(AutoBuildProblem.Type.STALE_PLAN, controller.getBlockPos(),
                    "gtpm.multiblock.autobuild.stale_plan"));
        }
        if (!plan.sharedProblems().isEmpty()) {
            return failAll(plan, plan.sharedProblems());
        }
        if (!plan.buildPlans().isEmpty()) {
            for (AutoBuildMaterialSource source : request.materialSources()) {
                if (!request.sharedOptions().useME() || !isSharedRequiredSource(source)) continue;
                AutoBuildMaterialSource.Session session = source.openSession();
                AutoBuildProblem sessionProblem = session.problem();
                if (sessionProblem != null) {
                    return failAll(plan, sessionProblem);
                }
            }
        }

        LinkedHashMap<String, AutoBuildStructureOptions> optionsByName = new LinkedHashMap<>();
        for (AutoBuildStructureOptions options : request.structures()) {
            optionsByName.put(options.structureName(), options);
        }
        ArrayList<AutoBuildStructureResult> results = new ArrayList<>(plan.executionOrder().size());
        for (ResolvedStructurePlan structure : plan.executionOrder()) {
            results.add(executeStructure(controller, player, request, structure));
        }
        finalizeSuccessfulBuilds(controller, results, optionsByName);
        return AutoBuildBatchResult.of(results);
    }

    static @Nullable AutoBuildProblem validateTarget(MultiblockControllerMachine controller, ServerPlayer player) {
        if (player.isSpectator()) {
            return new AutoBuildProblem(AutoBuildProblem.Type.PERMISSION_DENIED, controller.getBlockPos(),
                    Component.translatable("gtpm.multiblock.autobuild.spectator_denied"));
        }
        if (!(player.level() instanceof ServerLevel level) || controller.getLevel() != level) {
            return new AutoBuildProblem(AutoBuildProblem.Type.INVALID_OPTIONS, controller.getBlockPos(),
                    Component.translatable("gtpm.multiblock.autobuild.invalid_target_level"));
        }
        BlockPos controllerPos = controller.getBlockPos();
        if (!level.isLoaded(controllerPos)) {
            return new AutoBuildProblem(AutoBuildProblem.Type.UNLOADED, controllerPos,
                    Component.translatable("gtpm.multiblock.autobuild.controller_unloaded"));
        }
        if (MetaMachine.getMachine(level, controllerPos) != controller) {
            return new AutoBuildProblem(AutoBuildProblem.Type.SOURCE_CHANGED, controllerPos,
                    Component.translatable("gtpm.multiblock.autobuild.controller_changed"));
        }
        if (player.distanceToSqr(Vec3.atCenterOf(controllerPos)) > 64) {
            return new AutoBuildProblem(AutoBuildProblem.Type.PERMISSION_DENIED, controllerPos,
                    Component.translatable("gtpm.multiblock.autobuild.too_far"));
        }
        if (!level.mayInteract(player, controllerPos) ||
                !MachineOwner.canBreakOwnerMachine(player, controller)) {
            return new AutoBuildProblem(AutoBuildProblem.Type.PERMISSION_DENIED, controllerPos,
                    Component.translatable("gtpm.multiblock.autobuild.permission_denied",
                            controllerPos.toShortString()));
        }
        return null;
    }

    private static boolean isSharedRequiredSource(AutoBuildMaterialSource source) {
        if (source.kind() == SourceKind.ME) return true;
        if (source.kind() != SourceKind.UNAVAILABLE) return false;
        AutoBuildProblem unavailable = source.unavailableProblem();
        return unavailable != null && isStrictAEProblem(unavailable.type());
    }

    private static boolean isStrictAEProblem(AutoBuildProblem.Type type) {
        return switch (type) {
            case ME_UNAVAILABLE, AE_NOT_INSTALLED, AE_NOT_LINKED, AE_LINKED_DIMENSION_MISSING, AE_WRONG_DIMENSION, AE_ACCESS_POINT_MISSING, AE_ACCESS_POINT_INACTIVE, AE_OUT_OF_RANGE, AE_GRID_UNAVAILABLE -> true;
            default -> false;
        };
    }

    private static AutoBuildStructureResult executeStructure(MultiblockControllerMachine controller,
                                                             ServerPlayer player,
                                                             AutoBuildBatchRequest request,
                                                             ResolvedStructurePlan structure) {
        if (!structure.problems().isEmpty()) {
            return failed(structure, structure.problems());
        }
        if (!(player.level() instanceof ServerLevel level)) {
            return failed(structure, problem(AutoBuildProblem.Type.INVALID_OPTIONS, controller.getBlockPos(),
                    "gtpm.multiblock.autobuild.server_only"));
        }

        ArrayList<AutoBuildMaterialSource.Session> sessions = new ArrayList<>();
        if (structure.mode() == AutoBuildMode.BUILD && !player.isCreative()) {
            for (AutoBuildMaterialSource source : request.materialSources()) {
                AutoBuildMaterialSource.Session session = source.openSession();
                AutoBuildProblem sessionProblem = session.problem();
                if (sessionProblem != null) {
                    if (request.sharedOptions().useME() && isSharedRequiredSource(source)) {
                        return failed(structure, sessionProblem);
                    }
                    continue;
                }
                sessions.add(session);
            }
        }
        StructureWork work = assess(controller, player, level, request, structure, sessions);
        if (!work.problems().isEmpty()) {
            return failed(structure, work.problems());
        }
        if (!approveClears(player, level, work)) {
            return failed(structure, work.problems());
        }
        if (!commitMaterials(level, controller.getBlockPos(), work)) {
            refundCommitted(level, controller.getBlockPos(), work, 0);
            return failed(structure, work.problems());
        }

        int removed = clearBlocks(player, level, work);
        if (!work.problems().isEmpty()) {
            refundCommitted(level, controller.getBlockPos(), work, 0);
            return new AutoBuildStructureResult(structure.structureName(), structure.mode(), false, 0, removed,
                    work.problems());
        }
        if (!applyStateCorrections(level, work)) {
            refundCommitted(level, controller.getBlockPos(), work, 0);
            return new AutoBuildStructureResult(structure.structureName(), structure.mode(), false, 0, removed,
                    work.problems());
        }
        int placed = placeBlocks(player, level, work);
        if (!work.problems().isEmpty()) {
            refundCommitted(level, controller.getBlockPos(), work, work.consumedPlacements());
            controller.invalidateStructure(structure.structureName());
            return new AutoBuildStructureResult(structure.structureName(), structure.mode(), false, placed, removed,
                    work.problems());
        }

        if (structure.mode() == AutoBuildMode.DEMOLISH) {
            if (removed > 0) controller.invalidateStructure(structure.structureName());
            return new AutoBuildStructureResult(structure.structureName(), structure.mode(), true, 0, removed,
                    List.of());
        }
        return new AutoBuildStructureResult(structure.structureName(), structure.mode(), true, placed, removed,
                List.of());
    }

    private static StructureWork assess(MultiblockControllerMachine controller, ServerPlayer player,
                                        ServerLevel level, AutoBuildBatchRequest request,
                                        ResolvedStructurePlan structure,
                                        List<AutoBuildMaterialSource.Session> sessions) {
        StructureWork work = new StructureWork(sessions);
        for (PlannedCell cell : structure.cells()) {
            BlockPos pos = cell.worldPos();
            if (pos == null || !level.isLoaded(pos)) {
                work.problems().add(problem(AutoBuildProblem.Type.UNLOADED, pos,
                        "gtpm.multiblock.autobuild.unloaded", position(pos)));
                return work;
            }
            if (cell.action() == CellAction.CONTROLLER ||
                    cell.action() == CellAction.IGNORE_ANY) {
                continue;
            }
            BlockState current = level.getBlockState(pos);
            if (structure.mode() == AutoBuildMode.DEMOLISH) {
                if (!current.isAir() && matchesDemolitionCandidate(cell, current)) {
                    AutoBuildProblem clearProblem = validateClear(controller, player, level, pos, current);
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
                    if (!request.sharedOptions().replaceMode()) {
                        work.problems().add(problem(AutoBuildProblem.Type.BLOCKED, pos,
                                "gtpm.multiblock.autobuild.blocked", position(pos)));
                        return work;
                    }
                    AutoBuildProblem clearProblem = validateClear(controller, player, level, pos, current);
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
                AutoBuildProblem correctionProblem = validateStateCorrection(player, level, pos, cell);
                if (correctionProblem != null) {
                    work.problems().add(correctionProblem);
                    return work;
                }
                work.stateCorrections().add(new StateCorrection(pos, cell.blockState()));
                continue;
            }
            boolean replaceable = current.isAir() || current.canBeReplaced();
            if (!current.isAir() && !request.sharedOptions().replaceMode()) {
                work.problems().add(problem(AutoBuildProblem.Type.BLOCKED, pos,
                        "gtpm.multiblock.autobuild.blocked", position(pos)));
                return work;
            }
            if (!replaceable) {
                AutoBuildProblem clearProblem = validateClear(controller, player, level, pos, current);
                if (clearProblem != null) {
                    work.problems().add(clearProblem);
                    return work;
                }
                work.clears().add(pos);
            }
            AutoBuildItemKey materialKey = cell.materialKey();
            if (materialKey == null || !(materialKey.prototype().getItem() instanceof BlockItem)) {
                work.problems().add(problem(AutoBuildProblem.Type.UNSUPPORTED, pos,
                        "gtpm.multiblock.autobuild.no_candidate", position(pos)));
                return work;
            }
            AutoBuildProblem placementProblem = validatePlacement(player, level, pos, cell, !replaceable);
            if (placementProblem != null) {
                work.problems().add(placementProblem);
                return work;
            }
            Placement placement = new Placement(cell);
            SourcedReservation reservation = player.isCreative() ? null :
                    reserve(materialKey, 1, sessions, 0, work);
            if (!player.isCreative() && reservation == null) {
                if (!work.problems().isEmpty()) return work;
                work.problems().add(problem(AutoBuildProblem.Type.MISSING_MATERIAL, pos,
                        "gtpm.multiblock.autobuild.missing_material", position(pos)));
                return work;
            }
            if (reservation != null) placement.reservations().add(reservation);
            work.placements().add(placement);
        }
        return work;
    }

    private static @Nullable SourcedReservation reserve(AutoBuildItemKey key, long amount,
                                                        List<AutoBuildMaterialSource.Session> sessions,
                                                        int firstSource, StructureWork work) {
        for (int index = firstSource; index < sessions.size(); index++) {
            AutoBuildMaterialSource.Session session = sessions.get(index);
            AutoBuildProblem sessionProblem = session.problem();
            if (sessionProblem != null) {
                work.problems().add(sessionProblem);
                return null;
            }
            Reservation reservation = session.reserve(key, amount);
            if (reservation == null) continue;
            if (!key.equals(reservation.key()) || reservation.reservedAmount() <= 0 ||
                    reservation.reservedAmount() > amount) {
                work.problems().add(problem(AutoBuildProblem.Type.EXTRACTION_FAILED, null,
                        "gtpm.multiblock.autobuild.invalid_reservation"));
                return null;
            }
            return new SourcedReservation(index, reservation);
        }
        return null;
    }

    private static boolean approveClears(ServerPlayer player, ServerLevel level, StructureWork work) {
        for (BlockPos pos : work.clears()) {
            BlockState state = level.getBlockState(pos);
            if (!state.isAir() && CommonHooks.fireBlockBreak(level, player.gameMode.getGameModeForPlayer(), player,
                    pos, state).isCanceled()) {
                work.problems().add(problem(AutoBuildProblem.Type.PERMISSION_DENIED, pos,
                        "gtpm.multiblock.autobuild.break_event_denied", position(pos)));
                return false;
            }
        }
        return true;
    }

    private static boolean commitMaterials(ServerLevel level, BlockPos fallbackPos, StructureWork work) {
        for (Placement placement : work.placements()) {
            if (placement.reservations().isEmpty()) continue;
            long remaining = 1;
            int nextSource = 0;
            for (int index = 0; remaining > 0; index++) {
                SourcedReservation sourced;
                if (index < placement.reservations().size()) {
                    sourced = placement.reservations().get(index);
                } else {
                    sourced = reserve(placement.cell().materialKey(), remaining, work.sessions(), nextSource, work);
                    if (sourced == null) break;
                    placement.reservations().add(sourced);
                }
                AutoBuildMaterialSource.CommitResult result = sourced.reservation().commitExact();
                sourced.committed = Math.addExact(sourced.committed, result.committed());
                remaining -= result.committed();
                nextSource = sourced.sourceIndex() + 1;
                drainRejectedStacks(level, fallbackPos, work, sourced);
                if (result.problem() != null) {
                    work.problems().add(result.problem());
                    return false;
                }
                if (!work.problems().isEmpty()) return false;
            }
            if (remaining > 0) {
                if (work.problems().isEmpty()) {
                    work.problems().add(problem(AutoBuildProblem.Type.EXTRACTION_FAILED,
                            placement.cell().worldPos(),
                            "gtpm.multiblock.autobuild.extraction_changed",
                            position(placement.cell().worldPos())));
                }
                return false;
            }
        }
        return true;
    }

    private static int clearBlocks(ServerPlayer player, ServerLevel level, StructureWork work) {
        int removed = 0;
        for (BlockPos pos : work.clears()) {
            BlockState state = level.getBlockState(pos);
            if (state.isAir()) continue;
            List<ItemStack> drops = Block.getDrops(state, level, pos, level.getBlockEntity(pos), player,
                    player.getMainHandItem());
            if (!level.destroyBlock(pos, false, player)) {
                work.problems().add(problem(AutoBuildProblem.Type.DEMOLITION_FAILED, pos,
                        "gtpm.multiblock.autobuild.demolition_failed", position(pos)));
                return removed;
            }
            drops.forEach(drop -> insertOrDrop(level, pos, work.sessions(), drop));
            removed++;
        }
        return removed;
    }

    private static boolean applyStateCorrections(ServerLevel level, StructureWork work) {
        for (StateCorrection correction : work.stateCorrections()) {
            if (!level.setBlock(correction.pos(), correction.state(),
                    Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE) ||
                    !level.getBlockState(correction.pos()).equals(correction.state())) {
                work.problems().add(problem(AutoBuildProblem.Type.PLACE_FAILED, correction.pos(),
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
                work.problems().add(problem(AutoBuildProblem.Type.PLACE_FAILED, cell.worldPos(),
                        "gtpm.multiblock.autobuild.not_placeable", position(cell.worldPos())));
                return placed;
            }
            BlockPlaceContext context = new ExactPlaceContext(level, player, stack, cell.worldPos());
            InteractionResult result = blockItem.place(context);
            if (result == InteractionResult.FAIL) {
                work.problems().add(problem(AutoBuildProblem.Type.PLACE_FAILED, cell.worldPos(),
                        "gtpm.multiblock.autobuild.place_failed", position(cell.worldPos())));
                return placed;
            }
            work.consumePlacement();
            BlockState actual = level.getBlockState(cell.worldPos());
            if (!actual.is(cell.blockState().getBlock())) {
                work.problems().add(problem(AutoBuildProblem.Type.PLACE_FAILED, cell.worldPos(),
                        "gtpm.multiblock.autobuild.place_failed", position(cell.worldPos())));
                return placed;
            }
            if (!actual.equals(cell.blockState()) &&
                    !level.setBlock(cell.worldPos(), cell.blockState(),
                            Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE)) {
                work.problems().add(problem(AutoBuildProblem.Type.PLACE_FAILED, cell.worldPos(),
                        "gtpm.multiblock.autobuild.place_failed", position(cell.worldPos())));
                return placed;
            }
            if (!level.getBlockState(cell.worldPos()).equals(cell.blockState())) {
                work.problems().add(problem(AutoBuildProblem.Type.PLACE_FAILED, cell.worldPos(),
                        "gtpm.multiblock.autobuild.place_failed", position(cell.worldPos())));
                return placed;
            }
            if (cell.blockInfo().hasBlockEntity()) {
                BlockEntity blockEntity = level.getBlockEntity(cell.worldPos());
                if (blockEntity == null) {
                    work.problems().add(problem(AutoBuildProblem.Type.PLACE_FAILED, cell.worldPos(),
                            "gtpm.multiblock.autobuild.place_failed", position(cell.worldPos())));
                    return placed;
                }
                try {
                    cell.blockInfo().initialize(blockEntity);
                    blockEntity.setChanged();
                } catch (RuntimeException exception) {
                    GTCEu.LOGGER.error("Failed to initialize automatic-build block entity at {}", cell.worldPos(),
                            exception);
                    work.problems().add(problem(AutoBuildProblem.Type.PLACE_FAILED, cell.worldPos(),
                            "gtpm.multiblock.autobuild.place_failed", position(cell.worldPos())));
                    return placed;
                }
            }
            if (!level.getBlockState(cell.worldPos()).equals(cell.blockState())) {
                work.problems().add(problem(AutoBuildProblem.Type.PLACE_FAILED, cell.worldPos(),
                        "gtpm.multiblock.autobuild.place_failed", position(cell.worldPos())));
                return placed;
            }
            placed++;
        }
        return placed;
    }

    private static void refundCommitted(ServerLevel level, BlockPos fallbackPos, StructureWork work,
                                        int consumedPlacements) {
        for (int index = consumedPlacements; index < work.placements().size(); index++) {
            Placement placement = work.placements().get(index);
            for (SourcedReservation sourced : placement.reservations()) {
                if (sourced.committed == 0) {
                    drainRejectedStacks(level, fallbackPos, work, sourced);
                    continue;
                }
                AutoBuildMaterialSource.RollbackResult rollback = sourced.reservation().rollback();
                long restored = Math.min(sourced.committed, rollback.restored());
                long remainder = sourced.committed - restored;
                while (remainder-- > 0) {
                    insertOrDrop(level, fallbackPos, work.sessions(), sourced.reservation().key().prototype(),
                            sourced.sourceIndex());
                }
                drainRejectedStacks(level, fallbackPos, work, sourced);
                if (rollback.problem() != null || restored < sourced.committed) {
                    AutoBuildProblem rollbackProblem = rollback.problem() == null ?
                            problem(AutoBuildProblem.Type.REFUND_FAILED, fallbackPos,
                                    "gtpm.multiblock.autobuild.refund_failed") :
                            rollback.problem();
                    GTCEu.LOGGER.error("Failed to return automatic-build material at {}: {}", fallbackPos,
                            rollbackProblem.message().getString());
                    work.problems().add(rollbackProblem);
                }
            }
        }
    }

    private static void drainRejectedStacks(ServerLevel level, BlockPos fallbackPos, StructureWork work,
                                            SourcedReservation sourced) {
        List<ItemStack> rejected = sourced.reservation().rejectedStacks();
        if (rejected.size() <= sourced.rejectedStacksDrained) return;
        for (int index = sourced.rejectedStacksDrained; index < rejected.size(); index++) {
            ItemStack stack = rejected.get(index);
            if (stack.isEmpty()) continue;
            insertOrDrop(level, fallbackPos, work.sessions(), stack, sourced.sourceIndex());
        }
        int newlyRejected = rejected.size() - sourced.rejectedStacksDrained;
        sourced.rejectedStacksDrained = rejected.size();
        AutoBuildProblem problem = problem(AutoBuildProblem.Type.REFUND_FAILED, fallbackPos,
                "gtpm.multiblock.autobuild.rejected_stacks");
        GTCEu.LOGGER.error("Material source returned {} rejected stack(s) during automatic building at {}",
                newlyRejected, fallbackPos);
        work.problems().add(problem);
    }

    private static void insertOrDrop(ServerLevel level, BlockPos pos,
                                     List<AutoBuildMaterialSource.Session> sessions, ItemStack stack) {
        insertOrDrop(level, pos, sessions, stack, -1);
    }

    private static void insertOrDrop(ServerLevel level, BlockPos pos,
                                     List<AutoBuildMaterialSource.Session> sessions, ItemStack stack,
                                     int excludedSource) {
        ItemStack remainder = stack.copy();
        for (int index = 0; index < sessions.size(); index++) {
            if (index == excludedSource) continue;
            if (remainder.isEmpty()) return;
            remainder = sessions.get(index).insert(remainder, false);
        }
        if (!remainder.isEmpty()) Block.popResource(level, pos, remainder);
    }

    private static @Nullable AutoBuildProblem validateClear(MultiblockControllerMachine controller,
                                                            ServerPlayer player, ServerLevel level,
                                                            BlockPos pos, BlockState state) {
        if (pos.equals(controller.getBlockPos())) {
            return problem(AutoBuildProblem.Type.PERMISSION_DENIED, pos,
                    "gtpm.multiblock.autobuild.controller_protected");
        }
        if (!level.isLoaded(pos)) {
            return problem(AutoBuildProblem.Type.UNLOADED, pos, "gtpm.multiblock.autobuild.unloaded",
                    position(pos));
        }
        if (!level.mayInteract(player, pos) ||
                !player.mayUseItemAt(pos.relative(Direction.UP), Direction.UP, player.getMainHandItem())) {
            return problem(AutoBuildProblem.Type.PERMISSION_DENIED, pos,
                    "gtpm.multiblock.autobuild.permission_denied", position(pos));
        }
        if (state.hasBlockEntity() && level.getBlockEntity(pos) instanceof MetaMachine machine &&
                !MachineOwner.canBreakOwnerMachine(player, machine)) {
            return problem(AutoBuildProblem.Type.PERMISSION_DENIED, pos,
                    "gtpm.multiblock.autobuild.permission_denied", position(pos));
        }
        if (state.getDestroySpeed(level, pos) < 0) {
            return problem(AutoBuildProblem.Type.DEMOLITION_FAILED, pos,
                    "gtpm.multiblock.autobuild.unbreakable", position(pos));
        }
        return null;
    }

    private static @Nullable AutoBuildProblem validateStateCorrection(ServerPlayer player, ServerLevel level,
                                                                      BlockPos pos, PlannedCell cell) {
        ItemStack stack = cell.materialKey() == null ? player.getMainHandItem() : cell.materialKey().prototype();
        if (!level.mayInteract(player, pos) ||
                !player.mayUseItemAt(pos.relative(Direction.UP), Direction.UP, stack)) {
            return problem(AutoBuildProblem.Type.PERMISSION_DENIED, pos,
                    "gtpm.multiblock.autobuild.permission_denied", position(pos));
        }
        if (level.getBlockEntity(pos) instanceof MetaMachine machine &&
                !MachineOwner.canBreakOwnerMachine(player, machine)) {
            return problem(AutoBuildProblem.Type.PERMISSION_DENIED, pos,
                    "gtpm.multiblock.autobuild.permission_denied", position(pos));
        }
        if (!cell.blockState().canSurvive(level, pos) ||
                !level.isUnobstructed(cell.blockState(), pos, CollisionContext.of(player))) {
            return problem(AutoBuildProblem.Type.PLACE_FAILED, pos,
                    "gtpm.multiblock.autobuild.not_placeable", position(pos));
        }
        return null;
    }

    private static @Nullable AutoBuildProblem validatePlacement(ServerPlayer player, ServerLevel level,
                                                                BlockPos pos, PlannedCell cell, boolean willClear) {
        ItemStack stack = cell.materialKey().prototype();
        if (!(stack.getItem() instanceof BlockItem blockItem) ||
                !blockItem.getBlock().isEnabled(level.enabledFeatures()) ||
                !cell.blockState().is(blockItem.getBlock())) {
            return problem(AutoBuildProblem.Type.PLACE_FAILED, pos, "gtpm.multiblock.autobuild.not_placeable",
                    position(pos));
        }
        if (!level.mayInteract(player, pos) ||
                !player.mayUseItemAt(pos.relative(Direction.UP), Direction.UP, stack)) {
            return problem(AutoBuildProblem.Type.PERMISSION_DENIED, pos,
                    "gtpm.multiblock.autobuild.permission_denied", position(pos));
        }
        BlockPlaceContext context = new ExactPlaceContext(level, player, stack, pos);
        if (!willClear && !context.canPlace()) {
            return problem(AutoBuildProblem.Type.PLACE_FAILED, pos,
                    "gtpm.multiblock.autobuild.not_placeable", position(pos));
        }
        BlockPlaceContext updated = blockItem.updatePlacementContext(context);
        if (updated == null) {
            return problem(AutoBuildProblem.Type.PLACE_FAILED, pos,
                    "gtpm.multiblock.autobuild.not_placeable", position(pos));
        }
        BlockState placementState = blockItem.getBlock().getStateForPlacement(updated);
        if (placementState == null || !cell.blockState().canSurvive(level, pos) ||
                !level.isUnobstructed(cell.blockState(), pos, CollisionContext.of(player))) {
            return problem(AutoBuildProblem.Type.PLACE_FAILED, pos,
                    "gtpm.multiblock.autobuild.not_placeable", position(pos));
        }
        return null;
    }

    private static boolean matchesExactStructure(MultiblockControllerMachine controller,
                                                 AutoBuildStructureOptions options) {
        MultiBlockPattern pattern = controller.getPattern(options.structureName());
        if (options.repetitions().size() != pattern.aisleRepetitions.length) return false;
        int[] repetitions = options.repetitions().stream().mapToInt(Integer::intValue).toArray();
        var lock = controller.getPatternLock();
        lock.lock();
        try {
            MultiblockState state = controller.getMultiblockState(options.structureName());
            return pattern.checkPatternAtExact(state, controller.getBlockPos(), controller.getFrontFacing(),
                    controller.getUpwardsFacing(), options.flipMode(), true, repetitions);
        } finally {
            lock.unlock();
        }
    }

    private static void finalizeSuccessfulBuilds(MultiblockControllerMachine controller,
                                                 List<AutoBuildStructureResult> results,
                                                 Map<String, AutoBuildStructureOptions> optionsByName) {
        if (!(controller.getLevel() instanceof ServerLevel level)) return;
        for (int index = 0; index < results.size(); index++) {
            AutoBuildStructureResult result = results.get(index);
            if (!result.success() || result.mode() != AutoBuildMode.BUILD) continue;
            AutoBuildStructureOptions options = optionsByName.get(result.structureName());
            if (!matchesExactStructure(controller, options)) {
                controller.invalidateStructure(result.structureName());
                ArrayList<AutoBuildProblem> problems = new ArrayList<>(result.problems());
                problems.add(problem(AutoBuildProblem.Type.STRUCTURE_CHECK_FAILED, controller.getBlockPos(),
                        "gtpm.multiblock.autobuild.structure_check_failed", result.structureName()));
                results.set(index, new AutoBuildStructureResult(result.structureName(), result.mode(), false,
                        result.placed(), result.removed(), problems));
            }
        }
        for (AutoBuildStructureResult result : results) {
            if (!result.success() || result.mode() != AutoBuildMode.BUILD) continue;
            AutoBuildStructureOptions options = optionsByName.get(result.structureName());
            MultiblockState state = controller.getMultiblockState(result.structureName());
            if (MultiblockControllerMachine.DEFAULT_STRUCTURE.equals(result.structureName())) {
                controller.setFlipped(options.flipMode());
            }
            controller.formStructure(result.structureName());
            MultiblockWorldSavedData.getOrCreate(level).addMapping(state);
        }
    }

    private static boolean matchesDemolitionCandidate(PlannedCell cell, BlockState state) {
        for (BlockState candidate : cell.stateCandidates()) {
            if (state.equals(candidate)) return true;
        }
        return false;
    }

    private static AutoBuildBatchResult failAll(AutoBuildPlan plan, AutoBuildProblem problem) {
        return failAll(plan, List.of(problem));
    }

    private static AutoBuildBatchResult failAll(AutoBuildPlan plan, List<AutoBuildProblem> problems) {
        return AutoBuildBatchResult.of(plan.executionOrder().stream()
                .map(structure -> new AutoBuildStructureResult(structure.structureName(), structure.mode(), false,
                        0, 0, problems))
                .toList());
    }

    private static AutoBuildStructureResult failed(ResolvedStructurePlan structure, AutoBuildProblem problem) {
        return failed(structure, List.of(problem));
    }

    private static AutoBuildStructureResult failed(ResolvedStructurePlan structure,
                                                   List<AutoBuildProblem> problems) {
        return new AutoBuildStructureResult(structure.structureName(), structure.mode(), false, 0, 0, problems);
    }

    private static AutoBuildProblem problem(AutoBuildProblem.Type type, @Nullable BlockPos pos,
                                            String translationKey, Object... args) {
        return new AutoBuildProblem(type, pos, Component.translatable(translationKey, args));
    }

    private static String position(@Nullable BlockPos pos) {
        return pos == null ? "?" : pos.toShortString();
    }

    private static final class StructureWork {

        private final List<AutoBuildMaterialSource.Session> sessions;
        private final List<BlockPos> clears = new ArrayList<>();
        private final List<StateCorrection> stateCorrections = new ArrayList<>();
        private final List<Placement> placements = new ArrayList<>();
        private final List<AutoBuildProblem> problems = new ArrayList<>();
        private int consumedPlacements;

        private StructureWork(List<AutoBuildMaterialSource.Session> sessions) {
            this.sessions = List.copyOf(sessions);
        }

        private List<AutoBuildMaterialSource.Session> sessions() {
            return sessions;
        }

        private List<BlockPos> clears() {
            return clears;
        }

        private List<StateCorrection> stateCorrections() {
            return stateCorrections;
        }

        private List<Placement> placements() {
            return placements;
        }

        private List<AutoBuildProblem> problems() {
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
        private final List<SourcedReservation> reservations = new ArrayList<>();

        private Placement(PlannedCell cell) {
            this.cell = cell;
        }

        private PlannedCell cell() {
            return cell;
        }

        private List<SourcedReservation> reservations() {
            return reservations;
        }
    }

    private static final class SourcedReservation {

        private final int sourceIndex;
        private final Reservation reservation;
        private long committed;
        private int rejectedStacksDrained;

        private SourcedReservation(int sourceIndex, Reservation reservation) {
            this.sourceIndex = sourceIndex;
            this.reservation = reservation;
        }

        private int sourceIndex() {
            return sourceIndex;
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
