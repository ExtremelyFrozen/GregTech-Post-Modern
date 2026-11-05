package com.gregtechceu.gtceu.common.machine.storage;

import com.gregtechceu.gtceu.GTCEu;
import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.blockentity.BlockEntityCreationInfo;
import com.gregtechceu.gtceu.api.gui.GuiTextures;
import com.gregtechceu.gtceu.api.gui.UITemplate;
import com.gregtechceu.gtceu.api.gui.element.GTButtonElement;
import com.gregtechceu.gtceu.api.gui.element.GTImageElement;
import com.gregtechceu.gtceu.api.gui.element.GTLabelElement;
import com.gregtechceu.gtceu.api.gui.element.GTPhantomItemSlotElement;
import com.gregtechceu.gtceu.api.gui.element.GTTextFieldElement;
import com.gregtechceu.gtceu.api.gui.factory.MachineUIHolder;
import com.gregtechceu.gtceu.api.gui.fancy.ConfiguratorPanel;
import com.gregtechceu.gtceu.api.gui.fancy.FancyMachineUIWidget;
import com.gregtechceu.gtceu.api.gui.fancy.IFancyConfiguratorButton;
import com.gregtechceu.gtceu.api.gui.fancy.IFancyTooltip;
import com.gregtechceu.gtceu.api.gui.fancy.IFancyUIProvider;
import com.gregtechceu.gtceu.api.gui.fancy.TooltipsPanel;
import com.gregtechceu.gtceu.api.gui.texture.IGuiTexture;
import com.gregtechceu.gtceu.api.item.datacomponents.CreativeMachineInfo;
import com.gregtechceu.gtceu.api.machine.feature.IUIMachine;
import com.gregtechceu.gtceu.api.sync_system.annotations.SaveField;
import com.gregtechceu.gtceu.api.sync_system.annotations.ServerFieldChangeListener;
import com.gregtechceu.gtceu.api.sync_system.annotations.ServerFieldNormalizer;
import com.gregtechceu.gtceu.api.sync_system.annotations.SyncBoth;
import com.gregtechceu.gtceu.api.sync_system.rpc.RPCMethod;
import com.gregtechceu.gtceu.common.data.GTDataComponents;
import com.gregtechceu.gtceu.utils.ExtendedUseOnContext;

import com.lowdragmc.lowdraglib2.gui.ui.UI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.data.Horizontal;
import com.lowdragmc.lowdraglib2.gui.ui.data.Vertical;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;

import net.minecraft.core.component.DataComponentMap;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import lombok.Getter;

import java.util.List;

public class CreativeChestMachine extends QuantumChestMachine
                                  implements IUIMachine {

    private static final int PAGE_WIDTH = 176;
    private static final int PAGE_HEIGHT = 131;
    @Getter
    @SaveField
    @SyncBoth
    private int itemsPerCycle;
    @Getter
    @SaveField
    @SyncBoth
    private int ticksPerCycle = 1;

    public CreativeChestMachine(BlockEntityCreationInfo info) {
        super(info, GTValues.MAX, -1);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (!isRemote()) autoOutput.setTicksPerCycle(ticksPerCycle);
    }

    @Override
    protected ItemCache createCacheItemHandler() {
        return new InfiniteCache();
    }

    @RPCMethod
    public void setCreativeChestItem(ItemStack item) {
        stored = item.isEmpty() ? ItemStack.EMPTY : item.copyWithCount(1);
        onItemChanged();
    }

    private void setTicksPerCycle(int ticksPerCycle) {
        int normalizedTicksPerCycle = normalizeTicksPerCycle(ticksPerCycle);
        this.ticksPerCycle = normalizedTicksPerCycle;
        autoOutput.setTicksPerCycle(normalizedTicksPerCycle);
        onItemChanged();
    }

    private void setItemsPerCycle(int itemsPerCycle) {
        this.itemsPerCycle = normalizeItemsPerCycle(itemsPerCycle);
        onItemChanged();
    }

    @ServerFieldNormalizer(fieldName = "itemsPerCycle")
    private int normalizeItemsPerCycle(int candidate) {
        if (candidate <= 0) {
            throw new IllegalArgumentException("Items per cycle must be positive: " + candidate);
        }
        return candidate;
    }

    @ServerFieldNormalizer(fieldName = "ticksPerCycle")
    private int normalizeTicksPerCycle(int candidate) {
        if (candidate <= 0) {
            throw new IllegalArgumentException("Ticks per cycle must be positive: " + candidate);
        }
        return candidate;
    }

    @ServerFieldChangeListener(fieldName = "itemsPerCycle")
    private void onItemsPerCycleChanged(int oldValue, int newValue) {
        onItemChanged();
    }

    @ServerFieldChangeListener(fieldName = "ticksPerCycle")
    private void onTicksPerCycleChanged(int oldValue, int newValue) {
        autoOutput.setTicksPerCycle(newValue);
        onItemChanged();
    }

    @Override
    public InteractionResult onUseWithItem(ExtendedUseOnContext context) {
        var heldItem = context.getItemInHand();
        var player = context.getPlayer();

        if (context.getClickedFace() != getFrontFacing() || isRemote()) {
            return InteractionResult.PASS;
        }
        // Clear item if empty hand + shift-rclick
        if (player.getItemInHand(context.getHand()).isEmpty() && player.isShiftKeyDown() && !stored.isEmpty()) {
            setCreativeChestItem(ItemStack.EMPTY);
            return InteractionResult.SUCCESS;
        }
        return super.onUseWithItem(context);
    }

    @Override
    public boolean canCreateUI(Player player, MachineUIHolder holder) {
        return holder.getMachine() == this;
    }

    @Override
    public UI createUI(Player player, MachineUIHolder holder) {
        return UI.of(new FancyMachineUIWidget(new CreativeChestPage(player, holder),
                player.getInventory(), holder, PAGE_WIDTH, PAGE_HEIGHT));
    }

    private UIElement createUIWidget(Player player, MachineUIHolder holder) {
        UIElement root = UITemplate.setBounds(new UIElement(), 0, 0, PAGE_WIDTH, PAGE_HEIGHT);
        root.addChild(createStoredItemSlot(player, holder));
        root.addChild(createLabel(7, 9, 162, 10, "gtpm.creative.chest.item"));
        root.addChild(new GTImageElement(7, 48, 154, 14, GuiTextures.DISPLAY));
        root.addChild(createItemsPerCycleField());
        root.addChild(createLabel(7, 28, 162, 10, "gtpm.creative.chest.ipc"));
        root.addChild(new GTImageElement(7, 85, 154, 14, GuiTextures.DISPLAY));
        root.addChild(createTicksPerCycleField());
        root.addChild(createLabel(7, 65, 162, 10, "gtpm.creative.chest.tpc"));
        root.addChild(createActivityButton());
        return root;
    }

    private GTPhantomItemSlotElement createStoredItemSlot(Player player, MachineUIHolder holder) {
        GTPhantomItemSlotElement slot = new GTPhantomItemSlotElement(this::getStored,
                stack -> setStoredItem(player, holder, stack), () -> 1);
        slot.setBackgroundTexture(GuiTextures.SLOT);
        UITemplate.setBounds(slot, 36, 6, 18, 18);
        slot.addEventListener(UIEvents.MOUSE_DOWN, event -> {
            if (!player.level().isClientSide()) {
                return;
            }
            if (event.button == 0) {
                ItemStack carried = player.containerMenu.getCarried();
                if (!carried.isEmpty()) {
                    slot.setItem(carried.copyWithCount(1));
                    event.stopImmediatePropagation();
                    event.hasHandler = true;
                }
            } else if (event.button == 1 && !stored.isEmpty()) {
                slot.setItem(ItemStack.EMPTY);
                event.stopImmediatePropagation();
                event.hasHandler = true;
            }
        });
        return slot;
    }

    private GTLabelElement createLabel(int x, int y, int width, int height, String translationKey) {
        GTLabelElement label = new GTLabelElement(x, y, width, height, translationKey, true);
        label.textStyle(style -> style
                .textColor(0x404040)
                .textShadow(false)
                .textAlignHorizontal(Horizontal.LEFT)
                .textAlignVertical(Vertical.CENTER));
        return label;
    }

    GTTextFieldElement createItemsPerCycleField() {
        GTTextFieldElement field = new GTTextFieldElement(9, 50, 152, 10) {

            @Override
            public void screenTick() {
                if (!isFocused()) {
                    setText(Integer.toString(itemsPerCycle), false);
                }
                super.screenTick();
            }
        };
        field.setNumbersOnlyInt(1, Integer.MAX_VALUE);
        field.setText(Integer.toString(itemsPerCycle), false);
        field.textFieldStyle(style -> style
                .textColor(0x404040)
                .textShadow(false));
        field.setTextResponder(this::setItemsPerCycle);
        return field;
    }

    GTTextFieldElement createTicksPerCycleField() {
        GTTextFieldElement field = new GTTextFieldElement(9, 87, 152, 10) {

            @Override
            public void screenTick() {
                if (!isFocused()) {
                    setText(Integer.toString(ticksPerCycle), false);
                }
                super.screenTick();
            }
        };
        field.setNumbersOnlyInt(1, Integer.MAX_VALUE);
        field.setText(Integer.toString(ticksPerCycle), false);
        field.textFieldStyle(style -> style
                .textColor(0x404040)
                .textShadow(false));
        field.setTextResponder(this::setTicksPerCycle);
        return field;
    }

    private GTButtonElement createActivityButton() {
        return new GTButtonElement(7, 101, 162, 20, createActivityButtonTexture(),
                event -> setWorkingEnabledAndSync(!isWorkingEnabled())) {

            @Override
            public void screenTick() {
                setButtonTexture(createActivityButtonTexture());
                super.screenTick();
            }
        };
    }

    private IGuiTexture createActivityButtonTexture() {
        return GuiTextures.group(GuiTextures.BUTTON,
                GuiTextures.text(isWorkingEnabled() ? "gtpm.creative.activity.on" : "gtpm.creative.activity.off"));
    }

    IFancyConfiguratorButton.Toggle createWorkingEnabledConfigurator() {
        return new IFancyConfiguratorButton.Toggle(
                GuiTextures.BUTTON_POWER.getSubTexture(0, 0, 1, 0.5),
                GuiTextures.BUTTON_POWER.getSubTexture(0, 0.5, 1, 0.5),
                this::isWorkingEnabled,
                (event, pressed) -> {
                    setWorkingEnabledAndSync(pressed);
                    event.stopImmediatePropagation();
                    event.hasHandler = true;
                })
                .setTooltipsSupplier(pressed -> List.of(Component.translatable(
                        pressed ? "behaviour.soft_hammer.enabled" : "behaviour.soft_hammer.disabled")));
    }

    private void setStoredItem(Player player, MachineUIHolder holder, ItemStack item) {
        setCreativeChestItem(item);
        if (player.level().isClientSide()) {
            ItemStack storedItem = item.isEmpty() ? ItemStack.EMPTY : item.copyWithCount(1);
            holder.getMachine().rpcToServer("setCreativeChestItem", storedItem);
        }
    }

    private void setItemsPerCycle(String value) {
        if (value.isEmpty()) {
            return;
        }
        int parsedValue = parsePositiveInteger(value, "creative chest items per cycle");
        setItemsPerCycle(parsedValue);
        if (isRemote()) {
            sendServerSyncChanges();
        }
    }

    private void setTicksPerCycle(String value) {
        if (value.isEmpty()) {
            return;
        }
        int parsedValue = parsePositiveInteger(value, "creative chest ticks per cycle");
        setTicksPerCycle(parsedValue);
        if (isRemote()) {
            sendServerSyncChanges();
        }
    }

    private void setWorkingEnabledAndSync(boolean workingEnabled) {
        setWorkingEnabled(workingEnabled);
        if (isRemote()) {
            sendServerSyncChanges();
        }
    }

    private static int parsePositiveInteger(String value, String fieldName) {
        try {
            int parsedValue = Integer.parseInt(value);
            if (parsedValue <= 0) {
                throw new IllegalArgumentException(fieldName + " must be positive: " + parsedValue);
            }
            return parsedValue;
        } catch (NumberFormatException e) {
            GTCEu.LOGGER.error("Invalid {} input: {}", fieldName, value, e);
            throw e;
        }
    }

    private final class CreativeChestPage implements IFancyUIProvider {

        private final Player player;
        private final MachineUIHolder holder;

        private CreativeChestPage(Player player, MachineUIHolder holder) {
            this.player = player;
            this.holder = holder;
        }

        @Override
        public UIElement createUIWidget(FancyMachineUIWidget shell) {
            return CreativeChestMachine.this.createUIWidget(player, holder);
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
        public void attachConfigurators(ConfiguratorPanel configuratorPanel) {
            configuratorPanel.attachConfigurators(createWorkingEnabledConfigurator());
        }

        @Override
        public void attachTooltips(TooltipsPanel tooltipsPanel) {
            tooltipsPanel.attachTooltips(CreativeChestMachine.this);
            getTraitHolder().getAllTraits().stream()
                    .filter(IFancyTooltip.class::isInstance)
                    .map(IFancyTooltip.class::cast)
                    .forEach(tooltipsPanel::attachTooltips);
        }

        @Override
        public List<Component> getTabTooltips() {
            return List.of(Component.translatable(getDefinition().getDescriptionId()));
        }
    }

    @Override
    protected void applyImplicitComponents(DataComponentInput componentInput) {
        super.applyImplicitComponents(componentInput);
        CreativeMachineInfo info = componentInput.get(GTDataComponents.CREATIVE_MACHINE_INFO);
        if (info != null) {
            itemsPerCycle = info.outputPerCycle();
            ticksPerCycle = info.ticksPerCycle();
        }
    }

    @Override
    public void collectImplicitComponents(DataComponentMap.Builder components) {
        super.collectImplicitComponents(components);
        components.set(GTDataComponents.CREATIVE_MACHINE_INFO, new CreativeMachineInfo(itemsPerCycle, ticksPerCycle));
    }

    private class InfiniteCache extends ItemCache {

        public InfiniteCache() {
            super();
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            return stored;
        }

        @Override
        public void setStackInSlot(int index, ItemStack stack) {
            setCreativeChestItem(stack);
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            if (!stored.isEmpty() && ItemStack.isSameItemSameComponents(stored, stack)) return ItemStack.EMPTY;
            return stack;
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            if (!stored.isEmpty()) return stored.copyWithCount(itemsPerCycle);
            return ItemStack.EMPTY;
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return true;
        }

        @Override
        public int getSlotLimit(int slot) {
            return 1;
        }
    }
}
