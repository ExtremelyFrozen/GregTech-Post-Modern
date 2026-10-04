package com.gregtechceu.gtceu.common.item.terminal.menu;

import com.gregtechceu.gtceu.GTCEu;
import com.gregtechceu.gtceu.api.machine.MetaMachine;
import com.gregtechceu.gtceu.api.machine.MultiblockMachineDefinition;
import com.gregtechceu.gtceu.api.machine.multiblock.MultiblockControllerMachine;
import com.gregtechceu.gtceu.api.multiblock.autobuild.AutoBuildMaterialSource;
import com.gregtechceu.gtceu.api.multiblock.autobuild.AutoBuildMaterialSources;
import com.gregtechceu.gtceu.api.multiblock.autobuild.AutoBuildProblem;
import com.gregtechceu.gtceu.api.multiblock.autobuild.batch.AutoBuildBatchRequest;
import com.gregtechceu.gtceu.api.multiblock.autobuild.batch.AutoBuildBatchResult;
import com.gregtechceu.gtceu.api.multiblock.autobuild.batch.AutoBuildMode;
import com.gregtechceu.gtceu.api.multiblock.autobuild.plan.AutoBuildPlan;
import com.gregtechceu.gtceu.api.multiblock.autobuild.plan.MultiblockPlanResolver;
import com.gregtechceu.gtceu.api.multiblock.autobuild.plan.ResolvedAutoBuildSnapshot;
import com.gregtechceu.gtceu.api.multiblock.preview.MultiblockPreviewSnapshot;
import com.gregtechceu.gtceu.api.registry.GTRegistries;
import com.gregtechceu.gtceu.common.data.GTDataComponents;
import com.gregtechceu.gtceu.common.item.datacomponents.TerminalAutoBuildProfiles;
import com.gregtechceu.gtceu.common.item.datacomponents.TerminalMachineProfile;
import com.gregtechceu.gtceu.common.item.terminal.profile.TerminalProfileUpdate;
import com.gregtechceu.gtceu.common.machine.owner.MachineOwner;
import com.gregtechceu.gtceu.data.pattern.StructurePatternRegistry;

import com.lowdragmc.lowdraglib2.gui.factory.IContainerUIHolder;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/** Captures one exact terminal, controller and session for a server-authoritative automatic-build menu. */
public final class TerminalBoundMenuHolder implements MenuProvider, IContainerUIHolder {

    private final Player player;
    private final InteractionHand hand;
    private final ItemStack openedTerminal;
    private final @Nullable MultiblockControllerMachine openedController;
    private final MultiblockMachineDefinition definition;
    private final BlockPos controllerPos;
    private final ResourceKey<Level> dimension;
    private final ResourceLocation definitionId;
    private final UUID sessionId;
    private final MultiblockPlanResolver resolver = new MultiblockPlanResolver();
    private TerminalMachineProfile profile;
    private TerminalMenuState state;
    @Nullable
    private TerminalBoundMenu serverMenu;
    @Nullable
    private TerminalBoundMenuUI openedUI;
    private long acceptedSequence;
    private long revision;
    private long visibleSignature;
    private boolean executing;
    private boolean closed;
    private long patternGeneration;
    @Nullable
    private TerminalExecutionSummary lastExecution;

    /** Captures the exact server objects used for every later validation. */
    public TerminalBoundMenuHolder(ServerPlayer player, InteractionHand hand, ItemStack terminal,
                                   MultiblockControllerMachine controller) {
        this.player = player;
        this.hand = hand;
        this.openedTerminal = terminal;
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

    private TerminalBoundMenuHolder(Player player, InteractionHand hand, ItemStack openedTerminal,
                                    MultiblockMachineDefinition definition, BlockPos controllerPos,
                                    ResourceKey<Level> dimension, UUID sessionId, TerminalMenuState openingState) {
        this.player = player;
        this.hand = hand;
        this.openedTerminal = openedTerminal;
        this.openedController = null;
        this.definition = definition;
        this.controllerPos = controllerPos.immutable();
        this.dimension = dimension;
        this.definitionId = definition.getId();
        this.sessionId = sessionId;
        this.profile = openingState.profile();
        this.state = openingState;
        acceptedSequence = openingState.acknowledgedSequence();
        revision = openingState.revision();
        executing = openingState.executing();
        lastExecution = openingState.lastExecution();
        patternGeneration = StructurePatternRegistry.generation();
        visibleSignature = visibleSignature(openingState);
    }

    /** Reconstructs the client holder solely from server-owned opening identity and state. */
    static TerminalBoundMenuHolder client(Player player, InteractionHand hand, BlockPos pos,
                                          ResourceKey<Level> dimension, ResourceLocation definitionId,
                                          UUID sessionId, TerminalMenuState state) {
        if (!state.definitionId().equals(definitionId)) {
            throw new IllegalArgumentException("Terminal opening state has the wrong machine definition");
        }
        var registered = GTRegistries.MACHINES.get(definitionId);
        if (!(registered instanceof MultiblockMachineDefinition definition)) {
            throw new IllegalArgumentException("Terminal opening references an unknown multiblock definition: " +
                    definitionId);
        }
        return new TerminalBoundMenuHolder(player, hand, player.getItemInHand(hand), definition, pos, dimension,
                sessionId, state);
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

    @Override
    public Component getDisplayName() {
        return definition.getBlock().getName();
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        if (!(player instanceof ServerPlayer) || this.player != player || serverMenu != null) {
            throw new IllegalStateException("A terminal holder can create exactly one server menu");
        }
        requireValid("before menu creation");
        TerminalBoundMenu menu = new TerminalBoundMenu(containerId, inventory, this);
        serverMenu = menu;
        requireValid("after menu creation");
        return menu;
    }

    @Override
    public void writeClientSideData(AbstractContainerMenu menu, RegistryFriendlyByteBuf buffer) {
        if (menu != serverMenu) {
            throw new IllegalStateException("Terminal opening data belongs to another server menu");
        }
        buffer.writeEnum(hand);
        buffer.writeBlockPos(controllerPos);
        buffer.writeResourceLocation(dimension.location());
        buffer.writeResourceLocation(definitionId);
        buffer.writeUUID(sessionId);
        TerminalMenuState.STREAM_CODEC.encode(buffer, state);
    }

    @Override
    public ModularUI createUI(Player player) {
        if (this.player != player || openedUI != null || !isStillValid(player)) {
            throw new IllegalStateException("Terminal holder rejected UI creation");
        }
        openedUI = new TerminalBoundMenuUI(this, player);
        return ModularUI.of(openedUI.create(), player);
    }

    @Override
    public boolean isStillValid(Player player) {
        if (closed || this.player != player || player.isSpectator() ||
                !player.level().dimension().equals(dimension)) {
            return false;
        }
        ItemStack currentTerminal = player.getItemInHand(hand);
        if (player.level().isClientSide) {
            return ItemStack.isSameItem(currentTerminal, openedTerminal);
        }
        if (openedController == null || !player.level().isLoaded(controllerPos) ||
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
    boolean applyUpdate(ServerPlayer player, long sequence, long expectedRevision, TerminalProfileUpdate update) {
        if (!beginAction(player, sequence, expectedRevision)) {
            return false;
        }
        try {
            TerminalMachineProfile updated = update.apply(definition, profile);
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
    AutoBuildBatchResult execute(ServerPlayer player, long sequence, long expectedRevision) {
        if (!beginAction(player, sequence, expectedRevision)) {
            return null;
        }
        String expectedFingerprint = state.fingerprint();
        executing = true;
        state = new TerminalMenuState(definitionId, acceptedSequence, revision, profile, state.preview(), true,
                state.meSource(), lastExecution);
        try {
            MultiblockControllerMachine controller = serverController();
            AutoBuildBatchRequest request = profile.createRequest(definition,
                    createSources(player));
            long expectedPlanFingerprint = Long.parseUnsignedLong(expectedFingerprint, 16);
            AutoBuildBatchResult result = controller.autoBuildBatch(player, request, expectedPlanFingerprint);
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
    boolean refresh(ServerPlayer player) {
        requireValid("during refresh");
        long currentGeneration = StructurePatternRegistry.generation();
        if (currentGeneration != patternGeneration) {
            if (!isValidProfile(definition, profile)) {
                TerminalAutoBuildProfiles profiles = openedTerminal.getOrDefault(GTDataComponents.TERMINAL_AUTO_BUILD,
                        TerminalAutoBuildProfiles.EMPTY);
                TerminalMachineProfile reset = TerminalMachineProfile.defaults(definition);
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

    /** Installs a session-validated state update on the client. */
    boolean applyClientState(Player player, TerminalMenuState update) {
        if (!player.level().isClientSide || this.player != player || !isStillValid(player) ||
                !definitionId.equals(update.definitionId()) ||
                update.acknowledgedSequence() < state.acknowledgedSequence() || update.revision() < state.revision()) {
            return false;
        }
        state = update;
        acceptedSequence = update.acknowledgedSequence();
        revision = update.revision();
        executing = update.executing();
        lastExecution = update.lastExecution();
        visibleSignature = visibleSignature(update);
        if (openedUI != null) {
            openedUI.update(update);
        }
        return true;
    }

    void close(Player player) {
        if (this.player != player) {
            throw new IllegalArgumentException("Terminal menu belongs to another player");
        }
        closed = true;
    }

    void updateClientInteractivity() {
        if (openedUI != null) {
            openedUI.updateInteractivity();
        }
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
        List<AutoBuildMaterialSource> sources = createSources(player);
        MultiblockControllerMachine controller = serverController();
        AutoBuildBatchRequest request = profile.createRequest(definition, sources);
        ResolvedAutoBuildSnapshot resolved = resolver.resolveWorldSnapshot(controller, player, request);
        MultiblockPreviewSnapshot preview = MultiblockPreviewSnapshot.bound(definitionId, resolved);
        TerminalMESourceDisplay meSource = resolved.materialSnapshots().stream()
                .map(AutoBuildMaterialSource.Snapshot::descriptor)
                .filter(descriptor -> descriptor != null && descriptor.kind() == AutoBuildMaterialSource.SourceKind.ME)
                .map(TerminalMESourceDisplay::from)
                .findFirst().orElse(null);
        return new TerminalMenuState(definitionId, acceptedSequence, revision, profile, preview, executing, meSource,
                lastExecution);
    }

    private List<AutoBuildMaterialSource> createSources(ServerPlayer player) {
        boolean hasBuild = profile.structures().values().stream()
                .anyMatch(structure -> structure.selected() && structure.mode() == AutoBuildMode.BUILD);
        if (!hasBuild) {
            return List.of();
        }
        List<AutoBuildMaterialSource> sources = new ArrayList<>();
        if (profile.sharedOptions().useME()) {
            if (GTCEu.Mods.isAE2Loaded()) {
                sources.add(AutoBuildMaterialSources.me(player, openedTerminal));
            } else {
                sources.add(AutoBuildMaterialSources.unavailable(new AutoBuildProblem(
                        AutoBuildProblem.Type.AE_NOT_INSTALLED, null,
                        Component.translatable("gtpm.multiblock.autobuild.me_unavailable"))));
            }
        }
        sources.add(AutoBuildMaterialSources.playerInventory(player, openedTerminal));
        return List.copyOf(sources);
    }

    private void ensureSemanticallyValid(TerminalMachineProfile profile) {
        AutoBuildPlan plan = resolver.resolveCanonical(definition, profile.createRequest(definition, List.of()));
        boolean invalid = plan.executionOrder().stream().flatMap(structure -> structure.problems().stream())
                .anyMatch(problem -> switch (problem.type()) {
                    case UNKNOWN_STRUCTURE, INVALID_OPTIONS, INVALID_TIER_SELECTION, PATTERN_UNAVAILABLE -> true;
                    default -> false;
                });
        if (invalid) {
            throw new IllegalArgumentException("Profile update does not resolve to a valid structure plan");
        }
    }

    private static TerminalMachineProfile loadProfile(ServerPlayer player, ItemStack terminal,
                                                      MultiblockMachineDefinition definition) {
        TerminalAutoBuildProfiles profiles = terminal.getOrDefault(GTDataComponents.TERMINAL_AUTO_BUILD,
                TerminalAutoBuildProfiles.EMPTY);
        TerminalMachineProfile profile = profiles.profiles().get(definition.getId());
        if (profile == null || !isValidProfile(definition, profile)) {
            if (profile != null) {
                GTCEu.LOGGER.warn("Resetting stale terminal profile for {} held by {}", definition.getId(),
                        player.getGameProfile().getName());
            }
            profile = TerminalMachineProfile.defaults(definition);
            terminal.set(GTDataComponents.TERMINAL_AUTO_BUILD, profiles.withProfile(definition.getId(), profile));
        }
        return profile;
    }

    private static boolean isValidProfile(MultiblockMachineDefinition definition,
                                          TerminalMachineProfile profile) {
        if (!List.copyOf(profile.structures().keySet()).equals(definition.getStructureOrder())) {
            return false;
        }
        MultiblockPlanResolver resolver = new MultiblockPlanResolver();
        try {
            for (String structureName : definition.getStructureOrder()) {
                var stored = profile.structures().get(structureName);
                var defaults = resolver.defaultStructureOptions(definition, structureName);
                if (stored.repetitions().size() != defaults.repetitions().size() ||
                        stored.flipMode() && !definition.isAllowFlip()) {
                    return false;
                }
                for (int index = 0; index < stored.repetitions().size(); index++) {
                    int[] limits = definition.getPattern(structureName).aisleRepetitions[index];
                    int repetition = stored.repetitions().get(index);
                    if (repetition < limits[0] || repetition > limits[1]) {
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
                if (stored.selected() && stored.mode() == AutoBuildMode.BUILD) {
                    for (String dependency : definition.getRequiredStructures(structureName)) {
                        var dependencyProfile = profile.structures().get(dependency);
                        if (!dependencyProfile.selected() || dependencyProfile.mode() != AutoBuildMode.BUILD) {
                            return false;
                        }
                    }
                }
            }
            profile.createRequest(definition, List.of());
            return true;
        } catch (IllegalArgumentException | IllegalStateException exception) {
            GTCEu.LOGGER.warn("Terminal profile validation failed for {}: {}", definition.getId(),
                    exception.getMessage());
            return false;
        }
    }

    private static void persistProfile(ItemStack terminal, MultiblockMachineDefinition definition,
                                       TerminalMachineProfile profile) {
        TerminalAutoBuildProfiles profiles = terminal.getOrDefault(GTDataComponents.TERMINAL_AUTO_BUILD,
                TerminalAutoBuildProfiles.EMPTY);
        terminal.set(GTDataComponents.TERMINAL_AUTO_BUILD, profiles.withProfile(definition.getId(), profile));
    }

    private static long visibleSignature(TerminalMenuState state) {
        MultiblockPreviewSnapshot preview = state.preview();
        long result = preview.fingerprint().hashCode();
        for (MultiblockPreviewSnapshot.Material material : preview.materials()) {
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

    private MultiblockControllerMachine serverController() {
        if (openedController == null) {
            throw new IllegalStateException("Client terminal holder has no authoritative controller instance");
        }
        return openedController;
    }
}
