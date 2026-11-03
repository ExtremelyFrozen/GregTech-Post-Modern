package com.gregtechceu.gtceu.common.item.modules;

import com.gregtechceu.gtceu.GTCEu;
import com.gregtechceu.gtceu.api.gui.GuiTextures;
import com.gregtechceu.gtceu.api.gui.UITemplate;
import com.gregtechceu.gtceu.api.gui.element.GTButtonElement;
import com.gregtechceu.gtceu.api.gui.element.GTLabelElement;
import com.gregtechceu.gtceu.api.gui.element.GTScrollerViewElement;
import com.gregtechceu.gtceu.api.gui.element.GTTextFieldElement;
import com.gregtechceu.gtceu.api.item.IComponentItem;
import com.gregtechceu.gtceu.api.item.component.IAddInformation;
import com.gregtechceu.gtceu.api.item.component.IMonitorModuleItem;
import com.gregtechceu.gtceu.api.placeholder.MultiLineComponent;
import com.gregtechceu.gtceu.api.placeholder.PlaceholderContext;
import com.gregtechceu.gtceu.api.placeholder.PlaceholderHandler;
import com.gregtechceu.gtceu.client.renderer.monitor.IMonitorRenderer;
import com.gregtechceu.gtceu.client.renderer.monitor.MonitorTextRenderer;
import com.gregtechceu.gtceu.common.data.GTDataComponents;
import com.gregtechceu.gtceu.common.item.datacomponents.TextLineList;
import com.gregtechceu.gtceu.common.machine.multiblock.electric.CentralMonitorMachine;
import com.gregtechceu.gtceu.common.machine.multiblock.electric.monitor.MonitorGroup;
import com.gregtechceu.gtceu.data.lang.LangHandler;
import com.gregtechceu.gtceu.utils.GTStringUtils;

import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.data.Horizontal;
import com.lowdragmc.lowdraglib2.gui.ui.data.ScrollDisplay;
import com.lowdragmc.lowdraglib2.gui.ui.data.ScrollerMode;
import com.lowdragmc.lowdraglib2.gui.ui.data.Vertical;
import com.lowdragmc.lowdraglib2.gui.ui.elements.codeeditor.CodeEditor;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

public class TextModuleBehaviour implements IMonitorModuleItem, IAddInformation {

    private static final int EDITOR_WIDTH = 120;
    private static final int EDITOR_HEIGHT = 80;
    private static final int PLACEHOLDER_REFERENCE_WIDTH = 120;
    private static final int CONFIGURATION_GAP = 8;
    private static final int CONFIGURATION_WIDTH = PLACEHOLDER_REFERENCE_WIDTH + CONFIGURATION_GAP + EDITOR_WIDTH;
    private static final int CONFIGURATION_HEIGHT = 104;
    private static final int EDITOR_X = PLACEHOLDER_REFERENCE_WIDTH + CONFIGURATION_GAP;

    private void updateText(ItemStack stack, CentralMonitorMachine machine, MonitorGroup group) {
        if (!stack.has(GTDataComponents.PLACEHOLDER_UUID)) {
            stack.set(GTDataComponents.PLACEHOLDER_UUID, UUID.randomUUID());
        }
        MultiLineComponent text = PlaceholderHandler.processPlaceholders(
                getPlaceholderText(stack),
                new PlaceholderContext(
                        group.getTargetLevel(machine.getLevel()),
                        group.getTarget(machine.getLevel()),
                        group.getTargetCoverSide(),
                        group.getPlaceholderSlotsHandler(),
                        group.getTargetCover(machine.getLevel()),
                        null,
                        stack.get(GTDataComponents.PLACEHOLDER_UUID)));
        stack.update(GTDataComponents.TEXT_LINE_LIST, TextLineList.EMPTY, lines -> lines.withLines(text.toImmutable()));
    }

    @Override
    public void tick(ItemStack stack, CentralMonitorMachine machine, MonitorGroup group) {
        this.updateText(stack, machine, group);
    }

    @Override
    public IMonitorRenderer getRenderer(ItemStack stack) {
        TextLineList lines = stack.getOrDefault(GTDataComponents.TEXT_LINE_LIST, TextLineList.EMPTY);
        return new MonitorTextRenderer(MultiLineComponent.of(lines.lines()), Math.max(lines.scale(), .0001));
    }

    @Override
    public UIElement createConfigurationElement(ItemStack stack, CentralMonitorMachine machine, MonitorGroup group,
                                                Consumer<Runnable> actionSender) {
        UIElement builder = new UIElement();
        UITemplate.setBounds(builder, 0, 0, CONFIGURATION_WIDTH, CONFIGURATION_HEIGHT);

        CodeEditor editor = new CodeEditor();
        UITemplate.setBounds(editor, EDITOR_X, 0, EDITOR_WIDTH, EDITOR_HEIGHT);
        editor.setLines(getFormatStringLines(stack));

        GTTextFieldElement scaleInput = createScaleInput(stack);
        TextEditSession editSession = TextEditSession.open(machine, group, stack);
        GTButtonElement saveButton = new GTButtonElement(CONFIGURATION_WIDTH - 20, 84, 20, 20,
                GuiTextures.group(GuiTextures.VANILLA_BUTTON, GuiTextures.BUTTON_CHECK),
                event -> {
                    if (!machine.isRemote()) return;
                    saveText(machine, editSession, editor, scaleInput, actionSender);
                });
        saveButton.noText();

        builder.addChildren(editor, saveButton, scaleInput, createPlaceholderReference());
        return builder;
    }

    private static GTTextFieldElement createScaleInput(ItemStack stack) {
        GTTextFieldElement scaleInput = new GTTextFieldElement(EDITOR_X, 86, 72, 12);
        scaleInput.setAnyString();
        scaleInput.setText(getScaleText(stack), false);
        scaleInput.style(style -> style.tooltips(Component.translatable("gtpm.gui.central_monitor.text_scale")));
        return scaleInput;
    }

    private static List<String> getFormatStringLines(ItemStack stack) {
        return stack.getOrDefault(GTDataComponents.FORMAT_STRING_LIST, TextLineList.EMPTY)
                .lines()
                .stream()
                .map(Component::getString)
                .toList();
    }

    private static String getScaleText(ItemStack stack) {
        if (!stack.has(GTDataComponents.FORMAT_STRING_LIST)) {
            return "1";
        }
        TextLineList configuration = stack.getOrDefault(GTDataComponents.FORMAT_STRING_LIST, TextLineList.EMPTY);
        return String.valueOf(configuration.scale());
    }

    private static void saveText(CentralMonitorMachine machine, TextEditSession editSession, CodeEditor editor,
                                 GTTextFieldElement scaleInput, Consumer<Runnable> actionSender) {
        sendTextChange(machine, editSession, Arrays.asList(editor.getValue()), scaleInput.getValue(), actionSender);
    }

    private static void sendTextChange(CentralMonitorMachine machine, TextEditSession editSession,
                                       List<String> requestedLines, String scaleText, Consumer<Runnable> actionSender) {
        Float requestedScale = readScale(scaleText);
        if (requestedScale == null) {
            return;
        }
        TextLineList requestedConfiguration = createConfiguration(
                requestedLines, requestedScale);
        if (requestedConfiguration.equals(
                editSession.expectedModule.get(GTDataComponents.FORMAT_STRING_LIST))) {
            return;
        }
        actionSender.accept(() -> machine.rpcToServer("setCentralMonitorTextModuleConfiguration",
                editSession.holderIncarnation,
                editSession.groupIdentity,
                editSession.moduleSlotIncarnation,
                editSession.expectedConfigurationRevision,
                editSession.expectedModule,
                requestedLines,
                requestedScale));
    }

    private static final class TextEditSession {

        private final UUID holderIncarnation;
        private final UUID groupIdentity;
        private final UUID moduleSlotIncarnation;
        private final ItemStack expectedModule;
        private final long expectedConfigurationRevision;

        private TextEditSession(UUID holderIncarnation, UUID groupIdentity, UUID moduleSlotIncarnation,
                                long expectedConfigurationRevision, ItemStack expectedModule) {
            this.holderIncarnation = holderIncarnation;
            this.groupIdentity = groupIdentity;
            this.moduleSlotIncarnation = moduleSlotIncarnation;
            this.expectedConfigurationRevision = expectedConfigurationRevision;
            this.expectedModule = expectedModule;
        }

        private static TextEditSession open(CentralMonitorMachine machine, MonitorGroup group, ItemStack module) {
            return new TextEditSession(
                    machine.getCentralMonitorActionIncarnation(),
                    group.getIdentity(),
                    group.getModuleSlotIncarnation(),
                    group.getTextConfigurationRevision(),
                    captureExpectedModule(module));
        }
    }

    private static @Nullable Float readScale(String text) {
        try {
            return Float.parseFloat(text);
        } catch (NumberFormatException e) {
            GTCEu.LOGGER.error("Invalid monitor text module scale: {}", text, e);
            return null;
        }
    }

    public static TextLineList createConfiguration(List<String> lines, float scale) {
        return new TextLineList(lines.stream().map(Component::literal).map(Component.class::cast).toList(), scale);
    }

    /** Captures editable data without placeholder identity and server-derived display text. */
    public static ItemStack captureExpectedModule(ItemStack module) {
        ItemStack snapshot = module.copy();
        snapshot.remove(GTDataComponents.TEXT_LINE_LIST.get());
        snapshot.remove(GTDataComponents.PLACEHOLDER_UUID.get());
        return snapshot;
    }

    /** Compares stable module data while allowing derived display components to change. */
    public static boolean matchesExpectedModule(ItemStack current, ItemStack expected) {
        return ItemStack.matches(captureExpectedModule(current), expected);
    }

    public static boolean isTextModule(ItemStack stack) {
        return stack.getItem() instanceof IComponentItem componentItem &&
                componentItem.getComponents().stream().anyMatch(TextModuleBehaviour.class::isInstance);
    }

    private static UIElement createPlaceholderReference() {
        UIElement root = new UIElement();
        UITemplate.setBounds(root, 0, 0, PLACEHOLDER_REFERENCE_WIDTH, CONFIGURATION_HEIGHT);

        root.addChild(createLabel(0,
                Component.literal(GTStringUtils.componentsToString(
                        LangHandler.getMultiLang("gtpm.gui.computer_monitor_cover.placeholder_reference")))));

        GTScrollerViewElement placeholderReference = new GTScrollerViewElement(0, 15, PLACEHOLDER_REFERENCE_WIDTH,
                CONFIGURATION_HEIGHT - 15);
        placeholderReference.viewPort(viewPort -> viewPort.layout(layout -> layout.paddingAll(0)));
        placeholderReference.scrollerStyle(style -> style
                .mode(ScrollerMode.VERTICAL)
                .verticalScrollDisplay(ScrollDisplay.AUTO)
                .horizontalScrollDisplay(ScrollDisplay.NEVER));
        int y = 2;
        List<String> placeholders = new ArrayList<>(PlaceholderHandler.getAllPlaceholderNames());
        placeholders.sort(Comparator.naturalOrder());
        for (String placeholder : placeholders) {
            GTLabelElement placeholderName = createLabel(y, Component.literal(placeholder));
            placeholderName.style(style -> style.tooltips(tooltips(
                    LangHandler.getSingleOrMultiLang("gtpm.placeholder_info." + placeholder))));
            placeholderReference.addScrollViewChild(placeholderName);
            y += 15;
        }
        root.addChild(placeholderReference);
        return root;
    }

    private static GTLabelElement createLabel(int y, Component text) {
        GTLabelElement label = new GTLabelElement(0, y, PLACEHOLDER_REFERENCE_WIDTH, 15, text);
        label.textStyle(style -> style
                .textColor(0x404040)
                .textShadow(false)
                .textAlignHorizontal(Horizontal.LEFT)
                .textAlignVertical(Vertical.CENTER));
        return label;
    }

    private static Component[] tooltips(List<MutableComponent> tooltips) {
        return GTStringUtils.toImmutable(tooltips).toArray(Component[]::new);
    }

    @Override
    public String getType() {
        return "text";
    }

    public MultiLineComponent getText(ItemStack stack) {
        return MultiLineComponent.of(stack.getOrDefault(GTDataComponents.TEXT_LINE_LIST, TextLineList.EMPTY).lines());
    }

    public float getScale(ItemStack stack) {
        return Math.max(stack.getOrDefault(GTDataComponents.TEXT_LINE_LIST, TextLineList.EMPTY).scale(), .0001f);
    }

    public void setScale(ItemStack stack, float scale) {
        stack.update(GTDataComponents.TEXT_LINE_LIST, TextLineList.EMPTY, lines -> lines.withScale(scale));
    }

    public TextLineList createPlaceholderConfiguration(ItemStack stack, String text) {
        List<Component> lines = Arrays.stream(text.split("\n"))
                .map(Component::literal)
                .map(Component.class::cast)
                .toList();
        return stack.getOrDefault(GTDataComponents.FORMAT_STRING_LIST, TextLineList.EMPTY).withLines(lines);
    }

    public String getPlaceholderText(ItemStack stack) {
        StringBuilder formatStringLines = new StringBuilder();
        List<Component> lines = stack.getOrDefault(GTDataComponents.FORMAT_STRING_LIST, TextLineList.EMPTY).lines();
        for (Component line : lines) {
            formatStringLines.append(line.getString()).append('\n');
        }
        return formatStringLines.toString();
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Item.TooltipContext context,
                                List<Component> tooltipComponents,
                                TooltipFlag isAdvanced) {
        if (isAdvanced.isAdvanced()) {
            tooltipComponents.add(Component.literal("Placeholder text:").withStyle(ChatFormatting.GOLD));
            tooltipComponents
                    .addAll(stack.getOrDefault(GTDataComponents.FORMAT_STRING_LIST, TextLineList.EMPTY).lines());
            tooltipComponents.add(Component.literal("Processed text:").withStyle(ChatFormatting.GOLD));
            tooltipComponents.addAll(getText(stack));
        }
    }
}
