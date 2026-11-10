package com.gregtechceu.gtceu.api.gui.widget;

import com.gregtechceu.gtceu.GTCEu;
import com.gregtechceu.gtceu.api.gui.GuiTextures;
import com.gregtechceu.gtceu.api.gui.UITemplate;
import com.gregtechceu.gtceu.api.gui.element.GTButtonElement;
import com.gregtechceu.gtceu.api.gui.element.GTLabelElement;
import com.gregtechceu.gtceu.api.gui.element.GTScrollerViewElement;
import com.gregtechceu.gtceu.api.machine.MultiblockMachineDefinition;
import com.gregtechceu.gtceu.api.machine.multiblock.MultiblockControllerMachine;
import com.gregtechceu.gtceu.api.multiblock.autobuild.batch.BuildPolicy;
import com.gregtechceu.gtceu.api.multiblock.autobuild.batch.StructureOperation;
import com.gregtechceu.gtceu.api.multiblock.autobuild.batch.StructureRequest;
import com.gregtechceu.gtceu.api.multiblock.autobuild.plan.BatchPlanner;
import com.gregtechceu.gtceu.api.multiblock.pattern.layout.PatternAxis;
import com.gregtechceu.gtceu.api.multiblock.preview.PreviewVisibility;
import com.gregtechceu.gtceu.api.multiblock.preview.StructurePreview;
import com.gregtechceu.gtceu.client.gui.ClientMultiblockPreviewElement;
import com.gregtechceu.gtceu.data.pattern.StructurePatternRegistry;
import com.gregtechceu.gtceu.integration.xei.multiblock.MultiblockInfo;

import com.lowdragmc.lowdraglib2.gui.holder.ModularUIScreen;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.data.ScrollDisplay;
import com.lowdragmc.lowdraglib2.gui.ui.data.ScrollerMode;
import com.lowdragmc.lowdraglib2.gui.ui.data.TextWrap;
import com.lowdragmc.lowdraglib2.gui.ui.rendering.GUIContext;
import com.lowdragmc.lowdraglib2.math.Size;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.vfyjxf.taffy.style.TaffyPosition;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectList;
import org.jspecify.annotations.NullMarked;

import java.util.Locale;
import java.util.function.UnaryOperator;

/** A recipe-viewer page owns its local choices, controls and client scene. */
@OnlyIn(Dist.CLIENT)
@NullMarked
public final class PatternPreviewWidget extends UIElement {

    public static final int WIDTH = 300 + BlockChoiceList.COLUMN_WIDTH + 4;
    public static final int HEIGHT = 200;

    private final MultiblockMachineDefinition definition;
    private final MultiblockInfo info;
    private final UIElement main = new UIElement();
    private final ClientMultiblockPreviewElement preview;
    private final BlockChoiceList blocks;
    private final GTScrollerViewElement options = new GTScrollerViewElement();
    private final ObjectList<GTButtonElement> controls = new ObjectArrayList<>();
    private final GTLabelElement title;
    private final GTButtonElement fullscreen;
    private final boolean expanded;
    private int selectedStructure;
    private int pageWidth = WIDTH;
    private int pageHeight = HEIGHT;
    private long observedGeneration;

    private PatternPreviewWidget(MultiblockInfo info, boolean expanded) {
        this.info = info;
        definition = info.getDefinition();
        this.expanded = expanded;
        UITemplate.setBounds(this, 0, 0, WIDTH, HEIGHT);
        getLayout().positionType(TaffyPosition.RELATIVE);
        setOverflowVisible(true);
        main.getStyle().backgroundTexture(GuiTextures.BACKGROUND);
        main.setOverflowVisible(false);
        addChild(main);
        var initial = info.resolvePreview();
        observedGeneration = initial.generation();
        title = label(6, 6, 246, definition.getBlock().getName());
        main.addChild(title);
        preview = new ClientMultiblockPreviewElement(180, 176, new PreviewVisibility(initial.value()), false);
        main.addChild(preview);
        blocks = new BlockChoiceList((group, id) -> changeStructure(structure -> structure.withTierChoice(group, id)));
        addChild(blocks);
        options.scrollerStyle(style -> style.mode(ScrollerMode.VERTICAL)
                .verticalScrollDisplay(ScrollDisplay.AUTO).horizontalScrollDisplay(ScrollDisplay.NEVER)
                .scrollerViewStyle(0));
        options.viewPort(port -> {
            port.getLayout().paddingAll(0);
            port.getStyle().backgroundTexture(GuiTextures.BACKGROUND_INVERSE);
        });
        options.verticalScroller(scroll -> scroll.getLayout().width(2));
        main.addChild(options);
        fullscreen = button(258, 4, 16, Component.literal(expanded ? "↙" : "↗"));
        fullscreen.getStyle().tooltips(text(expanded ? "compact" : "fullscreen"));
        fullscreen.setOnClick(event -> {
            if (expanded) Minecraft.getInstance().screen.onClose();
            else openFullscreen();
            event.stopPropagation();
        });
        main.addChild(fullscreen);
        rebuildOptions();
    }

    public static ModularUI createModularUI(MultiblockInfo info) {
        var root = new PatternPreviewWidget(info, false);
        return ModularUI.of(UI.of(root, root::resize));
    }

    private Size resize(Size screen) {
        pageWidth = expanded ? screen.getWidth() : Math.min(WIDTH, screen.getWidth());
        pageHeight = expanded ? screen.getHeight() : Math.min(HEIGHT, screen.getHeight());
        int contentWidth = pageWidth - (expanded ? BlockChoiceList.WIDTH : BlockChoiceList.COLUMN_WIDTH) - 4;
        title.getLayout().width(contentWidth - 54);
        fullscreen.getLayout().left(contentWidth - 42);
        rebuildOptions();
        return Size.of(pageWidth, pageHeight);
    }

    private void layoutContent() {
        int contentWidth = pageWidth - (expanded ? BlockChoiceList.WIDTH : BlockChoiceList.COLUMN_WIDTH) - 4;
        UITemplate.setBounds(main, 0, 0, contentWidth, pageHeight);
        preview.getLayout().left(4).top(20);
        preview.resize(contentWidth - 66, pageHeight - 24);
        UITemplate.setBounds(options, contentWidth - 58, 20, 54, pageHeight - 24);
        blocks.resize(contentWidth + 4, 20, pageHeight - 24);
    }

    private String structureName() {
        return definition.getStructureOrder().get(selectedStructure);
    }

    private void rebuildOptions() {
        options.clearAllScrollViewChildren();
        controls.clear();
        int width = 50;
        var policy = info.getPolicy();
        String name = structureName();
        var structure = info.getStructures().get(name);
        var noHatches = button(0, 0, width, text("casings"));
        noHatches.textStyle(style -> style.textColor(policy.noHatchMode() ? 0xff167129 : 0xff404040));
        noHatches.setOnClick(event -> {
            applyChange(() -> info.setPolicy(new BuildPolicy(policy.replaceMode(), !policy.noHatchMode(), false)));
            event.stopPropagation();
        });
        options.addScrollViewChild(noHatches);
        controls.add(noHatches);
        int y = 20;
        if (definition.isAllowFlip()) {
            var flip = button(0, y, width, text("flip", text(structure.flipMode() ? "on" : "off")));
            flip.setOnClick(event -> {
                changeStructure(value -> value.withFlipMode(!value.flipMode()));
                event.stopPropagation();
            });
            options.addScrollViewChild(flip);
            controls.add(flip);
            y += 20;
        }
        var order = definition.getStructureOrder();
        if (order.size() > 1) {
            addStep(y, width, displayName(name), () -> {
                selectedStructure = (selectedStructure + order.size() - 1) % order.size();
                blocks.close();
                rebuildOptions();
            }, () -> {
                selectedStructure = (selectedStructure + 1) % order.size();
                blocks.close();
                rebuildOptions();
            }, true, true);
            y += 30;
            boolean selected = info.getIncluded().contains(name);
            boolean required = info.getIncluded().stream()
                    .anyMatch(included -> info.getStructures().get(included).mode() == StructureOperation.BUILD &&
                            definition.getRequiredStructures(included).contains(name));
            if (!required && (!selected || info.getIncluded().size() > 1)) {
                var included = button(0, y, width, text(selected ? "selected" : "unselected"));
                included.setOnClick(event -> {
                    applyChange(() -> info.setIncluded(name, !selected));
                    event.stopPropagation();
                });
                options.addScrollViewChild(included);
                controls.add(included);
                y += 20;
            }
        }
        var pattern = definition.getPattern(name);
        var axes = pattern.getSourceDefinition().getAxes();
        var layout = pattern.expandLayout(structure.repetitions().toIntArray(), structure.choices());
        for (var repetition : pattern.getRepeatDescriptors()) {
            if (repetition.minimum() == repetition.maximum()) continue;
            int index = repetition.index();
            int amount = structure.repetitions().getInt(index);
            var axis = PatternAxis.of(switch (repetition.axis()) {
                case DEPTH -> axes.getDepth();
                case HEIGHT -> axes.getHeight();
                case WIDTH -> axes.getWidth();
            });
            addStep(y, width,
                    text("dimension", text("axis." + axis.name().toLowerCase(Locale.ROOT)),
                            layout.size(repetition.axis())),
                    () -> changeRepetition(index, amount - 1),
                    () -> changeRepetition(index, amount + 1),
                    amount > repetition.minimum(), amount < repetition.maximum());
            y += 30;
        }
        for (var choice : pattern.getChoiceDescriptors()) {
            var values = choice.alternatives();
            int index = values.indexOf(structure.choices().getOrDefault(choice.path(), values.getFirst()));
            addStep(y, width, text("variant_index", index + 1, values.size()),
                    () -> changeStructure(value -> value.withChoice(choice.path(), values.get(index - 1))),
                    () -> changeStructure(value -> value.withChoice(choice.path(), values.get(index + 1))),
                    index > 0, index + 1 < values.size());
            y += 30;
        }
        options.viewContainer.getLayout().width(width).height(y);
        blocks.setChoices(new BatchPlanner().activeTierChoiceOptions(definition, structure),
                structure.tierChoices());
        layoutContent();
    }

    private void addStep(int y, int width, Component name, Runnable previous, Runnable next, boolean hasPrevious,
                         boolean hasNext) {
        options.addScrollViewChild(label(0, y, width, name));
        var back = button(0, y + 10, 16, Component.literal("◀"));
        back.setActive(hasPrevious);
        back.setOnClick(event -> {
            previous.run();
            event.stopPropagation();
        });
        var forward = button(width - 16, y + 10, 16, Component.literal("▶"));
        forward.setActive(hasNext);
        forward.setOnClick(event -> {
            next.run();
            event.stopPropagation();
        });
        options.addScrollViewChildren(back, forward);
        if (hasPrevious) controls.add(back);
        if (hasNext) controls.add(forward);
    }

    private void openFullscreen() {
        var client = Minecraft.getInstance();
        var parent = client.screen;
        var root = new PatternPreviewWidget(info, true);
        client.setScreen(new ModularUIScreen(ModularUI.of(UI.of(root, root::resize)), definition.getBlock().getName()) {

            @Override
            public void onClose() {
                publishPreview();
                client.setScreen(parent);
            }
        });
    }

    private void changeRepetition(int index, int amount) {
        changeStructure(structure -> {
            var repetitions = new IntArrayList(structure.repetitions());
            repetitions.set(index, amount);
            return structure.withRepetitions(repetitions);
        });
    }

    private void changeStructure(UnaryOperator<StructureRequest> update) {
        applyChange(() -> info.setStructure(update.apply(info.getStructures().get(structureName()))));
    }

    private void applyChange(Runnable update) {
        try {
            update.run();
            publishPreview();
        } catch (RuntimeException exception) {
            showFailure(exception);
        }
    }

    private void publishPreview() {
        var resolved = info.resolvePreview();
        observedGeneration = resolved.generation();
        preview.setPreview(resolved.value());
        rebuildOptions();
        blocks.setInteractive(true);
    }

    private void showFailure(RuntimeException exception) {
        observedGeneration = StructurePatternRegistry.generation();
        GTCEu.LOGGER.error("Failed to resolve multiblock preview for {}", definition.getId(), exception);
        preview.setPreview(new StructurePreview(definition.getId(), "reload-" + observedGeneration, false,
                ObjectList.of(), ObjectList.of(), ObjectList.of(),
                ObjectList.of(new StructurePreview.Diagnostic("PATTERN_RELOAD_INCOMPATIBLE", null, null))));
        blocks.setInteractive(false);
        controls.forEach(button -> button.setActive(false));
    }

    private static GTButtonElement button(int x, int y, int width, Component text) {
        var button = new GTButtonElement(x, y, width, 16);
        button.setButtonTexture(GuiTextures.VANILLA_BUTTON).setText(text);
        button.getLayout().paddingAll(0);
        button.text.getLayout().widthPercent(100).heightPercent(100).marginAll(0).minWidth(0);
        button.textStyle(style -> style.adaptiveWidth(false).adaptiveHeight(false).fontSize(8)
                .textColor(0xff404040).textShadow(false).textWrap(TextWrap.HIDE));
        button.getStyle().tooltips(text);
        return button;
    }

    private static GTLabelElement label(int x, int y, int width, Component text) {
        var label = new GTLabelElement(x, y, width, 10, text);
        label.textStyle(style -> style.adaptiveWidth(false).adaptiveHeight(false).fontSize(9)
                .textColor(0xff404040).textShadow(false).textWrap(TextWrap.HIDE));
        label.getStyle().tooltips(text);
        return label;
    }

    private static Component text(String key, Object... arguments) {
        return Component.translatable("gtpm.multiblock.configuration." + key, arguments);
    }

    private static Component displayName(String name) {
        return name.equals(MultiblockControllerMachine.DEFAULT_STRUCTURE) ? text("main") : Component.translatable(name);
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
