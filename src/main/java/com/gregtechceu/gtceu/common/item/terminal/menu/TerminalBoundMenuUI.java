package com.gregtechceu.gtceu.common.item.terminal.menu;

import com.gregtechceu.gtceu.api.gui.GuiTextures;
import com.gregtechceu.gtceu.api.gui.UITemplate;
import com.gregtechceu.gtceu.api.gui.element.GTButtonElement;
import com.gregtechceu.gtceu.api.gui.element.GTLabelElement;
import com.gregtechceu.gtceu.api.gui.element.GTScrollerViewElement;
import com.gregtechceu.gtceu.api.gui.widget.MultiblockPreviewPanel;
import com.gregtechceu.gtceu.api.multiblock.autobuild.batch.AutoBuildMode;
import com.gregtechceu.gtceu.api.multiblock.autobuild.plan.MultiblockPlanResolver;
import com.gregtechceu.gtceu.api.multiblock.pattern.match.MultiBlockPattern;
import com.gregtechceu.gtceu.common.item.datacomponents.TerminalStructureProfile;
import com.gregtechceu.gtceu.common.item.terminal.profile.TerminalProfileUpdate;

import com.lowdragmc.lowdraglib2.gui.ui.UI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

import java.util.IdentityHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Builds the two-sided terminal tree and routes every mutation through its bound menu session. */
final class TerminalBoundMenuUI {

    private static final int WIDTH = 360;
    private static final int HEIGHT = 240;
    private static final int PREVIEW_WIDTH = 226;

    private final TerminalBoundMenuHolder holder;
    private final Player player;
    private final UIElement root = new UIElement();
    private final MultiblockPreviewPanel previewPanel;
    private final GTScrollerViewElement controls = new GTScrollerViewElement(234, 28, 122, 190);
    private final GTLabelElement sourceLabel = new GTLabelElement(234, 4, 122, 10);
    private final GTLabelElement resultLabel = new GTLabelElement(234, 15, 122, 10);
    private final GTButtonElement executeButton;
    private final Map<GTButtonElement, Boolean> actionButtons = new IdentityHashMap<>();
    private TerminalMenuState state;

    TerminalBoundMenuUI(TerminalBoundMenuHolder holder, Player player) {
        this.holder = holder;
        this.player = player;
        this.state = holder.state();
        UITemplate.setLDLib2Bounds(root, 0, 0, WIDTH, HEIGHT);
        root.style(style -> style.backgroundTexture(GuiTextures.BACKGROUND));

        previewPanel = new MultiblockPreviewPanel(PREVIEW_WIDTH, 232, state.preview(), true);
        previewPanel.layout(layout -> {
            layout.left(4);
            layout.top(4);
        });
        root.addChild(previewPanel);
        root.addChild(sourceLabel);
        root.addChild(resultLabel);
        root.addChild(controls);

        executeButton = button(234, 220, 122, 16,
                Component.translatable("gtpm.multiblock.autobuild.terminal.execute"));
        executeButton.setOnClick(event -> {
            if (player.level().isClientSide && player.containerMenu instanceof TerminalBoundMenu menu) {
                menu.requestExecute();
            }
            event.stopPropagation();
        });
        root.addChild(executeButton);
        rebuildControls();
        updateLabels();
        updateInteractivity();
    }

    UI create() {
        return UI.of(root);
    }

    void update(TerminalMenuState update) {
        state = update;
        previewPanel.updateSnapshot(update.preview());
        rebuildControls();
        updateLabels();
        updateInteractivity();
    }

    void updateInteractivity() {
        boolean sessionReady = player.containerMenu instanceof TerminalBoundMenu menu &&
                !menu.hasPendingAction() && !state.executing();
        executeButton.setActive(sessionReady);
        actionButtons.forEach((button, enabled) -> button.setActive(sessionReady && enabled));
    }

    private void rebuildControls() {
        controls.clearAllScrollViewChildren();
        actionButtons.clear();
        int y = 0;
        var shared = state.profile().sharedOptions();
        controls.addScrollViewChild(controlButton(0, y, 118,
                Component.translatable("gtpm.multiblock.autobuild.terminal.replace", onOff(shared.replaceMode())),
                TerminalProfileUpdate.sharedReplace(!shared.replaceMode()), true));
        y += 17;
        controls.addScrollViewChild(controlButton(0, y, 118,
                Component.translatable("gtpm.multiblock.autobuild.terminal.no_hatch",
                        onOff(shared.noHatchMode())),
                TerminalProfileUpdate.sharedNoHatch(!shared.noHatchMode()), true));
        y += 17;
        controls.addScrollViewChild(controlButton(0, y, 118,
                Component.translatable("gtpm.multiblock.autobuild.terminal.ae", onOff(shared.useME())),
                TerminalProfileUpdate.sharedME(!shared.useME()), true));
        y += 20;

        MultiblockPlanResolver resolver = new MultiblockPlanResolver();
        for (String structureName : holder.definition().getStructureOrder()) {
            TerminalStructureProfile profile = state.profile().structures().get(structureName);
            if (profile == null) {
                continue;
            }
            boolean locked = isRequiredBuildDependency(structureName);
            String selectionKey = locked ? "gtpm.multiblock.autobuild.terminal.structure_locked" :
                    profile.selected() ? "gtpm.multiblock.autobuild.terminal.structure_selected" :
                            "gtpm.multiblock.autobuild.terminal.structure_unselected";
            controls.addScrollViewChild(controlButton(0, y, 58, Component.translatable(selectionKey, structureName),
                    TerminalProfileUpdate.structureSelected(structureName, !profile.selected()), !locked));
            controls.addScrollViewChild(controlButton(60, y, 58,
                    Component.translatable("gtpm.multiblock.autobuild.terminal.mode." +
                            profile.mode().name().toLowerCase(Locale.ROOT)),
                    TerminalProfileUpdate.structureMode(structureName,
                            profile.mode() == AutoBuildMode.BUILD ? AutoBuildMode.DEMOLISH : AutoBuildMode.BUILD),
                    !locked));
            y += 17;
            controls.addScrollViewChild(controlButton(0, y, 118,
                    Component.translatable("gtpm.multiblock.autobuild.terminal.flip", onOff(profile.flipMode())),
                    TerminalProfileUpdate.structureFlip(structureName, !profile.flipMode()),
                    holder.definition().isAllowFlip()));
            y += 17;

            MultiBlockPattern pattern = holder.definition().getPattern(structureName);
            for (int index = 0; index < profile.repetitions().size(); index++) {
                int value = profile.repetitions().get(index);
                int[] limits = pattern.aisleRepetitions[index];
                controls.addScrollViewChild(controlButton(0, y, 20, Component.literal("-"),
                        TerminalProfileUpdate.repetition(structureName, index, value - 1), value > limits[0]));
                controls.addScrollViewChild(new GTLabelElement(22, y + 3, 74, 10,
                        Component.translatable("gtpm.multiblock.autobuild.terminal.repeat", index, value)));
                controls.addScrollViewChild(controlButton(98, y, 20, Component.literal("+"),
                        TerminalProfileUpdate.repetition(structureName, index, value + 1), value < limits[1]));
                y += 17;
            }

            Map<ResourceLocation, List<ResourceLocation>> tierOptions = resolver.tierChoiceOptions(
                    holder.definition(), structureName);
            for (var tier : tierOptions.entrySet()) {
                ResourceLocation current = profile.tierChoices().get(tier.getKey());
                int currentIndex = tier.getValue().indexOf(current);
                if (current == null || currentIndex < 0 || tier.getValue().isEmpty()) {
                    continue;
                }
                ResourceLocation next = tier.getValue().get((currentIndex + 1) % tier.getValue().size());
                controls.addScrollViewChild(controlButton(0, y, 118,
                        Component.translatable("gtpm.multiblock.autobuild.terminal.tier",
                                tier.getKey().getPath(), current.getPath()),
                        TerminalProfileUpdate.tier(structureName, tier.getKey(), next), tier.getValue().size() > 1));
                y += 17;
            }
            y += 3;
        }
        int contentHeight = Math.max(190, y);
        controls.viewContainer.layout(layout -> layout.width(118).height(contentHeight));
    }

    private boolean isRequiredBuildDependency(String structureName) {
        return state.profile().structures().entrySet().stream()
                .anyMatch(entry -> entry.getValue().selected() && entry.getValue().mode() == AutoBuildMode.BUILD &&
                        holder.definition().getRequiredStructures(entry.getKey()).contains(structureName));
    }

    private void updateLabels() {
        TerminalMESourceDisplay source = state.meSource();
        sourceLabel.setValue(source == null ?
                Component.translatable("gtpm.multiblock.autobuild.terminal.ae_unavailable") :
                Component.translatable("gtpm.multiblock.autobuild.terminal.ae_source", source.terminalName(),
                        source.selectionIdentity(), source.linkTarget().dimension().location(),
                        source.linkTarget().pos().toShortString()));
        TerminalExecutionSummary result = state.lastExecution();
        resultLabel.setValue(result == null ?
                Component.translatable("gtpm.multiblock.autobuild.terminal.no_result") :
                Component.translatable("gtpm.multiblock.autobuild.terminal.result",
                        Component.translatable("gtpm.multiblock.autobuild.status." +
                                result.status().name().toLowerCase(Locale.ROOT)),
                        result.placed(), result.removed()));
    }

    private UIElement controlButton(int x, int y, int width, Component text, TerminalProfileUpdate update,
                                    boolean enabled) {
        GTButtonElement button = button(x, y, width, 16, text);
        actionButtons.put(button, enabled);
        button.setOnClick(event -> {
            if (player.level().isClientSide && player.containerMenu instanceof TerminalBoundMenu menu) {
                menu.requestUpdate(update);
            }
            event.stopPropagation();
        });
        return button;
    }

    private static GTButtonElement button(int x, int y, int width, int height, Component text) {
        GTButtonElement button = new GTButtonElement(x, y, width, height);
        button.setButtonTexture(GuiTextures.BUTTON);
        button.setText(text);
        button.textStyle(style -> style.textShadow(false));
        return button;
    }

    private static Component onOff(boolean value) {
        return Component.translatable(value ? "gtpm.multiblock.autobuild.terminal.on" :
                "gtpm.multiblock.autobuild.terminal.off");
    }
}
