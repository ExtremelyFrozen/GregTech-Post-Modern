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
import com.gregtechceu.gtceu.common.item.datacomponents.TerminalControllerBinding;
import com.gregtechceu.gtceu.common.item.datacomponents.TerminalMachine;
import com.gregtechceu.gtceu.common.item.terminal.TerminalData;
import com.gregtechceu.gtceu.common.item.terminal.menu.TerminalMenuState;
import com.gregtechceu.gtceu.common.item.terminal.menu.TerminalSession;
import com.gregtechceu.gtceu.common.item.terminal.network.TerminalChange;
import com.gregtechceu.gtceu.config.ConfigHolder;

import com.lowdragmc.lowdraglib2.gui.holder.ModularUIContainerScreen;
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

/** A per-opening terminal UI hosted by the existing held-item menu, with no separate menu registration. */
@NullMarked
public final class TerminalScreen extends UIElement {

    private static final int WIDTH = 300;
    private static final int HEIGHT = 200;

    private final Player player;
    private final TerminalData data;
    private final TerminalControllerBinding binding;
    private final @Nullable TerminalSession serverSession;
    private final MultiblockPreviewPanel preview;
    private final StructureControls configuration;
    private final GTButtonElement executeButton = new GTButtonElement(188, 180, 108, 16);
    private final GTLabelElement resultLabel = new GTLabelElement(192, 167, 100, 10);
    private final GTButtonElement fullscreen = new GTButtonElement(278, 4, 16, 14);
    private final GTLabelElement title;
    private boolean expanded;
    private TerminalMenuState state;
    private long nextSequence = 1;
    private long pendingSequence = -1;
    private int refreshTicks;

    public TerminalScreen(HeldItemUIHolder holder, Player player) {
        this.player = player;
        data = (TerminalData) holder.getSyncHolder();
        binding = data.getBinding();
        var definition = (MultiblockMachineDefinition) GTRegistries.MACHINES.get(binding.definitionId());
        if (player instanceof ServerPlayer serverPlayer) {
            var controller = (MultiblockControllerMachine) MetaMachine.getMachine(player.level(), binding.position());
            serverSession = new TerminalSession(serverPlayer, holder.getHand(), data, controller);
            data.setSession(serverSession);
            state = data.getState();
        } else {
            serverSession = null;
            var emptyPreview = new StructurePreview(definition.getId(), "awaiting-server", false,
                    ObjectList.of(), ObjectList.of(), ObjectList.of(), ObjectList.of());
            state = new TerminalMenuState(definition.getId(), 0, 0, TerminalMachine.defaults(definition),
                    emptyPreview, false, null);
        }

        UITemplate.setBounds(this, 0, 0, WIDTH, HEIGHT);
        style(style -> style.backgroundTexture(GuiTextures.BACKGROUND));

        title = new GTLabelElement(6, 6, 268, 10, definition.getBlock().getName());
        title.textStyle(style -> style.adaptiveWidth(false).adaptiveHeight(false).fontSize(9)
                .textColor(0xff404040).textShadow(false).textWrap(TextWrap.HIDE));
        title.style(style -> style.tooltips(Component.literal(binding.dimension().location() + " @ " +
                binding.position().toShortString())));
        addChild(title);
        preview = new MultiblockPreviewPanel(180, 176, state.preview(), true);
        preview.layout(layout -> layout.left(4).top(20));
        addChild(preview);
        configuration = new StructureControls(188, 20, 108, 132, definition, state.machine(), preview, true,
                this::requestAction);
        addChild(configuration);
        if (player.level().isClientSide && definition.isRenderWorldPreview()) {
            configuration.setWorldPreview(
                    name -> MultiblockInWorldPreviewRenderer.isPreviewVisible(binding.position(), name),
                    name -> MultiblockInWorldPreviewRenderer.togglePreview(binding.position(), state.preview(), name,
                            ConfigHolder.INSTANCE.client.inWorldPreviewDuration * 20));
        }
        resultLabel.textStyle(style -> style.adaptiveWidth(false).adaptiveHeight(false).fontSize(8)
                .textColor(0xff404040).textShadow(false).textWrap(TextWrap.HIDE));
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
        data.setStateListener(this::receiveState);
        updateDisplay();
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
        configuration.resize(width - 112, 20, 108, height - 56);
        resultLabel.getLayout().left(width - 108).top(height - 33);
        executeButton.getLayout().left(width - 112).top(height - 20);
        return Size.of(width, height);
    }

    private boolean installed() {
        var ui = getModularUI();
        return ui != null && ui.getMenu() == player.containerMenu;
    }

    private void requestAction(@Nullable TerminalChange update) {
        if (!player.level().isClientSide || data.getState() == null || pendingSequence >= 0 || state.executing() ||
                !installed()) {
            return;
        }
        pendingSequence = nextSequence++;
        if (update == null) {
            data.rpcToServer("execute", pendingSequence, state.revision(),
                    Long.parseUnsignedLong(state.preview().fingerprint(), 16));
        } else {
            data.rpcToServer("change", pendingSequence, state.revision(), update);
        }
        updateInteractivity();
    }

    private void receiveState() {
        state = data.getState();
        if (state.acknowledgedSequence() >= pendingSequence) pendingSequence = -1;
        nextSequence = state.acknowledgedSequence() + 1;
        updateDisplay();
    }

    private void updateDisplay() {
        preview.setPreview(state.preview());
        configuration.setMachine(state.machine());
        if (player.level().isClientSide && data.getState() != null) {
            MultiblockInWorldPreviewRenderer.updatePreview(binding.position(), state.preview(),
                    ConfigHolder.INSTANCE.client.inWorldPreviewDuration * 20);
        }
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
        boolean ready = player.level().isClientSide && data.getState() != null && pendingSequence < 0 &&
                !state.executing() && installed();
        configuration.setInteractive(ready);
        executeButton.setActive(ready);
    }

    @Override
    public void serverTick() {
        super.serverTick();
        if (!(player instanceof ServerPlayer serverPlayer) || serverSession == null || !installed()) return;
        if (!serverSession.isStillValid(player)) {
            player.closeContainer();
            return;
        }
        if (++refreshTicks < 20) return;
        refreshTicks = 0;
        try {
            serverSession.refresh(serverPlayer);
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
        data.setStateListener(null);
        if (serverSession != null) serverSession.close();
        super.onRemoved();
    }
}
