package com.gregtechceu.gtceu.api.gui.widget;

import com.gregtechceu.gtceu.GTCEu;
import com.gregtechceu.gtceu.api.gui.element.GTLabelElement;
import com.gregtechceu.gtceu.api.machine.MultiblockMachineDefinition;
import com.gregtechceu.gtceu.api.multiblock.autobuild.plan.MultiblockPlanResolver;
import com.gregtechceu.gtceu.api.multiblock.preview.MultiblockPreviewSnapshot;
import com.gregtechceu.gtceu.api.multiblock.preview.PatternGenerationGuard;
import com.gregtechceu.gtceu.common.item.datacomponents.TerminalMachineProfile;
import com.gregtechceu.gtceu.common.item.terminal.profile.TerminalProfileUpdate;
import com.gregtechceu.gtceu.common.item.terminal.ui.MultiblockConfigurationPanel;
import com.gregtechceu.gtceu.data.pattern.StructurePatternRegistry;

import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.rendering.GUIContext;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.vfyjxf.taffy.style.TaffyPosition;
import it.unimi.dsi.fastutil.objects.ObjectList;
import org.jspecify.annotations.NullMarked;

/**
 * One XEI opening owns its configuration and scene. The same controls and validation are used by the terminal,
 * while this local page resolves against representative materials and never changes a held item.
 */
@OnlyIn(Dist.CLIENT)
@NullMarked
public final class PatternPreviewWidget extends UIElement {

    public static final int WIDTH = 360;
    public static final int HEIGHT = 240;

    private final MultiblockMachineDefinition definition;
    private final MultiblockPlanResolver resolver = new MultiblockPlanResolver();
    private final MultiblockPreviewPanel preview;
    private final MultiblockConfigurationPanel configuration;
    private TerminalMachineProfile profile;
    private long observedGeneration;

    private PatternPreviewWidget(MultiblockMachineDefinition definition) {
        this.definition = definition;
        profile = TerminalMachineProfile.defaults(definition);
        layout(layout -> layout.positionType(TaffyPosition.ABSOLUTE).width(WIDTH).height(HEIGHT));
        setOverflowVisible(false);
        var initial = resolveSnapshot(false);
        observedGeneration = initial.generation();

        addChild(new GTLabelElement(4, 5, 222, 10, definition.getBlock().getName()));
        preview = new MultiblockPreviewPanel(226, 216, initial.value(), false);
        preview.layout(layout -> layout.left(4).top(20));
        addChild(preview);
        configuration = new MultiblockConfigurationPanel(234, 28, 122, 208, definition, profile, preview,
                this::applyChange);
        addChild(configuration);
    }

    public static ModularUI createModularUI(MultiblockMachineDefinition definition) {
        return ModularUI.of(UI.of(new PatternPreviewWidget(definition)));
    }

    private void applyChange(TerminalProfileUpdate update) {
        try {
            profile = update.apply(definition, profile);
            publishSnapshot(false);
        } catch (RuntimeException exception) {
            showFailure(exception);
        }
    }

    private PatternGenerationGuard.Resolution<MultiblockPreviewSnapshot> resolveSnapshot(boolean reset) {
        return PatternGenerationGuard.resolve(StructurePatternRegistry::generation, attempt -> {
            if (reset || attempt > 0) profile = TerminalMachineProfile.defaults(definition);
            var plan = resolver.resolveCanonical(definition, profile.createRequest(definition, ObjectList.of()));
            return MultiblockPreviewSnapshot.canonical(definition.getId(), plan);
        });
    }

    private void publishSnapshot(boolean reset) {
        var resolved = resolveSnapshot(reset);
        observedGeneration = resolved.generation();
        preview.updateSnapshot(resolved.value());
        configuration.updateProfile(profile);
        configuration.setInteractive(true);
    }

    private void showFailure(RuntimeException exception) {
        observedGeneration = StructurePatternRegistry.generation();
        GTCEu.LOGGER.error("Failed to resolve multiblock preview for {}", definition.getId(), exception);
        preview.updateSnapshot(new MultiblockPreviewSnapshot(definition.getId(),
                "reload-" + observedGeneration, false, ObjectList.of(), ObjectList.of(), ObjectList.of(),
                ObjectList.of(new MultiblockPreviewSnapshot.Diagnostic("PATTERN_RELOAD_INCOMPATIBLE", null, null))));
        configuration.setInteractive(false);
    }

    @Override
    public void screenTick() {
        super.screenTick();
        if (StructurePatternRegistry.generation() != observedGeneration) {
            try {
                publishSnapshot(true);
            } catch (RuntimeException exception) {
                showFailure(exception);
            }
        }
    }

    @Override
    public void drawBackgroundAdditional(GUIContext context) {
        RenderSystem.enableBlend();
        super.drawBackgroundAdditional(context);
    }
}
