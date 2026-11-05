package com.gregtechceu.gtceu.common.machine.multiblock.electric.research;

import com.gregtechceu.gtceu.api.blockentity.BlockEntityCreationInfo;
import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.gui.GuiTextures;
import com.gregtechceu.gtceu.api.gui.UITemplate;
import com.gregtechceu.gtceu.api.gui.element.GTComponentPanelElement;
import com.gregtechceu.gtceu.api.gui.element.GTLabelElement;
import com.gregtechceu.gtceu.api.gui.element.GTScrollerViewElement;
import com.gregtechceu.gtceu.api.gui.factory.MachineUIHolder;
import com.gregtechceu.gtceu.api.gui.factory.MachineUIHolderContext;
import com.gregtechceu.gtceu.api.gui.fancy.ConfiguratorPanel;
import com.gregtechceu.gtceu.api.gui.fancy.FancyMachineUIWidget;
import com.gregtechceu.gtceu.api.gui.fancy.IFancyUIProvider;
import com.gregtechceu.gtceu.api.gui.fancy.TabsWidget;
import com.gregtechceu.gtceu.api.gui.fancy.TooltipsPanel;
import com.gregtechceu.gtceu.api.gui.texture.IGuiTexture;
import com.gregtechceu.gtceu.api.machine.ConditionalSubscriptionHandler;
import com.gregtechceu.gtceu.api.machine.fancyconfigurator.BatchModeConfigurator;
import com.gregtechceu.gtceu.api.machine.fancyconfigurator.CombinedDirectionalFancyConfigurator;
import com.gregtechceu.gtceu.api.machine.fancyconfigurator.VoidingModeConfigurator;
import com.gregtechceu.gtceu.api.machine.fancyconfigurator.WorkingEnabledConfigurator;
import com.gregtechceu.gtceu.api.machine.feature.FancyActionMachine;
import com.gregtechceu.gtceu.api.machine.feature.IUIMachine;
import com.gregtechceu.gtceu.api.machine.feature.multiblock.FancyPartUIProvider;
import com.gregtechceu.gtceu.api.machine.feature.multiblock.IMaintenanceMachine;
import com.gregtechceu.gtceu.api.machine.feature.multiblock.IMultiPart;
import com.gregtechceu.gtceu.api.machine.multiblock.MultiblockDisplayText;
import com.gregtechceu.gtceu.api.machine.multiblock.WorkableElectricMultiblockMachine;
import com.gregtechceu.gtceu.api.machine.trait.NetworkedComputationContainer;
import com.gregtechceu.gtceu.api.misc.EnergyContainerList;
import com.gregtechceu.gtceu.api.sync_system.annotations.SyncToClient;
import com.gregtechceu.gtceu.common.computation.ComputationNetworkManager;
import com.gregtechceu.gtceu.common.machine.multiblock.part.ObjectHolderMachine;
import com.gregtechceu.gtceu.common.machine.multiblock.part.OpticalComputationHatchMachine;

import com.lowdragmc.lowdraglib2.gui.ui.UI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.data.Horizontal;
import com.lowdragmc.lowdraglib2.gui.ui.data.ScrollDisplay;
import com.lowdragmc.lowdraglib2.gui.ui.data.ScrollerMode;
import com.lowdragmc.lowdraglib2.gui.ui.data.Vertical;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;

import lombok.AccessLevel;
import lombok.Getter;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class ResearchStationMachine extends WorkableElectricMultiblockMachine
                                    implements IUIMachine, FancyActionMachine {

    @Getter
    private final NetworkedComputationContainer importComputation;
    @Getter
    private @Nullable ObjectHolderMachine objectHolder;
    @Getter(AccessLevel.PACKAGE)
    private final ConditionalSubscriptionHandler displaySnapshotSubscription;
    @Getter(AccessLevel.PACKAGE)
    @SyncToClient
    private List<Component> displaySnapshot = List.of();

    public ResearchStationMachine(BlockEntityCreationInfo info) {
        super(info);
        this.importComputation = attachTrait(new NetworkedComputationContainer(IO.IN));
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
        for (IMultiPart part : getParts()) {
            if (part instanceof ObjectHolderMachine holder) {
                if (holder.getFrontFacing() != getFrontFacing().getOpposite()) {
                    invalidateStructure(structureName);
                    return;
                }
                this.objectHolder = holder;
            }
        }

        if (objectHolder == null) {
            invalidateStructure(structureName);
            return;
        }
        if (!isRemote()) {
            refreshDisplaySnapshot();
            displaySnapshotSubscription.updateSubscription();
        }
    }

    @Override
    public boolean checkPattern(String structureName) {
        boolean isFormed = super.checkPattern(structureName);
        if (isFormed && objectHolder != null && objectHolder.getFrontFacing() != getFrontFacing().getOpposite()) {
            invalidateStructure(structureName);
        }
        return isFormed;
    }

    @Override
    public void invalidateStructure(String structureName) {
        boolean defaultStructure = DEFAULT_STRUCTURE.equals(structureName);
        if (defaultStructure) {
            for (IMultiPart part : getParts()) {
                if (part instanceof ObjectHolderMachine holder && holder == objectHolder) {
                    holder.setLocked(false);
                }
            }
            objectHolder = null;
        }
        super.invalidateStructure(structureName);
        if (defaultStructure && !isRemote()) {
            refreshDisplaySnapshot();
            displaySnapshotSubscription.updateSubscription();
        }
    }

    @Override
    public void onUnload() {
        super.onUnload();
        clearDisplayRuntimeState();
    }

    @Override
    public void onPartUnload() {
        super.onPartUnload();
        clearDisplayRuntimeState();
    }

    private void clearDisplayRuntimeState() {
        displaySnapshot = List.of();
        displaySnapshotSubscription.unsubscribe();
    }

    @Override
    public boolean regressWhenWaiting() {
        return false;
    }

    private int getMaxComputation() {
        if (!(getLevel() instanceof ServerLevel serverLevel)) {
            return 0;
        }
        for (IMultiPart part : getParts()) {
            if (part instanceof OpticalComputationHatchMachine opticalMachine) {
                return ComputationNetworkManager.get(serverLevel)
                        .getNetWorkAvailableCWUt(opticalMachine.getComputationPort());
            }
        }
        return 0;
    }

    @Override
    public void setWorkingEnabled(boolean isWorkingAllowed) {
        super.setWorkingEnabled(isWorkingAllowed);
        if (!isRemote()) {
            refreshDisplaySnapshot();
        }
    }

    @Override
    public void addDisplayText(List<Component> textList) {
        textList.addAll(displaySnapshot);
    }

    void refreshDisplaySnapshot() {
        List<Component> nextSnapshot = createDisplaySnapshot(captureDisplayState());
        if (!displaySnapshot.equals(nextSnapshot)) {
            displaySnapshot = nextSnapshot;
        }
    }

    protected DisplayState captureDisplayState() {
        var workLogic = getWorkLogic();
        return captureDisplayState(isFormed(), workLogic.isWorkingEnabled(), workLogic.isActive(),
                energyContainer, tier, getMaxComputation(), recipeLogic.getProgressPercent());
    }

    static DisplayState captureDisplayState(boolean formed, boolean workingEnabled, boolean active,
                                            @Nullable EnergyContainerList energyContainer, int tier,
                                            int maxComputation, double progressPercent) {
        return new DisplayState(formed, workingEnabled, active, energyContainer, tier, maxComputation,
                progressPercent);
    }

    static List<Component> createDisplaySnapshot(DisplayState state) {
        List<Component> text = new ArrayList<>(5);
        MultiblockDisplayText.builder(text, state.formed())
                .setWorkingStatus(state.workingEnabled(), state.active())
                .setWorkingStatusKeys("gtpm.multiblock.idling", "gtpm.multiblock.work_paused",
                        "gtpm.multiblock.research_station.researching")
                .addEnergyUsageLine(state.energyContainer())
                .addEnergyTierLine(state.tier())
                .addWorkingStatusLine()
                .addComputationUsageLine(state.maxComputation())
                .addProgressLineOnlyPercent(state.progressPercent());
        return List.copyOf(text);
    }

    record DisplayState(boolean formed, boolean workingEnabled, boolean active,
                        @Nullable EnergyContainerList energyContainer, int tier,
                        int maxComputation, double progressPercent) {}

    @Override
    public boolean canCreateUI(Player player, MachineUIHolder holder) {
        return holder.getMachine() == this;
    }

    @Override
    public UI createUI(Player player, MachineUIHolder holder) {
        IFancyUIProvider page = createPage(player, holder);
        return UI.of(new FancyMachineUIWidget(page, player.getInventory(), holder,
                page.getPageWidth(), page.getPageHeight()));
    }

    IFancyUIProvider createPage(Player player, MachineUIHolder holder) {
        requireMatchingHolder(holder);
        return new ResearchStationFancyPage(player, holder);
    }

    private void requireMatchingHolder(MachineUIHolder holder) {
        if (holder.getMachine() != this) {
            throw new IllegalArgumentException("Research Station UI holder must resolve the opened controller.");
        }
    }

    private final class ResearchStationFancyPage implements IFancyUIProvider {

        private static final int PAGE_WIDTH = 190;
        private static final int PAGE_HEIGHT = 125;

        private final MachineUIHolder holder;
        private final CombinedDirectionalFancyConfigurator directionalPage;
        private final List<IFancyUIProvider> partPages;

        private ResearchStationFancyPage(Player player, MachineUIHolder holder) {
            requireMatchingHolder(holder);
            this.holder = holder;
            this.directionalPage = new CombinedDirectionalFancyConfigurator(ResearchStationMachine.this, player,
                    holder);

            List<IFancyUIProvider> pages = new ArrayList<>();
            for (IMultiPart part : getParts()) {
                if (!(part instanceof FancyPartUIProvider pageProvider)) {
                    throw new IllegalStateException("Research Station part has no LDLib2 Fancy page: " +
                            part.self().getDefinition().getId());
                }
                MachineUIHolder partHolder = new MachineUIHolderContext(player, part.self());
                pages.add(pageProvider.createFancyPage(player, partHolder));
            }
            this.partPages = List.copyOf(pages);
        }

        @Override
        public UIElement createUIWidget(FancyMachineUIWidget shell) {
            if (holder.getMachine() != ResearchStationMachine.this) {
                throw new IllegalStateException("Research Station page holder no longer resolves its controller.");
            }

            UIElement root = UITemplate.setBounds(new UIElement(), 0, 0, PAGE_WIDTH, PAGE_HEIGHT);
            UITemplate.setBackgroundTexture(root, GuiTextures.BACKGROUND_INVERSE);

            GTScrollerViewElement screen = new GTScrollerViewElement(4, 4, 182, 117);
            UITemplate.setBackgroundTexture(screen, getScreenTexture());
            screen.viewPort.layout(layout -> layout.paddingAll(0));
            UITemplate.setBackgroundTexture(screen.viewPort, getScreenTexture());
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
                    ResearchStationMachine.this::addDisplayText)
                    .setMaxWidthLimit(200)
                    .clickHandler(ResearchStationMachine.this::handleDisplayClick));
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
        public void attachSideTabs(TabsWidget tabs) {
            tabs.attachSubTab(directionalPage);
        }

        @Override
        public void attachConfigurators(ConfiguratorPanel configuratorPanel) {
            VoidingModeConfigurator.attachConfigurators(configuratorPanel, ResearchStationMachine.this);
            BatchModeConfigurator.attachConfigurators(configuratorPanel, ResearchStationMachine.this);
            configuratorPanel.attachConfigurators(new WorkingEnabledConfigurator(
                    ResearchStationMachine.this, holder));
        }

        @Override
        public void attachTooltips(TooltipsPanel tooltipsPanel) {
            for (IMultiPart part : getParts()) {
                if (part instanceof IMaintenanceMachine maintenanceMachine) {
                    maintenanceMachine.attachMaintenanceTooltips(tooltipsPanel);
                }
            }
        }

        @Override
        public List<IFancyUIProvider> getSubTabs() {
            return partPages;
        }

        @Override
        public List<Component> getTabTooltips() {
            return List.of(Component.translatable(getDefinition().getDescriptionId()));
        }
    }
}
