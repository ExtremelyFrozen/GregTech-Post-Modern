package com.gregtechceu.gtceu.common.cover;

import com.gregtechceu.gtceu.GTCEu;
import com.gregtechceu.gtceu.api.capability.ICoverable;
import com.gregtechceu.gtceu.api.cover.CoverBehavior;
import com.gregtechceu.gtceu.api.cover.CoverDefinition;
import com.gregtechceu.gtceu.api.gui.GuiTextures;
import com.gregtechceu.gtceu.api.gui.UITemplate;
import com.gregtechceu.gtceu.api.gui.element.GTButtonElement;
import com.gregtechceu.gtceu.api.gui.element.GTIntInputElement;
import com.gregtechceu.gtceu.api.gui.element.GTItemSlotElement;
import com.gregtechceu.gtceu.api.gui.element.GTLabelElement;
import com.gregtechceu.gtceu.api.gui.element.GTScrollerViewElement;
import com.gregtechceu.gtceu.api.gui.element.GTTextFieldElement;
import com.gregtechceu.gtceu.api.gui.factory.CoverUIHelper;
import com.gregtechceu.gtceu.api.gui.factory.CoverUIProvider;
import com.gregtechceu.gtceu.api.gui.factory.UICoverHolder;
import com.gregtechceu.gtceu.api.machine.TickableSubscription;
import com.gregtechceu.gtceu.api.machine.feature.IDataStickInteractable;
import com.gregtechceu.gtceu.api.placeholder.IPlaceholderInfoProviderCover;
import com.gregtechceu.gtceu.api.placeholder.MultiLineComponent;
import com.gregtechceu.gtceu.api.placeholder.PlaceholderContext;
import com.gregtechceu.gtceu.api.placeholder.PlaceholderHandler;
import com.gregtechceu.gtceu.api.sync_system.annotations.SaveField;
import com.gregtechceu.gtceu.api.sync_system.annotations.SyncToClient;
import com.gregtechceu.gtceu.api.sync_system.rpc.RpcRegistry;
import com.gregtechceu.gtceu.api.transfer.item.CustomItemStackHandler;
import com.gregtechceu.gtceu.client.renderer.cover.CoverTextRenderer;
import com.gregtechceu.gtceu.client.renderer.cover.IDynamicCoverRenderer;
import com.gregtechceu.gtceu.common.data.GTDataComponents;
import com.gregtechceu.gtceu.common.item.datacomponents.ComputerMonitorConfig;
import com.gregtechceu.gtceu.data.lang.LangHandler;
import com.gregtechceu.gtceu.integration.create.GTCreateIntegration;
import com.gregtechceu.gtceu.utils.GTStringUtils;
import com.gregtechceu.gtceu.utils.GTUtil;

import com.lowdragmc.lowdraglib2.gui.ui.UI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.data.Horizontal;
import com.lowdragmc.lowdraglib2.gui.ui.data.Vertical;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

import javax.annotation.ParametersAreNonnullByDefault;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class ComputerMonitorCover extends CoverBehavior
                                  implements CoverUIProvider, IDataStickInteractable,
                                  IPlaceholderInfoProviderCover, ComputerMonitorCoverConfigActionTarget {

    private static final int FORMAT_LINE_COUNT = 8;
    private static final int TEXT_FIELD_WIDTH = 160;
    private static final int TEXT_FIELD_HEIGHT = 15;
    private static final int HORIZONTAL_PADDING = 10;
    private static final int VERTICAL_PADDING = 2;
    private static final int UPDATE_INTERVAL_MIN = 1;
    private static final int UPDATE_INTERVAL_MAX = 60 * 20;
    private static final int ROOT_WIDTH = 390;
    private static final int CONTENT_HEIGHT = 195;

    static {
        RpcRegistry.register(ComputerMonitorCoverConfigActions.INSTANCE);
    }

    private TickableSubscription subscription;
    private final CoverTextRenderer renderer;
    @SaveField
    @SyncToClient
    @Getter
    private List<String> formatStringArgs = new ArrayList<>(FORMAT_LINE_COUNT);
    @SaveField
    @SyncToClient
    @Getter
    private List<String> formatStringLines = new ArrayList<>(FORMAT_LINE_COUNT);
    @SaveField
    @SyncToClient
    @Getter
    private List<MutableComponent> text = new ArrayList<>();
    @SaveField
    public CustomItemStackHandler itemStackHandler = new CustomItemStackHandler(FORMAT_LINE_COUNT);
    @Setter
    private String placeholderSearch = "";
    @Getter
    @SaveField
    private int updateInterval = 100;
    @Getter
    @SaveField
    private long ticksSincePlaced = 0;
    @SaveField
    @Getter
    private List<MutableComponent> createDisplayTargetBuffer = new ArrayList<>();
    @SaveField
    @Getter
    private List<MutableComponent> computerCraftTextBuffer = new ArrayList<>();
    @SaveField
    @Getter
    private UUID placeholderUUID;

    public ComputerMonitorCover(CoverDefinition definition, ICoverable coverHolder, Direction attachedSide) {
        super(definition, coverHolder, attachedSide);
        renderer = new CoverTextRenderer(this::getText);
        placeholderUUID = UUID.randomUUID();
        for (int i = 0; i < 100; i++) {
            createDisplayTargetBuffer.add(Component.empty());
            computerCraftTextBuffer.add(Component.empty());
        }
    }

    public List<MutableComponent> getRenderedText() {
        String s = formatStringLines.stream().reduce((a, b) -> a + "\n" + b).orElse("");
        List<String> tmp = new ArrayList<>(formatStringArgs);
        tmp = tmp.stream().map(str -> '{' + str + '}').toList();
        return PlaceholderHandler.processPlaceholders(
                GTStringUtils.replace(s, "\\{}", tmp),
                new PlaceholderContext(coverHolder.getLevel(), coverHolder.getBlockPos(), attachedSide,
                        itemStackHandler,
                        this, new MultiLineComponent(text), placeholderUUID));
    }

    public void setDisplayTargetBufferLine(int line, MutableComponent component) {
        createDisplayTargetBuffer.set(line, component);
    }

    @Override
    public void setComputerCraftTextBufferLine(int line, MutableComponent component) {
        computerCraftTextBuffer.set(line, component);
    }

    @Override
    public boolean canPipePassThrough() {
        return false;
    }

    @Override
    public Supplier<IDynamicCoverRenderer> getDynamicRenderer() {
        return () -> renderer;
    }

    @Override
    public boolean canCreateUI(Player player, UICoverHolder holder) {
        return holder.getCover() == this;
    }

    @Override
    public UI createUI(Player player, UICoverHolder holder) {
        ensureFormatStringSlots();

        UIElement root = new UIElement();
        UITemplate.setBounds(root, 0, 0, ROOT_WIDTH, CONTENT_HEIGHT + 82);
        root.style(style -> style.backgroundTexture(GuiTextures.BACKGROUND));

        UIElement mainPage = new UIElement();
        UITemplate.setBounds(mainPage, 0, 0, ROOT_WIDTH, CONTENT_HEIGHT);
        UIElement formatStringArgsPage = new UIElement();
        UITemplate.setBounds(formatStringArgsPage, 0, 0, ROOT_WIDTH, CONTENT_HEIGHT);
        formatStringArgsPage.setVisible(false);

        for (int i = 0; i < FORMAT_LINE_COUNT; i++) {
            mainPage.addChild(createFormatStringLineField(player, holder, i));
            mainPage.addChild(createItemSlot(i));
            formatStringArgsPage.addChild(createFormatStringArgField(player, holder, i));
        }

        mainPage.addChild(createPlaceholderReference(placeholderSearch));
        mainPage.addChild(createUpdateIntervalInput(player, holder));

        GTButtonElement switchToFormatStringArgsPageButton = new GTButtonElement(
                HORIZONTAL_PADDING + 50,
                10 * (TEXT_FIELD_HEIGHT + VERTICAL_PADDING) + VERTICAL_PADDING,
                20, 20,
                GuiTextures.group(GuiTextures.VANILLA_BUTTON, GuiTextures.BUTTON_RIGHT),
                event -> {
                    mainPage.setVisible(false);
                    formatStringArgsPage.setVisible(true);
                });
        switchToFormatStringArgsPageButton.noText();
        switchToFormatStringArgsPageButton.style(style -> style.tooltips(
                Component.translatable("gtpm.gui.computer_monitor_cover.edit_blank_placeholders")));

        GTButtonElement switchBack = new GTButtonElement(
                HORIZONTAL_PADDING + 50,
                10 * (TEXT_FIELD_HEIGHT + VERTICAL_PADDING) + VERTICAL_PADDING,
                20, 20,
                GuiTextures.group(GuiTextures.VANILLA_BUTTON, GuiTextures.BUTTON_LEFT),
                event -> {
                    formatStringArgsPage.setVisible(false);
                    mainPage.setVisible(true);
                });
        switchBack.noText();
        switchBack.style(style -> style.tooltips(
                Component.translatable("gtpm.gui.computer_monitor_cover.edit_displayed_text")));

        mainPage.addChild(switchToFormatStringArgsPageButton);
        formatStringArgsPage.addChild(switchBack);
        root.addChild(mainPage);
        root.addChild(formatStringArgsPage);
        root.addChild(UITemplate.bindPlayerInventory(player.getInventory(), GuiTextures.SLOT, 7,
                CONTENT_HEIGHT, true));
        return UI.of(root);
    }

    private GTTextFieldElement createFormatStringLineField(Player player, UICoverHolder holder, int index) {
        GTTextFieldElement field = createFormatTextField(index);
        field.setText(formatStringLines.get(index), false);
        field.setTextResponder(value -> setFormatStringLine(player, holder, index, value));
        field.style(style -> style.tooltips(tooltips(
                LangHandler.getMultiLang("gtpm.gui.computer_monitor_cover.main_textbox_tooltip", index + 1))));
        return field;
    }

    private GTTextFieldElement createFormatStringArgField(Player player, UICoverHolder holder, int index) {
        GTTextFieldElement field = createFormatTextField(index);
        field.setText(formatStringArgs.get(index), false);
        field.setTextResponder(value -> setFormatStringArg(player, holder, index, value));
        field.style(style -> style.tooltips(tooltips(
                LangHandler.getMultiLang("gtpm.gui.computer_monitor_cover.second_page_textbox_tooltip",
                        GTStringUtils.getIntOrderingSuffix(index + 1)))));
        return field;
    }

    private static GTTextFieldElement createFormatTextField(int index) {
        GTTextFieldElement field = new GTTextFieldElement(
                HORIZONTAL_PADDING + TEXT_FIELD_WIDTH / 2,
                10 + VERTICAL_PADDING + index * (TEXT_FIELD_HEIGHT + VERTICAL_PADDING),
                TEXT_FIELD_WIDTH,
                TEXT_FIELD_HEIGHT);
        field.setAnyString();
        field.textFieldStyle(style -> style
                .textColor(0x404040)
                .textShadow(false));
        return field;
    }

    private GTItemSlotElement createItemSlot(int index) {
        GTItemSlotElement slot = new GTItemSlotElement(itemStackHandler, index)
                .setBackgroundTexture(GuiTextures.SLOT);
        UITemplate.setBounds(slot, HORIZONTAL_PADDING + 50, 20 * index, 18, 18);
        slot.style(style -> style.tooltips(tooltips(
                LangHandler.getMultiLang("gtpm.gui.computer_monitor_cover.slot_tooltip", index + 1))));
        return slot;
    }

    private UIElement createPlaceholderReference(String filter) {
        UIElement root = new UIElement();
        UITemplate.setBounds(root, 280, 0, 110, CONTENT_HEIGHT);

        root.addChild(createLabel(0, 0, 110, 15, Component.literal(GTStringUtils.componentsToString(
                LangHandler.getMultiLang("gtpm.gui.computer_monitor_cover.placeholder_reference")))));

        GTScrollerViewElement placeholderReference = new GTScrollerViewElement(0, 15, 100, CONTENT_HEIGHT - 15);
        int y = 2;
        List<String> placeholders = new ArrayList<>(PlaceholderHandler.getAllPlaceholderNames());
        placeholders.removeIf(placeholder -> placeholder == null || !placeholder.contains(filter));
        placeholders.sort(Comparator.naturalOrder());
        for (String placeholder : placeholders) {
            GTLabelElement placeholderName = createLabel(0, y, 100, 15, Component.literal(placeholder));
            placeholderName.style(style -> style.tooltips(tooltips(
                    LangHandler.getSingleOrMultiLang("gtpm.placeholder_info." + placeholder))));
            placeholderReference.addChild(placeholderName);
            y += 15;
        }
        root.addChild(placeholderReference);
        return root;
    }

    private GTIntInputElement createUpdateIntervalInput(Player player, UICoverHolder holder) {
        GTIntInputElement input = new GTIntInputElement(0, 0, 60, 20, this::getUpdateInterval,
                value -> setUpdateInterval(player, holder, value))
                .setMin(UPDATE_INTERVAL_MIN)
                .setMax(UPDATE_INTERVAL_MAX);
        input.style(style -> style.tooltips(
                Component.translatable("gtpm.gui.computer_monitor_cover.update_interval")));
        return input;
    }

    private static GTLabelElement createLabel(int x, int y, int width, int height, Component text) {
        GTLabelElement label = new GTLabelElement(x, y, width, height, text);
        label.textStyle(style -> style
                .textColor(0x404040)
                .textShadow(false)
                .textAlignHorizontal(Horizontal.LEFT)
                .textAlignVertical(Vertical.CENTER));
        return label;
    }

    private void setFormatStringLine(Player player, UICoverHolder holder, int index, String value) {
        setFormatStringEntry(formatStringLines, index, value);
        sendConfigAction(player, holder);
    }

    private void setFormatStringArg(Player player, UICoverHolder holder, int index, String value) {
        setFormatStringEntry(formatStringArgs, index, value);
        sendConfigAction(player, holder);
    }

    private void setUpdateInterval(Player player, UICoverHolder holder, int value) {
        setUpdateInterval(value);
        sendConfigAction(player, holder);
    }

    private void sendConfigAction(Player player, UICoverHolder holder) {
        if (player.level().isClientSide()) {
            CoverUIHelper.sendAction(holder, ComputerMonitorCoverConfigActions.createSetConfigAction(
                    formatStringLines, formatStringArgs, getUpdateInterval()));
        }
    }

    private void ensureFormatStringSlots() {
        ensureListSize(formatStringLines, FORMAT_LINE_COUNT);
        ensureListSize(formatStringArgs, FORMAT_LINE_COUNT);
    }

    private static void setFormatStringEntry(List<String> values, int index, String value) {
        ensureListSize(values, index + 1);
        values.set(index, value);
    }

    private static void ensureListSize(List<String> values, int size) {
        while (values.size() < size) {
            values.add("");
        }
    }

    private static Component[] tooltips(List<MutableComponent> tooltips) {
        return GTStringUtils.toImmutable(tooltips).toArray(Component[]::new);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        subscription = coverHolder.subscribeServerTick(subscription, this::update);
    }

    private void update() {
        ticksSincePlaced++;
        if (coverHolder.getOffsetTimer() % updateInterval == 0) {
            try {
                if (GTCEu.MODS.CREATE.isLoaded())
                    GTCreateIntegration.TemporaryRedstoneLinkTransmitter.destroyAll();
                setRedstoneSignalOutput(0);
                text = getRenderedText();
            } catch (RuntimeException e) {
                text = GTUtil.list(
                        Component.translatable("gtpm.computer_monitor_cover.error.exception", e.getMessage()));
            }
        }
    }

    @Override
    public void onRemoved() {
        super.onRemoved();
        if (subscription != null) {
            subscription.unsubscribe();
        }
    }

    @Override
    public boolean canConnectRedstone() {
        return true;
    }

    @Override
    public List<ItemStack> getAdditionalDrops() {
        List<ItemStack> drops = super.getAdditionalDrops();
        for (int i = 0; i < 8; i++) {
            if (!itemStackHandler.getStackInSlot(i).isEmpty()) {
                drops.add(itemStackHandler.getStackInSlot(i));
            }
        }
        return drops;
    }

    @Override
    public InteractionResult onDataStickUse(Player player, ItemStack dataStick) {
        ComputerMonitorConfig config = dataStick.get(GTDataComponents.COMPUTER_MONITOR_CONFIG);
        if (config == null) return InteractionResult.FAIL;

        formatStringLines.clear();
        formatStringLines.addAll(config.lines());

        formatStringArgs.clear();
        formatStringArgs.addAll(config.args());
        updateInterval = config.updateInterval();
        return InteractionResult.sidedSuccess(player.level().isClientSide);
    }

    @Override
    public InteractionResult onDataStickShiftUse(Player player, ItemStack dataStick) {
        dataStick.set(GTDataComponents.COMPUTER_MONITOR_CONFIG,
                new ComputerMonitorConfig(formatStringLines, formatStringArgs, updateInterval));
        return InteractionResult.sidedSuccess(player.level().isClientSide);
    }

    @Override
    public void replaceFormatStringLines(List<String> lines) {
        formatStringLines.clear();
        formatStringLines.addAll(lines);
        ensureListSize(formatStringLines, FORMAT_LINE_COUNT);
    }

    @Override
    public void replaceFormatStringArgs(List<String> args) {
        formatStringArgs.clear();
        formatStringArgs.addAll(args);
        ensureListSize(formatStringArgs, FORMAT_LINE_COUNT);
    }

    @Override
    public void setUpdateInterval(int updateInterval) {
        this.updateInterval = updateInterval;
    }
}
