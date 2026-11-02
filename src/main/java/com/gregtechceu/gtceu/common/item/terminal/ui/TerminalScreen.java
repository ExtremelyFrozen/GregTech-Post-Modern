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
import com.gregtechceu.gtceu.client.renderer.MultiblockInWorldPreviewRenderer;
import com.gregtechceu.gtceu.common.data.GTDataComponents;
import com.gregtechceu.gtceu.common.item.datacomponents.TerminalControllerBinding;
import com.gregtechceu.gtceu.common.item.datacomponents.TerminalMachine;
import com.gregtechceu.gtceu.common.item.terminal.menu.TerminalMenuState;
import com.gregtechceu.gtceu.common.item.terminal.menu.TerminalSession;
import com.gregtechceu.gtceu.common.item.terminal.network.TerminalAction;
import com.gregtechceu.gtceu.common.item.terminal.network.TerminalChange;
import com.gregtechceu.gtceu.common.item.terminal.network.TerminalStateUpdate;
import com.gregtechceu.gtceu.config.ConfigHolder;

import com.lowdragmc.lowdraglib2.gui.holder.ModularUIContainerScreen;
import com.lowdragmc.lowdraglib2.gui.sync.SyncValue;
import com.lowdragmc.lowdraglib2.gui.sync.rpc.RPCEmitter;
import com.lowdragmc.lowdraglib2.gui.sync.rpc.RPCEventBuilder;
import com.lowdragmc.lowdraglib2.gui.ui.UI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.data.TextWrap;
import com.lowdragmc.lowdraglib2.math.Size;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import it.unimi.dsi.fastutil.objects.ObjectList;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.Locale;
import java.util.UUID;

/** A per-opening terminal UI hosted by the existing held-item menu, with no separate menu registration. */
@NullMarked
public final class TerminalScreen extends UIElement {

    private static final int WIDTH = 300;
    private static final int HEIGHT = 200;

    private final Player player;
    private final TerminalControllerBinding binding;
    private final @Nullable TerminalSession serverSession;
    private final SyncValue<TerminalStateUpdate> stateSync = new SyncValue<>("terminal-state",
            TerminalStateUpdate.class,
            null);
    private final RPCEmitter actions;
    private final MultiblockPreviewPanel preview;
    private final StructureControls configuration;
    private final GTButtonElement executeButton = new GTButtonElement(188, 180, 108, 16);
    private final GTLabelElement sourceLabel = new GTLabelElement(192, 155, 100, 10);
    private final GTLabelElement resultLabel = new GTLabelElement(192, 167, 100, 10);
    private final GTButtonElement fullscreen = new GTButtonElement(278, 4, 16, 14);
    private final GTLabelElement title;
    private boolean expanded;
    private TerminalMenuState state;
    private @Nullable UUID sessionId;
    private long nextSequence = 1;
    private long pendingSequence = -1;
    private int refreshTicks;

    public TerminalScreen(HeldItemUIHolder holder, Player player) {
        this.player = player;
        binding = holder.getOpenedStack().get(GTDataComponents.TERMINAL_CONTROLLER);
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

        UITemplate.setBounds(this, 0, 0, WIDTH, HEIGHT);
        style(style -> style.backgroundTexture(GuiTextures.BACKGROUND));
        actions = addRPCEvent(RPCEventBuilder.simple(TerminalAction.class, this::receiveAction));
        stateSync.setAcceptSync(player.level().isClientSide);
        stateSync.setToSync(!player.level().isClientSide);
        stateSync.addListener(this::receiveState);
        addSyncValue(stateSync);

        title = new GTLabelElement(6, 6, 268, 10, definition.getBlock().getName());
        title.textStyle(style -> style.adaptiveWidth(false).adaptiveHeight(false).fontSize(9)
                .textColor(0xff404040).textShadow(false).textWrap(TextWrap.HIDE));
        title.style(style -> style.tooltips(Component.literal(binding.dimension().location() + " @ " +
                binding.position().toShortString())));
        addChild(title);
        preview = new MultiblockPreviewPanel(180, 176, state.preview(), true);
        preview.layout(layout -> layout.left(4).top(20));
        addChild(preview);
        configuration = new StructureControls(188, 20, 108, 132, definition, state.machine(), preview,
                GTCEu.MODS.AE2.isLoaded(),
                this::requestAction);
        addChild(configuration);
        if (player.level().isClientSide && definition.isRenderWorldPreview()) {
            configuration.setWorldPreview(
                    name -> MultiblockInWorldPreviewRenderer.isPreviewVisible(binding.position(), name),
                    name -> MultiblockInWorldPreviewRenderer.togglePreview(binding.position(), state.preview(), name,
                            ConfigHolder.INSTANCE.client.inWorldPreviewDuration * 20));
        }
        sourceLabel.textStyle(style -> style.adaptiveWidth(false).adaptiveHeight(false).fontSize(8)
                .textColor(0xff404040).textShadow(false).textWrap(TextWrap.HIDE));
        resultLabel.textStyle(style -> style.adaptiveWidth(false).adaptiveHeight(false).fontSize(8)
                .textColor(0xff404040).textShadow(false).textWrap(TextWrap.HIDE));
        addChild(sourceLabel);
        sourceLabel.setDisplay(GTCEu.MODS.AE2.isLoaded());
        addChild(resultLabel);
        executeButton.setButtonTexture(GuiTextures.BUTTON);
        executeButton.getLayout().paddingAll(1);
        executeButton.text.getLayout().marginAll(0).widthPercent(100).minWidth(0);
        executeButton.textStyle(style -> style.adaptiveWidth(false).adaptiveHeight(false).fontSize(9)
                .textColor(0xff404040).textShadow(false).textWrap(TextWrap.HIDE));
        executeButton.setText(Component.translatable("gtpm.multiblock.autobuild.terminal.execute"));
        executeButton.setOnClick(event -> {
            requestAction(null);
            event.stopPropagation();
        });
        addChild(executeButton);
        fullscreen.setButtonTexture(GuiTextures.BUTTON).setText(Component.literal("↗"));
        fullscreen.getLayout().paddingAll(0);
        fullscreen.textStyle(style -> style.fontSize(9).textColor(0xff404040).textShadow(false));
        fullscreen.getStyle().tooltips(Component.translatable("gtpm.multiblock.configuration.fullscreen"));
        fullscreen.setOnClick(event -> {
            expanded = !expanded;
            fullscreen.setText(Component.literal(expanded ? "↙" : "↗"));
            fullscreen.getStyle().tooltips(Component.translatable("gtpm.multiblock.configuration." +
                    (expanded ? "compact" : "fullscreen")));
            var ui = getModularUI();
            ui.init(ui.getScreenWidth(), ui.getScreenHeight());
            ((ModularUIContainerScreen) ui.getScreen()).init();
            event.stopPropagation();
        });
        addChild(fullscreen);
        updateDisplay();
        if (serverSession != null) publishState();
    }

    public UI createUI() {
        return UI.of(this, this::resize);
    }

    private Size resize(Size screen) {
        int width = expanded ? screen.getWidth() : Math.min(WIDTH, screen.getWidth());
        int height = expanded ? screen.getHeight() : Math.min(HEIGHT, screen.getHeight());
        title.getLayout().width(width - 32);
        fullscreen.getLayout().left(width - 22);
        preview.resize(width - 120, height - 24);
        configuration.resize(width - 112, 20, 108, height - (GTCEu.MODS.AE2.isLoaded() ? 68 : 56));
        sourceLabel.getLayout().left(width - 108).top(height - 45);
        resultLabel.getLayout().left(width - 108).top(height - 33);
        executeButton.getLayout().left(width - 112).top(height - 20);
        return Size.of(width, height);
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
        if (actions.send(new TerminalAction(sessionId, sequence, state.revision(), update))) {
            pendingSequence = sequence;
            nextSequence++;
            updateInteractivity();
        }
    }

    private void receiveAction(TerminalAction action) {
        if (!(player instanceof ServerPlayer serverPlayer) || serverSession == null || !installed()) return;
        try {
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
        stateSync.setValue(new TerminalStateUpdate(serverSession.sessionId(), state));
        stateSync.markAsChanged();
        updateDisplay();
    }

    private void receiveState(TerminalStateUpdate update) {
        if (!player.level().isClientSide) return;
        try {
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
        if (player.level().isClientSide && sessionId != null) {
            MultiblockInWorldPreviewRenderer.updatePreview(binding.position(), state.preview(),
                    ConfigHolder.INSTANCE.client.inWorldPreviewDuration * 20);
        }
        var source = state.meSource();
        sourceLabel.setValue(Component.translatable(source == null ?
                "gtpm.multiblock.autobuild.terminal.ae_unavailable" :
                "gtpm.multiblock.autobuild.terminal.ae_available"));
        sourceLabel.getStyle().tooltips(source == null ?
                Component.translatable("gtpm.multiblock.autobuild.terminal.ae_unavailable") :
                Component.translatable("gtpm.multiblock.autobuild.terminal.ae_source", source.terminalName(),
                        source.selectionIdentity(), source.linkTarget().dimension().location(),
                        source.linkTarget().pos().toShortString()));
        var result = state.lastExecution();
        resultLabel.setValue(result == null ? Component.translatable("gtpm.multiblock.autobuild.terminal.no_result") :
                Component.translatable("gtpm.multiblock.autobuild.status." +
                        result.status().name().toLowerCase(Locale.ROOT)));
        resultLabel.getStyle().tooltips(result == null ?
                Component.translatable("gtpm.multiblock.autobuild.terminal.no_result") :
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
}
