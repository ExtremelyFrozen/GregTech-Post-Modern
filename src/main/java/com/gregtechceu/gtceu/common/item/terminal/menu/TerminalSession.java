package com.gregtechceu.gtceu.common.item.terminal.menu;

import com.gregtechceu.gtceu.GTCEu;
import com.gregtechceu.gtceu.api.machine.MetaMachine;
import com.gregtechceu.gtceu.api.machine.MultiblockMachineDefinition;
import com.gregtechceu.gtceu.api.machine.multiblock.MultiblockControllerMachine;
import com.gregtechceu.gtceu.api.multiblock.autobuild.MaterialRequest;
import com.gregtechceu.gtceu.api.multiblock.autobuild.MaterialSource;
import com.gregtechceu.gtceu.api.multiblock.autobuild.MaterialSources;
import com.gregtechceu.gtceu.api.multiblock.autobuild.batch.BatchRequest;
import com.gregtechceu.gtceu.api.multiblock.autobuild.batch.BatchResult;
import com.gregtechceu.gtceu.api.multiblock.autobuild.batch.StructureOperation;
import com.gregtechceu.gtceu.api.multiblock.autobuild.plan.BatchPlan;
import com.gregtechceu.gtceu.api.multiblock.autobuild.plan.BatchPlanner;
import com.gregtechceu.gtceu.api.multiblock.autobuild.plan.BatchResolution;
import com.gregtechceu.gtceu.api.multiblock.preview.StructurePreview;
import com.gregtechceu.gtceu.common.item.datacomponents.TerminalMachine;
import com.gregtechceu.gtceu.common.item.terminal.TerminalData;
import com.gregtechceu.gtceu.common.item.terminal.network.TerminalChange;
import com.gregtechceu.gtceu.common.machine.owner.MachineOwner;
import com.gregtechceu.gtceu.data.pattern.StructurePatternRegistry;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import it.unimi.dsi.fastutil.objects.ObjectImmutableList;
import it.unimi.dsi.fastutil.objects.ObjectList;
import org.jspecify.annotations.Nullable;

import java.util.Locale;

/** Captures one exact terminal, controller and session for a server-authoritative automatic-build menu. */
public final class TerminalSession {

    private final ServerPlayer player;
    private final InteractionHand hand;
    private final TerminalData data;
    private final ItemStack openedTerminal;
    private final MultiblockControllerMachine openedController;
    private final MultiblockMachineDefinition definition;
    private final BlockPos controllerPos;
    private final ResourceKey<Level> dimension;
    private final ResourceLocation definitionId;
    private final BatchPlanner resolver = new BatchPlanner();
    private TerminalMachine machine;
    private ObjectList<MaterialRequest.Result> boundSources = ObjectList.of();
    private long acceptedSequence;
    private long revision;
    private boolean executing;
    private boolean closed;
    private long patternGeneration;
    @Nullable
    private TerminalExecutionSummary lastExecution;

    /** Captures the exact server objects used for every later validation. */
    public TerminalSession(ServerPlayer player, InteractionHand hand, TerminalData data,
                           MultiblockControllerMachine controller) {
        this.player = player;
        this.hand = hand;
        this.data = data;
        this.openedTerminal = data.getStack();
        this.openedController = controller;
        this.definition = controller.getDefinition();
        this.controllerPos = controller.getBlockPos().immutable();
        this.dimension = player.level().dimension();
        this.definitionId = definition.getId();
        this.machine = loadMachine(player, data, definition);
        patternGeneration = StructurePatternRegistry.generation();
        data.setState(buildState(player));
    }

    public boolean isStillValid(Player player) {
        if (closed || this.player != player || player.isSpectator() ||
                !player.level().dimension().equals(dimension)) {
            return false;
        }
        ItemStack currentTerminal = player.getItemInHand(hand);
        if (!player.level().isLoaded(controllerPos) ||
                player.distanceToSqr(Vec3.atCenterOf(controllerPos)) > 64 ||
                !player.level().mayInteract(player, controllerPos)) {
            return false;
        }
        MetaMachine current = MetaMachine.getMachine(player.level(), controllerPos);
        if (current != openedController || !current.getDefinition().getId().equals(definitionId)) {
            return false;
        }
        return currentTerminal == openedTerminal && MachineOwner.canOpenOwnerMachine(player, openedController) &&
                MachineOwner.canBreakOwnerMachine(player, openedController);
    }

    /** Applies one sequence- and revision-validated patch; the menu saves the annotated item fields. */
    public boolean applyUpdate(ServerPlayer player, long sequence, long expectedRevision,
                               TerminalChange update) {
        if (!beginAction(player, sequence, expectedRevision)) {
            return false;
        }
        try {
            TerminalMachine updated = update.apply(definition, machine);
            ensureSemanticallyValid(updated);
            data.getMachines().put(definitionId, updated);
            machine = updated;
            revision++;
            data.setState(buildState(player));
            return true;
        } catch (IllegalArgumentException | IllegalStateException exception) {
            GTCEu.LOGGER.warn("Rejected terminal machine update for {}: {}", definitionId, exception.getMessage());
            data.setState(stateWithCurrentSequence());
            return false;
        } catch (RuntimeException exception) {
            GTCEu.LOGGER.error("Failed to apply terminal machine update for {}", definitionId, exception);
            closeInvalidSession(player);
            return false;
        }
    }

    /** Executes only the server plan whose fingerprint is still visible to this exact menu session. */
    @Nullable
    public BatchResult execute(ServerPlayer player, long sequence, long expectedRevision, long expectedFingerprint) {
        if (!beginAction(player, sequence, expectedRevision)) {
            return null;
        }
        TerminalMenuState state = data.getState();
        if (expectedFingerprint != Long.parseUnsignedLong(state.preview().fingerprint(), 16)) {
            data.setState(stateWithCurrentSequence());
            return null;
        }
        executing = true;
        try {
            BatchRequest request = machine.createRequest(definition,
                    boundSources);
            BatchResult result = openedController.autoBuildBatch(player, request, expectedFingerprint);
            lastExecution = TerminalExecutionSummary.from(result);
            player.displayClientMessage(Component.translatable("gtpm.multiblock.autobuild.execution_result",
                    Component.translatable("gtpm.multiblock.autobuild.status." +
                            result.status().name().toLowerCase(Locale.ROOT)),
                    result.placed(), result.removed()), true);
            return result;
        } catch (RuntimeException exception) {
            GTCEu.LOGGER.error("Terminal automatic-build execution failed for {}", definitionId, exception);
            player.displayClientMessage(Component.translatable("gtpm.multiblock.autobuild.execution_failed"), true);
            closeInvalidSession(player);
            return null;
        } finally {
            executing = false;
            if (!closed && isStillValid(player)) {
                revision++;
                data.setState(buildState(player));
            }
        }
    }

    /** Re-resolves material/world state; annotated synchronization compares the complete encoded value. */
    public void refresh(ServerPlayer player) {
        long currentGeneration = StructurePatternRegistry.generation();
        if (currentGeneration != patternGeneration) {
            if (!isValidMachine(definition, machine)) {
                data.getMachines().put(definitionId, TerminalMachine.defaults(definition));
                GTCEu.LOGGER.warn("Closed terminal menu after incompatible pattern reload for {}", definitionId);
                player.displayClientMessage(Component.translatable(
                        "gtpm.multiblock.autobuild.pattern_reloaded_incompatible"), false);
                player.closeContainer();
                return;
            }
            patternGeneration = currentGeneration;
            revision++;
        }
        data.setState(buildState(player));
    }

    public void close() {
        closed = true;
    }

    private boolean beginAction(ServerPlayer player, long sequence, long expectedRevision) {
        if (sequence != acceptedSequence + 1) {
            GTCEu.LOGGER.warn("Rejected invalid terminal session action for {}", player.getGameProfile().getName());
            return false;
        }
        acceptedSequence = sequence;
        if (expectedRevision != revision || executing) {
            data.setState(stateWithCurrentSequence());
            return false;
        }
        return true;
    }

    private TerminalMenuState stateWithCurrentSequence() {
        return new TerminalMenuState(definitionId, acceptedSequence, revision, machine, data.getState().preview(),
                executing,
                lastExecution);
    }

    private TerminalMenuState buildState(ServerPlayer player) {
        ObjectList<MaterialRequest.Result> sources = createSources(player);
        BatchRequest request = machine.createRequest(definition, sources);
        BatchResolution resolved = resolver.resolveBatch(openedController, player, request);
        boundSources = sources;
        StructurePreview preview = StructurePreview.bound(definitionId, resolved);
        return new TerminalMenuState(definitionId, acceptedSequence, revision, machine, preview, executing,
                lastExecution);
    }

    private ObjectList<MaterialRequest.Result> createSources(ServerPlayer player) {
        boolean hasBuild = machine.structures().values().stream()
                .anyMatch(structure -> structure.selected() && structure.mode() == StructureOperation.BUILD);
        if (!hasBuild) {
            return ObjectList.of();
        }
        boolean useME = machine.sharedOptions().useME();
        MaterialRequest request = new MaterialRequest(ObjectList.of(), openedTerminal,
                kind -> useME || kind != MaterialSource.SourceKind.ME, null);
        return MaterialSources.request(player, request);
    }

    private void ensureSemanticallyValid(TerminalMachine machine) {
        BatchPlan plan = resolver.resolveCanonical(definition, machine.createRequest(definition, ObjectList.of()));
        boolean invalid = plan.executionOrder().stream().flatMap(structure -> structure.problems().stream())
                .anyMatch(problem -> switch (problem.type()) {
                    case UNKNOWN_STRUCTURE, INVALID_OPTIONS, INVALID_TIER_SELECTION, PATTERN_UNAVAILABLE -> true;
                    default -> false;
                });
        if (invalid) {
            throw new IllegalArgumentException("Machine update does not resolve to a valid structure plan");
        }
    }

    private static TerminalMachine loadMachine(ServerPlayer player, TerminalData data,
                                               MultiblockMachineDefinition definition) {
        TerminalMachine machine = data.getMachines().get(definition.getId());
        if (machine == null || !isValidMachine(definition, machine)) {
            if (machine != null) {
                GTCEu.LOGGER.warn("Resetting stale terminal machine for {} held by {}", definition.getId(),
                        player.getGameProfile().getName());
            }
            machine = TerminalMachine.defaults(definition);
            data.getMachines().put(definition.getId(), machine);
        }
        return machine;
    }

    private static boolean isValidMachine(MultiblockMachineDefinition definition,
                                          TerminalMachine machine) {
        if (!new ObjectImmutableList<>(machine.structures().keySet()).equals(definition.getStructureOrder())) {
            return false;
        }
        BatchPlanner resolver = new BatchPlanner();
        try {
            for (String structureName : definition.getStructureOrder()) {
                var stored = machine.structures().get(structureName);
                var defaults = resolver.defaultStructureOptions(definition, structureName);
                if (stored.repetitions().size() != defaults.repetitions().size() ||
                        stored.flipMode() && !definition.isAllowFlip()) {
                    return false;
                }
                for (int index = 0; index < stored.repetitions().size(); index++) {
                    var limits = definition.getPattern(structureName).getRepeatDescriptors().get(index);
                    int repetition = stored.repetitions().getInt(index);
                    if (repetition < limits.minimum() || repetition > limits.maximum()) {
                        return false;
                    }
                }
                var tierOptions = resolver.tierChoiceOptions(definition, structureName);
                if (!stored.tierChoices().keySet().equals(tierOptions.keySet())) {
                    return false;
                }
                for (var tier : stored.tierChoices().entrySet()) {
                    if (!tierOptions.get(tier.getKey()).contains(tier.getValue())) {
                        return false;
                    }
                }
                var choices = definition.getPattern(structureName).getChoiceDescriptors();
                for (var selected : stored.choices().object2ObjectEntrySet()) {
                    if (choices.stream().noneMatch(choice -> choice.path().equals(selected.getKey()) &&
                            choice.alternatives().contains(selected.getValue())))
                        return false;
                }
                if (stored.selected() && stored.mode() == StructureOperation.BUILD) {
                    for (String dependency : definition.getRequiredStructures(structureName)) {
                        var requiredStructure = machine.structures().get(dependency);
                        if (!requiredStructure.selected() || requiredStructure.mode() != StructureOperation.BUILD) {
                            return false;
                        }
                    }
                }
            }
            machine.createRequest(definition, ObjectList.of());
            return true;
        } catch (IllegalArgumentException | IllegalStateException exception) {
            GTCEu.LOGGER.warn("Terminal machine validation failed for {}: {}", definition.getId(),
                    exception.getMessage());
            return false;
        }
    }

    private void closeInvalidSession(ServerPlayer player) {
        closed = true;
        if (player.containerMenu.containerId == data.getMenuId()) {
            player.closeContainer();
        }
    }
}
