package com.gregtechceu.gtceu.common.machine.multiblock.generator;

import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.blockentity.BlockEntityCreationInfo;
import com.gregtechceu.gtceu.api.capability.recipe.FluidRecipeCapability;
import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.fluids.store.FluidStorageKeys;
import com.gregtechceu.gtceu.api.gui.GuiTextures;
import com.gregtechceu.gtceu.api.gui.fancy.ConfiguratorPanel;
import com.gregtechceu.gtceu.api.gui.fancy.IFancyTooltip;
import com.gregtechceu.gtceu.api.gui.fancy.TooltipsPanel;
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

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.neoforged.neoforge.fluids.FluidStack;

import lombok.AccessLevel;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Iterator;
import java.util.List;

public class LargeCombustionEngineMachine extends WorkableElectricMultiblockMachine {

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
    @SyncToClient
    private boolean intakeObstructed;

    public LargeCombustionEngineMachine(BlockEntityCreationInfo info, int tier) {
        super(info);
        this.tier = tier;
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
    public void onUnload() {
        super.onUnload();
        intakeObstructed = false;
    }

    @Override
    public void onPartUnload() {
        super.onPartUnload();
        intakeObstructed = false;
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
        boolean formed = isFormed();
        var workLogic = getWorkLogic();
        boolean active = workLogic.isActive();
        boolean workingEnabled = workLogic.isWorkingEnabled();
        GTRecipe lastRecipe = recipeLogic.getLastRecipe();
        long lastEUt = lastRecipe == null ? 0 : lastRecipe.getOutputEUt();
        var display = MultiblockDisplayText.builder(textList, formed)
                .setWorkingStatus(workingEnabled, active);

        if (tier > GTValues.EV) {
            display.addEnergyProductionLine(GTValues.V[tier + 1], lastEUt);
        } else {
            display.addEnergyProductionAmpsLine(GTValues.V[tier] * 3, 3);
        }
        if (active && workingEnabled) {
            display.addCurrentEnergyProductionLine(lastEUt);
        }
        if (formed && active) {
            display.addFuelNeededLine(getRecipeFluidInputInfo(), recipeLogic.getDuration());
        }
        if (formed && isOxygenBoosted) {
            String key = tier > GTValues.EV ?
                    "gtpm.multiblock.large_combustion_engine.liquid_oxygen_boosted" :
                    "gtpm.multiblock.large_combustion_engine.oxygen_boosted";
            textList.add(Component.translatable(key).withStyle(ChatFormatting.AQUA));
        }
        display.addWorkingStatusLine();
        intakeObstructed = formed && isIntakesObstructed();
    }

    @Nullable
    public String getRecipeFluidInputInfo() {
        GTRecipe recipe = recipeLogic.getLastRecipe();
        boolean preview = recipe == null;
        if (preview) {
            Iterator<GTRecipe> iterator = recipeLogic.searchRecipe();
            if (!iterator.hasNext()) return null;
            recipe = iterator.next();
        }

        long outputEUt = recipe.getOutputEUt();
        if (outputEUt <= 0) return null;
        // The running recipe already includes the parallel used by the engine.
        long parallel = preview ? getOverclockVoltage() / outputEUt : 1;
        var fuel = FluidRecipeCapability.CAP
                .of(recipe.getInputContents(FluidRecipeCapability.CAP).getFirst().getContent());
        int neededAmount = GTMath.saturatedCast(parallel * fuel.amount());
        return ChatFormatting.RED + FormattingUtil.formatNumbers(neededAmount) + "mB";
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
                LargeCombustionEngineMachine.this::isIntakeObstructed,
                () -> null));
    }
}
