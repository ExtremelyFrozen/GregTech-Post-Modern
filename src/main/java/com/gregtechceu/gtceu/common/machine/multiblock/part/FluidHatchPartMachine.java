package com.gregtechceu.gtceu.common.machine.multiblock.part;

import com.gregtechceu.gtceu.api.blockentity.BlockEntityCreationInfo;
import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.gui.GuiTextures;
import com.gregtechceu.gtceu.api.gui.UITemplate;
import com.gregtechceu.gtceu.api.gui.element.GTFluidSlotElement;
import com.gregtechceu.gtceu.api.gui.element.GTImageElement;
import com.gregtechceu.gtceu.api.gui.element.GTLabelElement;
import com.gregtechceu.gtceu.api.gui.element.GTPhantomFluidSlotElement;
import com.gregtechceu.gtceu.api.gui.element.GTToggleButtonElement;
import com.gregtechceu.gtceu.api.gui.fancy.ConfiguratorPanel;
import com.gregtechceu.gtceu.api.gui.fancy.FancyMachineUIWidget;
import com.gregtechceu.gtceu.api.machine.MachineDefinition;
import com.gregtechceu.gtceu.api.machine.TickableSubscription;
import com.gregtechceu.gtceu.api.machine.fancyconfigurator.CircuitFancyConfigurator;
import com.gregtechceu.gtceu.api.machine.fancyconfigurator.WorkingEnabledConfigurator;
import com.gregtechceu.gtceu.api.machine.feature.IHasCircuitSlot;
import com.gregtechceu.gtceu.api.machine.multiblock.MultiblockControllerMachine;
import com.gregtechceu.gtceu.api.machine.multiblock.part.TieredIOPartMachine;
import com.gregtechceu.gtceu.api.machine.trait.NotifiableFluidTank;
import com.gregtechceu.gtceu.api.machine.trait.NotifiableItemStackHandler;
import com.gregtechceu.gtceu.api.sync_system.annotations.SaveField;
import com.gregtechceu.gtceu.api.sync_system.annotations.SyncToClient;
import com.gregtechceu.gtceu.api.sync_system.rpc.RPCMethod;
import com.gregtechceu.gtceu.api.sync_system.rpc.RPCSender;
import com.gregtechceu.gtceu.common.data.GTMachines;
import com.gregtechceu.gtceu.common.item.behavior.IntCircuitBehaviour;
import com.gregtechceu.gtceu.config.ConfigHolder;
import com.gregtechceu.gtceu.integration.xei.GTXEIHelper;
import com.gregtechceu.gtceu.utils.ExtendedUseOnContext;
import com.gregtechceu.gtceu.utils.GTTransferUtils;
import com.gregtechceu.gtceu.utils.ISubscription;

import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.data.Horizontal;
import com.lowdragmc.lowdraglib2.gui.ui.data.Vertical;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.SoundActions;
import net.neoforged.neoforge.fluids.FluidActionResult;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.fluids.FluidUtil;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

import lombok.Getter;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import java.util.Optional;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class FluidHatchPartMachine extends TieredIOPartMachine
                                   implements IHasCircuitSlot {

    public static final int INITIAL_TANK_CAPACITY_1X = 8 * FluidType.BUCKET_VOLUME;
    public static final int INITIAL_TANK_CAPACITY_4X = 2 * FluidType.BUCKET_VOLUME;
    public static final int INITIAL_TANK_CAPACITY_9X = FluidType.BUCKET_VOLUME;

    @SaveField
    public final NotifiableFluidTank tank;
    private final int slots;
    @Nullable
    protected TickableSubscription autoIOSubs;
    @Nullable
    protected ISubscription tankSubs;
    @Getter
    @SaveField
    @SyncToClient
    protected boolean circuitSlotEnabled;
    @Getter
    @SaveField
    protected final NotifiableItemStackHandler circuitInventory;

    public FluidHatchPartMachine(BlockEntityCreationInfo info, int tier, IO io, int initialCapacity, int slots) {
        super(info, tier, io);
        this.slots = slots;
        this.tank = attachTrait(createTank(initialCapacity, slots));

        if (io == IO.IN) {
            this.circuitSlotEnabled = true;
            this.circuitInventory = attachTrait(new NotifiableItemStackHandler(1, IO.IN, IO.NONE))
                    .setFilter(IntCircuitBehaviour::isIntegratedCircuit).shouldSearchContent(false)
                    .shouldDropInventoryInWorld(!ConfigHolder.INSTANCE.machines.ghostCircuit);
        } else {
            this.circuitSlotEnabled = false;
            this.circuitInventory = attachTrait(new NotifiableItemStackHandler(0, IO.NONE)).shouldSearchContent(false);
        }
    }

    //////////////////////////////////////
    // ***** Initialization ******//
    //////////////////////////////////////

    protected NotifiableFluidTank createTank(int initialCapacity, int slots) {
        return new NotifiableFluidTank(slots, getTankCapacity(initialCapacity, getTier()), io);
    }

    public static int getTankCapacity(int initialCapacity, int tier) {
        return initialCapacity * (1 << Math.min(9, tier));
    }

    @Override
    public void onLoad() {
        super.onLoad();
        scheduleForNextServerTick(this::updateTankSubscription);
        getHandlerList().setColor(getPaintingColor());
        tankSubs = tank.addChangedListener(this::updateTankSubscription);
    }

    @Override
    public void onUnload() {
        super.onUnload();
        if (tankSubs != null) {
            tankSubs.unsubscribe();
            tankSubs = null;
        }
    }

    @Override
    public void onPaintingColorChanged(int color) {
        getHandlerList().setColor(color, true);
    }

    @Override
    public void addedToController(MultiblockControllerMachine controller, String structureName) {
        if (!controller.allowCircuitSlots()) {
            if (!ConfigHolder.INSTANCE.machines.ghostCircuit) {
                circuitInventory.dropInventoryInWorld();
            } else {
                circuitInventory.setStackInSlot(0, ItemStack.EMPTY);
            }
            setCircuitSlotEnabled(false);
        }
        super.addedToController(controller, structureName);
    }

    @Override
    public void removedFromController(MultiblockControllerMachine controller, String structureName) {
        super.removedFromController(controller, structureName);
        for (var c : controllers) {
            if (!c.allowCircuitSlots()) {
                return;
            }
        }
        setCircuitSlotEnabled(true);
    }

    @Override
    public int tintColor(int index) {
        if (index == 9) return getRealColor();
        return -1;
    }

    public void setCircuitSlotEnabled(boolean enabled) {
        circuitSlotEnabled = enabled;
    }

    //////////////////////////////////////
    // ******** Auto IO *********//
    //////////////////////////////////////

    @Override
    public void onNeighborChanged(Block block, BlockPos fromPos, boolean isMoving) {
        super.onNeighborChanged(block, fromPos, isMoving);
        updateTankSubscription();
    }

    @Override
    public void onRotated(Direction oldFacing, Direction newFacing) {
        super.onRotated(oldFacing, newFacing);
        updateTankSubscription(newFacing);
    }

    protected void updateTankSubscription() {
        updateTankSubscription(getFrontFacing());
    }

    protected void updateTankSubscription(Direction newFacing) {
        if (isWorkingEnabled() && ((io.support(IO.OUT) && !tank.isEmpty()) || io.support(IO.IN)) &&
                GTTransferUtils.hasAdjacentFluidHandler(getLevel(), getBlockPos(), newFacing)) {
            autoIOSubs = subscribeServerTick(autoIOSubs, this::autoIO);
        } else if (autoIOSubs != null) {
            autoIOSubs.unsubscribe();
            autoIOSubs = null;
        }
    }

    protected void autoIO() {
        if (getOffsetTimer() % 5 == 0) {
            if (isWorkingEnabled()) {
                if (io.support(IO.OUT)) {
                    tank.exportToNearby(getFrontFacing());
                } else if (io.support(IO.IN)) {
                    tank.importFromNearby(getFrontFacing());
                }
            }
            updateTankSubscription();
        }
    }

    @Override
    protected void onWorkingEnabledChanged() {
        super.onWorkingEnabledChanged();
        updateTankSubscription();
    }

    @Override
    protected InteractionResult onScrewdriverClick(ExtendedUseOnContext context) {
        InteractionResult superResult = super.onScrewdriverClick(context);
        if (superResult != InteractionResult.PASS) return superResult;
        if (io == IO.BOTH) return InteractionResult.PASS;
        if (context.getPlayer().isShiftKeyDown()) {
            if (swapIO()) {
                return InteractionResult.sidedSuccess(getLevel().isClientSide);
            }
        }
        return InteractionResult.PASS;
    }

    public boolean swapIO() {
        BlockPos blockPos = getBlockPos();
        MachineDefinition newDefinition = null;

        if (io.support(IO.IN)) {
            if (this.slots == 1) newDefinition = GTMachines.FLUID_EXPORT_HATCH[this.getTier()];
            else if (this.slots == 4) newDefinition = GTMachines.FLUID_EXPORT_HATCH_4X[this.getTier()];
            else if (this.slots == 9) newDefinition = GTMachines.FLUID_EXPORT_HATCH_9X[this.getTier()];
        } else if (io.support(IO.OUT)) {
            if (this.slots == 1) newDefinition = GTMachines.FLUID_IMPORT_HATCH[this.getTier()];
            else if (this.slots == 4) newDefinition = GTMachines.FLUID_IMPORT_HATCH_4X[this.getTier()];
            else if (this.slots == 9) newDefinition = GTMachines.FLUID_IMPORT_HATCH_9X[this.getTier()];
        }
        if (newDefinition == null) return false;

        BlockState newBlockState = newDefinition.getBlock().defaultBlockState();

        getLevel().setBlockAndUpdate(blockPos, newBlockState);

        if (getLevel().getBlockEntity(blockPos) instanceof FluidHatchPartMachine newMachine) {
            newMachine.setFrontFacing(this.getFrontFacing());
            newMachine.setUpwardsFacing(this.getUpwardsFacing());
            newMachine.setPaintingColor(this.getPaintingColor());
            for (int i = 0; i < this.tank.getTanks(); i++) {
                newMachine.tank.setFluidInTank(i, this.tank.getFluidInTank(i));
            }
        }
        return true;
    }

    //////////////////////////////////////
    // ********** GUI ***********//
    //////////////////////////////////////

    /** Determines whether this definition may reuse the generic holder-scoped LDLib2 fluid hatch page. */

    private static boolean containsDefinition(MachineDefinition definition, MachineDefinition[] definitions) {
        for (MachineDefinition candidate : definitions) {
            if (candidate == definition) {
                return true;
            }
        }
        return false;
    }

    public int getPageWidth() {
        if (slots == 1) {
            return 89;
        }
        int rowSize = getTankRowSize();
        return 18 * rowSize + 16;
    }

    public int getPageHeight() {
        if (slots == 1) {
            return 63;
        }
        int columnSize = getTankColumnSize();
        return 18 * columnSize + 16;
    }

    private int getTankRowSize() {
        return slots == 8 ? 4 : (int) Math.sqrt(slots);
    }

    private int getTankColumnSize() {
        return slots == 8 ? 2 : (int) Math.sqrt(slots);
    }

    private static Optional<FluidStack> getSelectedLockedFluid(Player player) {
        ItemStack carried = player.containerMenu.getCarried();
        if (carried.isEmpty()) {
            return Optional.of(FluidStack.EMPTY);
        }
        if (FluidUtil.getFluidHandler(carried).isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(FluidUtil.getFluidContained(carried)
                .map(fluid -> fluid.copyWithAmount(1))
                .orElse(FluidStack.EMPTY));
    }

    private UIElement createSingleSlotGUI(Player player) {
        UIElement root = UITemplate.setBounds(new UIElement(), 0, 0, 89, 63);
        UITemplate.setBackgroundTexture(root, GuiTextures.BACKGROUND_INVERSE);
        root.addChild(new GTImageElement(4, 4, 81, 55, GuiTextures.DISPLAY));

        GTFluidSlotElement fluidSlot = createFluidSlot(player, 0, 67, 22);
        root.addChild(fluidSlot);
        if (supportsFluidHatchLocking()) {
            root.addChild(createLockedFluidSlot(player));
            root.addChild(new GTToggleButtonElement(7, 40, 18, 18,
                    GuiTextures.BUTTON_LOCK, tank::isLocked,
                    locked -> {
                        if (isRemote()) rpcToServer("setFluidHatchLocked", locked);
                    })
                    .setTooltipText("gtpm.gui.fluid_lock.tooltip")
                    .setShouldUseBaseBackground());
        }

        root.addChild(createFluidAmountLabel());
        root.addChild(createFluidAmountValueLabel());
        root.addChild(createFluidNameLabel());
        return root;
    }

    private static GTLabelElement createFluidAmountLabel() {
        return new GTLabelElement(8, 8, 57, 10,
                Component.translatable("gtpm.gui.fluid_amount"))
                .setTextColor(-1)
                .setTextShadow(true)
                .setTextAlignHorizontal(Horizontal.LEFT)
                .setTextAlignVertical(Vertical.CENTER);
    }

    private GTLabelElement createFluidAmountValueLabel() {
        return new GTLabelElement(8, 18, 57, 10) {

            @Override
            public void screenTick() {
                setValue(Component.literal(getFluidAmountText()));
                super.screenTick();
            }
        }.setValue(Component.literal(getFluidAmountText()))
                .setTextColor(-1)
                .setTextShadow(true)
                .setTextAlignHorizontal(Horizontal.LEFT)
                .setTextAlignVertical(Vertical.CENTER);
    }

    private GTLabelElement createFluidNameLabel() {
        return new GTLabelElement(8, 28, 57, 10) {

            @Override
            public void screenTick() {
                setValue(getFluidName());
                super.screenTick();
            }
        }.setValue(getFluidName())
                .setTextColor(-1)
                .setTextShadow(true)
                .setTextAlignHorizontal(Horizontal.LEFT)
                .setTextAlignVertical(Vertical.CENTER);
    }

    private String getFluidAmountText() {
        FluidStack fluid = tank.getFluidInTank(0);
        if (!fluid.isEmpty()) {
            return getFormattedFluidAmount(fluid);
        }
        return tank.getLockedFluid().getFluid().isEmpty() ? "" : "0";
    }

    private Component getFluidName() {
        FluidStack fluid = tank.getFluidInTank(0);
        return (fluid.isEmpty() ? tank.getLockedFluid().getFluid() : fluid).getHoverName();
    }

    private GTPhantomFluidSlotElement createLockedFluidSlot(Player player) {
        GTPhantomFluidSlotElement slot = new GTPhantomFluidSlotElement(
                () -> tank.getLockedFluid().getFluid(),
                fluid -> {
                    if (isRemote()) {
                        rpcToServer("setFluidHatchLockedFluid", fluid);
                    }
                },
                () -> 1) {

            @Override
            public void screenTick() {
                refreshFromSupplier();
                setShowAmount(false);
                super.screenTick();
            }
        };
        slot.setBackgroundTexture(GuiTextures.FLUID_SLOT);
        slot.setShowAmount(false);
        UITemplate.setBounds(slot, 67, 40, 18, 18);
        slot.addEventListener(UIEvents.MOUSE_DOWN, event -> {
            if (isRemote() &&
                    (event.button == GLFW.GLFW_MOUSE_BUTTON_LEFT ||
                            event.button == GLFW.GLFW_MOUSE_BUTTON_RIGHT)) {
                getSelectedLockedFluid(player).ifPresent(fluid -> {
                    rpcToServer("setFluidHatchLockedFluid", fluid);
                    event.stopImmediatePropagation();
                    event.hasHandler = true;
                });
            }
        });
        return slot;
    }

    private UIElement createMultiSlotGUI(Player player) {
        int rowSize = getTankRowSize();
        int columnSize = getTankColumnSize();
        UIElement root = UITemplate.setBounds(new UIElement(), 0, 0,
                18 * rowSize + 16, 18 * columnSize + 16);
        UIElement container = UITemplate.setBounds(new UIElement(), 4, 4,
                18 * rowSize + 8, 18 * columnSize + 8);
        UITemplate.setBackgroundTexture(container, GuiTextures.BACKGROUND_INVERSE);

        int index = 0;
        for (int y = 0; y < columnSize; y++) {
            for (int x = 0; x < rowSize; x++) {
                container.addChild(createFluidSlot(player, index++,
                        4 + x * 18, 4 + y * 18));
            }
        }
        root.addChild(container);
        return root;
    }

    private GTFluidSlotElement createFluidSlot(Player player, int tankIndex, int x, int y) {
        GTFluidSlotElement fluidSlot = new GTFluidSlotElement()
                .setFluidTank(tank.getStorages()[tankIndex], 0)
                .setShowAmount(true)
                .setAllowClickFilled(true)
                .setAllowClickDrained(io.support(IO.IN))
                .setIngredientIO(io.support(IO.IN) ? GTXEIHelper.input() : GTXEIHelper.output())
                .setBackgroundTexture(GuiTextures.FLUID_SLOT);
        fluidSlot.addEventListener(UIEvents.MOUSE_DOWN, event -> {
            if (event.button == GLFW.GLFW_MOUSE_BUTTON_LEFT && isRemote() &&
                    FluidUtil.getFluidHandler(player.containerMenu.getCarried()).isPresent()) {

                rpcToServer("clickFluidHatchSlot", tankIndex, event.isShiftDown());
                event.stopImmediatePropagation();
                event.hasHandler = true;
            }
        });
        return UITemplate.setBounds(fluidSlot, x, y, 18, 18);
    }

    @ApiStatus.Internal
    public boolean supportsFluidHatchLocking() {
        return slots == 1 && io.support(IO.OUT);
    }

    @RPCMethod
    @ApiStatus.Internal
    public void clickFluidHatchSlot(RPCSender sender, int tankIndex, boolean shiftDown) {
        var player = sender.asPlayer();

        new FluidClickTarget(tank.getStorages()[tankIndex], true, io.support(IO.IN))
                .click(player, shiftDown);
    }

    @RPCMethod
    @ApiStatus.Internal
    public void setFluidHatchLockedFluid(FluidStack fluid) {
        if (!tank.getFluidInTank(0).isEmpty()) {
            return;
        }
        if (fluid.isEmpty()) {
            tank.setLocked(false);
        } else {
            tank.setLocked(true, fluid.copyWithAmount(1));
        }
    }

    @RPCMethod
    @ApiStatus.Internal
    public void setFluidHatchLocked(boolean locked) {
        tank.setLocked(locked);
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
            if (allowClickFilled && initialFluid.getAmount() > 0 &&
                    fillContainer(player, currentStack, maxAttempts, initialFluid)) {
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

        private static void finishContainerClick(ServerPlayer player, ItemStack currentStack,
                                                 ItemStack resultStack) {
            if (currentStack.isEmpty()) {
                player.containerMenu.setCarried(resultStack);
            } else {
                player.containerMenu.setCarried(currentStack);
                player.getInventory().placeItemBackInInventory(resultStack);
            }
            player.containerMenu.broadcastChanges();
        }
    }

    private static ItemStack mergeOrStoreFluidContainerResult(ServerPlayer player, ItemStack storedResult,
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

    /**
     * Adapts this legacy multipart Fancy page to a holder-scoped LDLib2 Fancy provider.
     */

    public String getFormattedFluidAmount(FluidStack fluidStack) {
        return String.format("%,d", fluidStack.isEmpty() ? 0 : fluidStack.getAmount());
    }

    @Override
    public UIElement createUIWidget(FancyMachineUIWidget widget) {
        return slots == 1 ? createSingleSlotGUI(widget.getOpeningPlayer()) :
                createMultiSlotGUI(widget.getOpeningPlayer());
    }

    @Override
    public void attachConfigurators(ConfiguratorPanel configuratorPanel) {
        configuratorPanel.attachConfigurators(new WorkingEnabledConfigurator(
                FluidHatchPartMachine.this));
        if (isCircuitSlotEnabled() && io.support(IO.IN)) {
            configuratorPanel.attachConfigurators(new CircuitFancyConfigurator(
                    FluidHatchPartMachine.this));
        }
    }
}
