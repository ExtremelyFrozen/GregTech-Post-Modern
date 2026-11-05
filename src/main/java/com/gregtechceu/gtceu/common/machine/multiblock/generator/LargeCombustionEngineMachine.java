package com.gregtechceu.gtceu.common.machine.multiblock.generator;

import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.blockentity.BlockEntityCreationInfo;
import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.fluids.store.FluidStorageKeys;
import com.gregtechceu.gtceu.api.gui.GuiTextures;
import com.gregtechceu.gtceu.api.gui.UITemplate;
import com.gregtechceu.gtceu.api.gui.element.GTComponentPanelElement;
import com.gregtechceu.gtceu.api.gui.element.GTLabelElement;
import com.gregtechceu.gtceu.api.gui.element.GTScrollerViewElement;
import com.gregtechceu.gtceu.api.gui.fancy.ConfiguratorPanel;
import com.gregtechceu.gtceu.api.gui.fancy.FancyMachineUIWidget;
import com.gregtechceu.gtceu.api.gui.fancy.IFancyTooltip;
import com.gregtechceu.gtceu.api.gui.fancy.TooltipsPanel;
import com.gregtechceu.gtceu.api.machine.ConditionalSubscriptionHandler;
import com.gregtechceu.gtceu.api.machine.MetaMachine;
import com.gregtechceu.gtceu.api.machine.fancyconfigurator.VoidingModeConfigurator;
import com.gregtechceu.gtceu.api.machine.fancyconfigurator.WorkingEnabledConfigurator;
import com.gregtechceu.gtceu.api.machine.feature.multiblock.IMaintenanceMachine;
import com.gregtechceu.gtceu.api.machine.feature.multiblock.IMultiPart;
import com.gregtechceu.gtceu.api.machine.multiblock.MultiblockDisplayText;
import com.gregtechceu.gtceu.api.machine.multiblock.WorkableElectricMultiblockMachine;
import com.gregtechceu.gtceu.api.multiblock.util.RelativeDirection;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import com.gregtechceu.gtceu.api.recipe.RecipeHelper;
import com.gregtechceu.gtceu.api.recipe.content.ContentModifier;
import com.gregtechceu.gtceu.api.recipe.modifier.ModifierFunction;
import com.gregtechceu.gtceu.api.recipe.modifier.ParallelLogic;
import com.gregtechceu.gtceu.api.recipe.modifier.RecipeModifier;
import com.gregtechceu.gtceu.api.sync_system.annotations.SyncToClient;
import com.gregtechceu.gtceu.common.data.GTMaterials;
import com.gregtechceu.gtceu.data.recipe.builder.GTRecipeBuilder;
import com.gregtechceu.gtceu.utils.FormattingUtil;
import com.gregtechceu.gtceu.utils.GTMath;

import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.data.Horizontal;
import com.lowdragmc.lowdraglib2.gui.ui.data.ScrollDisplay;
import com.lowdragmc.lowdraglib2.gui.ui.data.ScrollerMode;
import com.lowdragmc.lowdraglib2.gui.ui.data.Vertical;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.neoforged.neoforge.fluids.FluidStack;

import lombok.AccessLevel;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class LargeCombustionEngineMachine extends WorkableElectricMultiblockMachine {

    private static final int PAGE_WIDTH = 190;
    private static final int PAGE_HEIGHT = 125;

    private static final FluidStack OXYGEN_STACK = GTMaterials.Oxygen.getFluid(1);
    private static final FluidStack LIQUID_OXYGEN_STACK = GTMaterials.Oxygen.getFluid(FluidStorageKeys.LIQUID, 4);
    private static final FluidStack LUBRICANT_STACK = GTMaterials.Lubricant.getFluid(1);

    @Getter
    private final int tier;
    // runtime
    @SyncToClient
    private boolean isOxygenBoosted = false;
    private int runningTimer = 0;
    @Getter(AccessLevel.PACKAGE)
    private final ConditionalSubscriptionHandler displaySnapshotSubscription;
    @Getter(AccessLevel.PACKAGE)
    @SyncToClient
    private List<Component> displaySnapshot = List.of();
    @Getter(AccessLevel.PACKAGE)
    @SyncToClient
    private boolean intakeObstructionSnapshot;

    public LargeCombustionEngineMachine(BlockEntityCreationInfo info, int tier) {
        super(info);
        this.tier = tier;
        displaySnapshotSubscription = new ConditionalSubscriptionHandler(this, this::refreshDisplaySnapshot,
                this::isFormed);
    }

    protected boolean isIntakesObstructed() {
        for (int i = -1; i < 2; i++) {
            for (int j = -1; j < 2; j++) {
                // Skip the controller block itself
                if (i == 0 && j == 0) continue;
                var blockPos = RelativeDirection.offsetPos(getBlockPos(), getFrontFacing(), getUpwardsFacing(),
                        isFlipped(),
                        i, j, 1);
                var blockState = this.getLevel().getBlockState(blockPos);
                if (!blockState.isAir())
                    return true;
            }
        }
        return false;
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
        resetDisplaySnapshot();
    }

    @Override
    public void onPartUnload() {
        super.onPartUnload();
        resetDisplaySnapshot();
    }

    private void resetDisplaySnapshot() {
        displaySnapshot = List.of();
        intakeObstructionSnapshot = false;
        displaySnapshotSubscription.unsubscribe();
    }

    private boolean isExtreme() {
        return getTier() > GTValues.EV;
    }

    public boolean isBoostAllowed() {
        return getMaxVoltage() >= GTValues.V[getTier() + 1];
    }

    //////////////////////////////////////
    // ****** Recipe Logic *******//
    //////////////////////////////////////

    @Override
    public long getOverclockVoltage() {
        if (isOxygenBoosted) return GTValues.V[tier] * 2;
        else return GTValues.V[tier];
    }

    protected GTRecipe getLubricantRecipe() {
        return GTRecipeBuilder.ofRaw().inputFluids(LUBRICANT_STACK).build();
    }

    protected GTRecipe getBoostRecipe() {
        return GTRecipeBuilder.ofRaw().inputFluids(isExtreme() ? LIQUID_OXYGEN_STACK : OXYGEN_STACK).build();
    }

    /**
     * @return EUt multiplier that should be applied to the engine's output
     */
    protected double getProductionBoost() {
        if (!isOxygenBoosted) return 1;
        return isExtreme() ? 2.0 : 1.5;
    }

    void setOxygenBoosted(boolean oxygenBoosted) {
        if (this.isOxygenBoosted == oxygenBoosted) return;
        this.isOxygenBoosted = oxygenBoosted;
    }

    /**
     * Recipe Modifier for <b>Combustion Engine Multiblocks</b> - can be used as a valid {@link RecipeModifier}
     * <p>
     * Recipe is rejected if the machine's intakes are obstructed or if it doesn't have lubricant<br>
     * Recipe is parallelized up to {@code desiredEUt / recipeEUt} times.
     * EUt is further multiplied by the production boost of the engine.
     *
     * @param machine a {@link LargeCombustionEngineMachine}
     * @param recipe  recipe
     * @return A {@link ModifierFunction} for the given Combustion Engine
     */
    public static ModifierFunction recipeModifier(@NotNull MetaMachine machine, @NotNull GTRecipe recipe) {
        if (!(machine instanceof LargeCombustionEngineMachine engineMachine)) {
            return RecipeModifier.nullWrongType(LargeCombustionEngineMachine.class, machine);
        }
        long EUt = recipe.getOutputEUt();
        // has lubricant
        if (EUt > 0 && !engineMachine.isIntakesObstructed() &&
                RecipeHelper.matchRecipe(engineMachine, engineMachine.getLubricantRecipe()).isSuccess()) {
            int maxParallel = (int) (engineMachine.getOverclockVoltage() / EUt); // get maximum parallel
            int actualParallel = ParallelLogic.getParallelAmount(engineMachine, recipe, maxParallel);
            double eutMultiplier = actualParallel * engineMachine.getProductionBoost();

            return ModifierFunction.builder()
                    .inputModifier(ContentModifier.multiplier(actualParallel))
                    .outputModifier(ContentModifier.multiplier(actualParallel))
                    .eutMultiplier(eutMultiplier)
                    .parallels(actualParallel)
                    .build();
        }
        return ModifierFunction.NULL;
    }

    @Override
    public boolean onWorking() {
        boolean value = super.onWorking();
        // check lubricant

        if (runningTimer % 72 == 0) {
            // insufficient lubricant
            if (!RecipeHelper.handleRecipeIO(this, getLubricantRecipe(), IO.IN, this.recipeLogic.getChanceCaches())
                    .isSuccess()) {
                recipeLogic.interruptRecipe();
                return false;
            }
        }
        // check boost fluid
        if (isBoostAllowed()) {
            var boosterRecipe = getBoostRecipe();
            setOxygenBoosted(RecipeHelper.matchRecipe(this, boosterRecipe).isSuccess() &&
                    RecipeHelper.handleRecipeIO(this, boosterRecipe, IO.IN, this.recipeLogic.getChanceCaches())
                            .isSuccess());
        }

        runningTimer++;
        if (runningTimer > 72000) runningTimer %= 72000; // reset once every hour of running

        return value;
    }

    @Override
    public boolean regressWhenWaiting() {
        return false;
    }

    //////////////////////////////////////
    // ******* GUI ********//
    //////////////////////////////////////

    @Override
    public void addDisplayText(List<Component> textList) {
        textList.addAll(displaySnapshot);
    }

    void refreshDisplaySnapshot() {
        List<Component> nextSnapshot = createDisplaySnapshot(captureDisplayState());
        if (!displaySnapshot.equals(nextSnapshot)) {
            displaySnapshot = nextSnapshot;
        }

        boolean nextIntakeObstruction = isFormed() && isIntakesObstructed();
        if (intakeObstructionSnapshot != nextIntakeObstruction) {
            intakeObstructionSnapshot = nextIntakeObstruction;
        }
    }

    protected DisplayState captureDisplayState() {
        var workLogic = getWorkLogic();
        GTRecipe lastRecipe = recipeLogic.getLastRecipe();
        long lastEUt = lastRecipe == null ? 0 : lastRecipe.getOutputEUt();
        boolean active = workLogic.isActive();
        return new DisplayState(
                isFormed(), workLogic.isWorkingEnabled(), active, tier, lastEUt,
                isFormed() && active ? getRecipeFluidInputInfo() : null,
                recipeLogic.getDuration(), isOxygenBoosted);
    }

    static List<Component> createDisplaySnapshot(DisplayState state) {
        List<Component> text = new ArrayList<>();
        MultiblockDisplayText.Builder builder = MultiblockDisplayText.builder(text, state.formed())
                .setWorkingStatus(state.workingEnabled(), state.active());

        if (state.tier() > GTValues.EV) {
            builder.addEnergyProductionLine(GTValues.V[state.tier() + 1], state.lastEUt());
        } else {
            builder.addEnergyProductionAmpsLine(GTValues.V[state.tier()] * 3, 3);
        }

        if (state.active() && state.workingEnabled()) {
            builder.addCurrentEnergyProductionLine(state.lastEUt());
        }

        builder.addFuelNeededLine(state.requiredFuel(), state.recipeDuration());

        if (state.formed() && state.oxygenBoosted()) {
            String key = state.tier() > GTValues.EV ?
                    "gtpm.multiblock.large_combustion_engine.liquid_oxygen_boosted" :
                    "gtpm.multiblock.large_combustion_engine.oxygen_boosted";
            builder.addCustom(tl -> tl.add(Component.translatable(key).withStyle(ChatFormatting.AQUA)));
        }

        builder.addWorkingStatusLine();
        return List.copyOf(text);
    }

    record DisplayState(boolean formed, boolean workingEnabled, boolean active, int tier, long lastEUt,
                        @Nullable String requiredFuel, int recipeDuration, boolean oxygenBoosted) {}

    @Nullable
    public String getRecipeFluidInputInfo() {
        // Previous Recipe is always null on first world load, so try to acquire a new recipe
        GTRecipe recipe = recipeLogic.getLastRecipe();
        if (recipe == null) {
            Iterator<GTRecipe> iterator = recipeLogic.searchRecipe();
            recipe = iterator.hasNext() ? iterator.next() : null;
            if (recipe == null) return null;
        }
        FluidStack requiredFluidInput = RecipeHelper.getInputFluids(recipe).get(0);

        long outputEUt = recipe.getOutputEUt();
        if (outputEUt <= 0) return null;
        long ocAmount = getMaxVoltage() / outputEUt;
        int neededAmount = GTMath.saturatedCast(ocAmount * requiredFluidInput.getAmount());
        return ChatFormatting.RED + FormattingUtil.formatNumbers(neededAmount) + "mB";
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
                LargeCombustionEngineMachine.this::addDisplayText)
                .setMaxWidthLimit(200)
                .clickHandler(LargeCombustionEngineMachine.this::handleDisplayClick));
        root.addChild(screen);
        return root;
    }

    @Override
    public void attachConfigurators(ConfiguratorPanel configuratorPanel) {
        VoidingModeConfigurator.attachConfigurators(
                configuratorPanel, LargeCombustionEngineMachine.this);
        configuratorPanel.attachConfigurators(new WorkingEnabledConfigurator(
                LargeCombustionEngineMachine.this));
    }

    @Override
    public void attachTooltips(TooltipsPanel tooltipsPanel) {
        for (IMultiPart part : getParts()) {
            if (part instanceof IMaintenanceMachine maintenanceMachine) {
                maintenanceMachine.attachMaintenanceTooltips(tooltipsPanel);
            }
        }
        tooltipsPanel.attachTooltips(new IFancyTooltip.Basic(
                () -> GuiTextures.INDICATOR_NO_STEAM.get(false),
                () -> List.of(Component.translatable("gtpm.multiblock.large_combustion_engine.obstructed")
                        .setStyle(Style.EMPTY.withColor(ChatFormatting.RED))),
                LargeCombustionEngineMachine.this::isIntakeObstructionSnapshot,
                () -> null));
    }
}
