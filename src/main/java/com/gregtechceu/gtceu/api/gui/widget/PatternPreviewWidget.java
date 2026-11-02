package com.gregtechceu.gtceu.api.gui.widget;

import com.gregtechceu.gtceu.GTCEu;
import com.gregtechceu.gtceu.api.gui.GuiTextures;
import com.gregtechceu.gtceu.api.gui.UITemplate;
import com.gregtechceu.gtceu.api.gui.element.GTButtonElement;
import com.gregtechceu.gtceu.api.gui.element.GTLabelElement;
import com.gregtechceu.gtceu.api.machine.MultiblockMachineDefinition;
import com.gregtechceu.gtceu.api.multiblock.preview.StructurePreview;
import com.gregtechceu.gtceu.common.item.terminal.network.TerminalChange;
import com.gregtechceu.gtceu.common.item.terminal.ui.StructureControls;
import com.gregtechceu.gtceu.data.pattern.StructurePatternRegistry;
import com.gregtechceu.gtceu.integration.xei.multiblock.MultiblockInfo;

import com.lowdragmc.lowdraglib2.gui.holder.ModularUIScreen;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.data.TextWrap;
import com.lowdragmc.lowdraglib2.gui.ui.rendering.GUIContext;
import com.lowdragmc.lowdraglib2.math.Size;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
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

    public static final int WIDTH = 300;
    public static final int HEIGHT = 200;

    private final MultiblockMachineDefinition definition;
    private final MultiblockInfo info;
    private final MultiblockPreviewPanel preview;
    private final StructureControls configuration;
    private final GTLabelElement title;
    private final GTButtonElement fullscreen = new GTButtonElement(258, 4, 16, 14);
    private final boolean expanded;
    private long observedGeneration;

    private PatternPreviewWidget(MultiblockInfo info, boolean expanded) {
        this.info = info;
        this.definition = info.getDefinition();
        this.expanded = expanded;
        UITemplate.setBounds(this, 0, 0, WIDTH, HEIGHT);
        getLayout().positionType(TaffyPosition.RELATIVE);
        getStyle().backgroundTexture(GuiTextures.BACKGROUND);
        setOverflowVisible(false);
        var initial = info.resolvePreview();
        observedGeneration = initial.generation();

        title = new GTLabelElement(6, 6, 246, 10, definition.getBlock().getName());
        title.textStyle(style -> style.adaptiveWidth(false).adaptiveHeight(false).fontSize(9)
                .textColor(0xff404040).textShadow(false).textWrap(TextWrap.HIDE));
        title.getStyle().tooltips(definition.getBlock().getName());
        addChild(title);
        preview = new MultiblockPreviewPanel(180, 176, initial.value(), false);
        preview.layout(layout -> layout.left(4).top(20));
        addChild(preview);
        configuration = new StructureControls(188, 20, 108, 176, definition, info.getMachine(), preview, false,
                this::applyChange);
        addChild(configuration);
        fullscreen.setButtonTexture(GuiTextures.BUTTON).setText(Component.literal(expanded ? "↙" : "↗"));
        fullscreen.getLayout().paddingAll(0);
        fullscreen.textStyle(style -> style.fontSize(9).textColor(0xff404040).textShadow(false));
        fullscreen.getStyle().tooltips(Component.translatable("gtpm.multiblock.configuration." +
                (expanded ? "compact" : "fullscreen")));
        fullscreen.setOnClick(event -> {
            if (expanded) Minecraft.getInstance().screen.onClose();
            else openFullscreen();
            event.stopPropagation();
        });
        addChild(fullscreen);
    }

    /** Keeps the recipe's configuration when its render cache expires or the fullscreen page closes. */
    public static ModularUI createModularUI(MultiblockInfo info) {
        var root = new PatternPreviewWidget(info, false);
        return ModularUI.of(UI.of(root, root::resize));
    }

    private Size resize(Size screen) {
        int width = expanded ? screen.getWidth() : Math.min(WIDTH, screen.getWidth());
        int height = expanded ? screen.getHeight() : Math.min(HEIGHT, screen.getHeight());
        title.getLayout().width(width - 54);
        fullscreen.getLayout().left(width - 42);
        preview.resize(width - 120, height - 24);
        configuration.resize(width - 112, 20, 108, height - 24);
        return Size.of(width, height);
    }

    private void openFullscreen() {
        var client = Minecraft.getInstance();
        var parent = client.screen;
        var root = new PatternPreviewWidget(info, true);
        client.setScreen(new ModularUIScreen(ModularUI.of(UI.of(root, root::resize)), definition.getBlock().getName()) {

            @Override
            public void onClose() {
                observedGeneration = root.observedGeneration;
                preview.setPreview(root.preview.getState().getPreview());
                configuration.setMachine(info.getMachine());
                client.setScreen(parent);
            }
        });
    }

    private void applyChange(TerminalChange update) {
        try {
            info.setMachine(update.apply(definition, info.getMachine()));
            publishPreview();
        } catch (RuntimeException exception) {
            showFailure(exception);
        }
    }

    private void publishPreview() {
        var resolved = info.resolvePreview();
        observedGeneration = resolved.generation();
        preview.setPreview(resolved.value());
        configuration.setMachine(info.getMachine());
        configuration.setInteractive(true);
    }

    private void showFailure(RuntimeException exception) {
        observedGeneration = StructurePatternRegistry.generation();
        GTCEu.LOGGER.error("Failed to resolve multiblock preview for {}", definition.getId(), exception);
        preview.setPreview(new StructurePreview(definition.getId(),
                "reload-" + observedGeneration, false, ObjectList.of(), ObjectList.of(), ObjectList.of(),
                ObjectList.of(new StructurePreview.Diagnostic("PATTERN_RELOAD_INCOMPATIBLE", null, null))));
        configuration.setInteractive(false);
    }

    @Override
    public void screenTick() {
        super.screenTick();
        if (StructurePatternRegistry.generation() != observedGeneration) {
            try {
                publishPreview();
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
