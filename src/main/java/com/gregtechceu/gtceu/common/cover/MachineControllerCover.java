package com.gregtechceu.gtceu.common.cover;

import com.gregtechceu.gtceu.api.blockentity.ConfigCopyHelper;
import com.gregtechceu.gtceu.api.capability.GTCapabilityHelper;
import com.gregtechceu.gtceu.api.capability.IControllable;
import com.gregtechceu.gtceu.api.capability.ICoverable;
import com.gregtechceu.gtceu.api.cover.CoverBehavior;
import com.gregtechceu.gtceu.api.cover.CoverDefinition;
import com.gregtechceu.gtceu.api.gui.GuiTextures;
import com.gregtechceu.gtceu.api.gui.UITemplate;
import com.gregtechceu.gtceu.api.gui.element.GTButtonElement;
import com.gregtechceu.gtceu.api.gui.element.GTIntInputElement;
import com.gregtechceu.gtceu.api.gui.element.GTItemSlotElement;
import com.gregtechceu.gtceu.api.gui.element.GTLabelElement;
import com.gregtechceu.gtceu.api.gui.element.GTToggleButtonElement;
import com.gregtechceu.gtceu.api.gui.factory.CoverUIProvider;
import com.gregtechceu.gtceu.api.gui.factory.UICoverHolder;
import com.gregtechceu.gtceu.api.machine.MachineCoverContainer;
import com.gregtechceu.gtceu.api.sync_system.SyncFieldData;
import com.gregtechceu.gtceu.api.sync_system.annotations.SaveField;
import com.gregtechceu.gtceu.api.sync_system.annotations.SyncToClient;
import com.gregtechceu.gtceu.api.sync_system.rpc.RPCMethod;
import com.gregtechceu.gtceu.api.transfer.item.CustomItemStackHandler;
import com.gregtechceu.gtceu.common.cover.data.ControllerMode;

import com.lowdragmc.lowdraglib2.gui.ui.UI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.data.Horizontal;
import com.lowdragmc.lowdraglib2.gui.ui.data.Vertical;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

import lombok.Getter;
import lombok.experimental.Accessors;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class MachineControllerCover extends CoverBehavior
                                    implements CoverUIProvider {

    private @Nullable CustomItemStackHandler sideCoverSlot;
    private @Nullable GTButtonElement modeButton;

    @SaveField
    @SyncToClient
    @Getter
    private boolean isInverted = false;

    @SaveField
    @SyncToClient
    @Getter
    private int minRedstoneStrength = 1;

    @SaveField
    @SyncToClient
    @Getter
    @Nullable
    private ControllerMode controllerMode = ControllerMode.MACHINE;

    @Getter
    @Accessors(fluent = true)
    @SaveField
    @SyncToClient
    private boolean preventPowerFail = false;

    public MachineControllerCover(CoverDefinition definition, ICoverable coverHolder, Direction attachedSide) {
        super(definition, coverHolder, attachedSide);
    }

    @Override
    public boolean canAttach() {
        return super.canAttach() && !getAllowedModes().isEmpty();
    }

    @Override
    public void onAttached(ItemStack itemStack, @Nullable ServerPlayer player) {
        super.onAttached(itemStack, player);

        var allowedModes = getAllowedModes();
        setControllerMode(allowedModes.isEmpty() ? null : allowedModes.get(0));
    }

    @Override
    public void onRemoved() {
        super.onRemoved();

        resetCurrentControllable();
    }

    @Override
    public boolean canConnectRedstone() {
        return true;
    }

    @Override
    public void onNeighborChanged(Block block, BlockPos fromPos, boolean isMoving) {
        super.onNeighborChanged(block, fromPos, isMoving);

        updateInput();
    }

    public void setControllerMode(@Nullable ControllerMode controllerMode) {
        if (this.controllerMode == controllerMode) {
            updateUI();
            return;
        }
        resetCurrentControllable();

        this.controllerMode = controllerMode;
        syncDataHolder.markClientSyncFieldDirty("controllerMode");

        updateAll();
    }

    public void setMinRedstoneStrength(int minRedstoneStrength) {
        int clamped = Mth.clamp(minRedstoneStrength, 1, 15);
        if (this.minRedstoneStrength == clamped) {
            return;
        }
        this.minRedstoneStrength = clamped;
        syncDataHolder.markClientSyncFieldDirty("minRedstoneStrength");
        updateAll();
    }

    public void setInverted(boolean inverted) {
        if (isInverted == inverted) {
            return;
        }
        isInverted = inverted;
        syncDataHolder.markClientSyncFieldDirty("isInverted");
        updateAll();
    }

    public void setPreventPowerFail(boolean preventPowerFail) {
        if (this.preventPowerFail == preventPowerFail) {
            return;
        }
        this.preventPowerFail = preventPowerFail;
        syncDataHolder.markClientSyncFieldDirty("preventPowerFail");
    }

    private void updateAll() {
        updateInput();
        updateUI();
    }

    // Controller logic

    @Nullable
    private IControllable getControllable(@Nullable Direction side) {
        if (side == null) {
            return GTCapabilityHelper.getControllable(coverHolder.getLevel(), coverHolder.getBlockPos(), null);
        }

        if (coverHolder.getCoverAtSide(side) instanceof IControllable cover) {
            return cover;
        } else {
            return null;
        }
    }

    private void updateInput() {
        if (controllerMode == null)
            return;

        IControllable controllable = getControllable(controllerMode.side);
        if (controllable != null) {
            controllable.setWorkingEnabled(shouldAllowWorking() && doOthersAllowWorking());
        }
    }

    private void resetCurrentControllable() {
        if (controllerMode == null)
            return;

        IControllable controllable = getControllable(controllerMode.side);
        if (controllable != null) {
            controllable.setWorkingEnabled(doOthersAllowWorking());
        }
    }

    private boolean shouldAllowWorking() {
        boolean shouldAllowWorking = getInputSignal() < minRedstoneStrength;

        return isInverted != shouldAllowWorking;
    }

    private boolean doOthersAllowWorking() {
        return coverHolder.getCovers().stream()
                .filter(cover -> this.attachedSide != cover.attachedSide)
                .filter(cover -> cover instanceof MachineControllerCover)
                .filter(cover -> ((MachineControllerCover) cover).controllerMode == this.controllerMode)
                .allMatch(cover -> ((MachineControllerCover) cover).shouldAllowWorking());
    }

    public List<ControllerMode> getAllowedModes() {
        return Arrays.stream(ControllerMode.values())
                .filter(mode -> mode.side != this.attachedSide)
                .filter(mode -> getControllable(mode.side) != null)
                .collect(Collectors.toList());
    }

    private int getInputSignal() {
        Level level = coverHolder.getLevel();
        BlockPos sourcePos = coverHolder.getBlockPos().relative(attachedSide);

        return level.getSignal(sourcePos, attachedSide);
    }

    // GUI

    @Override
    public boolean canCreateUI(Player player, UICoverHolder holder) {
        return holder.getCover() == this;
    }

    @Override
    public UI createUI(Player player, UICoverHolder holder) {
        if (controllerMode != null && getControllable(controllerMode.side) == null) {
            setControllerMode(null);
        }
        UIElement root = new UIElement();
        UITemplate.setBounds(root, 0, 0, 176, 177);
        root.style(style -> style.backgroundTexture(GuiTextures.BACKGROUND));

        root.addChild(createLabel(10, 5, 156, 10, "cover.machine_controller.title"));
        root.addChild(new GTIntInputElement(10, 20, 131, 20,
                this::getMinRedstoneStrength, value -> setMinRedstoneStrength(player, holder, value))
                .setMin(1)
                .setMax(15));

        modeButton = createModeButton(player, holder);
        root.addChild(modeButton);

        root.addChild(new GTToggleButtonElement(
                146, 20, 20, 20,
                GuiTextures.INVERT_REDSTONE_BUTTON, this::isInverted,
                inverted -> setInverted(player, holder, inverted))
                .isMultiLang()
                .setTooltipText("cover.machine_controller.invert"));

        root.addChild(createLabel(10, 72, 132, 10, "cover.machine_controller.suspend_powerfail"));
        root.addChild(new GTToggleButtonElement(147, 68, 18, 18, GuiTextures.BUTTON_POWER,
                this::preventPowerFail, data -> setPreventPowerFail(player, holder, data)));

        sideCoverSlot = new CustomItemStackHandler(1);
        GTItemSlotElement sideCoverSlotElement = new GTItemSlotElement(sideCoverSlot, 0)
                .setBackgroundTexture(GuiTextures.SLOT)
                .setCanTakeItems(false)
                .setCanPutItems(false);
        root.addChild(UITemplate.setBounds(sideCoverSlotElement, 147, 46, 18, 18));

        root.addChild(UITemplate.bindPlayerInventory(player.getInventory(), GuiTextures.SLOT, 7, 95, true));
        updateUI();

        return UI.of(root);
    }

    private GTButtonElement createModeButton(Player player, UICoverHolder holder) {
        GTButtonElement button = new GTButtonElement(10, 45, 131, 20, GuiTextures.VANILLA_BUTTON,
                event -> setNextMode(player, holder)) {

            @Override
            public void screenTick() {
                updateModeButton();
                super.screenTick();
            }
        };
        button.noText();
        return button;
    }

    private GTLabelElement createLabel(int x, int y, int width, int height, String text) {
        GTLabelElement label = new GTLabelElement(x, y, width, height, text, true);
        label.textStyle(style -> style
                .textColor(0x404040)
                .textShadow(false)
                .textAlignHorizontal(Horizontal.LEFT)
                .textAlignVertical(Vertical.CENTER));
        return label;
    }

    private void selectNextMode() {
        var allowedModes = getAllowedModes();

        setControllerMode(allowedModes.stream()
                .dropWhile(mode -> this.controllerMode != null && mode != this.controllerMode)
                .skip(1)
                .findFirst()
                .orElse(allowedModes.isEmpty() ? null : allowedModes.get(0)));
    }

    private void updateUI() {
        updateModeButton();
        updateCoverSlot();
    }

    private void updateModeButton() {
        if (modeButton == null) {
            return;
        }

        modeButton.setButtonTexture(GuiTextures.group(
                GuiTextures.VANILLA_BUTTON,
                GuiTextures.text(controllerMode != null ? controllerMode.localeName : ControllerMode.nullLocaleName)
                        .setDropShadow(false)
                        .setColor(0x404040)
                        .setWidth(126)));
    }

    private void updateCoverSlot() {
        if (sideCoverSlot == null) {
            return;
        }

        if (controllerMode == null) {
            sideCoverSlot.setStackInSlot(0, ItemStack.EMPTY);
            return;
        }

        var side = controllerMode.side;
        if (side == null) {
            if (coverHolder instanceof MachineCoverContainer coverContainer) {
                sideCoverSlot.setStackInSlot(0, coverContainer.getMachine().getDefinition().asStack());
            } else {
                sideCoverSlot.setStackInSlot(0, ItemStack.EMPTY);
            }
            return;
        }

        var cover = coverHolder.getCoverAtSide(side);
        if (cover != null) {
            sideCoverSlot.setStackInSlot(0, cover.getAttachItem().copy());
        } else {
            sideCoverSlot.setStackInSlot(0, ItemStack.EMPTY);
        }
    }

    private void setNextMode(Player player, UICoverHolder holder) {
        selectNextMode();
        sendConfigAction(player, holder);
    }

    private void setMinRedstoneStrength(Player player, UICoverHolder holder, int minRedstoneStrength) {
        setMinRedstoneStrength(minRedstoneStrength);
        sendConfigAction(player, holder);
    }

    private void setInverted(Player player, UICoverHolder holder, boolean inverted) {
        setInverted(inverted);
        sendConfigAction(player, holder);
    }

    private void setPreventPowerFail(Player player, UICoverHolder holder, boolean preventPowerFail) {
        setPreventPowerFail(preventPowerFail);
        sendConfigAction(player, holder);
    }

    @RPCMethod
    public boolean configureMachineController(Optional<ControllerMode> requestedMode, int minRedstoneStrength,
                                              boolean inverted, boolean preventPowerFail) {
        ControllerMode mode = requestedMode.orElse(null);
        setControllerMode(mode);
        setMinRedstoneStrength(minRedstoneStrength);
        setInverted(inverted);
        setPreventPowerFail(preventPowerFail);
        return true;
    }

    private void sendConfigAction(Player player, UICoverHolder holder) {
        if (player.level().isClientSide()) {
            holder.getCover().rpcToServer("configureMachineController", Optional.ofNullable(getControllerMode()),
                    getMinRedstoneStrength(), isInverted(), preventPowerFail());
        }
    }

    @Override
    public SyncFieldData copyConfig(HolderLookup.Provider registries) {
        return ConfigCopyHelper.withFields(super.copyConfig(registries), fields -> fields
                .put(SyncFieldData.key("inverted"),
                        isInverted)
                .put(SyncFieldData.key("redstoneLvl"),
                        minRedstoneStrength)
                .put(SyncFieldData.key("preventPowerfail"),
                        preventPowerFail));
    }

    @Override
    public void pasteConfig(ServerPlayer player, HolderLookup.Provider registries, SyncFieldData config) {
        setInverted(ConfigCopyHelper.getBoolean(config, "inverted"));
        setMinRedstoneStrength(ConfigCopyHelper.getInt(config, "redstoneLvl"));
        setPreventPowerFail(ConfigCopyHelper.getBoolean(config, "preventPowerfail"));
        super.pasteConfig(player, registries, config);
    }
}
