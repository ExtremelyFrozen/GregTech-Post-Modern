package com.gregtechceu.gtceu.common.machine.multiblock.electric.gcym;

import com.gregtechceu.gtceu.api.blockentity.BlockEntityCreationInfo;
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
import com.gregtechceu.gtceu.api.sync_system.annotations.RerenderOnChanged;
import com.gregtechceu.gtceu.api.sync_system.annotations.SyncToClient;
import com.gregtechceu.gtceu.common.machine.trait.multiblock.MultiblockFluidRendererTrait;

import com.lowdragmc.lowdraglib2.gui.ui.UI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.data.Horizontal;
import com.lowdragmc.lowdraglib2.gui.ui.data.ScrollDisplay;
import com.lowdragmc.lowdraglib2.gui.ui.data.ScrollerMode;
import com.lowdragmc.lowdraglib2.gui.ui.data.Vertical;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

import lombok.AccessLevel;
import lombok.Getter;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class LargeMixerMachine extends WorkableElectricMultiblockMachine
                               implements MachineUIProvider, FancyActionMachine {

    @Getter
    @SyncToClient
    @RerenderOnChanged
    private final MultiblockFluidRendererTrait fluidRendererTrait;
    /** Keeps the client display supplied from a server-owned snapshot while the structure is formed. */
    @Getter(AccessLevel.PACKAGE)
    private final ConditionalSubscriptionHandler displaySnapshotSubscription;
    /** Carries the complete legacy display text without relying on the old Widget sync path. */
    @Getter(AccessLevel.PACKAGE)
    @SyncToClient
    private List<Component> displaySnapshot = List.of();

    public LargeMixerMachine(BlockEntityCreationInfo info) {
        super(info);
        fluidRendererTrait = attachTrait(new MultiblockFluidRendererTrait(this::saveOffsets));
        displaySnapshotSubscription = new ConditionalSubscriptionHandler(this, this::refreshDisplaySnapshot,
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
        if (!DEFAULT_STRUCTURE.equals(structureName) || isRemote()) return;
        refreshDisplaySnapshot();
        displaySnapshotSubscription.updateSubscription();
    }

    @Override
    public void invalidateStructure(String structureName) {
        super.invalidateStructure(structureName);
        if (!DEFAULT_STRUCTURE.equals(structureName) || isRemote()) return;
        refreshDisplaySnapshot();
        displaySnapshotSubscription.updateSubscription();
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

    /** Clears the synced display only when this controller can no longer serve its UI. */
    private void clearDisplayRuntimeState() {
        displaySnapshot = List.of();
        displaySnapshotSubscription.unsubscribe();
    }

    @Override
    public void addDisplayText(List<Component> textList) {
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

    public Set<BlockPos> saveOffsets() {
        Direction up = RelativeDirection.UP.getRelative(getFrontFacing(), getUpwardsFacing(), isFlipped());
        Direction back = getFrontFacing().getOpposite();
        Direction clockWise = RelativeDirection.RIGHT.getRelative(getFrontFacing(), getUpwardsFacing(), isFlipped());
        Direction counterClockWise = RelativeDirection.LEFT.getRelative(getFrontFacing(), getUpwardsFacing(),
                isFlipped());

        BlockPos pos = getBlockPos();
        BlockPos center = pos.relative(up, 3);

        Set<BlockPos> offsets = new HashSet<>();

        for (int i = 0; i < 3; i++) {
            center = center.relative(back);
            if (i % 2 == 0) {
                offsets.add(center.subtract(pos));
            }
            offsets.add(center.relative(clockWise).subtract(pos));
            offsets.add(center.relative(counterClockWise).subtract(pos));
        }
        return offsets;
    }

    @Override
    public boolean canCreateUI(Player player, MachineUIHolder holder) {
        return holder.getMachine() == this;
    }

    @Override
    public UI createUI(Player player, MachineUIHolder holder) {
        requireMatchingHolder(holder);
        LargeMixerFancyPage page = new LargeMixerFancyPage(player, holder);
        return UI.of(new FancyMachineUI(page, player.getInventory(), holder,
                page.getPageWidth(), page.getPageHeight()));
    }

    private void requireMatchingHolder(MachineUIHolder holder) {
        if (holder.getMachine() != this) {
            throw new IllegalArgumentException("Large Mixer UI holder must resolve the opened controller.");
        }
    }

    /** Owns the opening-scoped controller, directional, and contextual part pages. */
    private final class LargeMixerFancyPage implements FancyUIProvider {

        private static final int PAGE_WIDTH = 190;
        private static final int PAGE_HEIGHT = 125;

        private final MachineUIHolder holder;
        private final DirectionalConfigurator directionalPage;
        private final List<FancyUIProvider> partPages;

        private LargeMixerFancyPage(Player player, MachineUIHolder holder) {
            requireMatchingHolder(holder);
            this.holder = holder;
            this.directionalPage = new DirectionalConfigurator(LargeMixerMachine.this, player, holder);

            List<FancyUIProvider> pages = new ArrayList<>();
            for (IMultiPart part : getParts()) {
                if (!(part instanceof FancyPartUIProvider pageProvider)) {
                    throw new IllegalStateException("Large Mixer part has no LDLib2 Fancy page: " +
                            part.self().getDefinition().getId());
                }
                MachineUIHolder partHolder = new MachineUIHolderContext(player, part.self());
                pages.add(pageProvider.createFancyPage(player, partHolder));
            }
            this.partPages = List.copyOf(pages);
        }

        @Override
        public UIElement createMainPage(FancyMachineUI shell) {
            if (holder.getMachine() != LargeMixerMachine.this) {
                throw new IllegalStateException("Large Mixer page holder no longer resolves its controller.");
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
            screen.addScrollViewChild(new GTComponentPanelElement(4, 17, LargeMixerMachine.this::addDisplayText)
                    .setMaxWidthLimit(200)
                    .clickHandler(LargeMixerMachine.this::handleDisplayClick));
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
            VoidingModeConfigurator.attachConfigurators(configuratorPanel, LargeMixerMachine.this);
            BatchModeConfigurator.attachConfigurators(configuratorPanel, LargeMixerMachine.this);
            configuratorPanel.attachConfigurators(
                    new WorkingEnabledConfigurator(LargeMixerMachine.this, holder));
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
