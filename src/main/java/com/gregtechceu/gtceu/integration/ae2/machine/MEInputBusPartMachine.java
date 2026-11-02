package com.gregtechceu.gtceu.integration.ae2.machine;

import com.gregtechceu.gtceu.api.blockentity.BlockEntityCreationInfo;
import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.gui.GuiTextures;
import com.gregtechceu.gtceu.api.gui.UITemplate;
import com.gregtechceu.gtceu.api.gui.factory.MachineUIHelper;
import com.gregtechceu.gtceu.api.gui.factory.MachineUIHolder;
import com.gregtechceu.gtceu.api.gui.fancy.ConfiguratorPanel;
import com.gregtechceu.gtceu.api.gui.fancy.FancyMachineUI;
import com.gregtechceu.gtceu.api.gui.fancy.FancyTabs;
import com.gregtechceu.gtceu.api.gui.fancy.FancyTooltipsPanel;
import com.gregtechceu.gtceu.api.gui.fancy.FancyUIProvider;
import com.gregtechceu.gtceu.api.gui.fancy.IFancyTooltip;
import com.gregtechceu.gtceu.api.gui.texture.IGuiTexture;
import com.gregtechceu.gtceu.api.machine.fancyconfigurator.AutoStockingConfigurator;
import com.gregtechceu.gtceu.api.machine.fancyconfigurator.CircuitConfigurator;
import com.gregtechceu.gtceu.api.machine.fancyconfigurator.DirectionalConfigurator;
import com.gregtechceu.gtceu.api.machine.fancyconfigurator.DistinctPartConfigurator;
import com.gregtechceu.gtceu.api.machine.fancyconfigurator.WorkingEnabledConfigurator;
import com.gregtechceu.gtceu.api.machine.feature.IDataStickInteractable;
import com.gregtechceu.gtceu.api.machine.feature.IHasCircuitSlot;
import com.gregtechceu.gtceu.api.machine.feature.multiblock.FancyPartUIProvider;
import com.gregtechceu.gtceu.api.machine.trait.NotifiableItemStackHandler;
import com.gregtechceu.gtceu.api.sync_system.SyncActionData;
import com.gregtechceu.gtceu.api.sync_system.annotations.SyncToClient;
import com.gregtechceu.gtceu.common.data.GTDataComponents;
import com.gregtechceu.gtceu.common.data.datacomponents.AEInputConfigCopyData;
import com.gregtechceu.gtceu.common.data.machines.GTAEMachines;
import com.gregtechceu.gtceu.common.item.behavior.IntCircuitBehaviour;
import com.gregtechceu.gtceu.integration.ae2.gui.element.AEItemConfigElement;
import com.gregtechceu.gtceu.integration.ae2.gui.fancy.MEItemAutoPullConfigurator;
import com.gregtechceu.gtceu.integration.ae2.slot.ExportOnlyAEItemList;
import com.gregtechceu.gtceu.integration.ae2.slot.ExportOnlyAEItemSlot;
import com.gregtechceu.gtceu.integration.ae2.slot.ExportOnlyAESlot;
import com.gregtechceu.gtceu.utils.GTMath;

import com.lowdragmc.lowdraglib2.gui.ui.UI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvent;

import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
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

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.BooleanSupplier;
import java.util.function.Predicate;

public class MEInputBusPartMachine extends MEBusPartMachine
                                   implements IDataStickInteractable, IHasCircuitSlot,
                                   FancyPartUIProvider, MEItemConfigActionTarget {

    static {
        MEItemConfigActions.initialize();
    }

    protected final static int CONFIG_SIZE = 16;

    protected ExportOnlyAEItemList aeItemHandler;

    /** Lossless client-facing page state rebuilt only after an authoritative value change. */
    @SyncToClient
    @Getter
    private AEItemConfigSnapshot itemConfigSnapshot;

    public MEInputBusPartMachine(BlockEntityCreationInfo info) {
        super(info, IO.IN);
        this.itemConfigSnapshot = AEItemConfigSnapshot.empty(isOnline(), isMEItemStocking(),
                isMEItemConfigAutoPull());
        this.aeItemHandler.setSnapshotChangeListener(this::onItemConfigContentsChanged);
    }

    /////////////////////////////////
    // ***** Machine LifeCycle ****//
    /////////////////////////////////

    @Override
    public void onLoad() {
        super.onLoad();
        if (!isRemote()) {
            scheduleForNextServerTick(this::refreshItemConfigSnapshot);
        }
    }

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
    public void setOnline(boolean online) {
        boolean changed = isOnline != online;
        super.setOnline(online);
        if (changed && !isRemote()) {
            refreshItemConfigSnapshot();
        }
    }

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
        validateSlotIndex(index);
        return aeItemHandler.getInventory()[index];
    }

    private void onItemConfigContentsChanged() {
        if (!isRemote()) {
            refreshItemConfigSnapshot();
        }
    }

    /** Rebuilds and publishes the page snapshot only when one observable value actually changed. */
    protected final void refreshItemConfigSnapshot() {
        AEItemConfigSnapshot refreshed = AEItemConfigSnapshot.capture(isOnline(), isMEItemStocking(),
                isMEItemConfigAutoPull(), aeItemHandler.getInventory());
        if (!refreshed.equals(itemConfigSnapshot)) {
            itemConfigSnapshot = refreshed;
            syncDataHolder.markClientSyncFieldDirty("itemConfigSnapshot");
        }
    }

    @Override
    public boolean canCreateUI(Player player, MachineUIHolder holder) {
        return holder.getMachine() == this && supportsMEItemConfigActions();
    }

    @Override
    public UI createUI(Player player, MachineUIHolder holder) {
        FancyUIProvider page = createPage(player, holder);
        return UI.of(new FancyMachineUI(page, player.getInventory(), holder,
                page.getPageWidth(), page.getPageHeight()));
    }

    /** Creates one standalone opening-scoped page after validating the exact holder and definition. */
    FancyUIProvider createPage(Player player, MachineUIHolder holder) {
        requireMatchingHolder(holder);
        if (!supportsMEItemConfigActions()) {
            throw new IllegalStateException("ME item configuration page requires an exact input bus definition.");
        }
        return new MEItemBusFancyPage(player, holder);
    }

    @Override
    public FancyUIProvider createFancyPage(Player player, MachineUIHolder holder) {
        return createPage(player, holder);
    }

    private void requireMatchingHolder(MachineUIHolder holder) {
        if (holder.getMachine() != this) {
            throw new IllegalArgumentException("ME item configuration page holder must resolve the opened bus.");
        }
    }

    private UIElement createMainElement(Player player, MachineUIHolder holder) {
        return createMainElement(player, holder, MachineUIHelper::sendAction,
                () -> player.level().isClientSide() && holder.getMachine() == this, UIEvent::isCtrlDown);
    }

    /** Builds a directly testable page body with injected action transport and modifier state. */
    UIElement createMainElement(Player player, MachineUIHolder holder,
                                BiConsumer<MachineUIHolder, SyncActionData> actionSender,
                                BooleanSupplier canSendAction, Predicate<UIEvent> ctrlDown) {
        requireMatchingHolder(holder);
        UIElement root = UITemplate.setBounds(new UIElement(), 0, 0, 150, 88);
        root.style(style -> style.backgroundTexture(GuiTextures.BACKGROUND_INVERSE));
        root.addChild(new AEItemConfigElement(this::getItemConfigSnapshot, player, holder,
                actionSender, canSendAction, ctrlDown));
        return root;
    }

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

    @Override
    @ApiStatus.Internal
    public boolean supportsMEItemConfigActions() {
        return getDefinition() == GTAEMachines.ITEM_IMPORT_BUS_ME ||
                getDefinition() == GTAEMachines.STOCKING_IMPORT_BUS_ME;
    }

    @Override
    @ApiStatus.Internal
    public int getMEItemConfigSlotCount() {
        return CONFIG_SIZE;
    }

    @Override
    @ApiStatus.Internal
    public boolean isMEItemConfigAutoPull() {
        return false;
    }

    @Override
    @ApiStatus.Internal
    public boolean isMEItemStocking() {
        return false;
    }

    @Override
    @ApiStatus.Internal
    public boolean canSetMEItemConfig(int slot, @NotNull ItemStack item) {
        validateSlotIndex(slot);
        if (!supportsMEItemConfigActions() || isMEItemConfigAutoPull()) {
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

    @Override
    @ApiStatus.Internal
    public void setMEItemConfig(int slot, @NotNull ItemStack item) {
        if (!canSetMEItemConfig(slot, item)) {
            throw new IllegalStateException("ME item configuration mutation is not permitted.");
        }
        getMEItemConfigSlot(slot).setConfig(item.isEmpty() ? null :
                new GenericStack(AEItemKey.of(item), item.getCount()));
    }

    @Override
    @ApiStatus.Internal
    public boolean canSetMEItemConfigAmount(int slot, @NotNull ItemStack expectedItem, int amount) {
        validateSlotIndex(slot);
        if (!supportsMEItemConfigActions() || isMEItemStocking() || isMEItemConfigAutoPull() ||
                expectedItem.isEmpty() || amount <= 0) {
            return false;
        }
        GenericStack config = getMEItemConfigSlot(slot).getConfig();
        return config != null && config.what().equals(AEItemKey.of(expectedItem));
    }

    @Override
    @ApiStatus.Internal
    public void setMEItemConfigAmount(int slot, @NotNull ItemStack expectedItem, int amount) {
        validateSlotIndex(slot);
        ExportOnlyAEItemSlot configSlot = getMEItemConfigSlot(slot);
        GenericStack config = configSlot.getConfig();
        if (!supportsMEItemConfigActions() || isMEItemStocking() || isMEItemConfigAutoPull() ||
                expectedItem.isEmpty() || amount <= 0 || config == null ||
                !config.what().equals(AEItemKey.of(expectedItem))) {
            throw new IllegalStateException("ME item configuration amount mutation is not permitted.");
        }
        configSlot.setConfig(ExportOnlyAESlot.copy(config, amount));
    }

    @Override
    @ApiStatus.Internal
    public boolean canPickupMEItemConfigStock(@NotNull ServerPlayer player, int slot,
                                              @NotNull ItemStack expectedItem) {
        validateSlotIndex(slot);
        if (!supportsMEItemConfigActions() || isMEItemStocking() || isMEItemConfigAutoPull() ||
                expectedItem.isEmpty() || !player.containerMenu.getCarried().isEmpty()) {
            return false;
        }
        GenericStack stock = getMEItemConfigSlot(slot).getStock();
        return stock != null && stock.amount() > 0 && stock.what().equals(AEItemKey.of(expectedItem));
    }

    @Override
    @ApiStatus.Internal
    public void pickupMEItemConfigStock(@NotNull ServerPlayer player, int slot,
                                        @NotNull ItemStack expectedItem) {
        if (!canPickupMEItemConfigStock(player, slot, expectedItem)) {
            throw new IllegalStateException("ME item stock pickup is not permitted.");
        }
        ExportOnlyAEItemSlot configSlot = getMEItemConfigSlot(slot);
        GenericStack stock = configSlot.getStock();
        if (stock == null || !(stock.what() instanceof AEItemKey itemKey)) {
            throw new IllegalStateException("ME item stock pickup lost its validated item stack.");
        }
        ItemStack pickedUp = itemKey.toStack(GTMath.saturatedCast(stock.amount()));
        player.containerMenu.setCarried(pickedUp);
        long remaining = stock.amount() - pickedUp.getCount();
        configSlot.setStock(remaining > 0 ? new GenericStack(itemKey, remaining) : null);
    }

    @Override
    @ApiStatus.Internal
    public void setMEItemAutoPull(boolean autoPull) {
        throw new IllegalStateException("Ordinary ME item input buses do not support auto-pull.");
    }

    private static void validateSlotIndex(int slot) {
        if (slot < 0 || slot >= CONFIG_SIZE) {
            throw new IllegalArgumentException("ME item configuration slot is out of range: " + slot);
        }
    }

    /** One opening-scoped Fancy home page shared by standalone and contextual part navigation. */
    private final class MEItemBusFancyPage implements FancyUIProvider {

        private final Player player;
        private final MachineUIHolder holder;
        @Nullable
        private final DirectionalConfigurator directionalPage;

        private MEItemBusFancyPage(Player player, MachineUIHolder holder) {
            requireMatchingHolder(holder);
            this.player = player;
            this.holder = holder;
            this.directionalPage = isMEItemStocking() ? null :
                    new DirectionalConfigurator(MEInputBusPartMachine.this, player, holder);
        }

        @Override
        public UIElement createMainPage(FancyMachineUI shell) {
            if (holder.getMachine() != MEInputBusPartMachine.this) {
                throw new IllegalStateException("ME item configuration page holder no longer resolves its bus.");
            }
            return createMainElement(player, holder);
        }

        @Override
        public IGuiTexture getTabIcon() {
            return GuiTextures.itemStack(getDefinition().getItem());
        }

        @Override
        public Component getTitle() {
            return Component.translatable(getDefinition().getDescriptionId());
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
        public void attachSideTabs(FancyTabs tabs) {
            if (directionalPage != null) {
                tabs.attachSubTab(directionalPage);
            }
        }

        @Override
        public void attachConfigurators(ConfiguratorPanel configuratorPanel) {
            configuratorPanel.attachConfigurators(new WorkingEnabledConfigurator(
                    MEInputBusPartMachine.this, holder));
            DistinctPartConfigurator.attachConfigurators(configuratorPanel, MEInputBusPartMachine.this);
            if (isHasCircuitSlot() && isCircuitSlotEnabled()) {
                configuratorPanel.attachConfigurators(new CircuitConfigurator(
                        MEInputBusPartMachine.this, holder));
            }
            if (MEInputBusPartMachine.this instanceof MEStockingBusPartMachine stockingBus) {
                configuratorPanel.attachConfigurators(
                        new MEItemAutoPullConfigurator(stockingBus, holder),
                        new AutoStockingConfigurator(stockingBus, holder));
            }
        }

        @Override
        public void attachTooltips(FancyTooltipsPanel tooltipsPanel) {
            tooltipsPanel.attachTooltips(MEInputBusPartMachine.this);
            for (var trait : getTraitHolder().getAllTraits()) {
                if (trait instanceof IFancyTooltip tooltip) {
                    tooltipsPanel.attachTooltips(tooltip);
                }
            }
        }

        @Override
        public List<Component> getTabTooltips() {
            return List.of(Component.translatable(getDefinition().getDescriptionId()));
        }

        @Override
        public PageGroupingData getPageGroupingData() {
            return new PageGroupingData("gtpm.multiblock.page_switcher.io.import", 1);
        }
    }
}
