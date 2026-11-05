package com.gregtechceu.gtceu.integration.ae2.machine;

import com.gregtechceu.gtceu.api.blockentity.BlockEntityCreationInfo;
import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.gui.GuiTextures;
import com.gregtechceu.gtceu.api.gui.UITemplate;
import com.gregtechceu.gtceu.api.gui.element.GTImageElement;
import com.gregtechceu.gtceu.api.gui.element.GTItemSlotElement;
import com.gregtechceu.gtceu.api.gui.element.GTLabelElement;
import com.gregtechceu.gtceu.api.gui.factory.MachineUIHolder;
import com.gregtechceu.gtceu.api.gui.fancy.ConfiguratorPanel;
import com.gregtechceu.gtceu.api.gui.fancy.FancyMachineUIWidget;
import com.gregtechceu.gtceu.api.gui.texture.IGuiTexture;
import com.gregtechceu.gtceu.api.machine.fancyconfigurator.WorkingEnabledConfigurator;
import com.gregtechceu.gtceu.api.machine.trait.NotifiableItemStackHandler;
import com.gregtechceu.gtceu.api.sync_system.annotations.SaveField;
import com.gregtechceu.gtceu.api.sync_system.annotations.SyncToClient;
import com.gregtechceu.gtceu.api.transfer.item.CustomItemStackHandler;
import com.gregtechceu.gtceu.integration.ae2.utils.KeyStorage;

import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.data.Horizontal;
import com.lowdragmc.lowdraglib2.gui.ui.data.ScrollDisplay;
import com.lowdragmc.lowdraglib2.gui.ui.data.ScrollerMode;
import com.lowdragmc.lowdraglib2.gui.ui.data.Vertical;
import com.lowdragmc.lowdraglib2.gui.ui.elements.VirtualItemHeightMode;
import com.lowdragmc.lowdraglib2.gui.ui.elements.VirtualScrollerView;
import com.lowdragmc.lowdraglib2.integration.xei.IngredientIO;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import appeng.api.config.Actionable;
import appeng.api.stacks.AEItemKey;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;

/**
 * The Output Bus that can directly send its contents to ME storage network.
 */
public class MEOutputBusPartMachine extends MEBusPartMachine {

    private static final int PAGE_WIDTH = 170;
    private static final int PAGE_HEIGHT = 74;

    @SaveField
    @SyncToClient
    private KeyStorage internalBuffer; // Do not use KeyCounter, use our simple implementation

    public MEOutputBusPartMachine(BlockEntityCreationInfo info) {
        super(info, IO.OUT);
    }

    /////////////////////////////////
    // ***** Machine LifeCycle ****//

    /////////////////////////////////

    @Override
    protected NotifiableItemStackHandler createInventory() {
        this.internalBuffer = new KeyStorage();
        return new InaccessibleInfiniteHandler();
    }

    @Override
    public void onMachineDestroyed() {
        super.onMachineDestroyed();
        var grid = getMainNode().getGrid();
        if (grid != null && !internalBuffer.isEmpty()) {
            for (var entry : internalBuffer) {
                grid.getStorageService().getInventory().insert(entry.getKey(), entry.getLongValue(),
                        Actionable.MODULATE, actionSource);
            }
        }
    }

    /////////////////////////////////
    // ********** Sync ME *********//

    /////////////////////////////////

    @Override
    protected boolean shouldSubscribe() {
        return super.shouldSubscribe() && !internalBuffer.storage.isEmpty();
    }

    @Override
    public void autoIO() {
        if (!this.shouldSyncME()) return;
        if (this.updateMEStatus()) {
            var grid = getMainNode().getGrid();
            if (grid != null && !internalBuffer.isEmpty()) {
                internalBuffer.insertInventory(grid.getStorageService().getInventory(), actionSource);
            }
            this.updateInventorySubscription();
        }
    }

    ///////////////////////////////
    // ********** GUI ***********//

    ///////////////////////////////

    /**
     * Creates one opening-scoped page after validating the exact item output bus holder and definition.
     */

    private UIElement createWaitingList() {
        var view = new VirtualScrollerView<KeyStorage.Entry>() {

            private List<KeyStorage.Entry> displayed = List.of();

            @Override
            public void screenTick() {
                var current = internalBuffer.getEntries();
                if (current != displayed) {
                    displayed = current;
                    setItems(current);
                }
                super.screenTick();
            }
        };
        UITemplate.setBounds(view, 5, 20, 158, 54);
        view.scrollerStyle(style -> style.mode(ScrollerMode.VERTICAL)
                .verticalScrollDisplay(ScrollDisplay.AUTO).horizontalScrollDisplay(ScrollDisplay.NEVER)
                .adaptiveWidth(false).adaptiveHeight(false).minScrollPixel(18).maxScrollPixel(18));
        view.virtualScrollerViewStyle(style -> style.itemHeightMode(VirtualItemHeightMode.FIXED)
                .estimatedItemHeight(18).overscanPixels(18));
        view.viewPort.layout(layout -> layout.paddingAll(0));
        view.viewPort.style(style -> style.backgroundTexture(IGuiTexture.EMPTY));
        view.setItemUIProvider(this::createWaitingRow);
        return view;
    }

    private UIElement createWaitingRow(KeyStorage.Entry entry) {
        AEItemKey itemKey = (AEItemKey) entry.key();

        UIElement row = UITemplate.setBounds(new UIElement(), 0, 0, 158, 18);
        GTImageElement amountBackground = new GTImageElement(18, 0, 140, 18,
                GuiTextures.NUMBER_BACKGROUND);
        amountBackground.setAllowHitTest(false);
        row.addChild(amountBackground);

        GTItemSlotElement item = new GTItemSlotElement()
                .setCanPutItems(false)
                .setCanTakeItems(false)
                .setIngredientIO(IngredientIO.NONE)
                .setBackgroundTexture(GuiTextures.SLOT)
                .setOnAddedTooltips((slot, tooltips) -> tooltips.add(
                        Component.literal(String.format("%,d", entry.amount()))));
        item.setItem(itemKey.toStack(1), false);
        row.addChild(UITemplate.setBounds(item, 0, 0, 18, 18));

        GTLabelElement amount = new GTLabelElement(21, 0, 134, 18,
                Component.literal(String.format("x%,d", entry.amount())))
                .setTextColor(-1)
                .setTextShadow(true)
                .setTextAlignHorizontal(Horizontal.LEFT)
                .setTextAlignVertical(Vertical.CENTER);
        amount.setAllowHitTest(false);
        row.addChild(amount);
        return row;
    }

    private Component networkStatus() {
        return Component.translatable(isOnline ?
                "gtpm.gui.me_network.online" : "gtpm.gui.me_network.offline");
    }

    private class InaccessibleInfiniteHandler extends NotifiableItemStackHandler {

        public InaccessibleInfiniteHandler() {
            super(1, IO.OUT, IO.NONE, ItemStackHandlerDelegate::new);
            internalBuffer.setOnContentsChanged(this::onContentsChanged);
        }

        @Override
        public @NotNull List<Object> getContents() {
            return Collections.emptyList();
        }

        @Override
        public double getTotalContentAmount() {
            return 0;
        }

        @Override
        public boolean isEmpty() {
            return true;
        }
    }

    @NoArgsConstructor
    private class ItemStackHandlerDelegate extends CustomItemStackHandler {

        // Necessary for InaccessibleInfiniteHandler
        public ItemStackHandlerDelegate(Integer integer) {
            super();
        }

        @Override
        public int getSlots() {
            return Short.MAX_VALUE;
        }

        @Override
        public int getSlotLimit(int slot) {
            return Integer.MAX_VALUE;
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            return ItemStack.EMPTY;
        }

        @Override
        public void setStackInSlot(int slot, ItemStack stack) {
            // NO-OP
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            var key = AEItemKey.of(stack);
            int count = stack.getCount();
            long oldValue = internalBuffer.storage.getOrDefault(key, 0);
            long changeValue = Math.min(Long.MAX_VALUE - oldValue, count);
            if (changeValue > 0) {
                if (!simulate) {
                    internalBuffer.put(key, oldValue + changeValue);
                }
                return stack.copyWithCount((int) (count - changeValue));
            } else {
                return ItemStack.EMPTY;
            }
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            return ItemStack.EMPTY;
        }
    }

    /**
     * One opening-scoped Fancy page shared by standalone and contextual part navigation.
     */

    @Override
    public UIElement createUIWidget(FancyMachineUIWidget shell) {
        UIElement root = UITemplate.setBounds(new UIElement(), 0, 0, PAGE_WIDTH, PAGE_HEIGHT);
        root.style(style -> style.backgroundTexture(GuiTextures.BACKGROUND_INVERSE));

        GTLabelElement status = new GTLabelElement(5, 0, 160, 10, networkStatus()) {

            @Override
            public void screenTick() {
                setValue(MEOutputBusPartMachine.this.networkStatus());
                super.screenTick();
            }
        };
        status.setId("me_network_status");
        root.addChild(status);
        GTLabelElement title = new GTLabelElement(5, 10, 160, 10,
                Component.translatable("gtpm.gui.waiting_list"));
        title.setId("me_output_waiting_list_label");
        root.addChild(title);
        root.addChild(createWaitingList());
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
        MachineUIHolder holder = configuratorPanel.getHolder();
        configuratorPanel.attachConfigurators(new WorkingEnabledConfigurator(
                MEOutputBusPartMachine.this, holder));
    }
}
