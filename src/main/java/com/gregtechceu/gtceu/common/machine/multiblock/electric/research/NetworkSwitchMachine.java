package com.gregtechceu.gtceu.common.machine.multiblock.electric.research;

import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.blockentity.BlockEntityCreationInfo;
import com.gregtechceu.gtceu.api.capability.recipe.EURecipeCapability;
import com.gregtechceu.gtceu.api.gui.GuiTextures;
import com.gregtechceu.gtceu.api.gui.UITemplate;
import com.gregtechceu.gtceu.api.gui.element.GTComponentPanelElement;
import com.gregtechceu.gtceu.api.gui.element.GTLabelElement;
import com.gregtechceu.gtceu.api.gui.element.GTScrollerViewElement;
import com.gregtechceu.gtceu.api.gui.fancy.FancyMachineUIWidget;
import com.gregtechceu.gtceu.api.gui.fancy.TooltipsPanel;
import com.gregtechceu.gtceu.api.machine.ConditionalSubscriptionHandler;
import com.gregtechceu.gtceu.api.machine.TickableSubscription;
import com.gregtechceu.gtceu.api.machine.feature.multiblock.IMaintenanceMachine;
import com.gregtechceu.gtceu.api.machine.feature.multiblock.IMultiPart;
import com.gregtechceu.gtceu.api.machine.multiblock.MultiblockDisplayText;
import com.gregtechceu.gtceu.api.machine.multiblock.PartAbility;
import com.gregtechceu.gtceu.api.machine.multiblock.WorkableElectricMultiblockMachine;
import com.gregtechceu.gtceu.api.machine.trait.WorkLogic;
import com.gregtechceu.gtceu.api.sync_system.annotations.SyncToClient;
import com.gregtechceu.gtceu.common.computation.ComputationNetworkManager;
import com.gregtechceu.gtceu.common.machine.multiblock.part.OpticalComputationHatchMachine;

import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.data.Horizontal;
import com.lowdragmc.lowdraglib2.gui.ui.data.ScrollDisplay;
import com.lowdragmc.lowdraglib2.gui.ui.data.ScrollerMode;
import com.lowdragmc.lowdraglib2.gui.ui.data.Vertical;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;

import lombok.AccessLevel;
import lombok.Getter;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class NetworkSwitchMachine extends WorkableElectricMultiblockMachine {

    private static final int PAGE_WIDTH = 190;
    private static final int PAGE_HEIGHT = 125;

    public static final int EUT_PER_HATCH = GTValues.VA[GTValues.IV];

    private int energyUsage = 0;
    private boolean computationBridgeActive;

    @Nullable
    protected TickableSubscription tickSubs;
    private final ConditionalSubscriptionHandler displaySubscription;
    @Getter(AccessLevel.PACKAGE)
    @SyncToClient
    private List<Component> displayText = List.of();

    public NetworkSwitchMachine(BlockEntityCreationInfo info) {
        super(info);
        this.displaySubscription = new ConditionalSubscriptionHandler(this, this::refreshDisplayText,
                this::isFormed);
    }

    protected int calculateEnergyUsage() {
        int receivers = 0;
        int transmitters = 0;
        for (var part : this.getParts()) {
            var block = part.self().getBlockState().getBlock();
            if (PartAbility.COMPUTATION_DATA_RECEPTION.isApplicable(block)) {
                ++receivers;
            }
            if (PartAbility.COMPUTATION_DATA_TRANSMISSION.isApplicable(block)) {
                ++transmitters;
            }
        }
        return EUT_PER_HATCH * (receivers + transmitters);
    }

    @Override
    public void formStructure(String structureName) {
        super.formStructure(structureName);
        if (!DEFAULT_STRUCTURE.equals(structureName)) return;
        energyUsage = calculateEnergyUsage();
        updateTickSubscription();
        markComputationTopologyDirty();
        if (!isRemote()) {
            refreshDisplayText();
            displaySubscription.updateSubscription();
        }
    }

    @Override
    public void invalidateStructure(String structureName) {
        super.invalidateStructure(structureName);
        if (!DEFAULT_STRUCTURE.equals(structureName)) return;
        energyUsage = 0;
        updateComputationBridgeActive(false);
        updateTickSubscription();
        if (!isRemote()) {
            refreshDisplayText();
            displaySubscription.updateSubscription();
        }
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (!isRemote()) {
            refreshDisplayText();
            displaySubscription.initialize(getLevel());
        }
        scheduleForNextServerTick(this::updateTickSubscription);
    }

    @Override
    public void onUnload() {
        super.onUnload();
        clearRuntimeState();
    }

    @Override
    public void onPartUnload() {
        super.onPartUnload();
        clearRuntimeState();
    }

    private void clearRuntimeState() {
        energyUsage = 0;
        updateComputationBridgeActive(false);
        if (tickSubs != null) {
            tickSubs.unsubscribe();
            tickSubs = null;
        }
        displayText = List.of();
        displaySubscription.unsubscribe();
    }

    protected void updateTickSubscription() {
        if (isFormed() && isWorkingEnabled()) {
            tickSubs = subscribeServerTick(tickSubs, this::tick);
        } else if (tickSubs != null) {
            tickSubs.unsubscribe();
            tickSubs = null;
        }
    }

    public void tick() {
        if (energyContainer == null) {
            updateComputationBridgeActive(false);
            getWorkLogic().setWaiting(Component.translatable("gtpm.recipe_logic.insufficient_in").append(": ")
                    .append(EURecipeCapability.CAP.getName()));
            refreshDisplayText();
            updateTickSubscription();
            return;
        }

        int energyToConsume = getEnergyUsage();
        if (energyContainer.getEnergyStored() >= energyToConsume &&
                energyContainer.removeEnergy(energyToConsume) >= energyToConsume) {
            getWorkLogic().setStatus(WorkLogic.Status.WORKING);
            updateComputationBridgeActive(true);
        } else {
            getWorkLogic().setWaiting(Component.translatable("gtpm.recipe_logic.insufficient_in").append(": ")
                    .append(EURecipeCapability.CAP.getName()));
            updateComputationBridgeActive(false);
        }
        refreshDisplayText();
        updateTickSubscription();
    }

    private void updateComputationBridgeActive(boolean active) {
        if (computationBridgeActive == active) return;
        computationBridgeActive = active;
        markComputationTopologyDirty();
    }

    private void markComputationTopologyDirty() {
        if (getLevel() instanceof ServerLevel serverLevel) {
            ComputationNetworkManager.get(serverLevel).markTopologyDirty();
        }
    }

    private int getMaxCWUt() {
        if (!isFormed() || !isWorkingEnabled() || !computationBridgeActive) {
            return 0;
        }
        if (!(getLevel() instanceof ServerLevel serverLevel)) {
            return 0;
        }
        for (IMultiPart part : getParts()) {
            if (part instanceof OpticalComputationHatchMachine opticalHatch) {
                return ComputationNetworkManager.get(serverLevel)
                        .getNetWorkMaxCWUt(opticalHatch.getComputationPort());
            }
        }
        return 0;
    }

    private int getUsedCWUt() {
        if (!isFormed() || !isWorkingEnabled() || !computationBridgeActive) {
            return 0;
        }
        if (!(getLevel() instanceof ServerLevel serverLevel)) {
            return 0;
        }
        for (IMultiPart part : getParts()) {
            if (part instanceof OpticalComputationHatchMachine opticalHatch) {
                return ComputationNetworkManager.get(serverLevel)
                        .getNetWorkUsedCWUt(opticalHatch.getComputationPort());
            }
        }
        return 0;
    }

    public int getEnergyUsage() {
        return isFormed() ? energyUsage : 0;
    }

    @Override
    public boolean isWorkingEnabled() {
        return !getWorkLogic().isSuspend();
    }

    @Override
    public void setWorkingEnabled(boolean isWorkingAllowed) {
        if (isWorkingAllowed) {
            getWorkLogic().setStatus(WorkLogic.Status.IDLE);
        } else {
            getWorkLogic().setStatus(WorkLogic.Status.SUSPEND);
            updateComputationBridgeActive(false);
        }
        updateTickSubscription();
        if (!isRemote()) {
            refreshDisplayText();
        }
    }

    @Override
    public void addDisplayText(List<Component> textList) {
        textList.addAll(displayText);
    }

    void refreshDisplayText() {
        DisplayState state = captureDisplayState(isFormed(), isWorkingEnabled(), isActive(),
                computationBridgeActive, getEnergyUsage(), getMaxCWUt(), getUsedCWUt());
        List<Component> nextText = createDisplayText(state);
        if (!displayText.equals(nextText)) {
            displayText = nextText;
        }
    }

    static DisplayState captureDisplayState(boolean formed, boolean workingEnabled, boolean active,
                                            boolean bridgeActive, int energyUsage, int rawMaxCWUt,
                                            int rawUsedCWUt) {
        boolean computationAvailable = formed && workingEnabled && bridgeActive;
        return new DisplayState(formed, workingEnabled, active, energyUsage,
                computationAvailable ? rawMaxCWUt : 0,
                computationAvailable ? rawUsedCWUt : 0);
    }

    static List<Component> createDisplayText(DisplayState state) {
        List<Component> text = new ArrayList<>(4);
        MultiblockDisplayText.builder(text, state.formed())
                .setWorkingStatus(true, state.active() && state.workingEnabled())
                .setWorkingStatusKeys(
                        "gtpm.multiblock.idling",
                        "gtpm.multiblock.idling",
                        "gtpm.multiblock.data_bank.providing")
                .addEnergyUsageExactLine(state.energyUsage())
                .addComputationUsageLine(state.maxCWUt())
                .addComputationUsageExactLine(state.usedCWUt())
                .addWorkingStatusLine();
        return List.copyOf(text);
    }

    record DisplayState(boolean formed, boolean workingEnabled, boolean active,
                        int energyUsage, int maxCWUt, int usedCWUt) {}

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
                NetworkSwitchMachine.this::addDisplayText)
                .setMaxWidthLimit(200)
                .clickHandler(NetworkSwitchMachine.this::handleDisplayClick));
        root.addChild(screen);
        return root;
    }

    @Override
    public void attachTooltips(TooltipsPanel tooltipsPanel) {
        for (IMultiPart part : getParts()) {
            if (part instanceof IMaintenanceMachine maintenanceMachine) {
                maintenanceMachine.attachMaintenanceTooltips(tooltipsPanel);
            }
        }
    }
}
