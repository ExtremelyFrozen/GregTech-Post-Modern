package com.gregtechceu.gtceu.api.machine.multiblock;

import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.blockentity.BlockEntityCreationInfo;
import com.gregtechceu.gtceu.api.capability.IEnergyContainer;
import com.gregtechceu.gtceu.api.capability.recipe.EURecipeCapability;
import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.capability.recipe.IRecipeHandler;
import com.gregtechceu.gtceu.api.gui.GuiTextures;
import com.gregtechceu.gtceu.api.gui.UITemplate;
import com.gregtechceu.gtceu.api.gui.element.GTComponentPanelElement;
import com.gregtechceu.gtceu.api.gui.element.GTLabelElement;
import com.gregtechceu.gtceu.api.gui.element.GTScrollerViewElement;
import com.gregtechceu.gtceu.api.gui.factory.MachineUIHolder;
import com.gregtechceu.gtceu.api.gui.fancy.ConfiguratorPanel;
import com.gregtechceu.gtceu.api.gui.fancy.FancyMachineUIWidget;
import com.gregtechceu.gtceu.api.gui.fancy.IFancyUIProvider;
import com.gregtechceu.gtceu.api.gui.fancy.TooltipsPanel;
import com.gregtechceu.gtceu.api.machine.ConditionalSubscriptionHandler;
import com.gregtechceu.gtceu.api.machine.fancyconfigurator.BatchModeConfigurator;
import com.gregtechceu.gtceu.api.machine.fancyconfigurator.StructureCheckConfigurator;
import com.gregtechceu.gtceu.api.machine.fancyconfigurator.VoidingModeConfigurator;
import com.gregtechceu.gtceu.api.machine.fancyconfigurator.WorkingEnabledConfigurator;
import com.gregtechceu.gtceu.api.machine.feature.IFancyUIMachine;
import com.gregtechceu.gtceu.api.machine.feature.IOverclockMachine;
import com.gregtechceu.gtceu.api.machine.feature.ITieredMachine;
import com.gregtechceu.gtceu.api.machine.feature.multiblock.BatchModeMachine;
import com.gregtechceu.gtceu.api.machine.feature.multiblock.IDisplayUIMachine;
import com.gregtechceu.gtceu.api.machine.feature.multiblock.IMultiPart;
import com.gregtechceu.gtceu.api.machine.trait.RecipeLogic;
import com.gregtechceu.gtceu.api.misc.EnergyContainerList;
import com.gregtechceu.gtceu.api.recipe.modifier.RecipeModifierList;
import com.gregtechceu.gtceu.api.sync_system.annotations.SaveField;
import com.gregtechceu.gtceu.api.sync_system.annotations.SyncBoth;
import com.gregtechceu.gtceu.api.sync_system.annotations.SyncToClient;
import com.gregtechceu.gtceu.common.data.GTRecipeModifiers;
import com.gregtechceu.gtceu.common.machine.multiblock.part.ParallelHatchPartMachine;

import com.lowdragmc.lowdraglib2.gui.ui.UI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.data.Horizontal;
import com.lowdragmc.lowdraglib2.gui.ui.data.ScrollDisplay;
import com.lowdragmc.lowdraglib2.gui.ui.data.ScrollerMode;
import com.lowdragmc.lowdraglib2.gui.ui.data.Vertical;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import lombok.AccessLevel;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

public class WorkableElectricMultiblockMachine extends WorkableMultiblockMachine implements BatchModeMachine,
                                               IDisplayUIMachine, ITieredMachine, IOverclockMachine, IFancyUIMachine {

    private static final int PAGE_WIDTH = 190;
    private static final int PAGE_HEIGHT = 125;

    // runtime
    protected @Nullable EnergyContainerList energyContainer;
    @Getter
    protected int tier;
    @SaveField
    @SyncBoth
    @Getter
    protected boolean batchEnabled;
    private final ConditionalSubscriptionHandler displaySubscription;
    @Getter(AccessLevel.PACKAGE)
    @SyncToClient
    private List<Component> display = List.of();
    private final Map<FancyMachineUIWidget, DisplayOpening> displayOpenings = new IdentityHashMap<>();

    public WorkableElectricMultiblockMachine(BlockEntityCreationInfo info, RecipeLogic recipeLogic) {
        super(info, recipeLogic);
        displaySubscription = new ConditionalSubscriptionHandler(this,
                this::tickDisplayTracking,
                () -> !displayOpenings.isEmpty());
    }

    public WorkableElectricMultiblockMachine(BlockEntityCreationInfo info) {
        super(info);
        displaySubscription = new ConditionalSubscriptionHandler(this,
                this::tickDisplayTracking,
                () -> !displayOpenings.isEmpty());
    }

    @Override
    public WorkableElectricMultiblockMachine self() {
        return this;
    }

    //////////////////////////////////////
    // *** Multiblock Lifecycle ***//
    //////////////////////////////////////
    @Override
    public void invalidateStructure(String structureName) {
        super.invalidateStructure(structureName);
        if (DEFAULT_STRUCTURE.equals(structureName)) {
            this.energyContainer = null;
            this.tier = 0;
            refreshDisplayTracking();
        }
    }

    @Override
    public void formStructure(String structureName) {
        super.formStructure(structureName);
        if (DEFAULT_STRUCTURE.equals(structureName)) {
            this.energyContainer = getEnergyContainer();
            this.tier = energyContainer.getTier();
            refreshDisplayTracking();
        }
    }

    @Override
    public void onUnload() {
        super.onUnload();
        clearDisplayOpenings();
    }

    @Override
    public void onPartUnload() {
        super.onPartUnload();
        this.energyContainer = null;
        this.tier = 0;
        clearDisplayOpenings();
    }

    private void refreshDisplayTracking() {
        if (displayOpenings.isEmpty() || isRemote()) return;
        displaySubscription.updateSubscription();
        refreshDisplay();
    }

    private void startDisplayTracking(FancyMachineUIWidget page, Player player,
                                      AbstractContainerMenu menu) {
        if (isRemote()) return;
        boolean firstAttach = !displayOpenings.containsKey(page);
        displayOpenings.put(page, new DisplayOpening(player, menu));
        if (firstAttach) {
            refreshDisplayTracking();
        }
    }

    private void tickDisplayTracking() {
        displayOpenings.values().removeIf(opening -> opening.player().containerMenu != opening.menu());
        if (displayOpenings.isEmpty()) {
            stopDisplayTracking();
        } else if (isFormed()) {
            refreshDisplay();
        }
    }

    private void clearDisplayOpenings() {
        displayOpenings.clear();
        stopDisplayTracking();
    }

    private void stopDisplayTracking() {
        display = List.of();
        displaySubscription.unsubscribe();
    }

    @Override
    public boolean supportsBatchMode() {
        return getDefinition().getRecipeModifier() instanceof RecipeModifierList list &&
                Arrays.stream(list.getModifiers())
                        .anyMatch(modifier -> modifier == GTRecipeModifiers.BATCH_MODE);
    }

    @Override
    public void setBatchEnabled(boolean batchEnabled) {
        this.batchEnabled = batchEnabled;
    }

    //////////////////////////////////////
    // ********** GUI ***********//
    //////////////////////////////////////

    @Override
    public UI createUI(Player player, MachineUIHolder holder) {
        FancyMachineUIWidget shell = new FancyMachineUIWidget(this, player.getInventory(), holder,
                getPageWidth(), getPageHeight());
        shell.addEventListener(UIEvents.MUI_CHANGED, event -> {
            var modularUI = shell.getModularUI();
            if (modularUI != null) {
                var menu = modularUI.getMenu();
                if (menu != null) {
                    startDisplayTracking(shell, player, menu);
                }
            }
        });
        return UI.of(shell);
    }

    void refreshDisplay() {
        List<Component> nextDisplay = new ArrayList<>();
        addDisplayText(nextDisplay);
        nextDisplay = List.copyOf(nextDisplay);
        if (!display.equals(nextDisplay)) {
            display = nextDisplay;
        }
    }

    private void appendSyncedDisplay(List<Component> textList) {
        textList.addAll(display);
    }

    private record DisplayOpening(Player player, AbstractContainerMenu menu) {}

    @Override
    public void addDisplayText(@NotNull List<Component> textList) {
        var workLogic = getWorkLogic();
        int numParallels;
        int subtickParallels;
        int batchParallels;
        int totalRuns;
        boolean exact = false;
        if (workLogic.isActive() && recipeLogic.getLastRecipe() != null) {
            numParallels = recipeLogic.getLastRecipe().parallels;
            subtickParallels = recipeLogic.getLastRecipe().subtickParallels;
            batchParallels = recipeLogic.getLastRecipe().batchParallels;
            totalRuns = recipeLogic.getLastRecipe().getTotalRuns();
            exact = true;
        } else {
            numParallels = getParallelHatch()
                    .map(ParallelHatchPartMachine::getCurrentParallel)
                    .orElse(0);
            subtickParallels = 0;
            batchParallels = 0;
            totalRuns = 0;
        }

        MultiblockDisplayText.builder(textList, isFormed())
                .setWorkingStatus(workLogic.isWorkingEnabled(), workLogic.isActive())
                .addEnergyUsageLine(energyContainer)
                .addEnergyTierLine(tier)
                .addMachineModeLine(getRecipeType(), getRecipeTypes().length > 1)
                .addTotalRunsLine(totalRuns)
                .addParallelsLine(numParallels, exact)
                .addSubtickParallelsLine(subtickParallels)
                .addBatchModeLine(isBatchEnabled(), batchParallels)
                .addWorkingStatusLine()
                .addProgressLine(recipeLogic)
                .addRecipeFailReasonLine(recipeLogic)
                .addOutputLines(recipeLogic.getLastRecipe());
        getDefinition().getAdditionalDisplay().accept(this, textList);
        IDisplayUIMachine.super.addDisplayText(textList);
    }

    //////////////////////////////////////
    // ******** OVERCLOCK *********//
    //////////////////////////////////////
    public int getOverclockTier() {
        return getTier();
    }

    public int getMaxOverclockTier() {
        return getTier();
    }

    public int getMinOverclockTier() {
        return getTier();
    }

    @Override
    public void setOverclockTier(int tier) {}

    @Override
    public long getOverclockVoltage() {
        if (this.energyContainer == null) {
            this.energyContainer = getEnergyContainer();
        }
        return energyContainer.getEffectiveVoltage();
    }

    //////////////////////////////////////
    // ****** RECIPE LOGIC *******//
    //////////////////////////////////////

    public EnergyContainerList getEnergyContainer() {
        List<IEnergyContainer> containers = new ArrayList<>();
        var handlers = getCapabilitiesFlat(IO.IN, EURecipeCapability.CAP);
        if (handlers.isEmpty()) handlers = getCapabilitiesFlat(IO.OUT, EURecipeCapability.CAP);
        for (IRecipeHandler<?> handler : handlers) {
            if (handler instanceof IEnergyContainer container) {
                containers.add(container);
            }
        }
        return new EnergyContainerList(containers);
    }

    public long getMaxVoltage() {
        if (this.energyContainer == null) {
            this.energyContainer = getEnergyContainer();
        }
        if (this.isGenerator()) {
            return energyContainer.getEffectiveVoltage();
        }
        return GTValues.V[energyContainer.getTier()];
    }

    @Override
    public long getTierVoltage() {
        return getMaxVoltage();
    }

    @Override
    public long getDisplayRecipeVoltage() {
        return this.getEnergyContainer().getHighestVoltage();
    }

    /**
     * Is this multiblock a generator?
     * Used for max voltage calculations.
     */
    public boolean isGenerator() {
        return getDefinition().isGenerator();
    }

    @Override
    public UIElement createUIWidget(FancyMachineUIWidget shell) {
        UIElement root = UITemplate.setBounds(new UIElement(), 0, 0, PAGE_WIDTH, PAGE_HEIGHT);
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
                WorkableElectricMultiblockMachine.this::appendSyncedDisplay)
                .setMaxWidthLimit(200)
                .clickHandler(WorkableElectricMultiblockMachine.this::handleDisplayClick));
        root.addChild(screen);
        return root;
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
    public void attachConfigurators(ConfiguratorPanel configuratorPanel) {
        VoidingModeConfigurator.attachConfigurators(
                configuratorPanel, WorkableElectricMultiblockMachine.this);
        BatchModeConfigurator.attachConfigurators(
                configuratorPanel, WorkableElectricMultiblockMachine.this);
        configuratorPanel.attachConfigurators(new StructureCheckConfigurator(this));
        configuratorPanel.attachConfigurators(new WorkingEnabledConfigurator(
                WorkableElectricMultiblockMachine.this));
    }

    @Override
    public void attachTooltips(TooltipsPanel tooltipsPanel) {
        for (IMultiPart part : getParts()) {
            part.attachFancyTooltipsToController(
                    WorkableElectricMultiblockMachine.this, tooltipsPanel);
        }
    }

    @Override
    public List<IFancyUIProvider> getSubTabs() {
        return new ObjectArrayList<>(getParts());
    }
}
