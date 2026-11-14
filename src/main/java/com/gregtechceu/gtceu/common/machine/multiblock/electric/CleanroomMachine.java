package com.gregtechceu.gtceu.common.machine.multiblock.electric;

import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.block.IFilterType;
import com.gregtechceu.gtceu.api.blockentity.BlockEntityCreationInfo;
import com.gregtechceu.gtceu.api.capability.IEnergyContainer;
import com.gregtechceu.gtceu.api.capability.recipe.EURecipeCapability;
import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.gui.GuiTextures;
import com.gregtechceu.gtceu.api.gui.UITemplate;
import com.gregtechceu.gtceu.api.gui.element.GTComponentPanelElement;
import com.gregtechceu.gtceu.api.gui.element.GTLabelElement;
import com.gregtechceu.gtceu.api.gui.element.GTScrollerViewElement;
import com.gregtechceu.gtceu.api.gui.fancy.ConfiguratorPanel;
import com.gregtechceu.gtceu.api.gui.fancy.FancyMachineUIWidget;
import com.gregtechceu.gtceu.api.gui.fancy.TooltipsPanel;
import com.gregtechceu.gtceu.api.machine.ConditionalSubscriptionHandler;
import com.gregtechceu.gtceu.api.machine.fancyconfigurator.VoidingModeConfigurator;
import com.gregtechceu.gtceu.api.machine.fancyconfigurator.WorkingEnabledConfigurator;
import com.gregtechceu.gtceu.api.machine.feature.IDataInfoProvider;
import com.gregtechceu.gtceu.api.machine.feature.multiblock.IMaintenanceMachine;
import com.gregtechceu.gtceu.api.machine.feature.multiblock.IMultiPart;
import com.gregtechceu.gtceu.api.machine.multiblock.CleanroomType;
import com.gregtechceu.gtceu.api.machine.multiblock.WorkableElectricMultiblockMachine;
import com.gregtechceu.gtceu.api.misc.EnergyContainerList;
import com.gregtechceu.gtceu.api.sync_system.annotations.SaveField;
import com.gregtechceu.gtceu.api.sync_system.annotations.SyncToClient;
import com.gregtechceu.gtceu.common.item.behavior.PortableScannerBehavior;
import com.gregtechceu.gtceu.common.machine.electric.HullMachine;
import com.gregtechceu.gtceu.common.machine.multiblock.part.DiodePartMachine;
import com.gregtechceu.gtceu.common.machine.trait.CleanroomLogic;
import com.gregtechceu.gtceu.common.machine.trait.CleanroomProviderTrait;
import com.gregtechceu.gtceu.common.machine.trait.CleanroomReceiverTrait;
import com.gregtechceu.gtceu.utils.GTUtil;

import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.data.Horizontal;
import com.lowdragmc.lowdraglib2.gui.ui.data.ScrollDisplay;
import com.lowdragmc.lowdraglib2.gui.ui.data.ScrollerMode;
import com.lowdragmc.lowdraglib2.gui.ui.data.Vertical;

import net.minecraft.ChatFormatting;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.Style;
import net.minecraft.util.Mth;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMaps;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import lombok.AccessLevel;
import lombok.Getter;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class CleanroomMachine extends WorkableElectricMultiblockMachine
                              implements IDataInfoProvider {

    private static final int PAGE_WIDTH = 190;
    private static final int PAGE_HEIGHT = 125;

    public static final int CLEAN_AMOUNT_THRESHOLD = 95;
    public static final int MIN_CLEAN_AMOUNT = 0;

    public static final int MIN_RADIUS = 2;
    public static final int MIN_DEPTH = 4;

    @SaveField
    private int lDist = 0, rDist = 0, bDist = 0, fDist = 0, hDist = 0;
    @Nullable
    private CleanroomType cleanroomType = null;
    @SaveField
    private int cleanAmount;
    // runtime
    @Getter
    @Nullable
    private EnergyContainerList inputEnergyContainers;
    @Getter
    @Nullable
    private Collection<CleanroomReceiverTrait> cleanroomReceivers;

    private final CleanroomProviderTrait cleanroomProviderTrait;
    @Getter(AccessLevel.PACKAGE)
    private final ConditionalSubscriptionHandler displaySubscription;
    @Getter(AccessLevel.PACKAGE)
    @SyncToClient
    private List<Component> displayText = List.of();
    private boolean displayControllerLoaded;
    private boolean displayStructureAvailable;

    public CleanroomMachine(BlockEntityCreationInfo info) {
        super(info, new CleanroomLogic());
        this.cleanroomProviderTrait = attachTrait(new CleanroomProviderTrait());
        this.displaySubscription = new ConditionalSubscriptionHandler(this, this::refreshDisplayText,
                () -> displayControllerLoaded && displayStructureAvailable && isFormed());
    }

    @Override
    public CleanroomLogic getRecipeLogic() {
        return (CleanroomLogic) super.getRecipeLogic();
    }

    //////////////////////////////////////
    // *** Multiblock LifeCycle ***//
    //////////////////////////////////////

    @Override
    public void onLoad() {
        super.onLoad();
        if (!isRemote()) {
            displayControllerLoaded = true;
            displayStructureAvailable = isFormed();
            refreshDisplayText();
            displaySubscription.initialize(getLevel());
        }
    }

    @Override
    public void formStructure(String structureName) {
        super.formStructure(structureName);
        if (!DEFAULT_STRUCTURE.equals(structureName)) return;
        var facts = getMultiblockState(DEFAULT_STRUCTURE).getFacts();
        Object2IntMap<String> measurements = facts.get("pattern.measurements");
        lDist = measurements.getInt("left");
        rDist = measurements.getInt("right");
        fDist = measurements.getInt("front");
        bDist = measurements.getInt("back");
        hDist = measurements.getInt("down");
        initializeAbilities();
        IFilterType filterType = getMultiblockState(DEFAULT_STRUCTURE).getFacts().get("FilterType");
        if (filterType != null) {
            this.cleanroomType = filterType.getCleanroomType();
        } else {
            this.cleanroomType = CleanroomType.CLEANROOM;
        }
        cleanroomProviderTrait.setProvidedTypes(Set.of(this.cleanroomType));

        // bind cleanroom
        if (cleanroomReceivers != null) {
            this.cleanroomReceivers.forEach(CleanroomReceiverTrait::removeCleanroom);
            this.cleanroomReceivers = null;
        }
        Set<CleanroomReceiverTrait> receivers = getMultiblockState(DEFAULT_STRUCTURE).getFacts().getOrCreate(
                "cleanroomReceiver",
                Sets::newHashSet);
        this.cleanroomReceivers = ImmutableSet.copyOf(receivers);
        this.cleanroomReceivers.forEach(receiver -> receiver.setCleanroomProvider(cleanroomProviderTrait));

        // max progress is based roughly on the dimensions of the structure: ((w * d) ^ .8 * h)
        // taller cleanrooms take longer than wider ones
        // minimum of 100 is a 5x5x5 cleanroom: 125-25=100 ticks
        // max sized CR is around 1142 ticks per progression

        var area = (lDist + rDist + 1) * (bDist + fDist + 1);
        var duration = Math.pow(area, 0.8) * (hDist + 1);
        this.getRecipeLogic().setDuration(Math.max(100, (int) duration));
        if (!isRemote()) {
            displayStructureAvailable = true;
            refreshDisplayText();
            displaySubscription.updateSubscription();
        }
    }

    @Override
    public void invalidateStructure(String structureName) {
        super.invalidateStructure(structureName);
        if (!DEFAULT_STRUCTURE.equals(structureName)) return;
        this.inputEnergyContainers = null;
        this.cleanAmount = MIN_CLEAN_AMOUNT;
        cleanroomProviderTrait.setActive(false);
        if (cleanroomReceivers != null) {
            this.cleanroomReceivers.forEach(CleanroomReceiverTrait::removeCleanroom);
            this.cleanroomReceivers = null;
        }
        if (!isRemote()) {
            displayStructureAvailable = false;
            refreshDisplayText();
            displaySubscription.updateSubscription();
        }
    }

    @Override
    public void onUnload() {
        super.onUnload();
        displayControllerLoaded = false;
        suspendDisplayRuntimeState();
    }

    @Override
    public void onPartUnload() {
        super.onPartUnload();
        suspendDisplayRuntimeState();
    }

    private void suspendDisplayRuntimeState() {
        displayStructureAvailable = false;
        displayText = List.of();
        displaySubscription.unsubscribe();
    }

    @Override
    public boolean shouldAddPartToController(IMultiPart part) {
        var cache = getMultiblockState(DEFAULT_STRUCTURE).getCache();
        for (Direction side : GTUtil.DIRECTIONS) {
            if (!cache.contains(part.self().getBlockPos().relative(side))) {
                return true;
            }
        }
        return false;
    }

    protected void initializeAbilities() {
        List<IEnergyContainer> energyContainers = new ArrayList<>();
        Long2ObjectMap<IO> ioMap = getMultiblockState(DEFAULT_STRUCTURE).getFacts().getOrDefault("ioMap",
                Long2ObjectMaps.emptyMap());
        for (IMultiPart part : getParts()) {
            if (isPartIgnored(part)) continue;
            IO io = ioMap.getOrDefault(part.self().getBlockPos().asLong(), IO.BOTH);
            if (io == IO.NONE || io == IO.OUT) continue;
            var handlerLists = part.getRecipeHandlers();
            for (var handlerList : handlerLists) {
                if (!handlerList.isValid(io)) continue;
                handlerList.getCapability(EURecipeCapability.CAP).stream()
                        .filter(IEnergyContainer.class::isInstance)
                        .map(IEnergyContainer.class::cast)
                        .forEach(energyContainers::add);
            }

            if (part instanceof IMaintenanceMachine maintenanceMachine) {
                getRecipeLogic().setMaintenanceMachine(maintenanceMachine);
            }
        }
        this.inputEnergyContainers = new EnergyContainerList(energyContainers);
        getRecipeLogic().setEnergyContainer(this.inputEnergyContainers);
        this.tier = Math.min(GTValues.MAX, inputEnergyContainers.getTier());
    }

    @SuppressWarnings("RedundantIfStatement") // `return false` being a separate statement is better for readability
    private static boolean isPartIgnored(IMultiPart part) {
        if (part instanceof DiodePartMachine) return true;
        if (part instanceof HullMachine) return true;

        return false;
    }

    public int getDynamicPatternFloorArea() {
        return (lDist + rDist + 1) * (bDist + fDist + 1);
    }

    @Override
    public void addDisplayText(List<Component> textList) {
        textList.addAll(displayText);
    }

    void refreshDisplayText() {
        List<Component> nextText = new ArrayList<>();
        collectServerDisplayText(nextText);
        nextText = List.copyOf(nextText);
        if (!displayText.equals(nextText)) {
            displayText = nextText;
        }
    }

    protected void collectServerDisplayText(List<Component> textList) {
        textList.addAll(createDisplayText(captureDisplayState()));
    }

    protected DisplayState captureDisplayState() {
        var workLogic = getWorkLogic();
        return new DisplayState(isFormed(), getMaxVoltage(), cleanroomType, workLogic.isWorkingEnabled(),
                workLogic.isActive(), workLogic.isWaiting(), recipeLogic.getProgress(), recipeLogic.getDuration(),
                (int) (recipeLogic.getProgressPercent() * 100), cleanroomProviderTrait.isActive(), cleanAmount,
                lDist + rDist + 1, hDist + 1, fDist + bDist + 1);
    }

    static List<Component> createDisplayText(DisplayState state) {
        List<Component> text = new ArrayList<>();
        if (state.formed()) {
            if (state.maxVoltage() > 0) {
                String voltageName = GTValues.VNF[GTUtil.getFloorTierByVoltage(state.maxVoltage())];
                text.add(Component.translatable("gtpm.multiblock.max_energy_per_tick", state.maxVoltage(),
                        voltageName));
            }

            if (state.cleanroomType() != null) {
                text.add(Component.translatable(state.cleanroomType().translationKey()));
            }

            if (!state.workingEnabled()) {
                text.add(Component.translatable("gtpm.multiblock.work_paused"));
            } else if (state.active()) {
                text.add(Component.translatable("gtpm.multiblock.running"));
                float currentInSec = (float) state.progress() / 20.0f;
                float maxInSec = (float) state.duration() / 20.0f;
                text.add(Component.translatable("gtpm.multiblock.progress",
                        String.format(Locale.ROOT, "%.2f", currentInSec),
                        String.format(Locale.ROOT, "%.2f", maxInSec), state.progressPercent()));
            } else {
                text.add(Component.translatable("gtpm.multiblock.idling"));
            }

            if (state.waiting()) {
                text.add(Component.translatable("gtpm.multiblock.waiting")
                        .setStyle(Style.EMPTY.withColor(ChatFormatting.RED)));
            }

            text.add(Component.translatable(state.clean() ? "gtpm.multiblock.cleanroom.clean_state" :
                    "gtpm.multiblock.cleanroom.dirty_state"));
            text.add(Component.translatable("gtpm.multiblock.cleanroom.clean_amount", state.cleanAmount()));
            text.add(Component.translatable("gtpm.multiblock.dimensions.0"));
            text.add(Component.translatable("gtpm.multiblock.dimensions.1", state.width(), state.height(),
                    state.depth()));
        } else {
            Component tooltip = Component.translatable("gtpm.multiblock.invalid_structure.tooltip")
                    .withStyle(ChatFormatting.GRAY);
            text.add(Component.translatable("gtpm.multiblock.invalid_structure")
                    .withStyle(Style.EMPTY.withColor(ChatFormatting.RED)
                            .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, tooltip))));
        }
        return List.copyOf(text);
    }

    record DisplayState(boolean formed, long maxVoltage, @Nullable CleanroomType cleanroomType,
                        boolean workingEnabled, boolean active, boolean waiting,
                        int progress, int duration, int progressPercent,
                        boolean clean, int cleanAmount, int width, int height, int depth) {}

    /**
     * Adjust the cleanroom's clean amount
     *
     * @param amount the amount of cleanliness to increase/decrease by
     */
    public void adjustCleanAmount(int amount) {
        // do not allow negative cleanliness nor cleanliness above 100
        this.cleanAmount = Mth.clamp(this.cleanAmount + amount, 0, 100);
        cleanroomProviderTrait.setActive(this.cleanAmount >= CLEAN_AMOUNT_THRESHOLD);
    }

    @Override
    public List<Component> getDataInfo(PortableScannerBehavior.DisplayMode mode) {
        if (mode == PortableScannerBehavior.DisplayMode.SHOW_ALL ||
                mode == PortableScannerBehavior.DisplayMode.SHOW_MACHINE_INFO) {
            return Collections.singletonList(Component.translatable(
                    cleanroomProviderTrait.isActive() ? "gtpm.multiblock.cleanroom.clean_state" :
                            "gtpm.multiblock.cleanroom.dirty_state"));
        }
        return new ArrayList<>();
    }

    @Override
    public long getMaxVoltage() {
        if (inputEnergyContainers == null) return GTValues.LV;
        return GTValues.V[inputEnergyContainers.getTier()];
    }

    // Do not allow cleanroom to be paused due to custom recipe logic
    @Override
    public boolean isWorkingEnabled() {
        return true;
    }

    @Override
    public void setWorkingEnabled(boolean ignored) {}

    @Override
    public UIElement createUIWidget(FancyMachineUIWidget shell) {
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
        screen.addScrollViewChild(new GTComponentPanelElement(4, 17, CleanroomMachine.this::addDisplayText)
                .setMaxWidthLimit(200)
                .clickHandler(CleanroomMachine.this::handleDisplayClick));
        root.addChild(screen);
        return root;
    }

    @Override
    public void attachConfigurators(ConfiguratorPanel configuratorPanel) {
        VoidingModeConfigurator.attachConfigurators(configuratorPanel, CleanroomMachine.this);
        configuratorPanel.attachConfigurators(new WorkingEnabledConfigurator(
                CleanroomMachine.this));
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
