package com.gregtechceu.gtceu.common.machine.multiblock.part;

import com.gregtechceu.gtceu.api.blockentity.BlockEntityCreationInfo;
import com.gregtechceu.gtceu.api.blockentity.IPaintable;
import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.cover.filter.FilterHandler;
import com.gregtechceu.gtceu.api.cover.filter.FilterHandlers;
import com.gregtechceu.gtceu.api.cover.filter.ItemFilter;
import com.gregtechceu.gtceu.api.gui.GuiTextures;
import com.gregtechceu.gtceu.api.gui.UITemplate;
import com.gregtechceu.gtceu.api.gui.element.GTItemSlotElement;
import com.gregtechceu.gtceu.api.gui.factory.MachineUIHolder;
import com.gregtechceu.gtceu.api.gui.fancy.ConfiguratorPanel;
import com.gregtechceu.gtceu.api.gui.fancy.FancyMachineUIWidget;
import com.gregtechceu.gtceu.api.gui.fancy.IFancyTooltip;
import com.gregtechceu.gtceu.api.gui.fancy.IFancyUIProvider;
import com.gregtechceu.gtceu.api.gui.fancy.TabsWidget;
import com.gregtechceu.gtceu.api.gui.fancy.TooltipsPanel;
import com.gregtechceu.gtceu.api.gui.texture.IGuiTexture;
import com.gregtechceu.gtceu.api.machine.MachineDefinition;
import com.gregtechceu.gtceu.api.machine.TickableSubscription;
import com.gregtechceu.gtceu.api.machine.fancyconfigurator.CircuitFancyConfigurator;
import com.gregtechceu.gtceu.api.machine.fancyconfigurator.CombinedDirectionalFancyConfigurator;
import com.gregtechceu.gtceu.api.machine.fancyconfigurator.DistinctPartConfigurator;
import com.gregtechceu.gtceu.api.machine.fancyconfigurator.WorkingEnabledConfigurator;
import com.gregtechceu.gtceu.api.machine.feature.FancyActionMachine;
import com.gregtechceu.gtceu.api.machine.feature.IHasCircuitSlot;
import com.gregtechceu.gtceu.api.machine.feature.IUIMachine;
import com.gregtechceu.gtceu.api.machine.feature.multiblock.IDistinctPart;
import com.gregtechceu.gtceu.api.machine.multiblock.MultiblockControllerMachine;
import com.gregtechceu.gtceu.api.machine.multiblock.part.TieredIOPartMachine;
import com.gregtechceu.gtceu.api.machine.trait.NotifiableItemStackHandler;
import com.gregtechceu.gtceu.api.sync_system.annotations.SaveField;
import com.gregtechceu.gtceu.api.sync_system.annotations.ServerFieldChangeListener;
import com.gregtechceu.gtceu.api.sync_system.annotations.ServerFieldNormalizer;
import com.gregtechceu.gtceu.api.sync_system.annotations.SyncBoth;
import com.gregtechceu.gtceu.api.sync_system.annotations.SyncToClient;
import com.gregtechceu.gtceu.common.data.GTMachines;
import com.gregtechceu.gtceu.common.item.behavior.IntCircuitBehaviour;
import com.gregtechceu.gtceu.config.ConfigHolder;
import com.gregtechceu.gtceu.integration.xei.GTXEIHelper;
import com.gregtechceu.gtceu.utils.ExtendedUseOnContext;
import com.gregtechceu.gtceu.utils.GTTransferUtils;
import com.gregtechceu.gtceu.utils.ISubscription;

import com.lowdragmc.lowdraglib2.gui.ui.UI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import lombok.AccessLevel;
import lombok.Getter;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class ItemBusPartMachine extends TieredIOPartMachine
                                implements IDistinctPart, IHasCircuitSlot, IPaintable, FancyActionMachine,
                                IUIMachine {

    @Getter
    @SaveField
    private final NotifiableItemStackHandler inventory;
    @Nullable
    protected TickableSubscription autoIOSubs;
    @Nullable
    protected ISubscription inventorySubs;
    @Getter(AccessLevel.PROTECTED)
    private boolean hasCircuitSlot = true;
    @Getter
    @SaveField
    @SyncToClient
    protected boolean circuitSlotEnabled;
    @Getter
    @SaveField
    protected final NotifiableItemStackHandler circuitInventory;
    @Getter
    @SaveField
    @SyncBoth
    private boolean isDistinct = false;
    @SaveField
    @SyncToClient
    @Getter
    protected final FilterHandler<ItemStack, ItemFilter> filterHandler;

    public ItemBusPartMachine(BlockEntityCreationInfo info, int tier, IO io) {
        super(info, tier, io);
        this.inventory = attachTrait(createInventory());
        this.circuitSlotEnabled = true;
        this.circuitInventory = attachTrait(createCircuitItemHandler(io)).shouldSearchContent(false);
        filterHandler = FilterHandlers.item(this);

        inventory.setFilter(this::matchesFilter);
    }

    //////////////////////////////////////
    // ***** Initialization ******//
    //////////////////////////////////////

    protected int getInventorySize() {
        int sizeRoot = 1 + Math.min(9, getTier());
        return sizeRoot * sizeRoot;
    }

    protected NotifiableItemStackHandler createInventory() {
        return new NotifiableItemStackHandler(getInventorySize(), io);
    }

    protected boolean matchesFilter(ItemStack stack) {
        if (filterHandler.isFilterPresent())
            return filterHandler.getFilter().test(stack);
        return true;
    }

    protected NotifiableItemStackHandler createCircuitItemHandler(IO io) {
        if (io == IO.IN) {
            return new NotifiableItemStackHandler(1, IO.IN, IO.NONE)
                    .setFilter(IntCircuitBehaviour::isIntegratedCircuit)
                    .shouldDropInventoryInWorld(!ConfigHolder.INSTANCE.machines.ghostCircuit);
        } else {
            hasCircuitSlot = false;
            setCircuitSlotEnabled(false);
            return new NotifiableItemStackHandler(0, IO.NONE);
        }
    }

    @Override
    public void onLoad() {
        super.onLoad();
        scheduleForNextServerTick(this::updateInventorySubscription);
        getHandlerList().setDistinct(isDistinct);
        getHandlerList().setColor(getPaintingColor());
        inventorySubs = getInventory().addChangedListener(this::updateInventorySubscription);
    }

    @Override
    public void onUnload() {
        super.onUnload();
        if (inventorySubs != null) {
            inventorySubs.unsubscribe();
            inventorySubs = null;
        }
    }

    @Override
    public void onPaintingColorChanged(int color) {
        getHandlerList().setColor(color, true);
    }

    @Override
    public void setDistinct(boolean distinct) {
        isDistinct = normalizeDistinct(distinct);
        getHandlerList().setDistinctAndNotify(isDistinct);
    }

    @ServerFieldNormalizer(fieldName = "isDistinct")
    private boolean normalizeDistinct(boolean distinct) {
        return io != IO.OUT && distinct;
    }

    @ServerFieldChangeListener(fieldName = "isDistinct")
    private void onDistinctChanged(boolean oldDistinct, boolean newDistinct) {
        getHandlerList().setDistinctAndNotify(newDistinct);
    }

    @Override
    public void addedToController(MultiblockControllerMachine controller, String structureName) {
        if (hasCircuitSlot && !controller.allowCircuitSlots()) {
            if (!ConfigHolder.INSTANCE.machines.ghostCircuit) {
                circuitInventory.dropInventoryInWorld();
            } else {
                circuitInventory.setStackInSlot(0, ItemStack.EMPTY);
            }
            setCircuitSlotEnabled(false);
        }
        super.addedToController(controller, structureName);
    }

    @Override
    public void removedFromController(MultiblockControllerMachine controller, String structureName) {
        super.removedFromController(controller, structureName);
        if (!hasCircuitSlot) return;
        for (var c : controllers) {
            if (!c.allowCircuitSlots()) {
                return;
            }
        }
        setCircuitSlotEnabled(true);
    }

    @Override
    public int tintColor(int index) {
        if (index == 9) return getRealColor();
        return -1;
    }

    public void setCircuitSlotEnabled(boolean enabled) {
        circuitSlotEnabled = enabled;
    }

    //////////////////////////////////////
    // ******** Auto IO *********//
    //////////////////////////////////////

    @Override
    public void onNeighborChanged(Block block, BlockPos fromPos, boolean isMoving) {
        super.onNeighborChanged(block, fromPos, isMoving);
        updateInventorySubscription();
    }

    @Override
    public void onRotated(Direction oldFacing, Direction newFacing) {
        super.onRotated(oldFacing, newFacing);
        updateInventorySubscription(newFacing);
    }

    protected void updateInventorySubscription() {
        updateInventorySubscription(getFrontFacing());
    }

    protected void updateInventorySubscription(Direction newFacing) {
        if (isWorkingEnabled() && ((io.support(IO.OUT) && !getInventory().isEmpty()) || io.support(IO.IN)) &&
                GTTransferUtils.hasAdjacentItemHandler(getLevel(), getBlockPos(), newFacing)) {
            autoIOSubs = subscribeServerTick(autoIOSubs, this::autoIO);
        } else if (autoIOSubs != null) {
            autoIOSubs.unsubscribe();
            autoIOSubs = null;
        }
    }

    protected void autoIO() {
        if (getOffsetTimer() % 5 == 0) {
            if (isWorkingEnabled()) {
                if (io == IO.OUT) {
                    getInventory().exportToNearby(getFrontFacing());
                } else if (io == IO.IN) {
                    getInventory().importFromNearby(getFrontFacing());
                } else if (io == IO.BOTH) {
                    getInventory().importFromNearby(getFrontFacing());
                    getInventory().exportToNearby(getFrontFacing().getOpposite());
                }
            }
            updateInventorySubscription();
        }
    }

    @Override
    protected void onWorkingEnabledChanged() {
        super.onWorkingEnabledChanged();
        updateInventorySubscription();
    }

    @Override
    protected InteractionResult onScrewdriverClick(ExtendedUseOnContext context) {
        InteractionResult superResult = super.onScrewdriverClick(context);
        if (superResult != InteractionResult.PASS) return superResult;
        if (io == IO.BOTH) return InteractionResult.PASS;
        if (context.getPlayer().isShiftKeyDown()) {
            if (swapIO()) {
                return InteractionResult.sidedSuccess(isRemote());
            }
        }
        return InteractionResult.PASS;
    }

    public boolean swapIO() {
        BlockPos blockPos = getBlockPos();
        MachineDefinition newDefinition = null;
        if (io.support(IO.IN)) {
            newDefinition = GTMachines.ITEM_EXPORT_BUS[this.getTier()];
        } else if (io.support(IO.OUT)) {
            newDefinition = GTMachines.ITEM_IMPORT_BUS[this.getTier()];
        }
        if (newDefinition == null) return false;

        BlockState newBlockState = newDefinition.getBlock().defaultBlockState();
        getLevel().setBlockAndUpdate(blockPos, newBlockState);

        if (getLevel().getBlockEntity(blockPos) instanceof ItemBusPartMachine newMachine) {
            // We don't set the circuit or distinct buses, since
            // that doesn't make sense on an output bus.
            // Furthermore, existing inventory items
            // and conveyors will drop to the floor on block override.
            newMachine.setFrontFacing(this.getFrontFacing());
            newMachine.setUpwardsFacing(this.getUpwardsFacing());
            newMachine.setPaintingColor(this.getPaintingColor());
        }
        return true;
    }

    //////////////////////////////////////
    // ********** GUI ***********//
    //////////////////////////////////////

    @Override
    public boolean canCreateUI(Player player, MachineUIHolder holder) {
        return holder.getMachine() == this && supportsGenericPage();
    }

    @Override
    public UI createUI(Player player, MachineUIHolder holder) {
        IFancyUIProvider page = createPage(player, holder);
        return UI.of(new FancyMachineUIWidget(page, player.getInventory(), holder,
                page.getPageWidth(), page.getPageHeight()));
    }

    IFancyUIProvider createPage(Player player, MachineUIHolder holder) {
        requireBusHolder(holder);
        if (!supportsGenericPage()) {
            throw new IllegalStateException("Item Bus definition requires its specialized UI provider.");
        }
        return new ItemBusPage(player, holder);
    }

    private void requireBusHolder(MachineUIHolder holder) {
        if (holder.getMachine() != this) {
            throw new IllegalArgumentException("Item Bus page holder must resolve the opened machine.");
        }
    }

    /**
     * Determines whether this definition may reuse the generic holder-scoped LDLib2 Item Bus page.
     */
    protected boolean supportsGenericPage() {
        return getClass() == ItemBusPartMachine.class;
    }

    int getInventoryPageWidth() {
        return 18 * getRowSize() + 16;
    }

    int getInventoryPageHeight() {
        return 18 * getColumnSize() + 16;
    }

    @Nullable
    IFancyUIProvider.PageGroupingData getPageGroupingData() {
        return switch (io) {
            case IN -> new IFancyUIProvider.PageGroupingData(
                    "gtpm.multiblock.page_switcher.io.import", 1);
            case OUT -> new IFancyUIProvider.PageGroupingData(
                    "gtpm.multiblock.page_switcher.io.export", 2);
            case BOTH -> new IFancyUIProvider.PageGroupingData(
                    "gtpm.multiblock.page_switcher.io.both", 3);
            case NONE -> null;
        };
    }

    private int getRowSize() {
        return getInventorySize() == 8 ? 4 : (int) Math.sqrt(getInventorySize());
    }

    private int getColumnSize() {
        return getInventorySize() == 8 ? 2 : (int) Math.sqrt(getInventorySize());
    }

    private UIElement createMainElement() {
        int rowSize = getRowSize();
        int columnSize = getColumnSize();
        UIElement root = UITemplate.setBounds(new UIElement(), 0, 0,
                getInventoryPageWidth(), getInventoryPageHeight());
        UIElement container = UITemplate.setBounds(new UIElement(), 4, 4,
                18 * rowSize + 8, 18 * columnSize + 8);
        UITemplate.setBackgroundTexture(container, GuiTextures.BACKGROUND_INVERSE);

        int index = 0;
        for (int y = 0; y < columnSize; y++) {
            for (int x = 0; x < rowSize; x++) {
                container.addChild(createInventorySlot(index++, 4 + x * 18, 4 + y * 18));
            }
        }

        if (io == IO.OUT) {
            UIElement filterSlot = filterHandler.createFilterSlotUI(
                    71 + (18 * rowSize) / 2, 35 + 9 * rowSize);
            filterSlot.style(style -> style.tooltips(Component.translatable("cover.item_filter.title")));
            root.addChild(filterSlot);
        }
        root.addChild(container);
        return root;
    }

    private GTItemSlotElement createInventorySlot(int index, int x, int y) {
        GTItemSlotElement slot = new GTItemSlotElement(getInventory().storage, index)
                .setCanPutItems(io.support(IO.IN))
                .setCanTakeItems(true)
                .setBackgroundTexture(GuiTextures.SLOT)
                .setIngredientIO(io.support(IO.IN) ? GTXEIHelper.input() : GTXEIHelper.output());
        return UITemplate.setBounds(slot, x, y, 18, 18);
    }

    /**
     * Captures the validated opening-scoped holder for all Item Bus page actions and contextual tabs.
     */
    private final class ItemBusPage implements IFancyUIProvider {

        private final MachineUIHolder holder;
        private final CombinedDirectionalFancyConfigurator directionalPage;

        private ItemBusPage(Player player, MachineUIHolder holder) {
            requireBusHolder(holder);
            this.holder = holder;
            this.directionalPage = new CombinedDirectionalFancyConfigurator(ItemBusPartMachine.this, player, holder);
        }

        @Override
        public UIElement createUIWidget(FancyMachineUIWidget shell) {
            if (holder.getMachine() != ItemBusPartMachine.this) {
                throw new IllegalStateException("Item Bus page holder no longer resolves its opened machine.");
            }
            return createMainElement();
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
            return ItemBusPartMachine.this.getInventoryPageWidth();
        }

        @Override
        public int getPageHeight() {
            return ItemBusPartMachine.this.getInventoryPageHeight();
        }

        @Override
        public void attachSideTabs(TabsWidget tabs) {
            tabs.attachSubTab(directionalPage);
        }

        @Override
        public void attachConfigurators(ConfiguratorPanel configuratorPanel) {
            configuratorPanel.attachConfigurators(new WorkingEnabledConfigurator(
                    ItemBusPartMachine.this, holder));
            if (io != IO.IN) {
                return;
            }
            DistinctPartConfigurator.attachConfigurators(configuratorPanel, ItemBusPartMachine.this);
            if (isHasCircuitSlot() && isCircuitSlotEnabled()) {
                configuratorPanel.attachConfigurators(new CircuitFancyConfigurator(
                        ItemBusPartMachine.this, holder));
            }
        }

        @Override
        public void attachTooltips(TooltipsPanel tooltipsPanel) {
            tooltipsPanel.attachTooltips(ItemBusPartMachine.this);
            getTraitHolder().getAllTraits().stream()
                    .filter(IFancyTooltip.class::isInstance)
                    .map(IFancyTooltip.class::cast)
                    .forEach(tooltipsPanel::attachTooltips);
        }

        @Override
        public List<Component> getTabTooltips() {
            return List.of(Component.translatable(getDefinition().getDescriptionId()));
        }

        @Override
        @Nullable
        public IFancyUIProvider.PageGroupingData getPageGroupingData() {
            return ItemBusPartMachine.this.getPageGroupingData();
        }
    }
}
