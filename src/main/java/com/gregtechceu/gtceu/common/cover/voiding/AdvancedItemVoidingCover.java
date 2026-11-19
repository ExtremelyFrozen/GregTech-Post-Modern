package com.gregtechceu.gtceu.common.cover.voiding;

import com.gregtechceu.gtceu.api.blockentity.ConfigCopyHelper;
import com.gregtechceu.gtceu.api.capability.ICoverable;
import com.gregtechceu.gtceu.api.cover.CoverDefinition;
import com.gregtechceu.gtceu.api.cover.filter.ItemFilter;
import com.gregtechceu.gtceu.api.cover.filter.SimpleItemFilter;
import com.gregtechceu.gtceu.api.gui.element.GTEnumSelectorElement;
import com.gregtechceu.gtceu.api.gui.element.GTIntInputElement;
import com.gregtechceu.gtceu.api.gui.factory.UICoverHolder;
import com.gregtechceu.gtceu.api.sync_system.SyncFieldData;
import com.gregtechceu.gtceu.api.sync_system.annotations.SaveField;
import com.gregtechceu.gtceu.api.sync_system.annotations.SyncToClient;
import com.gregtechceu.gtceu.api.sync_system.rpc.RPCMethod;
import com.gregtechceu.gtceu.common.cover.data.VoidingMode;

import com.lowdragmc.lowdraglib2.gui.ui.UIElement;

import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;

import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

public class AdvancedItemVoidingCover extends ItemVoidingCover {

    @SaveField
    @SyncToClient
    @Getter
    private VoidingMode voidingMode = VoidingMode.VOID_ANY;

    @SaveField
    @Getter
    protected int globalVoidingLimit = 1;

    private @Nullable GTIntInputElement stackSizeInput;

    public AdvancedItemVoidingCover(CoverDefinition definition, ICoverable coverHolder, Direction attachedSide) {
        super(definition, coverHolder, attachedSide);
    }

    //////////////////////////////////////////////
    // *********** COVER LOGIC ***********//

    /// ///////////////////////////////////////////

    @Override
    protected void doVoidItems() {
        IItemHandler handler = getOwnItemHandler();
        if (handler == null) {
            return;
        }

        switch (voidingMode) {
            case VOID_ANY -> voidAny(handler);
            case VOID_OVERFLOW -> voidOverflow(handler);
        }
    }

    private void voidOverflow(IItemHandler handler) {
        Map<ItemStack, TypeItemInfo> sourceItemAmounts = countInventoryItemsByType(handler);

        for (TypeItemInfo itemInfo : sourceItemAmounts.values()) {
            int itemToVoidAmount = itemInfo.totalCount - getFilteredItemAmount(itemInfo.itemStack);

            if (itemToVoidAmount <= 0) {
                continue;
            }

            for (int slot = 0; slot < handler.getSlots(); slot++) {
                ItemStack is = handler.getStackInSlot(slot);
                if (!is.isEmpty() && ItemStack.isSameItemSameComponents(is, itemInfo.itemStack)) {
                    ItemStack extracted = handler.extractItem(slot, itemToVoidAmount, false);

                    if (!extracted.isEmpty()) {
                        itemToVoidAmount -= extracted.getCount();
                    }
                }
                if (itemToVoidAmount == 0) {
                    break;
                }
            }
        }
    }

    private int getFilteredItemAmount(ItemStack itemStack) {
        if (!filterHandler.isFilterPresent())
            return globalVoidingLimit;

        ItemFilter filter = filterHandler.getFilter();
        return filter.isBlackList() ? globalVoidingLimit : filter.testItemCount(itemStack);
    }

    public void setVoidingMode(VoidingMode voidingMode) {
        this.voidingMode = voidingMode;

        configureStackSizeInput();

        if (!coverHolder.isRemote()) {
            syncDataHolder.markClientSyncFieldDirty("voidingMode");
            configureFilter();
        }
    }

    //////////////////////////////////////
    // *********** GUI ***********//

    /// ///////////////////////////////////

    @Override
    protected @NotNull String getUITitle() {
        return "cover.item.voiding.advanced.title";
    }

    @Override
    protected void buildAdditionalUI(UIElement root, Player player, UICoverHolder holder) {
        root.addChild(GTEnumSelectorElement.selectable(146, 20, 20, 20, VoidingMode.values(), this::getVoidingMode,
                mode -> setVoidingMode(player, holder, mode)));

        this.stackSizeInput = new GTIntInputElement(64, 20, 80, 20,
                () -> globalVoidingLimit, value -> setGlobalVoidingLimit(player, holder, value));
        configureStackSizeInput();
        root.addChild(this.stackSizeInput);
    }

    @Override
    protected void configureFilter() {
        if (filterHandler.getFilter() instanceof SimpleItemFilter filter) {
            filter.setMaxStackSize(this.voidingMode.maxStackSize);
        }

        configureStackSizeInput();
    }

    private void configureStackSizeInput() {
        if (this.stackSizeInput == null)
            return;

        this.stackSizeInput.setVisible(shouldShowStackSize());
        this.stackSizeInput.setMin(1);
        this.stackSizeInput.setMax(this.voidingMode.maxStackSize);
    }

    private boolean shouldShowStackSize() {
        if (this.voidingMode == VoidingMode.VOID_ANY)
            return false;

        if (!this.filterHandler.isFilterPresent())
            return true;

        return this.filterHandler.getFilter().isBlackList();
    }

    private void setVoidingMode(Player player, UICoverHolder holder, VoidingMode mode) {
        setVoidingMode(mode);
        sendConfigAction(player, holder);
    }

    private void setGlobalVoidingLimit(Player player, UICoverHolder holder, int value) {
        setGlobalVoidingLimit(value);
        sendConfigAction(player, holder);
    }

    public void setGlobalVoidingLimit(int value) {
        this.globalVoidingLimit = Math.max(value, 1);
        configureStackSizeInput();
    }

    @RPCMethod
    public void configureAdvancedItemVoiding(VoidingMode voidingMode, int voidingLimit) {
        setVoidingMode(voidingMode);
        setGlobalVoidingLimit(voidingLimit);
    }

    private void sendConfigAction(Player player, UICoverHolder holder) {
        if (player.level().isClientSide()) {
            holder.getCover().rpcToServer("configureAdvancedItemVoiding", getVoidingMode(), getGlobalVoidingLimit());
        }
    }

    @Override
    public SyncFieldData copyConfig(HolderLookup.Provider registries) {
        return ConfigCopyHelper.withFields(super.copyConfig(registries), fields -> fields
                .put(SyncFieldData.key("voidingMode"),
                        getVoidingMode().ordinal())
                .put(SyncFieldData.key("voidSize"),
                        getGlobalVoidingLimit()));
    }

    @Override
    public void pasteConfig(ServerPlayer player, HolderLookup.Provider registries, SyncFieldData config) {
        setVoidingMode(VoidingMode.values()[ConfigCopyHelper.getInt(config, "voidingMode")]);
        setGlobalVoidingLimit(ConfigCopyHelper.getInt(config, "voidSize"));
        super.pasteConfig(player, registries, config);
    }
}
