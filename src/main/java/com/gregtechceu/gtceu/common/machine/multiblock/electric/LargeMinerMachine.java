package com.gregtechceu.gtceu.common.machine.multiblock.electric;

import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.blockentity.BlockEntityCreationInfo;
import com.gregtechceu.gtceu.api.capability.IEnergyContainer;
import com.gregtechceu.gtceu.api.capability.IMiner;
import com.gregtechceu.gtceu.api.capability.recipe.EURecipeCapability;
import com.gregtechceu.gtceu.api.capability.recipe.FluidRecipeCapability;
import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.data.chemical.material.Material;
import com.gregtechceu.gtceu.api.gui.element.GTComponentPanelElement;
import com.gregtechceu.gtceu.api.gui.factory.MachineUIHolder;
import com.gregtechceu.gtceu.api.gui.fancy.ConfiguratorPanel;
import com.gregtechceu.gtceu.api.gui.fancy.FancyMachineUIWidget;
import com.gregtechceu.gtceu.api.machine.fancyconfigurator.VoidingModeConfigurator;
import com.gregtechceu.gtceu.api.machine.fancyconfigurator.WorkingEnabledConfigurator;
import com.gregtechceu.gtceu.api.machine.feature.IDataInfoProvider;
import com.gregtechceu.gtceu.api.machine.feature.multiblock.IMultiPart;
import com.gregtechceu.gtceu.api.machine.multiblock.WorkableElectricMultiblockMachine;
import com.gregtechceu.gtceu.api.misc.EnergyContainerList;
import com.gregtechceu.gtceu.api.sync_system.managed.ISyncManaged;
import com.gregtechceu.gtceu.api.sync_system.rpc.RPCMethod;
import com.gregtechceu.gtceu.api.transfer.fluid.FluidHandlerList;
import com.gregtechceu.gtceu.common.data.GTBlocks;
import com.gregtechceu.gtceu.common.data.GTMaterials;
import com.gregtechceu.gtceu.common.item.behavior.PortableScannerBehavior;
import com.gregtechceu.gtceu.common.machine.trait.miner.LargeMinerLogic;
import com.gregtechceu.gtceu.utils.ExtendedUseOnContext;
import com.gregtechceu.gtceu.utils.GTTransferUtils;
import com.gregtechceu.gtceu.utils.GTUtil;

import com.lowdragmc.lowdraglib2.gui.util.ClickData;

import net.minecraft.ChatFormatting;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMaps;
import lombok.AccessLevel;
import lombok.Getter;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

import static com.gregtechceu.gtceu.common.data.GTMaterials.DrillingFluid;

public class LargeMinerMachine extends WorkableElectricMultiblockMachine
                               implements IMiner, IDataInfoProvider {

    public static final int CHUNK_LENGTH = 16;
    @Getter
    private final int tier;
    @Nullable
    protected EnergyContainerList energyContainer;
    @Nullable
    protected FluidHandlerList inputFluidInventory;
    @Getter(AccessLevel.PACKAGE)
    private final int drillingFluidConsumePerTick;

    public LargeMinerMachine(BlockEntityCreationInfo info, int tier, int speed, int maximumChunkDiameter, int fortune,
                             int drillingFluidConsumePerTick) {
        super(info, new LargeMinerLogic(fortune, speed, maximumChunkDiameter * CHUNK_LENGTH / 2));
        this.tier = tier;
        this.drillingFluidConsumePerTick = drillingFluidConsumePerTick;
    }

    @Override
    public LargeMinerLogic getRecipeLogic() {
        return (LargeMinerLogic) super.getRecipeLogic();
    }

    public static Material getMaterial(int tier) {
        if (tier == GTValues.EV) return GTMaterials.Steel;
        if (tier == GTValues.IV) return GTMaterials.Titanium;
        if (tier == GTValues.LuV) return GTMaterials.TungstenSteel;
        return GTMaterials.Steel;
    }

    public static Block getCasingState(int tier) {
        return GTBlocks.MATERIALS_TO_CASINGS.get(getMaterial(tier)).get();
    }

    public long getMaxVoltage() {
        return GTValues.V[getEnergyTier()];
    }

    //////////////////////////////////////
    // ******* Logic *********//
    //////////////////////////////////////
    @Override
    public void formStructure(String structureName) {
        super.formStructure(structureName);
        if (!DEFAULT_STRUCTURE.equals(structureName)) return;
        Direction opposite = this.getUpwardsFacing().getOpposite();
        getRecipeLogic().setDir(opposite == Direction.NORTH ? Direction.UP : Direction.DOWN);
        initializeAbilities();
    }

    @Override
    public boolean checkPattern(String structureName) {
        return super.checkPattern(structureName) &&
                (this.getUpwardsFacing() == Direction.NORTH || this.getUpwardsFacing() == Direction.SOUTH);
    }

    private void initializeAbilities() {
        List<IEnergyContainer> energyContainers = new ArrayList<>();
        List<IFluidHandler> fluidTanks = new ArrayList<>();
        Long2ObjectMap<IO> ioMap = getMultiblockState(DEFAULT_STRUCTURE).getFacts().getOrDefault("ioMap",
                Long2ObjectMaps.emptyMap());
        for (IMultiPart part : getParts()) {
            IO io = ioMap.getOrDefault(part.self().getBlockPos().asLong(), IO.BOTH);
            if (io == IO.NONE) continue;

            var handlerLists = part.getRecipeHandlers();
            for (var handlerList : handlerLists) {
                if (!handlerList.isValid(io)) continue;
                handlerList.getCapability(EURecipeCapability.CAP).stream()
                        .filter(IEnergyContainer.class::isInstance)
                        .map(IEnergyContainer.class::cast)
                        .forEach(energyContainers::add);
                handlerList.getCapability(FluidRecipeCapability.CAP).stream()
                        .filter(IFluidHandler.class::isInstance)
                        .map(IFluidHandler.class::cast)
                        .forEach(fluidTanks::add);
            }
        }
        this.energyContainer = new EnergyContainerList(energyContainers);
        this.inputFluidInventory = new FluidHandlerList(fluidTanks);

        int voltageTier = this.energyContainer.getTier();
        getRecipeLogic().setVoltageTier(voltageTier);
        getRecipeLogic().setOverclockAmount(Math.max(1, voltageTier - this.tier));
        getRecipeLogic().initPos(getBlockPos(), getRecipeLogic().getCurrentRadius());
    }

    public int getEnergyTier() {
        if (energyContainer == null) return this.tier;
        return Math.min(this.tier + 1,
                Math.max(this.tier, GTUtil.getFloorTierByVoltage(energyContainer.getEffectiveVoltage())));
    }

    @Override
    public boolean drainInput(boolean simulate) {
        // drain energy
        if (energyContainer != null && energyContainer.getEnergyStored() > 0) {
            long energyToDrain = GTValues.VA[getEnergyTier()];
            long resultEnergy = energyContainer.getEnergyStored() - energyToDrain;
            if (resultEnergy >= 0L && resultEnergy <= energyContainer.getEnergyCapacity()) {
                if (!simulate) {
                    energyContainer.changeEnergy(-energyToDrain);
                }
            } else {
                return false;
            }
        } else {
            return false;
        }

        // drain fluid
        if (inputFluidInventory != null && inputFluidInventory.handlers.length > 0) {
            FluidStack drillingFluid = DrillingFluid
                    .getFluid(this.drillingFluidConsumePerTick * getRecipeLogic().getOverclockAmount());
            FluidStack fluidStack = inputFluidInventory.getFluidInTank(0);
            if (fluidStack != FluidStack.EMPTY && fluidStack.is(DrillingFluid.getFluid()) &&
                    fluidStack.getAmount() >= drillingFluid.getAmount()) {
                if (!simulate) {
                    GTTransferUtils.drainFluidAccountNotifiableList(inputFluidInventory, drillingFluid,
                            IFluidHandler.FluidAction.EXECUTE);
                }
            } else {
                return false;
            }
        }
        return true;
    }

    //////////////////////////////////////
    // *********** GUI ***********//
    //////////////////////////////////////
    @Override
    public void addDisplayText(List<Component> textList) {
        super.addDisplayText(textList);
        if (!isFormed()) return;

        var logic = getRecipeLogic();
        boolean chunkMode = logic.isChunkMode();
        textList.add(Component.translatable("gtpm.machine.miner.startx", displayCoordinate(logic.getX())));
        textList.add(Component.translatable("gtpm.machine.miner.starty", displayCoordinate(logic.getY())));
        textList.add(Component.translatable("gtpm.machine.miner.startz", displayCoordinate(logic.getZ())));
        textList.add(Component.translatable("gtpm.universal.tooltip.silk_touch")
                .append(modeButton(logic.isSilkTouchMode(), "silk_touch")));
        textList.add(Component.translatable("gtpm.universal.tooltip.chunk_mode")
                .append(modeButton(chunkMode, "chunk_mode")));
        if (chunkMode) {
            int workingAreaChunks = logic.getCurrentRadius() * 2 / CHUNK_LENGTH;
            textList.add(Component.translatable("gtpm.universal.tooltip.working_area_chunks", workingAreaChunks,
                    workingAreaChunks));
        } else {
            int workingArea = IMiner.getWorkingArea(logic.getCurrentRadius());
            textList.add(Component.translatable("gtpm.universal.tooltip.working_area", workingArea, workingArea));
        }
        if (logic.isDone()) {
            textList.add(Component.translatable("gtpm.multiblock.large_miner.done")
                    .setStyle(Style.EMPTY.withColor(ChatFormatting.GREEN)));
        }
    }

    private static int displayCoordinate(int coordinate) {
        return coordinate == Integer.MAX_VALUE ? 0 : coordinate;
    }

    private static Component modeButton(boolean enabled, String componentData) {
        return GTComponentPanelElement.withButton(Component.literal("[")
                .append(enabled ? Component.translatable("gtpm.creative.activity.on") :
                        Component.translatable("gtpm.creative.activity.off"))
                .append(Component.literal("]")), componentData);
    }

    Consumer<ISyncManaged> resolveLargeMinerDisplayAction(MachineUIHolder holder, String componentData) {
        return switch (componentData) {
            case "silk_touch" -> managed -> managed.rpcToServer("toggleLargeMinerSilkTouch");
            case "chunk_mode" -> managed -> managed.rpcToServer("toggleLargeMinerChunkMode");
            default -> throw new IllegalArgumentException("Unknown Large Miner display action: " + componentData);
        };
    }

    @Override
    protected void handleDisplayClick(FancyMachineUIWidget shell, String componentData, ClickData clickData) {
        Player player = shell.getOpeningPlayer();
        MachineUIHolder holder = shell.getHolder(this);
        Consumer<ISyncManaged> action = resolveLargeMinerDisplayAction(holder, componentData);
        if (player.level().isClientSide()) {
            action.accept(holder.getMachine());
        }
    }

    @RPCMethod
    public void toggleLargeMinerSilkTouch() {
        LargeMinerLogic logic = getRecipeLogic();
        if (logic.isWorking()) {
            return;
        }
        logic.setSilkTouchMode(!logic.isSilkTouchMode());
    }

    @RPCMethod
    public void toggleLargeMinerChunkMode() {
        LargeMinerLogic logic = getRecipeLogic();
        if (logic.isWorking()) {
            return;
        }
        logic.setChunkMode(!logic.isChunkMode());
    }

    //////////////////////////////////////
    // ******* Interaction *******//
    //////////////////////////////////////
    @Override
    public InteractionResult onScrewdriverClick(ExtendedUseOnContext context) {
        if (isRemote() || !this.isFormed())
            return InteractionResult.SUCCESS;

        if (!this.isActive()) {
            int currentRadius = getRecipeLogic().getCurrentRadius();
            if (getRecipeLogic().isChunkMode()) {
                if (currentRadius - CHUNK_LENGTH <= 0) {
                    getRecipeLogic().setCurrentRadius(getRecipeLogic().getMaximumRadius());
                } else {
                    getRecipeLogic().setCurrentRadius(currentRadius - CHUNK_LENGTH);
                }
                int workingAreaChunks = getRecipeLogic().getCurrentRadius() * 2 / CHUNK_LENGTH;
                context.getPlayer()
                        .sendSystemMessage(Component.translatable("gtpm.universal.tooltip.working_area_chunks",
                                workingAreaChunks, workingAreaChunks));
            } else {
                if (currentRadius - CHUNK_LENGTH / 2 <= 0) {
                    getRecipeLogic().setCurrentRadius(getRecipeLogic().getMaximumRadius());
                } else {
                    getRecipeLogic().setCurrentRadius(currentRadius - CHUNK_LENGTH / 2);
                }
                int workingArea = IMiner.getWorkingArea(getRecipeLogic().getCurrentRadius());
                context.getPlayer().sendSystemMessage(
                        Component.translatable("gtpm.universal.tooltip.working_area", workingArea, workingArea));
            }
            getRecipeLogic().resetArea(true);
        } else {
            context.getPlayer().sendSystemMessage(Component.translatable("gtpm.multiblock.large_miner.errorradius"));
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public List<Component> getDataInfo(PortableScannerBehavior.DisplayMode mode) {
        if (mode == PortableScannerBehavior.DisplayMode.SHOW_ALL ||
                mode == PortableScannerBehavior.DisplayMode.SHOW_MACHINE_INFO) {
            int workingArea = IMiner.getWorkingArea(getRecipeLogic().getCurrentRadius());
            return Collections.singletonList(
                    Component.translatable("gtpm.universal.tooltip.working_area", workingArea, workingArea));
        }
        return new ArrayList<>();
    }

    @Override
    public void attachConfigurators(ConfiguratorPanel configuratorPanel) {
        VoidingModeConfigurator.attachConfigurators(configuratorPanel, LargeMinerMachine.this);
        configuratorPanel.attachConfigurators(new WorkingEnabledConfigurator(
                LargeMinerMachine.this));
    }
}
