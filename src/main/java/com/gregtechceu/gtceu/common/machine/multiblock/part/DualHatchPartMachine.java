package com.gregtechceu.gtceu.common.machine.multiblock.part;

import com.gregtechceu.gtceu.api.blockentity.BlockEntityCreationInfo;
import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.gui.GuiTextures;
import com.gregtechceu.gtceu.api.gui.UITemplate;
import com.gregtechceu.gtceu.api.gui.element.GTFluidSlotElement;
import com.gregtechceu.gtceu.api.gui.element.GTItemSlotElement;
import com.gregtechceu.gtceu.api.gui.factory.MachineUIHolder;
import com.gregtechceu.gtceu.api.gui.fancy.ConfiguratorPanel;
import com.gregtechceu.gtceu.api.gui.fancy.FancyMachineUIWidget;
import com.gregtechceu.gtceu.api.machine.MachineDefinition;
import com.gregtechceu.gtceu.api.machine.fancyconfigurator.CircuitFancyConfigurator;
import com.gregtechceu.gtceu.api.machine.fancyconfigurator.DistinctPartConfigurator;
import com.gregtechceu.gtceu.api.machine.fancyconfigurator.WorkingEnabledConfigurator;
import com.gregtechceu.gtceu.api.machine.trait.NotifiableFluidTank;
import com.gregtechceu.gtceu.api.sync_system.annotations.SaveField;
import com.gregtechceu.gtceu.api.sync_system.rpc.RPCMethod;
import com.gregtechceu.gtceu.api.sync_system.rpc.RPCSender;
import com.gregtechceu.gtceu.common.data.GTMachines;
import com.gregtechceu.gtceu.integration.xei.GTXEIHelper;
import com.gregtechceu.gtceu.utils.GTTransferUtils;
import com.gregtechceu.gtceu.utils.ISubscription;

import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.SoundActions;
import net.neoforged.neoforge.fluids.FluidActionResult;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.fluids.FluidUtil;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

public class DualHatchPartMachine extends ItemBusPartMachine {

    public static final int INITIAL_TANK_CAPACITY = 16 * FluidType.BUCKET_VOLUME;

    @SaveField
    public final NotifiableFluidTank tank;

    @Nullable
    protected ISubscription tankSubs;

    private boolean hasFluidHandler;
    private boolean hasItemHandler;

    public DualHatchPartMachine(BlockEntityCreationInfo info, int tier, IO io) {
        super(info, tier, io);
        this.tank = attachTrait(new NotifiableFluidTank((int) Math.sqrt(getInventorySize()),
                getTankCapacity(INITIAL_TANK_CAPACITY, getTier()), io));
    }

    ////////////////////////////////
    // ***** Initialization ******//
    ////////////////////////////////

    public static int getTankCapacity(int initialCapacity, int tier) {
        return initialCapacity * (1 << (tier - 6));
    }

    @Override
    public int getInventorySize() {
        return (int) Math.pow((getTier() - 4), 2);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        tankSubs = tank.addChangedListener(this::updateInventorySubscription);
    }

    @Override
    public void onUnload() {
        super.onUnload();
        if (tankSubs != null) {
            tankSubs.unsubscribe();
            tankSubs = null;
        }
    }

    ///////////////////////////////
    // ******** Auto IO *********//
    ///////////////////////////////

    @Override
    protected void updateInventorySubscription() {
        boolean canOutput = io == IO.OUT && (!tank.isEmpty() || !getInventory().isEmpty());
        var level = getLevel();
        if (level != null) {
            this.hasItemHandler = GTTransferUtils.hasAdjacentItemHandler(level, getBlockPos(), getFrontFacing());
            this.hasFluidHandler = GTTransferUtils.hasAdjacentFluidHandler(level, getBlockPos(), getFrontFacing());
        } else {
            this.hasItemHandler = false;
            this.hasFluidHandler = false;
        }

        if (isWorkingEnabled() && (canOutput || io.support(IO.IN)) && (hasItemHandler || hasFluidHandler)) {
            autoIOSubs = subscribeServerTick(autoIOSubs, this::autoIO);
        } else if (autoIOSubs != null) {
            autoIOSubs.unsubscribe();
            autoIOSubs = null;
        }
    }

    @Override
    protected void autoIO() {
        if (getOffsetTimer() % 5 == 0) {
            if (isWorkingEnabled()) {
                if (io.support(IO.OUT)) {
                    if (hasItemHandler) {
                        getInventory().exportToNearby(getFrontFacing());
                    }
                    if (hasFluidHandler) {
                        tank.exportToNearby(getFrontFacing());
                    }
                }
                if (io.support(IO.IN)) {
                    if (hasItemHandler) {
                        getInventory().importFromNearby(getFrontFacing());
                    }
                    if (hasFluidHandler) {
                        tank.importFromNearby(getFrontFacing());
                    }
                }
            }
            updateInventorySubscription();
        }
    }

    @Override
    public boolean swapIO() {
        BlockPos blockPos = getBlockPos();
        MachineDefinition newDefinition = null;

        if (io == IO.IN) {
            newDefinition = GTMachines.DUAL_EXPORT_HATCH[this.getTier()];
        } else if (io == IO.OUT) {
            newDefinition = GTMachines.DUAL_IMPORT_HATCH[this.getTier()];
        }
        if (newDefinition == null) return false;

        BlockState newBlockState = newDefinition.getBlock().defaultBlockState();

        getLevel().setBlockAndUpdate(blockPos, newBlockState);

        if (getLevel().getBlockEntity(blockPos) instanceof DualHatchPartMachine newMachine) {
            newMachine.setFrontFacing(this.getFrontFacing());
            newMachine.setUpwardsFacing(this.getUpwardsFacing());
            for (int i = 0; i < this.tank.getTanks(); i++) {
                newMachine.tank.setFluidInTank(i, this.tank.getFluidInTank(i));
            }
        }
        return true;
    }

    ///////////////////////////////
    // ********** GUI ***********//
    ///////////////////////////////

    /** Creates the preview-style page used when a multiblock controller exposes this hatch. */

    @Override
    int getInventoryPageWidth() {
        int tanks = (int) Math.sqrt(getInventorySize());
        return 18 * (tanks + 1) + 16;
    }

    @Override
    int getInventoryPageHeight() {
        int tanks = (int) Math.sqrt(getInventorySize());
        return 18 * tanks + 16;
    }

    @Override
    public UIElement createUIWidget(FancyMachineUIWidget widget) {
        Player player = widget.getOpeningPlayer();

        int tanks = (int) Math.sqrt(getInventorySize());
        UIElement root = UITemplate.setBounds(new UIElement(), 0, 0,
                getInventoryPageWidth(), getInventoryPageHeight());
        UIElement container = UITemplate.setBounds(new UIElement(), 4, 4,
                18 * (tanks + 1) + 8, 18 * tanks + 8);
        container.style(style -> style.backgroundTexture(GuiTextures.BACKGROUND_INVERSE));

        int index = 0;
        for (int y = 0; y < tanks; y++) {
            for (int x = 0; x < tanks; x++) {
                container.addChild(createItemSlot(index++, 4 + x * 18, 4 + y * 18));
            }
        }

        index = 0;
        for (int y = 0; y < tanks; y++) {
            container.addChild(createFluidSlot(player, index++, 4 + tanks * 18, 4 + y * 18));
        }

        root.addChild(container);
        return root;
    }

    private GTItemSlotElement createItemSlot(int slot, int x, int y) {
        GTItemSlotElement slotElement = new GTItemSlotElement(getInventory().storage, slot)
                .setCanPutItems(io.support(IO.IN))
                .setCanTakeItems(true)
                .setBackgroundTexture(GuiTextures.SLOT)
                .setIngredientIO(io.support(IO.IN) ? GTXEIHelper.input() : GTXEIHelper.output());
        return UITemplate.setBounds(slotElement, x, y, 18, 18);
    }

    private GTFluidSlotElement createFluidSlot(Player player, int tankIndex, int x, int y) {
        GTFluidSlotElement fluidSlot = new GTFluidSlotElement()
                .setFluidTank(tank.getStorages()[tankIndex], 0)
                .setShowAmount(true)
                .setAllowClickFilled(true)
                .setAllowClickDrained(io.support(IO.IN))
                .setIngredientIO(GTXEIHelper.none())
                .setBackgroundTexture(GuiTextures.FLUID_SLOT);
        fluidSlot.addEventListener(UIEvents.MOUSE_DOWN, event -> {
            if (event.button == GLFW.GLFW_MOUSE_BUTTON_LEFT && player.level().isClientSide() &&
                    FluidUtil.getFluidHandler(player.containerMenu.getCarried()).isPresent()) {

                rpcToServer("clickFluidSlot", tankIndex, event.isShiftDown());
                event.stopImmediatePropagation();
                event.hasHandler = true;
            }
        });
        return UITemplate.setBounds(fluidSlot, x, y, 18, 18);
    }

    @RPCMethod
    @ApiStatus.Internal
    public void clickFluidSlot(RPCSender sender, int tankIndex, boolean shiftDown) {
        var player = sender.asPlayer();
        new FluidClickTarget(tank.getStorages()[tankIndex], true, io.support(IO.IN)).click(player, shiftDown);
    }

    private record FluidClickTarget(IFluidHandler fluidTank, boolean allowClickFilled,
                                    boolean allowClickDrained) {

        private void click(ServerPlayer player, boolean shiftDown) {
            ItemStack currentStack = player.containerMenu.getCarried();
            if (FluidUtil.getFluidHandler(currentStack).isEmpty()) {
                return;
            }
            int maxAttempts = shiftDown ? currentStack.getCount() : 1;
            FluidStack initialFluid = fluidTank.getFluidInTank(0).copy();
            if (allowClickFilled && initialFluid.getAmount() > 0 && fillContainer(player, currentStack,
                    maxAttempts, initialFluid)) {
                return;
            }
            if (allowClickDrained) {
                emptyContainer(player, currentStack, maxAttempts);
            }
        }

        private boolean fillContainer(ServerPlayer player, ItemStack currentStack, int maxAttempts,
                                      FluidStack initialFluid) {
            boolean performedFill = false;
            ItemStack filledResult = ItemStack.EMPTY;
            for (int i = 0; i < maxAttempts; i++) {
                FluidActionResult result = FluidUtil.tryFillContainer(currentStack, fluidTank,
                        Integer.MAX_VALUE, null, false);
                if (!result.isSuccess()) {
                    break;
                }
                ItemStack remainingStack = FluidUtil.tryFillContainer(currentStack, fluidTank,
                        Integer.MAX_VALUE, null, true).getResult();
                performedFill = true;
                currentStack.shrink(1);
                filledResult = mergeOrStoreFluidContainerResult(player, filledResult, remainingStack);
            }
            if (!performedFill) {
                return false;
            }
            SoundEvent sound = initialFluid.getFluid().getFluidType().getSound(initialFluid,
                    SoundActions.BUCKET_FILL);
            if (sound == null) {
                sound = SoundEvents.BUCKET_FILL;
            }
            player.level().playSound(null, player, sound, SoundSource.BLOCKS, 1.0F, 1.0F);
            finishContainerClick(player, currentStack, filledResult);
            return true;
        }

        private void emptyContainer(ServerPlayer player, ItemStack currentStack, int maxAttempts) {
            boolean performedEmptying = false;
            ItemStack drainedResult = ItemStack.EMPTY;
            for (int i = 0; i < maxAttempts; i++) {
                int remainingCapacity = fluidTank.getTankCapacity(0) - fluidTank.getFluidInTank(0).getAmount();
                FluidActionResult result = FluidUtil.tryEmptyContainer(currentStack, fluidTank,
                        remainingCapacity, null, false);
                if (!result.isSuccess()) {
                    break;
                }
                ItemStack remainingStack = FluidUtil.tryEmptyContainer(currentStack, fluidTank,
                        remainingCapacity, null, true).getResult();
                performedEmptying = true;
                currentStack.shrink(1);
                drainedResult = mergeOrStoreFluidContainerResult(player, drainedResult, remainingStack);
            }
            FluidStack filledFluid = fluidTank.getFluidInTank(0);
            if (performedEmptying) {
                SoundEvent sound = filledFluid.getFluid().getFluidType().getSound(filledFluid,
                        SoundActions.BUCKET_EMPTY);
                if (sound == null) {
                    sound = SoundEvents.BUCKET_EMPTY;
                }
                player.level().playSound(null, player, sound, SoundSource.BLOCKS, 1.0F, 1.0F);
                finishContainerClick(player, currentStack, drainedResult);
            }
        }

        private void finishContainerClick(ServerPlayer player, ItemStack currentStack, ItemStack resultStack) {
            if (currentStack.isEmpty()) {
                player.containerMenu.setCarried(resultStack);
            } else {
                player.containerMenu.setCarried(currentStack);
                player.getInventory().placeItemBackInInventory(resultStack);
            }
            player.containerMenu.broadcastChanges();
        }
    }

    static ItemStack mergeOrStoreFluidContainerResult(ServerPlayer player, ItemStack storedResult,
                                                      ItemStack remainingStack) {
        if (storedResult.isEmpty()) {
            return remainingStack.copy();
        }
        if (ItemStack.isSameItemSameComponents(storedResult, remainingStack)) {
            int availableSpace = storedResult.getMaxStackSize() - storedResult.getCount();
            if (remainingStack.getCount() <= availableSpace) {
                storedResult.grow(remainingStack.getCount());
            } else {
                player.getInventory().placeItemBackInInventory(remainingStack);
            }
            return storedResult;
        }
        player.getInventory().placeItemBackInInventory(storedResult);
        return remainingStack.copy();
    }

    public void attachConfigurators(ConfiguratorPanel configuratorPanel) {
        MachineUIHolder holder = configuratorPanel.getHolder();
        configuratorPanel.attachConfigurators(new WorkingEnabledConfigurator(
                this, holder));
        if (io != IO.IN) {
            return;
        }
        DistinctPartConfigurator.attachConfigurators(configuratorPanel, this);
        if (isHasCircuitSlot() && isCircuitSlotEnabled()) {
            configuratorPanel.attachConfigurators(new CircuitFancyConfigurator(
                    this, holder));
        }
    }

    /** Keeps the legacy controller page as a preview while retaining Dual Hatch controls and grouping. */

    /**
     * Adapts the inherited legacy Fancy machine metadata to an LDLib2 page without making the machine implement two
     * incompatible Fancy provider contracts.
     */
}
