package com.gregtechceu.gtceu.common.cover.voiding;

import com.gregtechceu.gtceu.api.blockentity.ConfigCopyHelper;
import com.gregtechceu.gtceu.api.capability.ICoverable;
import com.gregtechceu.gtceu.api.cover.CoverDefinition;
import com.gregtechceu.gtceu.api.cover.filter.FluidFilter;
import com.gregtechceu.gtceu.api.cover.filter.SimpleFluidFilter;
import com.gregtechceu.gtceu.api.gui.element.GTEnumSelectorElement;
import com.gregtechceu.gtceu.api.gui.element.GTIntInputElement;
import com.gregtechceu.gtceu.api.gui.factory.CoverUIHelper;
import com.gregtechceu.gtceu.api.gui.factory.UICoverHolder;
import com.gregtechceu.gtceu.api.sync_system.SyncFieldData;
import com.gregtechceu.gtceu.api.sync_system.annotations.SaveField;
import com.gregtechceu.gtceu.api.sync_system.annotations.SyncToClient;
import com.gregtechceu.gtceu.api.sync_system.rpc.RpcRegistry;
import com.gregtechceu.gtceu.api.transfer.fluid.IFluidHandlerModifiable;
import com.gregtechceu.gtceu.common.cover.data.BucketMode;
import com.gregtechceu.gtceu.common.cover.data.VoidingMode;
import com.gregtechceu.gtceu.utils.GTMath;

import com.lowdragmc.lowdraglib2.gui.ui.UIElement;

import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

import it.unimi.dsi.fastutil.objects.Object2LongMaps;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class AdvancedFluidVoidingCover extends FluidVoidingCover
                                       implements AdvancedFluidVoidingCoverConfigActionTarget {

    static {
        RpcRegistry.register(AdvancedFluidVoidingCoverConfigActions.INSTANCE);
    }

    @SaveField
    @SyncToClient
    @Getter
    private VoidingMode voidingMode = VoidingMode.VOID_ANY;

    @SaveField
    @SyncToClient
    @Getter
    protected int globalTransferSizeMillibuckets = 1;
    @SaveField
    @SyncToClient
    @Getter
    private BucketMode transferBucketMode = BucketMode.MILLI_BUCKET;

    private @Nullable GTIntInputElement stackSizeInput;
    private @Nullable GTEnumSelectorElement<BucketMode> stackSizeBucketModeInput;

    public AdvancedFluidVoidingCover(CoverDefinition definition, ICoverable coverHolder, Direction attachedSide) {
        super(definition, coverHolder, attachedSide);
    }

    //////////////////////////////////////////////
    // *********** COVER LOGIC ***********//
    //////////////////////////////////////////////

    @Override
    protected void doVoidFluids() {
        IFluidHandlerModifiable fluidHandler = getOwnFluidHandler();
        if (fluidHandler == null) {
            return;
        }

        switch (voidingMode) {
            case VOID_ANY -> voidAny(fluidHandler);
            case VOID_OVERFLOW -> voidOverflow(fluidHandler);
        }
    }

    private void voidOverflow(IFluidHandlerModifiable fluidHandler) {
        var fluidAmounts = enumerateDistinctFluids(fluidHandler, TransferDirection.EXTRACT);

        for (var entry : Object2LongMaps.fastIterable(fluidAmounts)) {
            var stack = entry.getKey();
            long presentAmount = entry.getLongValue();
            int targetAmount = getFilteredFluidAmount(stack);
            if (targetAmount <= 0L || targetAmount > presentAmount) continue;

            long diff = presentAmount - targetAmount;
            for (int op : GTMath.split(diff)) {
                var toDrain = stack.copyWithAmount(op);
                fluidHandler.drain(toDrain, IFluidHandler.FluidAction.EXECUTE);
            }
        }
    }

    private int getFilteredFluidAmount(FluidStack fluidStack) {
        if (!filterHandler.isFilterPresent())
            return globalTransferSizeMillibuckets;

        FluidFilter filter = filterHandler.getFilter();
        return filter.isBlackList() ? globalTransferSizeMillibuckets : filter.testFluidAmount(fluidStack);
    }

    @Override
    public void setVoidingMode(VoidingMode voidingMode) {
        this.voidingMode = voidingMode;
        syncDataHolder.markClientSyncFieldDirty("voidingMode");
        configureStackSizeInput();

        if (!coverHolder.isRemote()) {
            configureFilter();
        }
    }

    @Override
    public void setTransferBucketMode(BucketMode transferBucketMode) {
        this.transferBucketMode = transferBucketMode;
        syncDataHolder.markClientSyncFieldDirty("transferBucketMode");

        if (stackSizeInput == null) return;
        stackSizeInput.setValue(getCurrentBucketModeTransferSize());
    }

    //////////////////////////////////////
    // *********** GUI ***********//
    //////////////////////////////////////

    @Override
    protected @NotNull String getUITitle() {
        return "cover.fluid.voiding.advanced.title";
    }

    @Override
    protected void buildAdditionalUI(UIElement root, Player player, UICoverHolder holder) {
        root.addChild(GTEnumSelectorElement.selectable(146, 20, 20, 20, VoidingMode.values(), this::getVoidingMode,
                mode -> setVoidingMode(player, holder, mode)));

        this.stackSizeInput = new GTIntInputElement(35, 20, 84, 20,
                this::getCurrentBucketModeTransferSize,
                value -> setCurrentBucketModeTransferSize(player, holder, value));
        this.stackSizeInput.setMin(1);
        this.stackSizeInput.setMax(Integer.MAX_VALUE);
        root.addChild(this.stackSizeInput);

        this.stackSizeBucketModeInput = GTEnumSelectorElement.selectable(121, 20, 20, 20, BucketMode.values(),
                this::getTransferBucketMode, mode -> setTransferBucketMode(player, holder, mode));
        root.addChild(this.stackSizeBucketModeInput);
        configureStackSizeInput();
    }

    private int getCurrentBucketModeTransferSize() {
        return this.globalTransferSizeMillibuckets / this.transferBucketMode.multiplier;
    }

    private void setCurrentBucketModeTransferSize(int transferSize) {
        this.globalTransferSizeMillibuckets = Math.max(transferSize * this.transferBucketMode.multiplier, 0);
        syncDataHolder.markClientSyncFieldDirty("globalTransferSizeMillibuckets");
    }

    @Override
    public void setGlobalTransferSizeMillibuckets(int transferSize) {
        this.globalTransferSizeMillibuckets = Math.max(transferSize, 1);
        syncDataHolder.markClientSyncFieldDirty("globalTransferSizeMillibuckets");
        configureStackSizeInput();
    }

    @Override
    protected void configureFilter() {
        if (filterHandler.getFilter() instanceof SimpleFluidFilter filter) {
            filter.setMaxStackSize(voidingMode == VoidingMode.VOID_ANY ? 1 : Integer.MAX_VALUE);
        }

        configureStackSizeInput();
    }

    private void configureStackSizeInput() {
        if (this.stackSizeInput == null || stackSizeBucketModeInput == null)
            return;

        this.stackSizeInput.setVisible(shouldShowStackSize());
        this.stackSizeBucketModeInput.setVisible(shouldShowStackSize());
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

    private void setTransferBucketMode(Player player, UICoverHolder holder, BucketMode mode) {
        setTransferBucketMode(mode);
        sendConfigAction(player, holder);
    }

    private void setCurrentBucketModeTransferSize(Player player, UICoverHolder holder, int transferSize) {
        setCurrentBucketModeTransferSize(transferSize);
        sendConfigAction(player, holder);
    }

    private void sendConfigAction(Player player, UICoverHolder holder) {
        if (player.level().isClientSide()) {
            CoverUIHelper.sendAction(holder, AdvancedFluidVoidingCoverConfigActions.createSetConfigAction(
                    getVoidingMode(), getGlobalTransferSizeMillibuckets(), getTransferBucketMode()));
        }
    }

    @Override
    public DataComponentMap copyConfig(HolderLookup.Provider registries) {
        return ConfigCopyHelper.withFields(super.copyConfig(registries), fields -> fields
                .put(SyncFieldData.key("voidingMode"),
                        ConfigCopyHelper.intValue(getVoidingMode().ordinal()))
                .put(SyncFieldData.key("voidSize"),
                        ConfigCopyHelper.intValue(getGlobalTransferSizeMillibuckets()))
                .put(SyncFieldData.key("voidBucketMode"),
                        ConfigCopyHelper.intValue(getTransferBucketMode().ordinal())));
    }

    @Override
    public void pasteConfig(ServerPlayer player, HolderLookup.Provider registries, DataComponentMap config) {
        setVoidingMode(VoidingMode.values()[ConfigCopyHelper.getInt(config, "voidingMode")]);
        setTransferBucketMode(BucketMode.values()[ConfigCopyHelper.getInt(config, "voidBucketMode")]);
        setGlobalTransferSizeMillibuckets(ConfigCopyHelper.getInt(config, "voidSize"));
        super.pasteConfig(player, registries, config);
    }
}
