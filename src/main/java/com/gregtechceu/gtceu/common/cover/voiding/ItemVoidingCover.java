package com.gregtechceu.gtceu.common.cover.voiding;

import com.gregtechceu.gtceu.api.capability.IControllable;
import com.gregtechceu.gtceu.api.capability.ICoverable;
import com.gregtechceu.gtceu.api.cover.CoverDefinition;
import com.gregtechceu.gtceu.api.cover.filter.ItemFilter;
import com.gregtechceu.gtceu.api.gui.GuiTextures;
import com.gregtechceu.gtceu.api.gui.UITemplate;
import com.gregtechceu.gtceu.api.gui.element.GTLabelElement;
import com.gregtechceu.gtceu.api.gui.element.GTToggleButtonElement;
import com.gregtechceu.gtceu.api.gui.factory.CoverUIHelper;
import com.gregtechceu.gtceu.api.gui.factory.UICoverHolder;
import com.gregtechceu.gtceu.api.item.tool.GTToolType;
import com.gregtechceu.gtceu.api.item.tool.GridHighlightTexture;
import com.gregtechceu.gtceu.api.sync_system.rpc.RPCRegistry;
import com.gregtechceu.gtceu.common.cover.ConveyorCover;
import com.gregtechceu.gtceu.common.data.item.GTItemAbilities;
import com.gregtechceu.gtceu.utils.ExtendedUseOnContext;

import com.lowdragmc.lowdraglib2.gui.ui.UI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.data.Horizontal;
import com.lowdragmc.lowdraglib2.gui.ui.data.Vertical;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.IItemHandler;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Set;

public class ItemVoidingCover extends ConveyorCover implements IControllable {

    static {
        RPCRegistry.register(ItemVoidingCoverConfigActions.INSTANCE);
    }

    public ItemVoidingCover(CoverDefinition definition, ICoverable coverHolder, Direction attachedSide) {
        super(definition, coverHolder, attachedSide, 0);
        setWorkingEnabled(false);
    }

    @Override
    protected boolean isSubscriptionActive() {
        return isWorkingEnabled();
    }

    //////////////////////////////////////////////
    // *********** COVER LOGIC ***********//
    //////////////////////////////////////////////

    @Override
    protected void update() {
        if (coverHolder.getOffsetTimer() % 5 != 0)
            return;

        doVoidItems();
        subscriptionHandler.updateSubscription();
    }

    protected void doVoidItems() {
        IItemHandler handler = getOwnItemHandler();
        if (handler == null) {
            return;
        }
        voidAny(handler);
    }

    void voidAny(IItemHandler handler) {
        ItemFilter filter = filterHandler.getFilter();

        for (int slot = 0; slot < handler.getSlots(); slot++) {
            ItemStack sourceStack = handler.extractItem(slot, Integer.MAX_VALUE, true);
            if (sourceStack.isEmpty() || !filter.test(sourceStack)) {
                continue;
            }
            handler.extractItem(slot, Integer.MAX_VALUE, false);
        }
    }

    //////////////////////////////////////
    // *********** GUI ***********//
    //////////////////////////////////////

    @Override
    public boolean canCreateUI(Player player, UICoverHolder holder) {
        return holder.getCover() == this;
    }

    @Override
    public UI createUI(Player player, UICoverHolder holder) {
        UIElement root = new UIElement();
        UITemplate.setBounds(root, 0, 0, 176, 202);
        root.style(style -> style.backgroundTexture(GuiTextures.BACKGROUND));

        root.addChild(createLabel());
        root.addChild(new GTToggleButtonElement(10, 20, 20, 20, GuiTextures.BUTTON_POWER,
                this::isWorkingEnabled, enabled -> setWorkingEnabled(player, holder, enabled)));
        buildAdditionalUI(root, player, holder);
        root.addChild(filterHandler.createFilterSlotUI(148, 91));
        root.addChild(filterHandler.createFilterConfigUI(10, 50, 126, 60));
        root.addChild(UITemplate.bindPlayerInventory(player.getInventory(), GuiTextures.SLOT, 7, 120, true));
        return UI.of(root);
    }

    protected void buildAdditionalUI(UIElement root, Player player, UICoverHolder holder) {}

    private GTLabelElement createLabel() {
        GTLabelElement label = new GTLabelElement(10, 5, 156, 10, getUITitle(), true);
        label.textStyle(style -> style
                .textColor(0x404040)
                .textShadow(false)
                .textAlignHorizontal(Horizontal.LEFT)
                .textAlignVertical(Vertical.CENTER));
        return label;
    }

    private void setWorkingEnabled(Player player, UICoverHolder holder, boolean enabled) {
        setWorkingEnabled(enabled);
        if (player.level().isClientSide()) {
            CoverUIHelper.sendAction(holder, ItemVoidingCoverConfigActions.createSetWorkingEnabledAction(enabled));
        }
    }

    @NotNull
    protected String getUITitle() {
        return "cover.item.voiding.title";
    }

    @Override
    public InteractionResult onSoftMalletClick(ExtendedUseOnContext context) {
        if (!context.getItemInHand().canPerformAction(GTItemAbilities.MALLET_PAUSE)) {
            return InteractionResult.PASS;
        }
        if (!coverHolder.isRemote()) {
            setWorkingEnabled(!isWorkingEnabled);
            context.getPlayer().sendSystemMessage(Component.translatable(isWorkingEnabled() ?
                    "cover.voiding.message.enabled" : "cover.voiding.message.disabled"));
        }
        return InteractionResult.sidedSuccess(coverHolder.isRemote());
    }

    // TODO: Decide grid behavior
    @Override
    public boolean shouldRenderGrid(Player player, BlockPos pos, BlockState state, ItemStack held,
                                    Set<GTToolType> toolTypes) {
        return super.shouldRenderGrid(player, pos, state, held, toolTypes);
    }

    @Override
    public @Nullable GridHighlightTexture sideTips(Player player, BlockPos pos, BlockState state,
                                                   Set<GTToolType> toolTypes, ItemStack held, Direction side) {
        var superTips = super.sideTips(player, pos, state, toolTypes, held, side);
        if (superTips != null) return superTips;
        if (toolTypes.contains(GTToolType.SOFT_MALLET)) {
            return isWorkingEnabled() ? GridHighlightTexture.TOOL_START : GridHighlightTexture.TOOL_PAUSE;
        }
        return null;
    }
}
