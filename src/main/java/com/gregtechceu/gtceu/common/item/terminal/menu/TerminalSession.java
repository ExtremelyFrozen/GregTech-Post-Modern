package com.gregtechceu.gtceu.common.item.terminal.menu;

import com.gregtechceu.gtceu.GTCEu;
import com.gregtechceu.gtceu.api.machine.MetaMachine;
import com.gregtechceu.gtceu.api.machine.MultiblockMachineDefinition;
import com.gregtechceu.gtceu.api.machine.multiblock.MultiblockControllerMachine;
import com.gregtechceu.gtceu.api.multiblock.autobuild.BuildProblem;
import com.gregtechceu.gtceu.api.multiblock.autobuild.MaterialSource;
import com.gregtechceu.gtceu.api.multiblock.autobuild.MaterialSources;
import com.gregtechceu.gtceu.api.multiblock.autobuild.batch.BatchRequest;
import com.gregtechceu.gtceu.api.multiblock.autobuild.batch.BatchResult;
import com.gregtechceu.gtceu.api.multiblock.autobuild.batch.StructureOperation;
import com.gregtechceu.gtceu.api.multiblock.autobuild.plan.BatchPlan;
import com.gregtechceu.gtceu.api.multiblock.autobuild.plan.BatchPlanner;
import com.gregtechceu.gtceu.api.multiblock.autobuild.plan.BatchResolution;
import com.gregtechceu.gtceu.api.multiblock.preview.StructurePreview;
import com.gregtechceu.gtceu.common.data.GTDataComponents;
import com.gregtechceu.gtceu.common.item.datacomponents.TerminalControllerBinding;
import com.gregtechceu.gtceu.common.item.datacomponents.TerminalMachine;
import com.gregtechceu.gtceu.common.item.datacomponents.TerminalMachines;
import com.gregtechceu.gtceu.common.item.terminal.profile.TerminalChange;
import com.gregtechceu.gtceu.common.machine.owner.MachineOwner;
import com.gregtechceu.gtceu.data.pattern.StructurePatternRegistry;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectImmutableList;
import it.unimi.dsi.fastutil.objects.ObjectList;
import org.jspecify.annotations.Nullable;

import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

/** Captures one exact terminal, controller and session for a server-authoritative automatic-build menu. */
public final class TerminalSession {

    private final ServerPlayer player;
    private final InteractionHand hand;
    private final ItemStack openedTerminal;
    private final @Nullable TerminalControllerBinding openedBinding;
    private final MultiblockControllerMachine openedController;
    private final MultiblockMachineDefinition definition;
    private final BlockPos controllerPos;
    private final ResourceKey<Level> dimension;
    private final ResourceLocation definitionId;
    private final UUID sessionId;
    private final BatchPlanner resolver = new BatchPlanner();
    private TerminalMachine profile;
    private TerminalMenuState state;
    @Nullable
    private AbstractContainerMenu serverMenu;
    private long acceptedSequence;
    private long revision;
    private long visibleSignature;
    private boolean executing;
    private boolean closed;
    private long patternGeneration;
    @Nullable
    private TerminalExecutionSummary lastExecution;

    /** Captures the exact server objects used for every later validation. */
    public TerminalSession(ServerPlayer player, InteractionHand hand, ItemStack terminal,
                           MultiblockControllerMachine controller) {
        this.player = player;
        this.hand = hand;
        this.openedTerminal = terminal;
        this.openedBinding = terminal.get(GTDataComponents.TERMINAL_CONTROLLER);
        this.openedController = controller;
        this.definition = controller.getDefinition();
        this.controllerPos = controller.getBlockPos().immutable();
        this.dimension = player.level().dimension();
        this.definitionId = definition.getId();
        this.sessionId = UUID.randomUUID();
        this.profile = loadProfile(player, terminal, definition);
        patternGeneration = StructurePatternRegistry.generation();
        state = buildState(player);
        visibleSignature = visibleSignature(state);
    }

    public UUID sessionId() {
        return sessionId;
    }

    public TerminalMenuState state() {
        return state;
    }

    public MultiblockMachineDefinition definition() {
        return definition;
    }

    /** Attaches this session once to the generic held-item menu that owns its ModularUI. */
    public void attachMenu(AbstractContainerMenu menu) {
        if (serverMenu != null && serverMenu != menu) {
            throw new IllegalStateException("A terminal session cannot move to another menu");
        }
        serverMenu = menu;
    }

    public boolean isStillValid(Player player) {
        if (closed || this.player != player || player.isSpectator() ||
                !player.level().dimension().equals(dimension)) {
            return false;
        }
        ItemStack currentTerminal = player.getItemInHand(hand);
        if (!Objects.equals(openedBinding, currentTerminal.get(GTDataComponents.TERMINAL_CONTROLLER))) {
            return false;
        }
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

    /** Applies one sequence- and revision-validated profile patch and persists it immediately. */
    public boolean applyUpdate(ServerPlayer player, long sequence, long expectedRevision,
                               TerminalChange update) {
        if (!beginAction(player, sequence, expectedRevision)) {
            return false;
        }
        try {
            TerminalMachine updated = update.apply(definition, profile);
            ensureSemanticallyValid(updated);
            persistProfile(openedTerminal, definition, updated);
            profile = updated;
            revision = Math.addExact(revision, 1);
            state = buildState(player);
            visibleSignature = visibleSignature(state);
            return true;
        } catch (IllegalArgumentException | IllegalStateException exception) {
            GTCEu.LOGGER.warn("Rejected terminal profile update for {}: {}", definitionId, exception.getMessage());
            state = stateWithCurrentSequence();
            return false;
        } catch (RuntimeException exception) {
            GTCEu.LOGGER.error("Failed to apply terminal profile update for {}", definitionId, exception);
            closeInvalidSession(player);
            return false;
        }
    }

    /** Executes only the server plan whose fingerprint is still visible to this exact menu session. */
    @Nullable
    public BatchResult execute(ServerPlayer player, long sequence, long expectedRevision) {
        if (!beginAction(player, sequence, expectedRevision)) {
            return null;
        }
        String expectedFingerprint = state.preview().fingerprint();
        executing = true;
        state = new TerminalMenuState(definitionId, acceptedSequence, revision, profile, state.preview(), true,
                state.meSource(), lastExecution);
        try {
            MultiblockControllerMachine controller = openedController;
            BatchRequest request = profile.createRequest(definition,
                    createSources(player));
            long expectedPlanFingerprint = Long.parseUnsignedLong(expectedFingerprint, 16);
            BatchResult result = controller.autoBuildBatch(player, request, expectedPlanFingerprint);
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
                revision = Math.addExact(revision, 1);
                TerminalMenuState refreshed = buildState(player);
                state = withRevision(refreshed, revision);
                visibleSignature = visibleSignature(state);
            }
        }
    }

    /** Refreshes material/world state at the bounded menu polling cadence and returns whether it changed. */
    public boolean refresh(ServerPlayer player) {
        requireValid("during refresh");
        long currentGeneration = StructurePatternRegistry.generation();
        if (currentGeneration != patternGeneration) {
            if (!isValidProfile(definition, profile)) {
                TerminalMachines profiles = openedTerminal.getOrDefault(GTDataComponents.TERMINAL_AUTO_BUILD,
                        TerminalMachines.EMPTY);
                TerminalMachine reset = TerminalMachine.defaults(definition);
                openedTerminal.set(GTDataComponents.TERMINAL_AUTO_BUILD,
                        profiles.withProfile(definitionId, reset));
                GTCEu.LOGGER.warn("Closed terminal menu after incompatible pattern reload for {}", definitionId);
                player.displayClientMessage(Component.translatable(
                        "gtpm.multiblock.autobuild.pattern_reloaded_incompatible"), false);
                player.closeContainer();
                return false;
            }
            patternGeneration = currentGeneration;
            revision = Math.addExact(revision, 1);
            state = withRevision(buildState(player), revision);
            visibleSignature = visibleSignature(state);
            return true;
        }
        TerminalMenuState refreshed = buildState(player);
        long signature = visibleSignature(refreshed);
        if (signature == visibleSignature) {
            return false;
        }
        revision = Math.addExact(revision, 1);
        state = withRevision(refreshed, revision);
        visibleSignature = visibleSignature(state);
        return true;
    }

    public void close(Player player) {
        if (this.player != player) {
            throw new IllegalArgumentException("Terminal menu belongs to another player");
        }
        closed = true;
    }

    private boolean beginAction(ServerPlayer player, long sequence, long expectedRevision) {
        if (!isStillValid(player)) {
            GTCEu.LOGGER.warn("Closed invalid terminal session for {}", player.getGameProfile().getName());
            closeInvalidSession(player);
            return false;
        }
        if (player.containerMenu != serverMenu || sequence != acceptedSequence + 1) {
            GTCEu.LOGGER.warn("Rejected invalid terminal session action for {}", player.getGameProfile().getName());
            return false;
        }
        acceptedSequence = sequence;
        if (expectedRevision != revision || executing) {
            state = stateWithCurrentSequence();
            return false;
        }
        return true;
    }

    private TerminalMenuState stateWithCurrentSequence() {
        return new TerminalMenuState(definitionId, acceptedSequence, revision, profile, state.preview(), executing,
                state.meSource(), lastExecution);
    }

    private static TerminalMenuState withRevision(TerminalMenuState state, long revision) {
        return new TerminalMenuState(state.definitionId(), state.acknowledgedSequence(), revision, state.profile(),
                state.preview(), state.executing(), state.meSource(), state.lastExecution());
    }

    private TerminalMenuState buildState(ServerPlayer player) {
        ObjectList<MaterialSource> sources = createSources(player);
        MultiblockControllerMachine controller = openedController;
        BatchRequest request = profile.createRequest(definition, sources);
        BatchResolution resolved = resolver.resolveWorldSnapshot(controller, player, request);
        StructurePreview preview = StructurePreview.bound(definitionId, resolved);
        TerminalMESourceDisplay meSource = resolved.materialSnapshots().stream()
                .map(MaterialSource.Availability::descriptor)
                .filter(descriptor -> descriptor != null && descriptor.kind() == MaterialSource.SourceKind.ME)
                .map(TerminalMESourceDisplay::from)
                .findFirst().orElse(null);
        return new TerminalMenuState(definitionId, acceptedSequence, revision, profile, preview, executing, meSource,
                lastExecution);
    }

    private ObjectList<MaterialSource> createSources(ServerPlayer player) {
        boolean hasBuild = profile.structures().values().stream()
                .anyMatch(structure -> structure.selected() && structure.mode() == StructureOperation.BUILD);
        if (!hasBuild) {
            return ObjectList.of();
        }
        ObjectList<MaterialSource> sources = new ObjectArrayList<>();
        if (profile.sharedOptions().useME()) {
            if (GTCEu.MODS.AE2.isLoaded()) {
                sources.add(MaterialSources.me(player, openedTerminal));
            } else {
                sources.add(MaterialSources.unavailable(new BuildProblem(
                        BuildProblem.Type.AE_NOT_INSTALLED, null,
                        Component.translatable("gtpm.multiblock.autobuild.me_unavailable"))));
            }
        }
        sources.add(MaterialSources.playerInventory(player, openedTerminal));
        return new ObjectImmutableList<>(sources);
    }

    private void ensureSemanticallyValid(TerminalMachine profile) {
        BatchPlan plan = resolver.resolveCanonical(definition, profile.createRequest(definition, ObjectList.of()));
        boolean invalid = plan.executionOrder().stream().flatMap(structure -> structure.problems().stream())
                .anyMatch(problem -> switch (problem.type()) {
                    case UNKNOWN_STRUCTURE, INVALID_OPTIONS, INVALID_TIER_SELECTION, PATTERN_UNAVAILABLE -> true;
                    default -> false;
                });
        if (invalid) {
            throw new IllegalArgumentException("Profile update does not resolve to a valid structure plan");
        }
    }

    private static TerminalMachine loadProfile(ServerPlayer player, ItemStack terminal,
                                               MultiblockMachineDefinition definition) {
        TerminalMachines profiles = terminal.getOrDefault(GTDataComponents.TERMINAL_AUTO_BUILD,
                TerminalMachines.EMPTY);
        TerminalMachine profile = profiles.profiles().get(definition.getId());
        if (profile == null || !isValidProfile(definition, profile)) {
            if (profile != null) {
                GTCEu.LOGGER.warn("Resetting stale terminal profile for {} held by {}", definition.getId(),
                        player.getGameProfile().getName());
            }
            profile = TerminalMachine.defaults(definition);
            terminal.set(GTDataComponents.TERMINAL_AUTO_BUILD, profiles.withProfile(definition.getId(), profile));
        }
        return profile;
    }

    private static boolean isValidProfile(MultiblockMachineDefinition definition,
                                          TerminalMachine profile) {
        if (!new ObjectImmutableList<>(profile.structures().keySet()).equals(definition.getStructureOrder())) {
            return false;
        }
        BatchPlanner resolver = new BatchPlanner();
        try {
            for (String structureName : definition.getStructureOrder()) {
                var stored = profile.structures().get(structureName);
                var defaults = resolver.defaultStructureOptions(definition, structureName);
                if (stored.repetitions().size() != defaults.repetitions().size() ||
                        stored.flipMode() && !definition.isAllowFlip()) {
                    return false;
                }
                for (int index = 0; index < stored.repetitions().size(); index++) {
                    var limits = definition.getPattern(structureName).getRepeatDescriptors().get(index);
                    int repetition = stored.repetitions().get(index);
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
                        var dependencyProfile = profile.structures().get(dependency);
                        if (!dependencyProfile.selected() || dependencyProfile.mode() != StructureOperation.BUILD) {
                            return false;
                        }
                    }
                }
            }
            profile.createRequest(definition, ObjectList.of());
            return true;
        } catch (IllegalArgumentException | IllegalStateException exception) {
            GTCEu.LOGGER.warn("Terminal profile validation failed for {}: {}", definition.getId(),
                    exception.getMessage());
            return false;
        }
    }

    private static void persistProfile(ItemStack terminal, MultiblockMachineDefinition definition,
                                       TerminalMachine profile) {
        TerminalMachines profiles = terminal.getOrDefault(GTDataComponents.TERMINAL_AUTO_BUILD,
                TerminalMachines.EMPTY);
        terminal.set(GTDataComponents.TERMINAL_AUTO_BUILD, profiles.withProfile(definition.getId(), profile));
    }

    private static long visibleSignature(TerminalMenuState state) {
        StructurePreview preview = state.preview();
        long result = preview.fingerprint().hashCode();
        for (StructurePreview.Material material : preview.materials()) {
            result = 31 * result + Long.hashCode(material.required());
            result = 31 * result + Long.hashCode(material.meAvailable());
            result = 31 * result + Long.hashCode(material.meAllocated());
            result = 31 * result + Long.hashCode(material.playerAvailable());
            result = 31 * result + Long.hashCode(material.playerAllocated());
            result = 31 * result + Long.hashCode(material.missing());
            result = 31 * result + Boolean.hashCode(material.unlimited());
            for (ItemStack candidate : material.candidates()) {
                result = 31 * result + ItemStack.hashItemAndComponents(candidate);
            }
        }
        result = 31 * result + preview.diagnostics().hashCode();
        result = 31 * result + (state.meSource() == null ? 0 : state.meSource().hashCode());
        result = 31 * result + (state.lastExecution() == null ? 0 : state.lastExecution().hashCode());
        return result;
    }

    private void requireValid(String phase) {
        if (!isStillValid(player)) {
            throw new IllegalStateException("Terminal opening became invalid " + phase);
        }
    }

    private void closeInvalidSession(ServerPlayer player) {
        closed = true;
        if (player.containerMenu == serverMenu) {
            player.closeContainer();
        }
    }
}
