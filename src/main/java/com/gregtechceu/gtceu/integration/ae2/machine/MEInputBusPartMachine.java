package com.gregtechceu.gtceu.integration.ae2.machine;

import com.gregtechceu.gtceu.GTCEu;
import com.gregtechceu.gtceu.api.blockentity.BlockEntityCreationInfo;
import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.gui.GuiTextures;
import com.gregtechceu.gtceu.api.gui.UITemplate;
import com.gregtechceu.gtceu.api.gui.element.GTImageElement;
import com.gregtechceu.gtceu.api.gui.element.GTIntInputElement;
import com.gregtechceu.gtceu.api.gui.element.GTItemSlotElement;
import com.gregtechceu.gtceu.api.gui.element.GTLabelElement;
import com.gregtechceu.gtceu.api.gui.element.GTPhantomItemSlotElement;
import com.gregtechceu.gtceu.api.gui.factory.MachineUIHolder;
import com.gregtechceu.gtceu.api.gui.fancy.ConfiguratorPanel;
import com.gregtechceu.gtceu.api.gui.fancy.FancyMachineUIWidget;
import com.gregtechceu.gtceu.api.gui.fancy.TabsWidget;
import com.gregtechceu.gtceu.api.machine.fancyconfigurator.AutoStockingConfigurator;
import com.gregtechceu.gtceu.api.machine.fancyconfigurator.CircuitFancyConfigurator;
import com.gregtechceu.gtceu.api.machine.fancyconfigurator.CombinedDirectionalFancyConfigurator;
import com.gregtechceu.gtceu.api.machine.fancyconfigurator.DistinctPartConfigurator;
import com.gregtechceu.gtceu.api.machine.fancyconfigurator.WorkingEnabledConfigurator;
import com.gregtechceu.gtceu.api.machine.feature.IDataStickInteractable;
import com.gregtechceu.gtceu.api.machine.trait.NotifiableItemStackHandler;
import com.gregtechceu.gtceu.api.sync_system.rpc.RPCMethod;
import com.gregtechceu.gtceu.api.sync_system.rpc.RPCSender;
import com.gregtechceu.gtceu.common.data.GTDataComponents;
import com.gregtechceu.gtceu.common.data.datacomponents.AEInputConfigCopyData;
import com.gregtechceu.gtceu.common.item.behavior.IntCircuitBehaviour;
import com.gregtechceu.gtceu.integration.ae2.gui.fancy.MEItemAutoPullConfigurator;
import com.gregtechceu.gtceu.integration.ae2.slot.ExportOnlyAEItemList;
import com.gregtechceu.gtceu.integration.ae2.slot.ExportOnlyAEItemSlot;
import com.gregtechceu.gtceu.integration.ae2.slot.ExportOnlyAESlot;
import com.gregtechceu.gtceu.utils.FormattingUtil;
import com.gregtechceu.gtceu.utils.GTMath;

import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.data.Horizontal;
import com.lowdragmc.lowdraglib2.gui.ui.data.Vertical;
import com.lowdragmc.lowdraglib2.gui.ui.event.HoverTooltips;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvent;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;

import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import appeng.api.config.Actionable;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.GenericStack;
import appeng.api.storage.MEStorage;
import lombok.Getter;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

public class MEInputBusPartMachine extends MEBusPartMachine
                                   implements IDataStickInteractable {

    protected final static int CONFIG_SIZE = 16;

    protected ExportOnlyAEItemList aeItemHandler;

    public MEInputBusPartMachine(BlockEntityCreationInfo info) {
        super(info, IO.IN);
    }

    /////////////////////////////////
    // ***** Machine LifeCycle ****//
    /////////////////////////////////

    @Override
    public void onMachineDestroyed() {
        super.onMachineDestroyed();
        flushInventory();
    }

    @Override
    protected NotifiableItemStackHandler createInventory() {
        this.aeItemHandler = new ExportOnlyAEItemList(CONFIG_SIZE);
        return this.aeItemHandler;
    }

    /////////////////////////////////
    // ********** Sync ME *********//
    /////////////////////////////////

    @Override
    public void autoIO() {
        if (!this.isWorkingEnabled()) return;
        if (!this.shouldSyncME()) return;

        if (this.updateMEStatus()) {
            this.syncME();
            this.updateInventorySubscription();
        }
    }

    protected void syncME() {
        MEStorage networkInv = this.getMainNode().getGrid().getStorageService().getInventory();
        for (ExportOnlyAEItemSlot aeSlot : this.aeItemHandler.getInventory()) {
            // Try to clear the wrong item
            GenericStack exceedItem = aeSlot.exceedStack();
            if (exceedItem != null) {
                long total = exceedItem.amount();
                long inserted = networkInv.insert(exceedItem.what(), exceedItem.amount(), Actionable.MODULATE,
                        this.actionSource);
                if (inserted > 0) {
                    aeSlot.extractItem(0, GTMath.saturatedCast(inserted), false);
                    continue;
                } else {
                    aeSlot.extractItem(0, GTMath.saturatedCast(total), false);
                }
            }
            // Fill it
            GenericStack reqItem = aeSlot.requestStack();
            if (reqItem != null) {
                long extracted = networkInv.extract(reqItem.what(), reqItem.amount(), Actionable.MODULATE,
                        this.actionSource);
                if (extracted != 0) {
                    aeSlot.addStack(new GenericStack(reqItem.what(), extracted));
                }
            }
        }
    }

    protected void flushInventory() {
        var grid = getMainNode().getGrid();
        if (grid != null) {
            for (var aeSlot : aeItemHandler.getInventory()) {
                GenericStack stock = aeSlot.getStock();
                if (stock != null) {
                    grid.getStorageService().getInventory().insert(stock.what(), stock.amount(), Actionable.MODULATE,
                            actionSource);
                }
            }
        }
    }

    ///////////////////////////////
    // ********** GUI ***********//
    ///////////////////////////////

    /** Returns one authoritative slot for action execution and direct behavior verification. */
    public ExportOnlyAEItemSlot getMEItemConfigSlot(int index) {
        return aeItemHandler.getInventory()[index];
    }

    /** Creates one standalone opening-scoped page after validating the exact holder and definition. */

    ////////////////////////////////
    // ******* Interaction *******//
    ////////////////////////////////

    @Override
    public final InteractionResult onDataStickShiftUse(Player player, ItemStack dataStick) {
        if (!isRemote()) {
            dataStick.set(GTDataComponents.AE_INPUT_CONFIG_COPY_DATA, writeConfigData());
            dataStick.set(DataComponents.CUSTOM_NAME,
                    Component.translatable("gtpm.machine.me.item_import.data_stick.name"));
            player.sendSystemMessage(Component.translatable("gtpm.machine.me.import_copy_settings"));
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public final InteractionResult onDataStickUse(Player player, ItemStack dataStick) {
        AEInputConfigCopyData data = dataStick.get(GTDataComponents.AE_INPUT_CONFIG_COPY_DATA);
        if (data == null) {
            return InteractionResult.PASS;
        }

        if (!isRemote()) {
            readConfigData(data);
            this.updateInventorySubscription();
            player.sendSystemMessage(Component.translatable("gtpm.machine.me.import_paste_settings"));
        }
        return InteractionResult.sidedSuccess(isRemote());
    }

    ////////////////////////////////
    // ****** Configuration ******//
    ////////////////////////////////

    protected AEInputConfigCopyData writeConfigData() {
        List<@Nullable GenericStack> stacks = new ArrayList<>(CONFIG_SIZE);
        for (int i = 0; i < CONFIG_SIZE; i++) {
            stacks.add(this.aeItemHandler.getInventory()[i].getConfig());
        }
        byte ghostCircuit = (byte) IntCircuitBehaviour.getCircuitConfiguration(circuitInventory.getStackInSlot(0));
        return new AEInputConfigCopyData(stacks, ghostCircuit, isDistinct(), false);
    }

    protected void readConfigData(AEInputConfigCopyData data) {
        List<@Nullable GenericStack> stacks = data.stacks();
        for (int index = 0; index < Math.min(stacks.size(), CONFIG_SIZE); index++) {
            GenericStack stack = stacks.get(index);
            if (stack != null && (!(stack.what() instanceof AEItemKey) ||
                    stack.amount() <= 0 || stack.amount() > Integer.MAX_VALUE)) {
                throw new IllegalArgumentException("ME item input data stick contains an invalid stack at slot " +
                        index + '.');
            }
        }
        for (int i = 0; i < CONFIG_SIZE; i++) {
            this.aeItemHandler.getInventory()[i].setConfig(i < stacks.size() ? stacks.get(i) : null);
        }
        circuitInventory.setStackInSlot(0, IntCircuitBehaviour.stack(data.ghostCircuit()));
        setDistinct(data.distinctBuses());
    }

    @ApiStatus.Internal
    public int getMEItemConfigSlotCount() {
        return CONFIG_SIZE;
    }

    @ApiStatus.Internal
    public boolean isMEItemConfigAutoPull() {
        return false;
    }

    @ApiStatus.Internal
    public boolean isMEItemStocking() {
        return false;
    }

    @ApiStatus.Internal
    public boolean canSetMEItemConfig(int slot, @NotNull ItemStack item) {
        if (isMEItemConfigAutoPull()) {
            return false;
        }
        if (item.isEmpty() || !isMEItemStocking()) {
            return true;
        }
        AEItemKey key = AEItemKey.of(item);
        for (int index = 0; index < CONFIG_SIZE; index++) {
            GenericStack configured = aeItemHandler.getInventory()[index].getConfig();
            if (index != slot && configured != null && configured.what().equals(key)) {
                return false;
            }
        }
        return !isConfiguredInOtherStockingPart(new GenericStack(key, item.getCount()));
    }

    /** Returns whether another formed stocking part already owns the candidate item key. */
    protected boolean isConfiguredInOtherStockingPart(@NotNull GenericStack stack) {
        return false;
    }

    @RPCMethod
    @ApiStatus.Internal
    public void setMEItemConfig(int slot, @NotNull ItemStack item) {
        if (!canSetMEItemConfig(slot, item)) {
            throw new IllegalStateException("ME item configuration mutation is not permitted.");
        }
        getMEItemConfigSlot(slot).setConfig(item.isEmpty() ? null :
                new GenericStack(AEItemKey.of(item), item.getCount()));
    }

    @RPCMethod
    @ApiStatus.Internal
    public void setMEItemConfigAmount(int slot, @NotNull ItemStack expectedItem, int amount) {
        ExportOnlyAEItemSlot configSlot = getMEItemConfigSlot(slot);
        GenericStack config = configSlot.getConfig();
        if (isMEItemStocking() || isMEItemConfigAutoPull() ||
                config == null ||
                !config.what().equals(AEItemKey.of(expectedItem))) {
            throw new IllegalStateException("ME item configuration amount mutation is not permitted.");
        }
        configSlot.setConfig(ExportOnlyAESlot.copy(config, amount));
    }

    @RPCMethod
    @ApiStatus.Internal
    public void pickupMEItemConfigStock(RPCSender sender, int slot, ItemStack expectedItem) {
        var player = sender.asPlayer();
        var configSlot = getMEItemConfigSlot(slot);
        var stock = configSlot.getStock();
        if (isMEItemStocking() || isMEItemConfigAutoPull() || !player.containerMenu.getCarried().isEmpty() ||
                stock == null || !stock.what().equals(AEItemKey.of(expectedItem)))
            return;
        var key = (AEItemKey) stock.what();
        ItemStack pickedUp = key.toStack(GTMath.saturatedCast(stock.amount()));
        player.containerMenu.setCarried(pickedUp);
        long remaining = stock.amount() - pickedUp.getCount();
        configSlot.setStock(remaining > 0 ? new GenericStack(key, remaining) : null);
    }

    @RPCMethod
    @ApiStatus.Internal
    public void setMEItemAutoPull(boolean autoPull) {
        throw new IllegalStateException("Ordinary ME item input buses do not support auto-pull.");
    }

    /** One opening-scoped Fancy home page shared by standalone and contextual part navigation. */

    public final class ItemConfig extends UIElement {

        private final Player player;
        private final UIElement amountEditor;
        private int selectedSlot = -1;

        public ItemConfig(Player player) {
            this.player = player;
            UITemplate.setBounds(this, 0, 0, 150, 88);
            style(style -> style.backgroundTexture(GuiTextures.BACKGROUND_INVERSE));
            addChild(new GTLabelElement(3, 0, 144, 10) {

                @Override
                public void screenTick() {
                    setValue(Component
                            .translatable(isOnline() ? "gtpm.gui.me_network.online" : "gtpm.gui.me_network.offline"));
                    super.screenTick();
                }
            });
            for (int index = 0; index < CONFIG_SIZE; index++) {
                addChild(new ItemConfigSlot(index, 3 + index % 8 * 18, 10 + index / 8 * 38));
            }
            amountEditor = UITemplate.setBounds(new UIElement(), 35, 29, 80, 30);
            amountEditor.style(style -> style.backgroundTexture(GuiTextures.NUMBER_BACKGROUND));
            amountEditor
                    .addChild(new GTLabelElement(3, 2, 74, 10, Component.translatable("gtpm.gui.config_slot.amount")));
            amountEditor.addChild(new GTIntInputElement(3, 14, 74, 14,
                    () -> {
                        var config = selectedConfig();
                        return config == null ? 1 : Math.toIntExact(config.amount());
                    },
                    amount -> {
                        var config = selectedConfig();
                        if (config != null) requestAmount(selectedSlot, config, amount);
                    }).setMin(1).setMax(Integer.MAX_VALUE));
            addChild(amountEditor);
            amountEditor.setVisible(false);
            addEventListener(UIEvents.MOUSE_DOWN, event -> {
                if (selectedSlot >= 0 && !amountEditor.isAncestorOf(event.target)) selectedSlot = -1;
            }, true);
        }

        @Override
        public void screenTick() {
            if (isMEItemStocking() || isMEItemConfigAutoPull()) selectedSlot = -1;
            amountEditor.setVisible(selectedConfig() != null);
            super.screenTick();
        }

        private GenericStack selectedConfig() {
            return selectedSlot < 0 ? null : getMEItemConfigSlot(selectedSlot).getConfig();
        }

        private void requestAmount(int index, GenericStack config, int amount) {
            getMEItemConfigSlot(index).setConfig(new GenericStack(config.what(), amount));
            rpcToServer("setMEItemConfigAmount", index, ((AEItemKey) config.what()).toStack(1), amount);
        }

        public final class ItemConfigSlot extends UIElement {

            @Getter
            private final int index;
            private final ConfigItemElement configElement;
            private final StockItemElement stockElement;

            public ItemConfigSlot(int index, int x, int y) {
                this.index = index;
                UITemplate.setBounds(this, x, y, 18, 36);

                this.configElement = new ConfigItemElement();
                this.stockElement = new StockItemElement();
                UITemplate.setBounds(configElement, 0, 0, 18, 18);
                UITemplate.setBounds(stockElement, 0, 18, 18, 18);
                configElement.setId("me_item_config_" + index);
                stockElement.setId("me_item_stock_" + index);
                addChildren(configElement, stockElement);

                GTImageElement selection = new GTImageElement(0, 0, 18, 18, GuiTextures.SELECT_BOX)
                        .setVisibleSupplier(() -> selectedSlot == index);
                selection.setAllowHitTest(false);
                addChild(selection);
                addChild(createConfigAmountLabel());
                addChild(createStockAmountLabel());
            }

            /** Returns the upper phantom config element for direct event verification. */
            public GTPhantomItemSlotElement getConfigElement() {
                return configElement;
            }

            /** Returns the lower read-only stock element for direct event verification. */
            public GTItemSlotElement getStockElement() {
                return stockElement;
            }

            private GTLabelElement createConfigAmountLabel() {
                GTLabelElement label = new GTLabelElement(1, 7, 16, 10) {

                    @Override
                    public void screenTick() {
                        GenericStack config = getMEItemConfigSlot(index).getConfig();
                        setValue(config == null || isMEItemStocking() ? Component.empty() :
                                Component.literal(FormattingUtil.formatNumberReadable(config.amount(), false)));
                        super.screenTick();
                    }
                }.setTextColor(-1)
                        .setTextShadow(true)
                        .setTextAlignHorizontal(Horizontal.RIGHT)
                        .setTextAlignVertical(Vertical.BOTTOM)
                        .setFontSize(4.5f);
                label.setVisible(!isMEItemStocking());
                label.setId("me_item_config_amount_" + index);
                label.setAllowHitTest(false);
                return label;
            }

            private GTLabelElement createStockAmountLabel() {
                GTLabelElement label = new GTLabelElement(1, 25, 16, 10) {

                    @Override
                    public void screenTick() {
                        GenericStack stock = getMEItemConfigSlot(index).getStock();
                        setValue(stock == null ? Component.empty() : Component.literal(
                                FormattingUtil.formatNumberReadable(stock.amount(), false)));
                        super.screenTick();
                    }
                }.setTextColor(-1)
                        .setTextShadow(true)
                        .setTextAlignHorizontal(Horizontal.RIGHT)
                        .setTextAlignVertical(Vertical.BOTTOM)
                        .setFontSize(4.5f);
                label.setId("me_item_stock_amount_" + index);
                label.setAllowHitTest(false);
                return label;
            }

            private ItemStack displayConfigItem() {
                GenericStack config = getMEItemConfigSlot(index).getConfig();
                return config == null ? ItemStack.EMPTY : displayItem(config, 1);
            }

            private ItemStack displayStockItem() {
                GenericStack stock = getMEItemConfigSlot(index).getStock();
                return stock == null ? ItemStack.EMPTY : displayItem(stock, 1);
            }

            private ItemStack displayItem(GenericStack stack, int amount) {
                return ((AEItemKey) stack.what()).toStack(amount);
            }

            private void sendSetConfig(ItemStack item) {
                if (!isRemote() || isMEItemConfigAutoPull()) return;
                getMEItemConfigSlot(index)
                        .setConfig(item.isEmpty() ? null : new GenericStack(AEItemKey.of(item), item.getCount()));
                rpcToServer("setMEItemConfig", index, item);
            }

            private void handleConfigClick(UIEvent event) {
                if (!isRemote() || isMEItemConfigAutoPull()) {
                    return;
                }
                boolean handled = false;
                if (event.button == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
                    sendSetConfig(ItemStack.EMPTY);
                    selectedSlot = -1;
                    handled = true;
                } else if (event.button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
                    if (!isMEItemStocking()) {
                        selectedSlot = index;
                        handled = true;
                    }
                    ItemStack carried = player.containerMenu.getCarried();
                    if (!carried.isEmpty()) {
                        sendSetConfig(carried);
                        handled = true;
                    }
                }
                if (handled) {
                    event.stopImmediatePropagation();
                    event.hasHandler = true;
                }
            }

            private void handleConfigWheel(UIEvent event) {
                GenericStack config = getMEItemConfigSlot(index).getConfig();
                if (!isRemote() || isMEItemStocking() || isMEItemConfigAutoPull() || config == null ||
                        event.deltaY == 0)
                    return;
                long current = config.amount();
                long amount = event.isCtrlDown() ? (event.deltaY > 0 ? current * 2 : current / 2) :
                        current + (event.deltaY > 0 ? 1 : -1);
                if (amount < 1 || amount > Integer.MAX_VALUE) return;
                requestAmount(index, config, (int) amount);
                event.stopImmediatePropagation();
                event.hasHandler = true;
            }

            private void handleStockClick(UIEvent event) {
                GenericStack stock = getMEItemConfigSlot(index).getStock();
                if (event.button != GLFW.GLFW_MOUSE_BUTTON_LEFT || isMEItemStocking() || stock == null ||
                        !isRemote() || !player.containerMenu.getCarried().isEmpty())
                    return;
                rpcToServer("pickupMEItemConfigStock", index, ((AEItemKey) stock.what()).toStack(1));
                event.stopImmediatePropagation();
                event.hasHandler = true;
            }

            private final class ConfigItemElement extends GTPhantomItemSlotElement {

                private ConfigItemElement() {
                    setItemSupplier(ItemConfigSlot.this::displayConfigItem);
                    setItemConsumer(ItemConfigSlot.this::sendSetConfig);
                    setMaxStackSizeSupplier(() -> Integer.MAX_VALUE);
                    setCanPutItems(false);
                    setCanTakeItems(false);
                    setOnAddedTooltips((slot, tooltips) -> tooltips.addAll(configOperationTooltips()));
                    addEventListener(UIEvents.MOUSE_DOWN, ItemConfigSlot.this::handleConfigClick);
                    addEventListener(UIEvents.MOUSE_WHEEL, ItemConfigSlot.this::handleConfigWheel);
                    refresh();
                }

                @Override
                public void screenTick() {
                    refresh();
                    super.screenTick();
                }

                @Override
                protected void onHoverTooltips(UIEvent event) {
                    if (getValue().isEmpty()) {
                        event.hoverTooltips = new HoverTooltips(configOperationTooltips(), null, null, null);
                        return;
                    }
                    super.onHoverTooltips(event);
                }

                private List<Component> configOperationTooltips() {
                    if (isMEItemConfigAutoPull()) {
                        return List.of(
                                Component.translatable("gtpm.gui.config_slot"),
                                Component.translatable("gtpm.gui.config_slot.auto_pull_managed"));
                    }
                    if (isMEItemStocking()) {
                        return List.of(
                                Component.translatable("gtpm.gui.config_slot"),
                                Component.translatable("gtpm.gui.config_slot.set_only"),
                                Component.translatable("gtpm.gui.config_slot.remove"));
                    }
                    return List.of(
                            Component.translatable("gtpm.gui.config_slot"),
                            Component.translatable("gtpm.gui.config_slot.set"),
                            Component.translatable("gtpm.gui.config_slot.scroll"),
                            Component.translatable("gtpm.gui.config_slot.remove"));
                }

                private void refresh() {
                    setBackgroundTexture(isMEItemConfigAutoPull() ?
                            GuiTextures.group(GuiTextures.SLOT_DARK, GuiTextures.CONFIG_ARROW_DARK) :
                            GuiTextures.group(GuiTextures.SLOT, GuiTextures.CONFIG_ARROW));
                    setItem(displayConfigItem(), false);
                }
            }

            private final class StockItemElement extends GTItemSlotElement {

                private StockItemElement() {
                    setBackgroundTexture(GuiTextures.SLOT_DARK);
                    setCanPutItems(false);
                    setCanTakeItems(false);
                    setOnAddedTooltips((slot, tooltips) -> {
                        GenericStack stock = getMEItemConfigSlot(index).getStock();
                        if (stock != null) {
                            tooltips.add(Component.literal(FormattingUtil.formatNumbers(stock.amount())));
                        }
                    });
                    addEventListener(UIEvents.MOUSE_DOWN, ItemConfigSlot.this::handleStockClick);
                    refresh();
                }

                @Override
                public void screenTick() {
                    refresh();
                    super.screenTick();
                }

                private void refresh() {
                    setItem(displayStockItem(), false);
                }
            }
        }
    }

    @Override
    public UIElement createUIWidget(FancyMachineUIWidget shell) {
        Player player = shell.getOpeningPlayer();
        return new ItemConfig(player);
    }

    @Override
    public int getPageWidth() {
        return 150;
    }

    @Override
    public int getPageHeight() {
        return 88;
    }

    @Override
    public void attachConfigurators(ConfiguratorPanel configuratorPanel) {
        MachineUIHolder holder = configuratorPanel.getHolder();
        configuratorPanel.attachConfigurators(new WorkingEnabledConfigurator(
                MEInputBusPartMachine.this));
        DistinctPartConfigurator.attachConfigurators(configuratorPanel, MEInputBusPartMachine.this);
        if (isHasCircuitSlot() && isCircuitSlotEnabled()) {
            configuratorPanel.attachConfigurators(new CircuitFancyConfigurator(
                    MEInputBusPartMachine.this));
        }
        if (MEInputBusPartMachine.this instanceof MEStockingBusPartMachine stockingBus) {
            configuratorPanel.attachConfigurators(
                    new MEItemAutoPullConfigurator(stockingBus, holder),
                    new AutoStockingConfigurator(stockingBus, holder));
        }
    }

    @Override
    public void attachSideTabs(TabsWidget tabs) {
        if (!isMEItemStocking()) tabs.attachCachedSubTab(GTCEu.id("directional"),
                () -> new CombinedDirectionalFancyConfigurator(this, tabs.getPlayer()));
    }
}
