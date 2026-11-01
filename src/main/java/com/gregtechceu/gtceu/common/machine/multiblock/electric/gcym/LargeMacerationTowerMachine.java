package com.gregtechceu.gtceu.common.machine.multiblock.electric.gcym;

import com.gregtechceu.gtceu.api.blockentity.BlockEntityCreationInfo;
import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.capability.recipe.ItemRecipeCapability;
import com.gregtechceu.gtceu.api.gui.GuiTextures;
import com.gregtechceu.gtceu.api.gui.UITemplate;
import com.gregtechceu.gtceu.api.gui.element.GTComponentPanelElement;
import com.gregtechceu.gtceu.api.gui.element.GTLabelElement;
import com.gregtechceu.gtceu.api.gui.element.GTScrollerViewElement;
import com.gregtechceu.gtceu.api.gui.factory.MachineUIHolder;
import com.gregtechceu.gtceu.api.gui.factory.MachineUIHolderContext;
import com.gregtechceu.gtceu.api.gui.factory.MachineUIProvider;
import com.gregtechceu.gtceu.api.gui.fancy.ConfiguratorPanel;
import com.gregtechceu.gtceu.api.gui.fancy.FancyMachineUI;
import com.gregtechceu.gtceu.api.gui.fancy.FancyTabs;
import com.gregtechceu.gtceu.api.gui.fancy.FancyTooltipsPanel;
import com.gregtechceu.gtceu.api.gui.fancy.FancyUIProvider;
import com.gregtechceu.gtceu.api.gui.texture.IGuiTexture;
import com.gregtechceu.gtceu.api.machine.ConditionalSubscriptionHandler;
import com.gregtechceu.gtceu.api.machine.TickableSubscription;
import com.gregtechceu.gtceu.api.machine.fancyconfigurator.BatchModeConfigurator;
import com.gregtechceu.gtceu.api.machine.fancyconfigurator.DirectionalConfigurator;
import com.gregtechceu.gtceu.api.machine.fancyconfigurator.VoidingModeConfigurator;
import com.gregtechceu.gtceu.api.machine.fancyconfigurator.WorkingEnabledConfigurator;
import com.gregtechceu.gtceu.api.machine.feature.FancyActionMachine;
import com.gregtechceu.gtceu.api.machine.feature.multiblock.FancyPartUIProvider;
import com.gregtechceu.gtceu.api.machine.feature.multiblock.IMaintenanceMachine;
import com.gregtechceu.gtceu.api.machine.feature.multiblock.IMultiPart;
import com.gregtechceu.gtceu.api.machine.multiblock.WorkableElectricMultiblockMachine;
import com.gregtechceu.gtceu.api.multiblock.util.RelativeDirection;
import com.gregtechceu.gtceu.api.sync_system.annotations.SyncToClient;

import com.lowdragmc.lowdraglib2.gui.ui.UI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.data.Horizontal;
import com.lowdragmc.lowdraglib2.gui.ui.data.ScrollDisplay;
import com.lowdragmc.lowdraglib2.gui.ui.data.ScrollerMode;
import com.lowdragmc.lowdraglib2.gui.ui.data.Vertical;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;

import lombok.AccessLevel;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class LargeMacerationTowerMachine extends WorkableElectricMultiblockMachine
                                         implements MachineUIProvider, FancyActionMachine {

    @NotNull
    private AABB grindBound = new AABB(BlockPos.ZERO);
    @NotNull
    private final List<IItemHandler> handlers = new ArrayList<>();

    private TickableSubscription hurtSub;
    /** Keeps the client display supplied from a server-owned snapshot while the structure is formed. */
    @Getter(AccessLevel.PACKAGE)
    private final ConditionalSubscriptionHandler displaySnapshotSubscription;
    /** Carries the complete legacy display text without relying on the old Widget sync path. */
    @Getter(AccessLevel.PACKAGE)
    @SyncToClient
    private List<Component> displaySnapshot = List.of();

    public LargeMacerationTowerMachine(BlockEntityCreationInfo info) {
        super(info);
        this.displaySnapshotSubscription = new ConditionalSubscriptionHandler(this, this::refreshDisplaySnapshot,
                this::isFormed);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (!isRemote()) {
            refreshDisplaySnapshot();
            displaySnapshotSubscription.initialize(getLevel());
        }
    }

    @Override
    public void formStructure(String structureName) {
        super.formStructure(structureName);
        if (!DEFAULT_STRUCTURE.equals(structureName)) return;
        updateBounds();
        for (var holder : getCapabilitiesFlat(IO.IN, ItemRecipeCapability.CAP)) {
            if (holder instanceof IItemHandler ih) {
                handlers.add(ih);
            }
        }
        hurtSub = subscribeServerTick(this::spinWheels);
        if (!isRemote()) {
            refreshDisplaySnapshot();
            displaySnapshotSubscription.updateSubscription();
        }
    }

    @Override
    public void invalidateStructure(String structureName) {
        super.invalidateStructure(structureName);
        if (!DEFAULT_STRUCTURE.equals(structureName)) return;
        clearFormedRuntimeState();
        if (!isRemote()) {
            refreshDisplaySnapshot();
            displaySnapshotSubscription.updateSubscription();
        }
    }

    @Override
    public void onUnload() {
        super.onUnload();
        resetRuntimeState();
    }

    @Override
    public void onPartUnload() {
        super.onPartUnload();
        resetRuntimeState();
    }

    /** Clears the wheel runtime that is valid only while the primary structure remains formed. */
    private void clearFormedRuntimeState() {
        unsubscribe(hurtSub);
        hurtSub = null;
        handlers.clear();
    }

    /** Clears every runtime subscription and snapshot when the controller can no longer serve its UI. */
    private void resetRuntimeState() {
        clearFormedRuntimeState();
        displaySnapshot = List.of();
        displaySnapshotSubscription.unsubscribe();
    }

    @Override
    public void addDisplayText(@NotNull List<Component> textList) {
        textList.addAll(displaySnapshot);
    }

    /** Rebuilds the immutable client snapshot from the server's complete legacy display contract. */
    void refreshDisplaySnapshot() {
        List<Component> nextSnapshot = new ArrayList<>();
        collectServerDisplayText(nextSnapshot);
        nextSnapshot = List.copyOf(nextSnapshot);
        if (!displaySnapshot.equals(nextSnapshot)) {
            displaySnapshot = nextSnapshot;
        }
    }

    /** Separates live server-state collection from snapshot publication for direct lifecycle verification. */
    protected void collectServerDisplayText(List<Component> textList) {
        super.addDisplayText(textList);
    }

    private void updateBounds() {
        var fl = RelativeDirection.offsetPos(getBlockPos(), getFrontFacing(), getUpwardsFacing(), isFlipped(), 1, 1,
                -1);
        var br = RelativeDirection.offsetPos(getBlockPos(), getFrontFacing(), getUpwardsFacing(), isFlipped(), 2, -2,
                -4);
        grindBound = AABB.encapsulatingFullBlocks(fl, br);
    }

    private void spinWheels() {
        if (isRemote() || getLevel() == null) return;
        if (getOffsetTimer() % 10 != 0) return;

        List<ItemEntity> itemEntities = new ArrayList<>();
        for (var entity : getLevel().getEntities(null, grindBound)) {
            if (entity instanceof ItemEntity ie) {
                itemEntities.add(ie);
            } else {
                if (getWorkLogic().isWorking()) {
                    entity.hurt(entity.damageSources().cramming(), 2.0f);
                }
            }
        }

        if (handlers.isEmpty()) return;

        for (ItemEntity item : itemEntities) {
            if (item.isRemoved()) continue;
            for (var holder : handlers) {
                item.setItem(ItemHandlerHelper.insertItem(holder, item.getItem(), false));
                if (item.getItem().isEmpty()) {
                    item.discard();
                    break;
                }
            }
        }
    }

    @Override
    public boolean canCreateUI(Player player, MachineUIHolder holder) {
        return holder.getMachine() == this;
    }

    @Override
    public UI createUI(Player player, MachineUIHolder holder) {
        requireMatchingHolder(holder);
        LargeMacerationTowerFancyPage page = new LargeMacerationTowerFancyPage(player, holder);
        return UI.of(new FancyMachineUI(page, player.getInventory(), holder,
                page.getPageWidth(), page.getPageHeight()));
    }

    private void requireMatchingHolder(MachineUIHolder holder) {
        if (holder.getMachine() != this) {
            throw new IllegalArgumentException("Large Maceration Tower UI holder must resolve the opened controller.");
        }
    }

    /** Owns the opening-scoped controller, directional, and contextual part pages. */
    private final class LargeMacerationTowerFancyPage implements FancyUIProvider {

        private static final int PAGE_WIDTH = 190;
        private static final int PAGE_HEIGHT = 125;

        private final MachineUIHolder holder;
        private final DirectionalConfigurator directionalPage;
        private final List<FancyUIProvider> partPages;

        private LargeMacerationTowerFancyPage(Player player, MachineUIHolder holder) {
            requireMatchingHolder(holder);
            this.holder = holder;
            this.directionalPage = new DirectionalConfigurator(LargeMacerationTowerMachine.this,
                    player, holder);

            List<FancyUIProvider> pages = new ArrayList<>();
            for (IMultiPart part : getParts()) {
                if (!(part instanceof FancyPartUIProvider pageProvider)) {
                    throw new IllegalStateException("Large Maceration Tower part has no LDLib2 Fancy page: " +
                            part.self().getDefinition().getId());
                }
                MachineUIHolder partHolder = new MachineUIHolderContext(player, part.self());
                pages.add(pageProvider.createFancyPage(player, partHolder));
            }
            this.partPages = List.copyOf(pages);
        }

        @Override
        public UIElement createMainPage(FancyMachineUI shell) {
            if (holder.getMachine() != LargeMacerationTowerMachine.this) {
                throw new IllegalStateException(
                        "Large Maceration Tower page holder no longer resolves its controller.");
            }

            UIElement root = UITemplate.setLDLib2Bounds(new UIElement(), 0, 0, PAGE_WIDTH, PAGE_HEIGHT);
            root.style(style -> style.backgroundTexture(GuiTextures.BACKGROUND_INVERSE));

            GTScrollerViewElement screen = new GTScrollerViewElement(4, 4, 182, 117);
            screen.style(style -> style.backgroundTexture(getScreenTexture()));
            screen.viewPort(viewPort -> viewPort
                    .layout(layout -> layout.paddingAll(0))
                    .style(style -> style.backgroundTexture(getScreenTexture())));
            screen.scrollerStyle(style -> style
                    .mode(ScrollerMode.VERTICAL)
                    .verticalScrollDisplay(ScrollDisplay.AUTO)
                    .horizontalScrollDisplay(ScrollDisplay.NEVER));

            GTLabelElement title = new GTLabelElement(4, 5, 174, 10,
                    getBlockState().getBlock().getDescriptionId(), true);
            title.textStyle(style -> style
                    .textColor(0x404040)
                    .textShadow(false)
                    .textAlignHorizontal(Horizontal.LEFT)
                    .textAlignVertical(Vertical.CENTER));
            screen.addScrollViewChild(title);
            screen.addScrollViewChild(new GTComponentPanelElement(4, 17,
                    LargeMacerationTowerMachine.this::addDisplayText)
                    .setMaxWidthLimit(200)
                    .clickHandler(LargeMacerationTowerMachine.this::handleDisplayClick));
            root.addChild(screen);
            return root;
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
            return PAGE_WIDTH;
        }

        @Override
        public int getPageHeight() {
            return PAGE_HEIGHT;
        }

        @Override
        public void attachSideTabs(FancyTabs tabs) {
            tabs.attachSubTab(directionalPage);
        }

        @Override
        public void attachConfigurators(ConfiguratorPanel configuratorPanel) {
            VoidingModeConfigurator.attachConfigurators(
                    configuratorPanel, LargeMacerationTowerMachine.this);
            BatchModeConfigurator.attachConfigurators(
                    configuratorPanel, LargeMacerationTowerMachine.this);
            configuratorPanel.attachConfigurators(new WorkingEnabledConfigurator(
                    LargeMacerationTowerMachine.this, holder));
        }

        @Override
        public void attachTooltips(FancyTooltipsPanel tooltipsPanel) {
            for (IMultiPart part : getParts()) {
                if (part instanceof IMaintenanceMachine maintenanceMachine) {
                    maintenanceMachine.attachMaintenanceTooltips(tooltipsPanel);
                }
            }
        }

        @Override
        public List<FancyUIProvider> getSubTabs() {
            return partPages;
        }

        @Override
        public List<Component> getTabTooltips() {
            return List.of(Component.translatable(getDefinition().getDescriptionId()));
        }
    }
}
