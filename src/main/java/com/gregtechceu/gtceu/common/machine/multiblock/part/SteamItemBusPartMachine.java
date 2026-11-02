package com.gregtechceu.gtceu.common.machine.multiblock.part;

import com.gregtechceu.gtceu.api.blockentity.BlockEntityCreationInfo;
import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.gui.GuiTextures;
import com.gregtechceu.gtceu.api.gui.UITemplate;
import com.gregtechceu.gtceu.api.gui.element.GTItemSlotElement;
import com.gregtechceu.gtceu.api.gui.element.GTLabelElement;
import com.gregtechceu.gtceu.api.gui.element.GTToggleButtonElement;
import com.gregtechceu.gtceu.api.gui.factory.MachineUIHolder;
import com.gregtechceu.gtceu.api.gui.factory.MachineUIProvider;
import com.gregtechceu.gtceu.api.gui.fancy.FancyUIProvider;
import com.gregtechceu.gtceu.api.machine.MachineDefinition;
import com.gregtechceu.gtceu.api.machine.feature.multiblock.FancyPartUIProvider;
import com.gregtechceu.gtceu.api.machine.feature.multiblock.SteamItemBus;
import com.gregtechceu.gtceu.common.data.GTMachines;
import com.gregtechceu.gtceu.config.ConfigHolder;

import com.lowdragmc.lowdraglib2.gui.ui.UI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.data.Horizontal;
import com.lowdragmc.lowdraglib2.gui.ui.data.Vertical;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;

public class SteamItemBusPartMachine extends ItemBusPartMachine
                                     implements MachineUIProvider, FancyPartUIProvider, SteamItemBus {

    private final String autoTooltipKey;

    public SteamItemBusPartMachine(BlockEntityCreationInfo info, IO io) {
        super(info, 1, io);
        autoTooltipKey = io == IO.IN ? "gtpm.gui.item_auto_input.tooltip" : "gtpm.gui.item_auto_output.tooltip";
    }

    @Override
    public boolean canCreateUI(Player player, MachineUIHolder holder) {
        return holder.getMachine() == this;
    }

    @Override
    public UI createUI(Player player, MachineUIHolder holder) {
        int rowSize = (int) Math.sqrt(getInventorySize());
        int xOffset = rowSize == 10 ? 9 : 0;
        int rootWidth = 176 + xOffset * 2;
        int rootHeight = 18 + 18 * rowSize + 105;
        boolean steelSteamMultiblocks = ConfigHolder.INSTANCE.machines.steelSteamMultiblocks;

        UIElement root = new UIElement();
        UITemplate.setBounds(root, 0, 0, rootWidth, rootHeight);
        UITemplate.setBackgroundTexture(root,
                GuiTextures.BACKGROUND_STEAM.get(steelSteamMultiblocks));
        root.addChild(createTitleLabel(rootWidth));
        root.addChild(createWorkingEnabledToggle(7 + xOffset, 18 + 18 * rowSize));
        root.addChild(UITemplate.bindPlayerInventory(player.getInventory(),
                GuiTextures.SLOT_STEAM.get(steelSteamMultiblocks),
                7 + xOffset, 18 + 18 * rowSize + 24, true));

        for (int y = 0; y < rowSize; y++) {
            for (int x = 0; x < rowSize; x++) {
                int index = y * rowSize + x;
                root.addChild(createInventorySlot(index,
                        (88 - rowSize * 9 + x * 18) + xOffset, 18 + y * 18 + 6, steelSteamMultiblocks));
            }
        }

        return UI.of(root);
    }

    /**
     * Allows the steam-specific standalone screen to reuse the ordinary Item Bus body in a controller context.
     */
    @Override
    protected boolean supportsGenericPage() {
        return true;
    }

    /**
     * Creates a holder-scoped Item Bus page for one surrounding steam multiblock UI opening.
     */
    @Override
    public FancyUIProvider createFancyPage(Player player, MachineUIHolder holder) {
        return createPage(player, holder);
    }

    private GTLabelElement createTitleLabel(int rootWidth) {
        GTLabelElement label = new GTLabelElement(10, 5, rootWidth - 20, 10,
                getBlockState().getBlock().getDescriptionId(), true);
        label.textStyle(style -> style
                .textColor(0x404040)
                .textShadow(false)
                .textAlignHorizontal(Horizontal.LEFT)
                .textAlignVertical(Vertical.CENTER));
        return label;
    }

    private GTItemSlotElement createInventorySlot(int index, int x, int y, boolean steelSteamMultiblocks) {
        GTItemSlotElement slot = new GTItemSlotElement(getInventory().storage, index)
                .setBackgroundTexture(GuiTextures.SLOT_STEAM.get(steelSteamMultiblocks))
                .setCanTakeItems(true)
                .setCanPutItems(io.support(IO.IN));
        UITemplate.setBounds(slot, x, y, 18, 18);
        return slot;
    }

    GTToggleButtonElement createWorkingEnabledToggle(int x, int y) {
        return new GTToggleButtonElement(x, y, 18, 18,
                GuiTextures.BUTTON_ITEM_OUTPUT, this::isWorkingEnabled, enabled -> {
                    setWorkingEnabled(enabled);
                    if (isRemote()) {
                        sendServerSyncChanges();
                    }
                })
                .setShouldUseBaseBackground()
                .setTooltipText(autoTooltipKey);
    }

    @Override
    public boolean swapIO() {
        BlockPos blockPos = getBlockPos();
        MachineDefinition newDefinition = null;
        if (io == IO.IN) {
            newDefinition = GTMachines.STEAM_EXPORT_BUS;
        } else if (io == IO.OUT) {
            newDefinition = GTMachines.STEAM_IMPORT_BUS;
        }

        if (newDefinition == null) return false;
        BlockState newBlockState = newDefinition.getBlock().defaultBlockState();

        getLevel().setBlockAndUpdate(blockPos, newBlockState);

        if (getLevel().getBlockEntity(blockPos) instanceof SteamItemBusPartMachine newMachine) {
            // We don't set the circuit or distinct busses, since
            // that doesn't make sense on an output bus.
            // Furthermore, existing inventory items
            // and conveyors will drop to the floor on block override.
            newMachine.setFrontFacing(this.getFrontFacing());
            newMachine.setUpwardsFacing(this.getUpwardsFacing());
        }
        return true;
    }
}
