package com.gregtechceu.gtceu.common.machine.storage;

import com.gregtechceu.gtceu.api.blockentity.BlockEntityCreationInfo;
import com.gregtechceu.gtceu.api.capability.IControllable;
import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.gui.GuiTextures;
import com.gregtechceu.gtceu.api.gui.UITemplate;
import com.gregtechceu.gtceu.api.gui.element.GTButtonElement;
import com.gregtechceu.gtceu.api.gui.element.GTImageElement;
import com.gregtechceu.gtceu.api.gui.element.GTItemSlotElement;
import com.gregtechceu.gtceu.api.gui.element.GTLabelElement;
import com.gregtechceu.gtceu.api.gui.element.GTPhantomItemSlotElement;
import com.gregtechceu.gtceu.api.gui.element.GTToggleButtonElement;
import com.gregtechceu.gtceu.api.gui.factory.MachineUIHolder;
import com.gregtechceu.gtceu.api.gui.fancy.ConfiguratorPanel;
import com.gregtechceu.gtceu.api.gui.fancy.FancyMachineUIWidget;
import com.gregtechceu.gtceu.api.gui.fancy.IFancyConfiguratorButton;
import com.gregtechceu.gtceu.api.item.datacomponents.LargeItemContent;
import com.gregtechceu.gtceu.api.item.tool.GTToolType;
import com.gregtechceu.gtceu.api.item.tool.GridHighlightTexture;
import com.gregtechceu.gtceu.api.machine.TieredMachine;
import com.gregtechceu.gtceu.api.machine.feature.IFancyUIMachine;
import com.gregtechceu.gtceu.api.machine.trait.MachineTrait;
import com.gregtechceu.gtceu.api.machine.trait.MachineTraitType;
import com.gregtechceu.gtceu.api.sync_system.annotations.SaveField;
import com.gregtechceu.gtceu.api.sync_system.annotations.SyncBoth;
import com.gregtechceu.gtceu.api.sync_system.annotations.SyncToClient;
import com.gregtechceu.gtceu.api.sync_system.rpc.RPCMethod;
import com.gregtechceu.gtceu.api.sync_system.rpc.RPCSender;
import com.gregtechceu.gtceu.api.transfer.fluid.IFluidHandlerModifiable;
import com.gregtechceu.gtceu.api.transfer.item.CustomItemStackHandler;
import com.gregtechceu.gtceu.common.data.GTDataComponents;
import com.gregtechceu.gtceu.common.machine.trait.AutoOutputTrait;
import com.gregtechceu.gtceu.utils.ExtendedUseOnContext;
import com.gregtechceu.gtceu.utils.FormattingUtil;
import com.gregtechceu.gtceu.utils.GTMath;
import com.gregtechceu.gtceu.utils.GTTransferUtils;

import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.data.Horizontal;
import com.lowdragmc.lowdraglib2.gui.ui.data.Vertical;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.items.IItemHandlerModifiable;

import it.unimi.dsi.fastutil.objects.Object2LongOpenHashMap;
import lombok.Getter;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;

public class QuantumChestMachine extends TieredMachine implements IControllable,
                                 IFancyUIMachine {

    /**
     * Sourced from FunctionalStorage's
     * <a
     * href=https://github.com/Buuz135/FunctionalStorage/blob/1.21/src/main/java/com/buuz135/functionalstorage/block/tile/ItemControllableDrawerTile.java>
     * ItemControllerDrawerTile</a>
     */
    public static final Object2LongOpenHashMap<UUID> INTERACTION_LOGGER = new Object2LongOpenHashMap<>();

    private static final int PAGE_WIDTH = 109;
    private static final int PAGE_HEIGHT = 63;

    @SaveField
    @SyncBoth
    private boolean isVoiding;

    private final long maxAmount;
    protected final ItemCache cache;
    @SyncToClient
    @SaveField
    private final CustomItemStackHandler lockedItem;

    @Getter
    @SyncToClient
    @SaveField
    protected ItemStack stored = ItemStack.EMPTY;
    @Getter
    @SyncToClient
    @SaveField
    protected long storedAmount = 0;

    @SaveField
    @SyncToClient
    public final AutoOutputTrait autoOutput;

    public QuantumChestMachine(BlockEntityCreationInfo info, int tier, long maxAmount) {
        super(info, tier);
        this.maxAmount = maxAmount;
        this.cache = attachTrait(createCacheItemHandler());
        this.lockedItem = new CustomItemStackHandler();
        this.autoOutput = attachTrait(AutoOutputTrait.ofItems(cache));
        lockedItem.setOnContentsChanged(() -> syncDataHolder.markClientSyncFieldDirty("lockedItem"));
    }

    //////////////////////////////////////
    // ***** Initialization ******//
    //////////////////////////////////////

    protected ItemCache createCacheItemHandler() {
        return new ItemCache();
    }

    protected void onItemChanged() {
        if (!isRemote()) {
            syncDataHolder.markClientSyncFieldDirty("storedAmount");
            syncDataHolder.markClientSyncFieldDirty("stored");
        }
    }

    @Override
    protected void applyImplicitComponents(DataComponentInput componentInput) {
        super.applyImplicitComponents(componentInput);
        LargeItemContent storage = componentInput.getOrDefault(GTDataComponents.LARGE_ITEM_CONTENT,
                LargeItemContent.EMPTY);
        stored = storage.stored();
        storedAmount = storage.amount();
    }

    @Override
    public void collectImplicitComponents(DataComponentMap.Builder components) {
        super.collectImplicitComponents(components);
        if (!stored.isEmpty())
            components.set(GTDataComponents.LARGE_ITEM_CONTENT, new LargeItemContent(stored, storedAmount));
    }

    //////////////////////////////////////
    // ****** Capability ********//
    //////////////////////////////////////

    @Override
    public @Nullable IItemHandlerModifiable getItemHandlerCap(@Nullable Direction side, boolean useCoverCapability) {
        if (side == getFrontFacing()) {
            return null;
        }
        return super.getItemHandlerCap(side, useCoverCapability);
    }

    @Override
    public @Nullable IFluidHandlerModifiable getFluidHandlerCap(@Nullable Direction side, boolean useCoverCapability) {
        if (side == getFrontFacing()) {
            return null;
        }
        return super.getFluidHandlerCap(side, useCoverCapability);
    }

    //////////////////////////////////////
    // ******* Auto Output *******//
    //////////////////////////////////////

    @Override
    public boolean isWorkingEnabled() {
        return autoOutput.isAutoOutputItems();
    }

    @Override
    public void setWorkingEnabled(boolean isWorkingAllowed) {
        autoOutput.setAllowAutoOutputItems(isWorkingAllowed);
    }

    //////////////////////////////////////
    // ******* Interaction *******//
    //////////////////////////////////////

    @Override
    public InteractionResult onUseWithItem(ExtendedUseOnContext context) {
        if (context.getClickedFace() == getFrontFacing() && !isRemote()) {
            var hit = context.getHitResult();

            var aabb = new AABB(hit.getBlockPos()).deflate(0.12);
            var hitVector = hit.getLocation().relative(getFrontFacing(), -0.5);
            if (!aabb.contains(hitVector)) return InteractionResult.PASS;

            var held = context.getItemInHand();
            var player = context.getPlayer();

            if (cache.canInsert(held)) { // push
                var remaining = cache.insertItem(0, held, false);
                player.setItemInHand(InteractionHand.MAIN_HAND, remaining);
                return InteractionResult.SUCCESS;
            } else if (isDoubleHit(player.getUUID())) {
                for (var stack : player.getInventory().items) {
                    if (!stack.isEmpty() && cache.canInsert(stack)) {
                        stack.setCount(cache.insertItem(0, stack, false).getCount());
                    }
                }
            }
            INTERACTION_LOGGER.put(player.getUUID(), System.currentTimeMillis());
            return InteractionResult.SUCCESS;

        }

        return super.onUseWithItem(context);
    }

    private static boolean isDoubleHit(UUID uuid) {
        return (System.currentTimeMillis() - INTERACTION_LOGGER.getLong(uuid)) < 300;
    }

    @Override
    public boolean onLeftClick(Player player, InteractionHand hand,
                               @Nullable Direction direction) {
        if (direction == getFrontFacing() && !isRemote()) {
            if (GTToolType.WRENCH.matchTags.stream().anyMatch(player.getItemInHand(hand)::is)) return false;
            if (!stored.isEmpty()) { // pull
                var drained = cache.extractItem(0, player.isShiftKeyDown() ? stored.getMaxStackSize() : 1, false);
                if (!drained.isEmpty()) {
                    if (!player.addItem(drained)) {
                        Block.popResourceFromFace(getLevel(), getBlockPos(), getFrontFacing(), drained);
                    }
                }
            }
        }
        return super.onLeftClick(player, hand, direction);
    }

    public boolean isLocked() {
        return !lockedItem.getStackInSlot(0).isEmpty();
    }

    protected void setLocked(boolean locked) {
        if (!stored.isEmpty() && locked) {
            var copied = stored.copyWithCount(1);
            lockedItem.setStackInSlot(0, copied);
        } else if (!locked) {
            lockedItem.setStackInSlot(0, ItemStack.EMPTY);
        }
    }

    protected void setLocked(ItemStack stack) {
        if (stack.isEmpty()) {
            setLocked(false);
        } else if (canLockItem(stack)) {
            lockedItem.setStackInSlot(0, stack.copyWithCount(1));
        }
    }

    @RPCMethod
    public void setQuantumChestLockedItem(ItemStack stack) {
        if (stack.isEmpty()) {
            setLocked(false);
            return;
        }
        if (!canLockItem(stack)) {
            throw new IllegalArgumentException("Quantum chest locked item does not match stored item.");
        }
        lockedItem.setStackInSlot(0, stack.copyWithCount(1));
    }

    @RPCMethod
    public void setQuantumChestLocked(boolean locked) {
        setLocked(locked);
    }

    private boolean canLockItem(ItemStack stack) {
        return stored.isEmpty() || ItemStack.isSameItemSameComponents(stack, stored);
    }

    public ItemStack getLockedItem() {
        return lockedItem.getStackInSlot(0);
    }

    //////////////////////////////////////
    // *********** GUI ***********//
    //////////////////////////////////////

    private static GTLabelElement createStoredAmountLabel() {
        GTLabelElement label = new GTLabelElement(8, 8, 76, 10,
                "gtpm.machine.quantum_chest.items_stored", true);
        label.textStyle(style -> style
                .textColor(0x404040)
                .textShadow(false)
                .textAlignHorizontal(Horizontal.LEFT)
                .textAlignVertical(Vertical.CENTER));
        return label;
    }

    private GTLabelElement createStoredAmountValueLabel() {
        GTLabelElement label = new GTLabelElement(8, 18, 58, 10) {

            @Override
            public void screenTick() {
                setValue(Component.literal(FormattingUtil.formatNumbers(storedAmount)));
                super.screenTick();
            }
        };
        label.setValue(Component.literal(FormattingUtil.formatNumbers(storedAmount)));
        label.textStyle(style -> style
                .textColor(-1)
                .textShadow(true)
                .textAlignHorizontal(Horizontal.LEFT)
                .textAlignVertical(Vertical.CENTER));
        return label;
    }

    private GTItemSlotElement createImportSlot(Player player, MachineUIHolder holder) {
        GTItemSlotElement slot = new GTItemSlotElement()
                .setCanPutItems(false)
                .setCanTakeItems(false)
                .setBackgroundTexture(GuiTextures.group(GuiTextures.SLOT, GuiTextures.IN_SLOT_OVERLAY));
        slot.addEventListener(UIEvents.MOUSE_DOWN, event -> {
            if (!player.level().isClientSide()) {
                return;
            }
            ItemStack carried = player.containerMenu.getCarried();
            if ((event.button == 0 || event.button == 1) && !carried.isEmpty()) {
                ItemStack item = carried.copy();
                boolean rightClick = event.button == 1;
                int sequence = ItemStack.hashItemAndComponents(item) * 31 + item.getCount();
                holder.getMachine().rpcToServer("clickQuantumChestImportSlot", item, rightClick);
                event.stopImmediatePropagation();
                event.hasHandler = true;
            }
        });
        return UITemplate.setBounds(slot, 87, 5, 18, 18);
    }

    private GTItemSlotElement createStoredDisplaySlot() {
        GTItemSlotElement slot = new GTItemSlotElement() {

            @Override
            public void screenTick() {
                setValue(getDisplayedStoredItem(), false);
                super.screenTick();
            }
        };
        slot.setValue(getDisplayedStoredItem(), false);
        slot.setCanPutItems(false)
                .setCanTakeItems(false)
                .setBackgroundTexture(GuiTextures.SLOT);
        return UITemplate.setBounds(slot, 87, 23, 18, 18);
    }

    private ItemStack getDisplayedStoredItem() {
        if (stored.isEmpty()) {
            return ItemStack.EMPTY;
        }
        return stored.copyWithCount((int) Math.min(storedAmount, stored.getMaxStackSize()));
    }

    private GTButtonElement createExportButton(Player player, MachineUIHolder holder) {
        return new GTButtonElement(87, 42, 18, 18,
                GuiTextures.group(GuiTextures.VANILLA_BUTTON, GuiTextures.ICON_DOWN.copy().scale(0.7f)),
                event -> {
                    if (player.level().isClientSide()) {
                        holder.getMachine().rpcToServer("exportQuantumChestItem");
                    }
                    event.stopImmediatePropagation();
                    event.hasHandler = true;
                });
    }

    private GTPhantomItemSlotElement createLockedItemSlot(Player player, MachineUIHolder holder) {
        GTPhantomItemSlotElement slot = new GTPhantomItemSlotElement(this::getLockedItem,
                stack -> setLockedItem(player, holder, stack), () -> 1) {

            @Override
            public void screenTick() {
                refreshFromSupplier();
                super.screenTick();
            }
        };
        UITemplate.setBounds(slot, 58, 41, 18, 18);
        slot.addEventListener(UIEvents.MOUSE_DOWN, event -> {
            if (!player.level().isClientSide()) {
                return;
            }
            if (event.button == 0 || event.button == 1) {
                ItemStack carried = player.containerMenu.getCarried();
                if (event.button == 0 && !carried.isEmpty() && canLockItem(carried)) {
                    slot.setItem(carried.copyWithCount(1));
                } else if (event.button == 1) {
                    slot.setItem(ItemStack.EMPTY);
                }
                event.stopImmediatePropagation();
                event.hasHandler = true;
            }
        });
        return slot;
    }

    private IFancyConfiguratorButton.Toggle createWorkingEnabledConfigurator(Player player,
                                                                             MachineUIHolder holder) {
        return new IFancyConfiguratorButton.Toggle(
                GuiTextures.BUTTON_POWER.getSubTexture(0, 0, 1, 0.5),
                GuiTextures.BUTTON_POWER.getSubTexture(0, 0.5, 1, 0.5),
                this::isWorkingEnabled,
                (event, pressed) -> {
                    setWorkingEnabled(pressed);
                    event.stopImmediatePropagation();
                    event.hasHandler = true;
                })
                .setTooltipsSupplier(pressed -> List.of(Component.translatable(
                        pressed ? "behaviour.soft_hammer.enabled" : "behaviour.soft_hammer.disabled")));
    }

    private void setLockedItem(Player player, MachineUIHolder holder, ItemStack item) {
        setLocked(item);
        if (player.level().isClientSide()) {
            ItemStack locked = item.isEmpty() ? ItemStack.EMPTY : item.copyWithCount(1);
            holder.getMachine().rpcToServer("setQuantumChestLockedItem", locked);
        }
    }

    private void setLocked(Player player, MachineUIHolder holder, boolean locked) {
        setLocked(locked);
        if (player.level().isClientSide()) {
            holder.getMachine().rpcToServer("setQuantumChestLocked", locked);
        }
    }

    GTToggleButtonElement createVoidingButton() {
        return new GTToggleButtonElement(40, 41, 18, 18,
                GuiTextures.BUTTON_VOID, () -> isVoiding, value -> isVoiding = value)
                .setShouldUseBaseBackground()
                .setTooltipText("gtpm.gui.item_voiding_partial.tooltip");
    }

    @RPCMethod
    public void clickQuantumChestImportSlot(RPCSender sender, ItemStack requestedItem, boolean rightClick) {
        var player = sender.asPlayer();
        ItemStack carried = player.containerMenu.getCarried();
        if (requestedItem.isEmpty() || carried.isEmpty() ||
                !ItemStack.isSameItemSameComponents(requestedItem, carried)) {
            return;
        }
        int requestedCount = rightClick ? 1 : Math.min(requestedItem.getCount(), carried.getCount());
        ItemStack item = carried.copyWithCount(requestedCount);
        if (!cache.canInsert(item)) {
            return;
        }
        ItemStack remainder = cache.insertItem(0, item, false);
        int inserted = item.getCount() - remainder.getCount();
        if (inserted > 0) {
            carried.shrink(inserted);
            player.containerMenu.setCarried(carried.isEmpty() ? ItemStack.EMPTY : carried);
            player.containerMenu.broadcastChanges();
        }
    }

    @RPCMethod
    public void exportQuantumChestItem(RPCSender sender) {
        var player = sender.asPlayer();
        if (stored.isEmpty()) {
            return;
        }
        ItemStack extracted = cache.extractItem(0, (int) Math.min(storedAmount, stored.getMaxStackSize()), false);
        if (extracted.isEmpty()) {
            return;
        }
        if (!player.addItem(extracted)) {
            Block.popResource(player.level(), player.getOnPos(), extracted);
        }
        player.containerMenu.broadcastChanges();
    }

    //////////////////////////////////////
    // ******* Rendering ********//
    //////////////////////////////////////

    @Override
    public @Nullable GridHighlightTexture sideTips(Player player, BlockPos pos, BlockState state,
                                                   Set<GTToolType> toolTypes, ItemStack held, Direction side) {
        if (toolTypes.contains(GTToolType.SOFT_MALLET)) {
            if (side == getFrontFacing()) return null;
        }
        return super.sideTips(player, pos, state, toolTypes, held, side);
    }

    protected class ItemCache extends MachineTrait implements IItemHandlerModifiable {

        public static final MachineTraitType<ItemCache> TYPE = new MachineTraitType<>(ItemCache.class);

        @Override
        public MachineTraitType<ItemCache> getTraitType() {
            return TYPE;
        }

        private final Predicate<ItemStack> filter = i -> !isLocked() ||
                ItemStack.isSameItemSameComponents(i, getLockedItem());

        public ItemCache() {
            super();
        }

        @Override
        public void setStackInSlot(int index, ItemStack stack) {
            stored = stack.copyWithCount(1);
            storedAmount = stack.getCount();
            onItemChanged();
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            return stored.copyWithCount(GTMath.saturatedCast(storedAmount));
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            long free = isVoiding ? Long.MAX_VALUE : maxAmount - storedAmount;
            long canStore = 0;
            if ((stored.isEmpty() || ItemStack.isSameItemSameComponents(stored, stack)) && filter.test(stack)) {
                canStore = Math.min(stack.getCount(), free);
            }
            if (!simulate && canStore > 0) {
                if (stored.isEmpty()) stored = stack.copyWithCount(1);
                storedAmount = Math.min(maxAmount, storedAmount + canStore);
                onItemChanged();
            }
            return stack.copyWithCount((int) (stack.getCount() - canStore));
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            if (stored.isEmpty()) return ItemStack.EMPTY;
            long toExtract = Math.min(storedAmount, amount);
            var copy = stored.copyWithCount((int) toExtract);
            if (!simulate && toExtract > 0) {
                storedAmount -= toExtract;
                if (storedAmount == 0) stored = ItemStack.EMPTY;
                onItemChanged();
            }
            return copy;
        }

        @Override
        public int getSlotLimit(int slot) {
            return GTMath.saturatedCast(maxAmount);
        }

        @Override
        public int getSlots() {
            return 1;
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return filter.test(stack);
        }

        public void exportToNearby(Direction... facings) {
            if (stored.isEmpty()) return;
            var level = getMachine().getLevel();
            var pos = getMachine().getBlockPos();
            for (Direction facing : facings) {
                var filter = getMachine().getItemCapFilter(facing, IO.OUT);
                GTTransferUtils.getAdjacentItemHandler(level, pos, facing)
                        .ifPresent(adj -> GTTransferUtils.transferItemsFiltered(this, adj, filter));
            }
        }

        public boolean canInsert(ItemStack stack) {
            return filter.test(stack) && (insertItem(0, stack, true).getCount() != stack.getCount());
        }
    }

    @Override
    public UIElement createUIWidget(FancyMachineUIWidget shell) {
        Player player = shell.getOpeningPlayer();
        MachineUIHolder holder = shell.getHolder(this);
        UIElement root = UITemplate.setBounds(new UIElement(), 0, 0, PAGE_WIDTH, PAGE_HEIGHT);
        root.style(style -> style.backgroundTexture(GuiTextures.BACKGROUND_INVERSE));
        root.addChild(new GTImageElement(4, 4, 81, 55, GuiTextures.DISPLAY));
        root.addChild(createStoredAmountLabel());
        root.addChild(createStoredAmountValueLabel());
        root.addChild(createImportSlot(player, holder));
        root.addChild(createStoredDisplaySlot());
        root.addChild(createExportButton(player, holder));
        root.addChild(createLockedItemSlot(player, holder));
        root.addChild(new GTToggleButtonElement(4, 41, 18, 18,
                GuiTextures.BUTTON_ITEM_OUTPUT, this.autoOutput::isAutoOutputItems,
                this::setWorkingEnabled)
                .setShouldUseBaseBackground()
                .setTooltipText("gtpm.gui.item_auto_output.tooltip"));
        root.addChild(new GTToggleButtonElement(22, 41, 18, 18,
                GuiTextures.BUTTON_LOCK, this::isLocked,
                locked -> setLocked(player, holder, locked))
                .setShouldUseBaseBackground()
                .setTooltipText("gtpm.gui.item_lock.tooltip"));
        root.addChild(createVoidingButton());
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
        Player player = configuratorPanel.getPlayer();
        MachineUIHolder holder = configuratorPanel.getHolder();
        configuratorPanel.attachConfigurators(createWorkingEnabledConfigurator(player, holder));
    }
}
