package com.gregtechceu.gtceu.api.gui.widget;

import com.gregtechceu.gtceu.GTCEu;
import com.gregtechceu.gtceu.api.gui.ColorPattern;
import com.gregtechceu.gtceu.api.gui.GuiTextures;
import com.gregtechceu.gtceu.api.gui.element.GTButtonElement;
import com.gregtechceu.gtceu.api.machine.MultiblockMachineDefinition;
import com.gregtechceu.gtceu.api.machine.multiblock.MultiblockControllerMachine;
import com.gregtechceu.gtceu.api.multiblock.autobuild.batch.AutoBuildBatchRequest;
import com.gregtechceu.gtceu.api.multiblock.autobuild.batch.AutoBuildMode;
import com.gregtechceu.gtceu.api.multiblock.autobuild.batch.AutoBuildSharedOptions;
import com.gregtechceu.gtceu.api.multiblock.autobuild.batch.AutoBuildStructureOptions;
import com.gregtechceu.gtceu.api.multiblock.autobuild.plan.AutoBuildPlan;
import com.gregtechceu.gtceu.api.multiblock.autobuild.plan.MultiblockPlanResolver;
import com.gregtechceu.gtceu.api.multiblock.pattern.match.MultiBlockPattern;
import com.gregtechceu.gtceu.api.multiblock.preview.MultiblockPreviewSnapshot;
import com.gregtechceu.gtceu.api.multiblock.preview.PatternGenerationGuard;
import com.gregtechceu.gtceu.data.pattern.StructurePatternRegistry;

import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.rendering.GUIContext;

import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.vfyjxf.taffy.style.TaffyPosition;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * XEI-local multiblock configuration page.
 *
 * <p>
 * Every recipe opening owns one instance and therefore one independent configuration. Changes re-run the shared
 * resolver locally and never write a terminal component or send a network action.
 * </p>
 */
@OnlyIn(Dist.CLIENT)
@NullMarked
public final class PatternPreviewWidget extends UIElement {

    private static final int SIZE = 160;
    private static final int MAX_PUBLICATION_ATTEMPTS = 8;

    private final MultiblockMachineDefinition definition;
    private final MultiblockPlanResolver resolver = new MultiblockPlanResolver();
    private final LinkedHashMap<String, AutoBuildStructureOptions> structureOptions = new LinkedHashMap<>();
    private final LinkedHashSet<String> selectedStructures = new LinkedHashSet<>();
    private final Map<String, List<ResourceLocation>> tierGroups = new LinkedHashMap<>();
    private final Map<String, Map<ResourceLocation, List<ResourceLocation>>> tierCandidates = new LinkedHashMap<>();
    private final List<GTButtonElement> optionButtons = new ArrayList<>();
    private final MultiblockPreviewPanel panel;
    private AutoBuildSharedOptions sharedOptions = AutoBuildSharedOptions.DEFAULT;
    private int structureCursor;
    private int repetitionCursor;
    private int tierGroupCursor;
    private long observedGeneration;
    private boolean reloadFailed;

    private PatternPreviewWidget(MultiblockMachineDefinition definition) {
        this.definition = definition;
        layout(layout -> {
            layout.positionType(TaffyPosition.ABSOLUTE);
            layout.width(SIZE);
            layout.height(SIZE);
        });
        setOverflowVisible(false);
        PatternGenerationGuard.Resolution<MultiblockPreviewSnapshot> initial = resolveStableSnapshot(true);
        panel = new MultiblockPreviewPanel(SIZE, SIZE,
                initial.value(), false);
        addChild(panel);
        addConfigurationButtons();
        updateButtonText();
        observedGeneration = initial.generation();
        if (StructurePatternRegistry.generation() != observedGeneration) {
            publishResolvedPreview(true);
        }
    }

    public static ModularUI createModularUI(MultiblockMachineDefinition definition) {
        return ModularUI.of(UI.of(getPatternWidget(definition)));
    }

    public static PatternPreviewWidget getPatternWidget(MultiblockMachineDefinition definition) {
        if (Minecraft.getInstance().level == null) {
            GTCEu.LOGGER.error("Tried to create a multiblock preview before the client level was loaded");
            throw new IllegalStateException("A client level is required for multiblock previews");
        }
        return new PatternPreviewWidget(definition);
    }

    private void resetConfiguration() {
        structureOptions.clear();
        selectedStructures.clear();
        tierGroups.clear();
        tierCandidates.clear();
        List<String> order = definition.getStructureOrder();
        if (order.isEmpty()) {
            throw new IllegalStateException("Multiblock definition has no structures: " + definition.getId());
        }
        for (String structureName : order) {
            structureOptions.put(structureName, resolver.defaultStructureOptions(definition, structureName));
            Map<ResourceLocation, List<ResourceLocation>> candidates = resolver.tierChoiceOptions(definition,
                    structureName);
            tierCandidates.put(structureName, candidates);
            tierGroups.put(structureName, List.copyOf(candidates.keySet()));
        }
        String main = order.contains(MultiblockControllerMachine.DEFAULT_STRUCTURE) ?
                MultiblockControllerMachine.DEFAULT_STRUCTURE : order.getFirst();
        selectedStructures.add(main);
        selectedStructures.addAll(definition.getRequiredStructures(main));
        sharedOptions = AutoBuildSharedOptions.DEFAULT;
        structureCursor = order.indexOf(main);
        repetitionCursor = 0;
        tierGroupCursor = 0;
        reloadFailed = false;
    }

    private void addConfigurationButtons() {
        optionButtons.add(addOptionButton(2, 2, "structure", this::nextStructure));
        optionButtons.add(addOptionButton(22, 2, "selected", this::toggleSelected));
        optionButtons.add(addOptionButton(42, 2, "mode", this::toggleMode));
        optionButtons.add(addOptionButton(62, 2, "flip", this::toggleFlip));
        optionButtons.add(addOptionButton(82, 2, "repeat_unit", this::nextRepetitionUnit));
        optionButtons.add(addOptionButton(102, 2, "repeat", this::nextRepetitionValue));
        optionButtons.add(addOptionButton(2, 22, "no_hatch", this::toggleNoHatch));
        optionButtons.add(addOptionButton(22, 22, "replace", this::toggleReplace));
        optionButtons.add(addOptionButton(42, 22, "me", this::toggleME));
        optionButtons.add(addOptionButton(62, 22, "tier_group", this::nextTierGroup));
        optionButtons.add(addOptionButton(82, 22, "tier_value", this::nextTierValue));
    }

    private GTButtonElement addOptionButton(int x, int y, String actionName, Runnable action) {
        GTButtonElement button = new GTButtonElement(x, y, 18, 18);
        button.textStyle(style -> {
            style.textColor(-1);
            style.textShadow(false);
        });
        button.setButtonTextures(ColorPattern.T_GRAY.rectTexture(),
                GuiTextures.group(ColorPattern.T_GRAY.rectTexture(), GuiTextures.colorRect(0x4fffffff)),
                ColorPattern.T_GRAY.rectTexture());
        button.style(style -> style.tooltips(
                Component.translatable("gtpm.multiblock.preview.tooltip." + actionName)));
        button.setOnClick(event -> {
            if (!reloadFailed) action.run();
            event.stopPropagation();
        });
        addChild(button);
        return button;
    }

    private void nextStructure() {
        structureCursor = (structureCursor + 1) % definition.getStructureOrder().size();
        repetitionCursor = 0;
        tierGroupCursor = 0;
        updateButtonText();
    }

    private void toggleSelected() {
        String structure = currentStructure();
        if (selectedStructures.contains(structure)) {
            if (selectedStructures.size() == 1 || requiredBySelectedBuild(structure)) {
                return;
            }
            selectedStructures.remove(structure);
        } else {
            selectedStructures.add(structure);
            AutoBuildStructureOptions options = optionsFor(structure);
            if (options.mode() == AutoBuildMode.BUILD) {
                selectBuildDependencies(structure);
            }
        }
        reResolve();
    }

    private void toggleMode() {
        String structure = currentStructure();
        AutoBuildStructureOptions options = optionsFor(structure);
        AutoBuildMode mode = options.mode() == AutoBuildMode.BUILD ? AutoBuildMode.DEMOLISH : AutoBuildMode.BUILD;
        if (mode == AutoBuildMode.DEMOLISH && requiredBySelectedBuild(structure)) {
            return;
        }
        replaceOptions(options, options.repetitions(), options.tierChoices(), options.flipMode(), mode);
        if (mode == AutoBuildMode.BUILD && selectedStructures.contains(structure)) {
            selectBuildDependencies(structure);
        }
        reResolve();
    }

    private void toggleFlip() {
        if (!definition.isAllowFlip()) {
            return;
        }
        AutoBuildStructureOptions options = currentOptions();
        replaceOptions(options, options.repetitions(), options.tierChoices(), !options.flipMode(), options.mode());
        reResolve();
    }

    private void nextRepetitionUnit() {
        List<Integer> repetitions = currentOptions().repetitions();
        if (!repetitions.isEmpty()) {
            repetitionCursor = (repetitionCursor + 1) % repetitions.size();
        }
        updateButtonText();
    }

    private void nextRepetitionValue() {
        AutoBuildStructureOptions options = currentOptions();
        if (options.repetitions().isEmpty()) {
            return;
        }
        MultiBlockPattern pattern = definition.getPattern(options.structureName());
        int[] limits = pattern.aisleRepetitions[repetitionCursor];
        List<Integer> repetitions = new ArrayList<>(options.repetitions());
        int current = repetitions.get(repetitionCursor);
        repetitions.set(repetitionCursor, current >= limits[1] ? limits[0] : current + 1);
        replaceOptions(options, repetitions, options.tierChoices(), options.flipMode(), options.mode());
        reResolve();
    }

    private void toggleNoHatch() {
        sharedOptions = new AutoBuildSharedOptions(sharedOptions.replaceMode(), !sharedOptions.noHatchMode(),
                sharedOptions.useME());
        reResolve();
    }

    private void toggleReplace() {
        sharedOptions = new AutoBuildSharedOptions(!sharedOptions.replaceMode(), sharedOptions.noHatchMode(),
                sharedOptions.useME());
        reResolve();
    }

    private void toggleME() {
        sharedOptions = new AutoBuildSharedOptions(sharedOptions.replaceMode(), sharedOptions.noHatchMode(),
                !sharedOptions.useME());
        reResolve();
    }

    private void nextTierGroup() {
        List<ResourceLocation> groups = tierGroups.getOrDefault(currentStructure(), List.of());
        if (!groups.isEmpty()) {
            tierGroupCursor = (tierGroupCursor + 1) % groups.size();
        }
        updateButtonText();
    }

    private void nextTierValue() {
        List<ResourceLocation> groups = tierGroups.getOrDefault(currentStructure(), List.of());
        if (groups.isEmpty()) {
            return;
        }
        ResourceLocation group = groups.get(Math.min(tierGroupCursor, groups.size() - 1));
        List<ResourceLocation> candidates = tierCandidates.getOrDefault(
                currentStructure(), Map.of()).getOrDefault(group, List.of());
        if (candidates.isEmpty()) {
            return;
        }
        AutoBuildStructureOptions options = currentOptions();
        @Nullable
        ResourceLocation current = options.tierChoices().get(group);
        int index = current == null ? -1 : candidates.indexOf(current);
        ResourceLocation next = candidates.get((index + 1) % candidates.size());
        Map<ResourceLocation, ResourceLocation> choices = new LinkedHashMap<>(options.tierChoices());
        choices.put(group, next);
        replaceOptions(options, options.repetitions(), choices, options.flipMode(), options.mode());
        reResolve();
    }

    private void selectBuildDependencies(String structure) {
        for (String dependency : definition.getRequiredStructures(structure)) {
            selectedStructures.add(dependency);
            AutoBuildStructureOptions options = optionsFor(dependency);
            if (options.mode() != AutoBuildMode.BUILD) {
                replaceOptions(options, options.repetitions(), options.tierChoices(), options.flipMode(),
                        AutoBuildMode.BUILD);
            }
        }
    }

    private boolean requiredBySelectedBuild(String dependency) {
        for (String structure : selectedStructures) {
            if (structure.equals(dependency)) {
                continue;
            }
            AutoBuildStructureOptions options = optionsFor(structure);
            if (options.mode() == AutoBuildMode.BUILD &&
                    definition.getRequiredStructures(structure).contains(dependency)) {
                return true;
            }
        }
        return false;
    }

    private void replaceOptions(AutoBuildStructureOptions previous, List<Integer> repetitions,
                                Map<ResourceLocation, ResourceLocation> tiers, boolean flip, AutoBuildMode mode) {
        structureOptions.put(previous.structureName(), new AutoBuildStructureOptions(
                previous.structureName(), repetitions, tiers, flip, mode));
    }

    private void reResolve() {
        publishResolvedPreview(false);
    }

    private PatternGenerationGuard.Resolution<MultiblockPreviewSnapshot> resolveStableSnapshot(
                                                                                               boolean resetFirstAttempt) {
        return PatternGenerationGuard.resolve(StructurePatternRegistry::generation, attempt -> {
            if (resetFirstAttempt || attempt > 0) {
                resetConfiguration();
            }
            AutoBuildPlan plan = resolve();
            return MultiblockPreviewSnapshot.canonical(definition.getId(), plan);
        });
    }

    private void publishResolvedPreview(boolean resetFirstAttempt) {
        for (int attempt = 0; attempt < MAX_PUBLICATION_ATTEMPTS; attempt++) {
            PatternGenerationGuard.Resolution<MultiblockPreviewSnapshot> resolved = resolveStableSnapshot(
                    resetFirstAttempt || attempt > 0);
            if (StructurePatternRegistry.generation() != resolved.generation()) {
                continue;
            }
            panel.updateSnapshot(resolved.value());
            if (StructurePatternRegistry.generation() == resolved.generation()) {
                observedGeneration = resolved.generation();
                updateButtonText();
                return;
            }
        }
        throw new IllegalStateException("Pattern generation did not stabilize while publishing the XEI preview");
    }

    private AutoBuildPlan resolve() {
        List<AutoBuildStructureOptions> selected = definition.getStructureOrder().stream()
                .filter(selectedStructures::contains)
                .map(this::optionsFor)
                .toList();
        return resolver.resolveCanonical(definition, new AutoBuildBatchRequest(selected, sharedOptions, List.of()));
    }

    private String currentStructure() {
        return definition.getStructureOrder().get(structureCursor);
    }

    private AutoBuildStructureOptions currentOptions() {
        return optionsFor(currentStructure());
    }

    private AutoBuildStructureOptions optionsFor(String structureName) {
        @Nullable
        AutoBuildStructureOptions options = structureOptions.get(structureName);
        if (options == null) {
            throw new IllegalStateException("Missing XEI options for structure: " + structureName);
        }
        return options;
    }

    private void updateButtonText() {
        AutoBuildStructureOptions options = currentOptions();
        boolean dependencyLocked = requiredBySelectedBuild(options.structureName());
        boolean selectionLocked = selectedStructures.contains(options.structureName()) &&
                (selectedStructures.size() == 1 || dependencyLocked);
        optionButtons.get(0).setText(Component.translatable("gtpm.multiblock.preview.button.structure",
                abbreviate(options.structureName())));
        optionButtons.get(1).setText(selectionLocked ?
                Component.translatable("gtpm.multiblock.preview.button.selected.locked") :
                Component.translatable("gtpm.multiblock.preview.button.selected." +
                        selectedStructures.contains(options.structureName())));
        optionButtons.get(1).style(style -> style.tooltips(Component.translatable(selectionLocked ?
                "gtpm.multiblock.preview.tooltip.selected_locked" :
                "gtpm.multiblock.preview.tooltip.selected")));
        optionButtons.get(2).setText(dependencyLocked ?
                Component.translatable("gtpm.multiblock.preview.button.mode.locked") :
                Component.translatable("gtpm.multiblock.preview.button.mode." +
                        options.mode().name().toLowerCase(Locale.ROOT)));
        optionButtons.get(2).style(style -> style.tooltips(Component.translatable(dependencyLocked ?
                "gtpm.multiblock.preview.tooltip.mode_locked" :
                "gtpm.multiblock.preview.tooltip.mode")));
        optionButtons.get(3).setText(Component.translatable("gtpm.multiblock.preview.button.flip." +
                options.flipMode()));
        optionButtons.get(4).setText(Component.translatable(
                "gtpm.multiblock.preview.button.repeat_unit", repetitionCursor));
        optionButtons.get(5).setText(options.repetitions().isEmpty() ?
                Component.translatable("gtpm.multiblock.preview.button.repeat_none") :
                Component.translatable("gtpm.multiblock.preview.button.repeat",
                        options.repetitions().get(Math.min(repetitionCursor, options.repetitions().size() - 1))));
        optionButtons.get(6).setText(Component.translatable("gtpm.multiblock.preview.button.no_hatch." +
                sharedOptions.noHatchMode()));
        optionButtons.get(7).setText(Component.translatable("gtpm.multiblock.preview.button.replace." +
                sharedOptions.replaceMode()));
        optionButtons.get(8).setText(Component.translatable("gtpm.multiblock.preview.button.me." +
                sharedOptions.useME()));
        List<ResourceLocation> groups = tierGroups.getOrDefault(options.structureName(), List.of());
        if (groups.isEmpty()) {
            optionButtons.get(9).setText(Component.translatable("gtpm.multiblock.preview.button.tier_group_none"));
            optionButtons.get(10).setText(Component.translatable("gtpm.multiblock.preview.button.tier_value_none"));
            optionButtons.get(9).style(style -> style.tooltips(
                    Component.translatable("gtpm.multiblock.preview.tooltip.tier_group")));
            optionButtons.get(10).style(style -> style.tooltips(
                    Component.translatable("gtpm.multiblock.preview.tooltip.tier_value")));
        } else {
            ResourceLocation group = groups.get(Math.min(tierGroupCursor, groups.size() - 1));
            optionButtons.get(9).setText(Component.translatable(
                    "gtpm.multiblock.preview.button.tier_group", abbreviate(group.getPath())));
            optionButtons.get(9).style(style -> style.tooltips(Component.translatable(
                    "gtpm.multiblock.preview.tooltip.tier_group_selected", group.toString())));
            List<ResourceLocation> candidates = tierCandidates.getOrDefault(
                    options.structureName(), Map.of()).getOrDefault(group, List.of());
            @Nullable
            ResourceLocation selected = options.tierChoices().get(group);
            optionButtons.get(10).setText(selected == null || !candidates.contains(selected) ?
                    Component.translatable("gtpm.multiblock.preview.button.tier_value_none") :
                    Component.translatable("gtpm.multiblock.preview.button.tier_value",
                            abbreviate(selected.getPath())));
            if (selected != null && candidates.contains(selected)) {
                @Nullable
                Block selectedBlock = BuiltInRegistries.BLOCK.get(selected);
                if (selectedBlock == null) {
                    throw new IllegalStateException("Unknown tier candidate block: " + selected);
                }
                optionButtons.get(10).style(style -> style.tooltips(Component.translatable(
                        "gtpm.multiblock.preview.tooltip.tier_value_selected",
                        selectedBlock.getName(), selected.toString())));
            } else {
                optionButtons.get(10).style(style -> style.tooltips(
                        Component.translatable("gtpm.multiblock.preview.tooltip.tier_value")));
            }
        }
    }

    private void showReloadFailure(long generation, RuntimeException exception) {
        reloadFailed = true;
        GTCEu.LOGGER.warn("Multiblock preview became incompatible after pattern reload for {}",
                definition.getId(), exception);
        MultiblockPreviewSnapshot failure = new MultiblockPreviewSnapshot(definition.getId(),
                "reload-" + generation, false, List.of(), List.of(), List.of(),
                List.of(new MultiblockPreviewSnapshot.Diagnostic("PATTERN_RELOAD_INCOMPATIBLE", null, null)));
        panel.updateSnapshot(failure);
        for (GTButtonElement button : optionButtons) {
            button.setText(Component.translatable("gtpm.multiblock.preview.button.unavailable"));
            button.style(style -> style.tooltips(
                    Component.translatable("gtpm.multiblock.preview.diagnostic.pattern_reload_incompatible")));
        }
    }

    private static String abbreviate(String value) {
        return value.length() <= 4 ? value : value.substring(0, 4);
    }

    @Override
    public void screenTick() {
        super.screenTick();
        long generation = StructurePatternRegistry.generation();
        if (generation != observedGeneration) {
            try {
                publishResolvedPreview(true);
            } catch (RuntimeException exception) {
                showReloadFailure(generation, exception);
                observedGeneration = StructurePatternRegistry.generation();
            }
        }
    }

    @Override
    public void drawBackgroundAdditional(GUIContext context) {
        RenderSystem.enableBlend();
        super.drawBackgroundAdditional(context);
    }
}
