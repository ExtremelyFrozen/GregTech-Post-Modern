package com.gregtechceu.gtceu.integration.ae2.machine;

import com.gregtechceu.gtceu.GTCEu;
import com.gregtechceu.gtceu.api.blockentity.BlockEntityCreationInfo;
import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.gui.GuiTextures;
import com.gregtechceu.gtceu.api.gui.UITemplate;
import com.gregtechceu.gtceu.api.gui.element.GTFluidSlotElement;
import com.gregtechceu.gtceu.api.gui.element.GTImageElement;
import com.gregtechceu.gtceu.api.gui.element.GTIntInputElement;
import com.gregtechceu.gtceu.api.gui.element.GTLabelElement;
import com.gregtechceu.gtceu.api.gui.fancy.ConfiguratorPanel;
import com.gregtechceu.gtceu.api.gui.fancy.FancyMachineUIWidget;
import com.gregtechceu.gtceu.api.gui.fancy.TabsWidget;
import com.gregtechceu.gtceu.api.machine.fancyconfigurator.AutoStockingConfigurator;
import com.gregtechceu.gtceu.api.machine.fancyconfigurator.CircuitFancyConfigurator;
import com.gregtechceu.gtceu.api.machine.fancyconfigurator.CombinedDirectionalFancyConfigurator;
import com.gregtechceu.gtceu.api.machine.fancyconfigurator.WorkingEnabledConfigurator;
import com.gregtechceu.gtceu.api.machine.feature.IDataStickInteractable;
import com.gregtechceu.gtceu.api.machine.trait.NotifiableFluidTank;
import com.gregtechceu.gtceu.api.sync_system.rpc.RPCMethod;
import com.gregtechceu.gtceu.common.data.GTDataComponents;
import com.gregtechceu.gtceu.common.data.datacomponents.AEInputConfigCopyData;
import com.gregtechceu.gtceu.common.item.behavior.IntCircuitBehaviour;
import com.gregtechceu.gtceu.core.mixins.FluidStackAccessor;
import com.gregtechceu.gtceu.integration.ae2.gui.fancy.MEFluidAutoPullConfigurator;
import com.gregtechceu.gtceu.integration.ae2.slot.ExportOnlyAEFluidList;
import com.gregtechceu.gtceu.integration.ae2.slot.ExportOnlyAEFluidSlot;
import com.gregtechceu.gtceu.integration.ae2.slot.ExportOnlyAESlot;
import com.gregtechceu.gtceu.utils.FormattingUtil;
import com.gregtechceu.gtceu.utils.GTMath;

import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.data.Horizontal;
import com.lowdragmc.lowdraglib2.gui.ui.data.Vertical;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvent;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;

import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.fluids.FluidUtil;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

import appeng.api.config.Actionable;
import appeng.api.stacks.AEFluidKey;
import appeng.api.stacks.GenericStack;
import appeng.api.storage.MEStorage;
import lombok.Getter;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

/**
 * ME fluid import hatch with a configuration page built from its synchronized slots.
 */
public class MEInputHatchPartMachine extends MEHatchPartMachine
                                     implements IDataStickInteractable {

    /** Authoritative sixteen-slot fluid request and stock inventory. */
    protected ExportOnlyAEFluidList aeFluidHandler;

    public MEInputHatchPartMachine(BlockEntityCreationInfo info) {
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
    protected NotifiableFluidTank createTank(int initialCapacity, int slots) {
        this.aeFluidHandler = new ExportOnlyAEFluidList(this, slots);
        return aeFluidHandler;
    }

    /////////////////////////////////
    // ********** Sync ME *********//
    /////////////////////////////////

    @Override
    protected void autoIO() {
        if (!this.isWorkingEnabled()) return;
        if (!this.shouldSyncME()) return;

        if (this.updateMEStatus()) {
            this.syncME();
            this.updateTankSubscription();
        }
    }

    protected void syncME() {
        MEStorage networkInv = this.getMainNode().getGrid().getStorageService().getInventory();
        for (ExportOnlyAEFluidSlot aeTank : this.aeFluidHandler.getInventory()) {
            // Try to clear the wrong fluid
            GenericStack exceedFluid = aeTank.exceedStack();
            if (exceedFluid != null) {
                int total = GTMath.saturatedCast(exceedFluid.amount());
                int inserted = GTMath.saturatedCast(networkInv.insert(exceedFluid.what(), exceedFluid.amount(),
                        Actionable.MODULATE, this.actionSource));
                if (inserted > 0) {
                    aeTank.drain(inserted, IFluidHandler.FluidAction.EXECUTE);
                    continue;
                } else {
                    aeTank.drain(total, IFluidHandler.FluidAction.EXECUTE);
                }
            }
            // Fill it
            GenericStack reqFluid = aeTank.requestStack();
            if (reqFluid != null) {
                long extracted = networkInv.extract(reqFluid.what(), reqFluid.amount(), Actionable.MODULATE,
                        this.actionSource);
                if (extracted > 0) {
                    aeTank.addStack(new GenericStack(reqFluid.what(), extracted));
                }
            }
        }
    }

    protected void flushInventory() {
        var grid = getMainNode().getGrid();
        if (grid != null) {
            for (var aeSlot : aeFluidHandler.getInventory()) {
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
    public ExportOnlyAEFluidSlot getMEFluidConfigSlot(int index) {
        return aeFluidHandler.getInventory()[index];
    }

    /** Creates one standalone opening-scoped page after validating the exact holder and definition. */

    ////////////////////////////////
    // ******* Interaction *******//
    ////////////////////////////////

    @Override
    public final InteractionResult onDataStickShiftUse(Player player, ItemStack dataStick) {
        if (!isRemote()) {
            dataStick.set(GTDataComponents.AE_INPUT_CONFIG_COPY_DATA, writeConfigData());
            dataStick.set(DataComponents.ITEM_NAME,
                    Component.translatable("gtpm.machine.me.fluid_import.data_stick.name"));
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
            this.updateTankSubscription();
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
            stacks.add(this.aeFluidHandler.getInventory()[i].getConfig());
        }
        byte ghostCircuit = (byte) IntCircuitBehaviour.getCircuitConfiguration(circuitInventory.getStackInSlot(0));
        return new AEInputConfigCopyData(stacks, ghostCircuit, false, false);
    }

    /** Validates a complete fluid-only data-stick payload before applying any slot mutation. */
    protected void readConfigData(AEInputConfigCopyData data) {
        List<@Nullable GenericStack> stacks = data.stacks();
        for (int index = 0; index < Math.min(stacks.size(), CONFIG_SIZE); index++) {
            GenericStack stack = stacks.get(index);
            if (stack != null && (!(stack.what() instanceof AEFluidKey) ||
                    stack.amount() <= 0 || stack.amount() > Integer.MAX_VALUE)) {
                throw new IllegalArgumentException("ME fluid input data stick contains an invalid stack at slot " +
                        index + '.');
            }
        }
        for (int i = 0; i < CONFIG_SIZE; i++) {
            this.aeFluidHandler.getInventory()[i].setConfig(i < stacks.size() ? stacks.get(i) : null);
        }
        circuitInventory.setStackInSlot(0, IntCircuitBehaviour.stack(data.ghostCircuit()));
    }

    @ApiStatus.Internal
    public int getMEFluidConfigSlotCount() {
        return CONFIG_SIZE;
    }

    @ApiStatus.Internal
    public boolean isMEFluidConfigAutoPull() {
        return false;
    }

    @ApiStatus.Internal
    public boolean isMEFluidStocking() {
        return false;
    }

    @ApiStatus.Internal
    public boolean canSetMEFluidConfig(int slot, @NotNull FluidStack fluid) {
        if (isMEFluidConfigAutoPull()) {
            return false;
        }
        if (fluid.isEmpty() || !isMEFluidStocking()) {
            return true;
        }
        AEFluidKey key = AEFluidKey.of(fluid);
        for (int index = 0; index < CONFIG_SIZE; index++) {
            GenericStack configured = aeFluidHandler.getInventory()[index].getConfig();
            if (index != slot && configured != null && configured.what().equals(key)) {
                return false;
            }
        }
        return !isConfiguredInOtherStockingPart(new GenericStack(key, fluid.getAmount()));
    }

    /** Returns whether another formed stocking part already owns the candidate fluid key. */
    protected boolean isConfiguredInOtherStockingPart(@NotNull GenericStack stack) {
        return false;
    }

    @RPCMethod
    @ApiStatus.Internal
    public void setMEFluidConfig(int slot, @NotNull FluidStack fluid) {
        if (!canSetMEFluidConfig(slot, fluid)) {
            throw new IllegalStateException("ME fluid configuration mutation is not permitted.");
        }
        getMEFluidConfigSlot(slot).setConfig(fluid.isEmpty() ? null :
                new GenericStack(AEFluidKey.of(fluid), fluid.getAmount()));
    }

    @RPCMethod
    @ApiStatus.Internal
    public void setMEFluidConfigAmount(int slot, @NotNull FluidStack expectedFluid, int amount) {
        ExportOnlyAEFluidSlot configSlot = getMEFluidConfigSlot(slot);
        GenericStack config = configSlot.getConfig();
        if (isMEFluidStocking() || isMEFluidConfigAutoPull() ||
                config == null ||
                !config.what().equals(AEFluidKey.of(expectedFluid))) {
            throw new IllegalStateException("ME fluid configuration amount mutation is not permitted.");
        }
        configSlot.setConfig(ExportOnlyAESlot.copy(config, amount));
    }

    /** One opening-scoped Fancy home page shared by standalone and contextual part navigation. */

    public final class FluidConfig extends UIElement {

        private final Player player;
        private final UIElement amountEditor;
        private int selectedSlot = -1;

        public FluidConfig(Player player) {
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
                addChild(new FluidConfigSlot(index, 3 + index % 8 * 18, 10 + index / 8 * 38));
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
            if (isMEFluidStocking() || isMEFluidConfigAutoPull()) selectedSlot = -1;
            amountEditor.setVisible(selectedConfig() != null);
            super.screenTick();
        }

        private GenericStack selectedConfig() {
            return selectedSlot < 0 ? null : getMEFluidConfigSlot(selectedSlot).getConfig();
        }

        private void requestAmount(int index, GenericStack config, int amount) {
            getMEFluidConfigSlot(index).setConfig(new GenericStack(config.what(), amount));
            rpcToServer("setMEFluidConfigAmount", index, ((AEFluidKey) config.what()).toStack(1), amount);
        }

        public final class FluidConfigSlot extends UIElement {

            @Getter
            private final int index;
            private final ConfigFluidElement configElement;
            private final StockFluidElement stockElement;

            public FluidConfigSlot(int index, int x, int y) {
                this.index = index;
                UITemplate.setBounds(this, x, y, 18, 36);

                this.configElement = new ConfigFluidElement();
                this.stockElement = new StockFluidElement();
                UITemplate.setBounds(configElement, 0, 0, 18, 18);
                UITemplate.setBounds(stockElement, 0, 18, 18, 18);
                configElement.setId("me_fluid_config_" + index);
                stockElement.setId("me_fluid_stock_" + index);
                addChildren(configElement, stockElement);

                GTImageElement selection = new GTImageElement(0, 0, 18, 18, GuiTextures.SELECT_BOX)
                        .setVisibleSupplier(() -> selectedSlot == index);
                selection.setAllowHitTest(false);
                addChild(selection);
                addChild(createStockAmountLabel());
            }

            /**
             * Returns the upper phantom config element for direct event verification.
             */
            public GTFluidSlotElement getConfigElement() {
                return configElement;
            }

            /**
             * Returns the lower read-only stock element for direct event verification.
             */
            public GTFluidSlotElement getStockElement() {
                return stockElement;
            }

            private GTLabelElement createStockAmountLabel() {
                GTLabelElement label = new GTLabelElement(1, 25, 16, 10) {

                    @Override
                    public void screenTick() {
                        GenericStack stock = getMEFluidConfigSlot(index).getStock();
                        setValue(stock == null ? Component.empty() : Component.literal(
                                FormattingUtil.formatNumberReadable(stock.amount(), true,
                                        FormattingUtil.DECIMAL_FORMAT_0F, "B")));
                        super.screenTick();
                    }
                }.setTextColor(-1)
                        .setTextShadow(true)
                        .setTextAlignHorizontal(Horizontal.RIGHT)
                        .setTextAlignVertical(Vertical.BOTTOM)
                        .setFontSize(4.5f);
                label.setAllowHitTest(false);
                return label;
            }

            private FluidStack displayFluid(GenericStack stack) {
                return ((AEFluidKey) stack.what()).toStack(GTMath.saturatedCast(stack.amount()));
            }

            private void sendSetConfig(FluidStack fluid) {
                if (!isRemote() || isMEFluidConfigAutoPull()) return;
                FluidStack value = normalizeGhostFluid(fluid);
                getMEFluidConfigSlot(index)
                        .setConfig(value.isEmpty() ? null : new GenericStack(AEFluidKey.of(value), value.getAmount()));
                rpcToServer("setMEFluidConfig", index, value);
            }

            private FluidStack normalizeGhostFluid(FluidStack fluid) {
                if (fluid == FluidStack.EMPTY) {
                    return FluidStack.EMPTY;
                }
                var rawFluid = ((FluidStackAccessor) (Object) fluid).getRawFluid();
                if (rawFluid == Fluids.EMPTY) {
                    return FluidStack.EMPTY;
                }
                if (fluid.getAmount() > 0) {
                    return fluid.copy();
                }
                FluidStack normalized = new FluidStack(rawFluid, FluidType.BUCKET_VOLUME);
                normalized.applyComponents(fluid.getComponents());
                return normalized;
            }

            private void handleConfigClick(UIEvent event) {
                if (!isRemote() || isMEFluidConfigAutoPull()) {
                    return;
                }
                boolean handled = false;
                if (event.button == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
                    sendSetConfig(FluidStack.EMPTY);
                    selectedSlot = -1;
                    handled = true;
                } else if (event.button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
                    if (!isMEFluidStocking()) {
                        selectedSlot = index;
                        handled = true;
                    }
                    var carriedFluid = FluidUtil.getFluidContained(player.containerMenu.getCarried());
                    if (carriedFluid.isPresent()) {
                        sendSetConfig(carriedFluid.get());
                        handled = true;
                    }
                }
                if (handled) {
                    event.stopImmediatePropagation();
                    event.hasHandler = true;
                }
            }

            private void handleConfigWheel(UIEvent event) {
                GenericStack config = getMEFluidConfigSlot(index).getConfig();
                if (!isRemote() || isMEFluidStocking() || isMEFluidConfigAutoPull() || config == null ||
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
                if (event.button != GLFW.GLFW_MOUSE_BUTTON_LEFT || isMEFluidStocking() ||
                        getMEFluidConfigSlot(index).getStock() == null ||
                        !isRemote() || FluidUtil.getFluidHandler(player.containerMenu.getCarried()).isEmpty())
                    return;
                rpcToServer("clickFluidHatchSlot", index, event.isShiftDown());
                event.stopImmediatePropagation();
                event.hasHandler = true;
            }

            private final class ConfigFluidElement extends GTFluidSlotElement {

                private boolean refreshing;

                private ConfigFluidElement() {
                    setAllowClickFilled(false);
                    setAllowClickDrained(false);
                    setOnAddedTooltips((slot, tooltips) -> {
                        tooltips.add(Component.translatable("gtpm.gui.config_slot"));
                        if (isMEFluidConfigAutoPull()) {
                            tooltips.add(Component.translatable("gtpm.gui.config_slot.auto_pull_managed"));
                        } else if (isMEFluidStocking()) {
                            tooltips.add(Component.translatable("gtpm.gui.config_slot.set_only"));
                            tooltips.add(Component.translatable("gtpm.gui.config_slot.remove"));
                        } else {
                            tooltips.add(Component.translatable("gtpm.gui.config_slot.set"));
                            tooltips.add(Component.translatable("gtpm.gui.config_slot.scroll"));
                            tooltips.add(Component.translatable("gtpm.gui.config_slot.remove"));
                        }
                    });
                    addEventListener(UIEvents.MOUSE_DOWN, FluidConfigSlot.this::handleConfigClick);
                    addEventListener(UIEvents.MOUSE_WHEEL, FluidConfigSlot.this::handleConfigWheel);
                    xeiPhantom();
                    refresh();
                }

                @Override
                public ConfigFluidElement setFluid(FluidStack fluid) {
                    if (refreshing) {
                        super.setFluid(fluid);
                    } else {
                        sendSetConfig(fluid);
                    }
                    return this;
                }

                @Override
                public void screenTick() {
                    refresh();
                    super.screenTick();
                }

                private void refresh() {
                    GenericStack config = getMEFluidConfigSlot(index).getConfig();
                    setBackgroundTexture(isMEFluidConfigAutoPull() ?
                            GuiTextures.group(GuiTextures.SLOT_DARK, GuiTextures.CONFIG_ARROW_DARK) :
                            GuiTextures.group(GuiTextures.FLUID_SLOT, GuiTextures.CONFIG_ARROW));
                    setShowAmount(!isMEFluidStocking());
                    refreshing = true;
                    super.setFluid(config == null ? FluidStack.EMPTY : displayFluid(config));
                    refreshing = false;
                }
            }

            private final class StockFluidElement extends GTFluidSlotElement {

                private StockFluidElement() {
                    setBackgroundTexture(GuiTextures.SLOT_DARK);
                    setAllowClickFilled(false);
                    setAllowClickDrained(false);
                    setOnAddedTooltips((slot, tooltips) -> {
                        GenericStack stock = getMEFluidConfigSlot(index).getStock();
                        if (stock != null) {
                            tooltips.add(Component.literal(FormattingUtil.formatNumbers(stock.amount()) + " B"));
                        }
                    });
                    addEventListener(UIEvents.MOUSE_DOWN, FluidConfigSlot.this::handleStockClick);
                    refresh();
                }

                @Override
                public void screenTick() {
                    refresh();
                    super.screenTick();
                }

                private void refresh() {
                    GenericStack stock = getMEFluidConfigSlot(index).getStock();
                    setFluid(stock == null ? FluidStack.EMPTY : displayFluid(stock));
                }
            }
        }
    }

    @Override
    public UIElement createUIWidget(FancyMachineUIWidget shell) {
        Player player = shell.getOpeningPlayer();
        return new FluidConfig(player);
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
        configuratorPanel.attachConfigurators(new WorkingEnabledConfigurator(
                MEInputHatchPartMachine.this));
        if (isCircuitSlotEnabled()) {
            configuratorPanel.attachConfigurators(new CircuitFancyConfigurator(
                    MEInputHatchPartMachine.this));
        }
        if (MEInputHatchPartMachine.this instanceof MEStockingHatchPartMachine stockingHatch) {
            configuratorPanel.attachConfigurators(
                    new MEFluidAutoPullConfigurator(stockingHatch),
                    new AutoStockingConfigurator(stockingHatch));
        }
    }

    @Override
    public void attachSideTabs(TabsWidget tabs) {
        if (!isMEFluidStocking()) tabs.attachCachedSubTab(GTCEu.id("directional"),
                () -> new CombinedDirectionalFancyConfigurator(this, tabs.getPlayer()));
    }
}
