package com.gregtechceu.gtceu.common.item.terminal.ui;

import com.gregtechceu.gtceu.GTCEu;
import com.gregtechceu.gtceu.api.gui.GuiTextures;
import com.gregtechceu.gtceu.api.gui.UITemplate;
import com.gregtechceu.gtceu.api.gui.element.GTButtonElement;
import com.gregtechceu.gtceu.api.gui.element.GTLabelElement;
import com.gregtechceu.gtceu.api.gui.factory.HeldItemUIHolder;
import com.gregtechceu.gtceu.api.gui.widget.MultiblockPreviewPanel;
import com.gregtechceu.gtceu.api.machine.MetaMachine;
import com.gregtechceu.gtceu.api.machine.MultiblockMachineDefinition;
import com.gregtechceu.gtceu.api.machine.multiblock.MultiblockControllerMachine;
import com.gregtechceu.gtceu.api.multiblock.preview.StructurePreview;
import com.gregtechceu.gtceu.api.registry.GTRegistries;
import com.gregtechceu.gtceu.common.data.GTDataComponents;
import com.gregtechceu.gtceu.common.item.datacomponents.TerminalControllerBinding;
import com.gregtechceu.gtceu.common.item.datacomponents.TerminalMachine;
import com.gregtechceu.gtceu.common.item.terminal.menu.TerminalMenuState;
import com.gregtechceu.gtceu.common.item.terminal.menu.TerminalSession;
import com.gregtechceu.gtceu.common.item.terminal.network.TerminalAction;
import com.gregtechceu.gtceu.common.item.terminal.network.TerminalChange;
import com.gregtechceu.gtceu.common.item.terminal.network.TerminalStateUpdate;

import com.lowdragmc.lowdraglib2.gui.sync.SyncValue;
import com.lowdragmc.lowdraglib2.gui.sync.rpc.RPCEmitter;
import com.lowdragmc.lowdraglib2.gui.sync.rpc.RPCEventBuilder;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import io.netty.buffer.Unpooled;
import it.unimi.dsi.fastutil.objects.ObjectList;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.Locale;
import java.util.UUID;

/** A per-opening terminal UI hosted by the existing held-item menu, with no separate menu registration. */
@NullMarked
public final class TerminalScreen extends UIElement {

    private static final int WIDTH = 360;
    private static final int HEIGHT = 240;

    private final Player player;
    private final @Nullable TerminalSession serverSession;
    private final SyncValue<byte[]> stateSync = new SyncValue<>("terminal-state", byte[].class, new byte[0]);
    private final RPCEmitter actions;
    private final MultiblockPreviewPanel preview;
    private final MultiblockConfigurationPanel configuration;
    private final GTButtonElement executeButton = new GTButtonElement(234, 220, 122, 16);
    private final GTLabelElement sourceLabel = new GTLabelElement(234, 4, 122, 10);
    private final GTLabelElement resultLabel = new GTLabelElement(234, 15, 122, 10);
    private TerminalMenuState state;
    private @Nullable UUID sessionId;
    private long nextSequence = 1;
    private long pendingSequence = -1;
    private int refreshTicks;

    public TerminalScreen(HeldItemUIHolder holder, Player player) {
        this.player = player;
        TerminalControllerBinding binding = holder.getOpenedStack().get(GTDataComponents.TERMINAL_CONTROLLER);
        if (binding == null || !(GTRegistries.MACHINES
                .get(binding.definitionId()) instanceof MultiblockMachineDefinition definition)) {
            throw new IllegalArgumentException("Terminal UI requires a registered bound multiblock");
        }
        if (player instanceof ServerPlayer serverPlayer) {
            if (!(MetaMachine.getMachine(player.level(),
                    binding.position()) instanceof MultiblockControllerMachine controller)) {
                throw new IllegalStateException("Bound controller disappeared before terminal UI creation");
            }
            serverSession = new TerminalSession(serverPlayer, holder.getHand(), holder.getHeld(), controller);
            state = serverSession.state();
        } else {
            serverSession = null;
            var emptyPreview = new StructurePreview(definition.getId(), "awaiting-server", false,
                    ObjectList.of(), ObjectList.of(), ObjectList.of(), ObjectList.of());
            state = new TerminalMenuState(definition.getId(), 0, 0, TerminalMachine.defaults(definition),
                    emptyPreview, false, null, null);
        }

        UITemplate.setLDLib2Bounds(this, 0, 0, WIDTH, HEIGHT);
        style(style -> style.backgroundTexture(GuiTextures.BACKGROUND));
        actions = addRPCEvent(RPCEventBuilder.simple(byte[].class, this::receiveAction));
        stateSync.setAcceptSync(player.level().isClientSide);
        stateSync.setToSync(!player.level().isClientSide);
        stateSync.addListener(this::receiveState);
        addSyncValue(stateSync);

        GTLabelElement title = new GTLabelElement(6, 5, 220, 10, definition.getBlock().getName());
        title.style(style -> style.tooltips(Component.literal(binding.dimension().location() + " @ " +
                binding.position().toShortString())));
        addChild(title);
        preview = new MultiblockPreviewPanel(226, 216, state.preview(), true);
        preview.layout(layout -> layout.left(4).top(20));
        addChild(preview);
        configuration = new MultiblockConfigurationPanel(234, 28, 122, 190, definition, state.machine(), preview,
                change -> requestAction(change));
        addChild(configuration);
        addChild(sourceLabel);
        addChild(resultLabel);
        executeButton.setButtonTexture(GuiTextures.BUTTON);
        executeButton.textStyle(style -> style.textShadow(false));
        executeButton.setText(Component.translatable("gtpm.multiblock.autobuild.terminal.execute"));
        executeButton.setOnClick(event -> {
            requestAction(null);
            event.stopPropagation();
        });
        addChild(executeButton);
        updateDisplay();
        if (serverSession != null) publishState();
    }

    private boolean installed() {
        var ui = getModularUI();
        return ui != null && ui.getMenu() == player.containerMenu;
    }

    private void requestAction(@Nullable TerminalChange update) {
        if (!player.level().isClientSide || sessionId == null || pendingSequence >= 0 || state.executing() ||
                !installed()) {
            return;
        }
        long sequence = nextSequence;
        if (actions.send(encode(TerminalAction.STREAM_CODEC,
                new TerminalAction(sessionId, sequence, state.revision(), update)))) {
            pendingSequence = sequence;
            nextSequence++;
            updateInteractivity();
        }
    }

    private void receiveAction(byte[] bytes) {
        if (!(player instanceof ServerPlayer serverPlayer) || serverSession == null || !installed()) return;
        try {
            if (bytes.length > 16_384)
                throw new IllegalArgumentException("Terminal action exceeds its bounded payload");
            TerminalAction action = decode(TerminalAction.STREAM_CODEC, bytes);
            if (!serverSession.sessionId().equals(action.sessionId())) {
                throw new IllegalArgumentException("Terminal action belongs to another opening");
            }
            serverSession.attachMenu(player.containerMenu);
            if (action.update() == null)
                serverSession.execute(serverPlayer, action.sequence(), action.expectedRevision());
            else serverSession.applyUpdate(serverPlayer, action.sequence(), action.expectedRevision(), action.update());
            if (installed()) publishState();
        } catch (RuntimeException exception) {
            GTCEu.LOGGER.error("Closed terminal UI after an invalid action for {}", state.definitionId(), exception);
            player.closeContainer();
        }
    }

    private void publishState() {
        if (serverSession == null) throw new IllegalStateException("Only the server publishes terminal state");
        state = serverSession.state();
        stateSync.setValue(encode(TerminalStateUpdate.STREAM_CODEC,
                new TerminalStateUpdate(serverSession.sessionId(), state)));
        stateSync.markAsChanged();
        updateDisplay();
    }

    private void receiveState(byte[] bytes) {
        if (!player.level().isClientSide || bytes.length == 0) return;
        try {
            TerminalStateUpdate update = decode(TerminalStateUpdate.STREAM_CODEC, bytes);
            if (sessionId != null && !sessionId.equals(update.sessionId()) ||
                    !state.definitionId().equals(update.state().definitionId()) ||
                    update.state().revision() < state.revision() ||
                    update.state().acknowledgedSequence() < state.acknowledgedSequence()) {
                throw new IllegalArgumentException("Terminal state has a stale or different session identity");
            }
            sessionId = update.sessionId();
            state = update.state();
            if (state.acknowledgedSequence() >= pendingSequence) pendingSequence = -1;
            nextSequence = Math.max(nextSequence, state.acknowledgedSequence() + 1);
            updateDisplay();
        } catch (RuntimeException exception) {
            GTCEu.LOGGER.error("Closed terminal UI after an invalid state for {}", state.definitionId(), exception);
            player.closeContainer();
        }
    }

    private void updateDisplay() {
        preview.setPreview(state.preview());
        configuration.setMachine(state.machine());
        var source = state.meSource();
        sourceLabel.setValue(source == null ?
                Component.translatable("gtpm.multiblock.autobuild.terminal.ae_unavailable") :
                Component.translatable("gtpm.multiblock.autobuild.terminal.ae_source", source.terminalName(),
                        source.selectionIdentity(), source.linkTarget().dimension().location(),
                        source.linkTarget().pos().toShortString()));
        var result = state.lastExecution();
        resultLabel.setValue(result == null ? Component.translatable("gtpm.multiblock.autobuild.terminal.no_result") :
                Component.translatable("gtpm.multiblock.autobuild.terminal.result",
                        Component.translatable("gtpm.multiblock.autobuild.status." +
                                result.status().name().toLowerCase(Locale.ROOT)),
                        result.placed(), result.removed()));
        updateInteractivity();
    }

    private void updateInteractivity() {
        boolean ready = player.level().isClientSide && sessionId != null && pendingSequence < 0 &&
                !state.executing() && installed();
        configuration.setInteractive(ready);
        executeButton.setActive(ready);
    }

    @Override
    public void serverTick() {
        super.serverTick();
        if (!(player instanceof ServerPlayer serverPlayer) || serverSession == null || !installed()) return;
        serverSession.attachMenu(player.containerMenu);
        if (!serverSession.isStillValid(player)) {
            player.closeContainer();
            return;
        }
        if (++refreshTicks < 20) return;
        refreshTicks = 0;
        try {
            if (serverSession.refresh(serverPlayer) && installed()) publishState();
        } catch (RuntimeException exception) {
            GTCEu.LOGGER.error("Closed terminal UI after refresh failed for {}", state.definitionId(), exception);
            player.closeContainer();
        }
    }

    @Override
    public void screenTick() {
        super.screenTick();
        updateInteractivity();
    }

    @Override
    protected void onRemoved() {
        if (serverSession != null) serverSession.close(player);
        super.onRemoved();
    }

    private <T> byte[] encode(StreamCodec<RegistryFriendlyByteBuf, T> codec, T value) {
        RegistryFriendlyByteBuf buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), player.registryAccess());
        try {
            codec.encode(buffer, value);
            byte[] bytes = new byte[buffer.readableBytes()];
            buffer.readBytes(bytes);
            return bytes;
        } finally {
            buffer.release();
        }
    }

    private <T> T decode(StreamCodec<RegistryFriendlyByteBuf, T> codec, byte[] bytes) {
        RegistryFriendlyByteBuf buffer = new RegistryFriendlyByteBuf(Unpooled.wrappedBuffer(bytes),
                player.registryAccess());
        try {
            T value = codec.decode(buffer);
            if (buffer.isReadable()) throw new IllegalArgumentException("Terminal payload contains trailing data");
            return value;
        } finally {
            buffer.release();
        }
    }
}
