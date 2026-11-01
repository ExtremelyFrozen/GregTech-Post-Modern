package com.gregtechceu.gtceu.common.machine.multiblock.part;

import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.blockentity.BlockEntityCreationInfo;
import com.gregtechceu.gtceu.api.gui.factory.MachineUIHolder;
import com.gregtechceu.gtceu.api.gui.factory.MachineUIProvider;
import com.gregtechceu.gtceu.api.gui.fancy.FancyMachineUI;
import com.gregtechceu.gtceu.api.gui.fancy.FancyPreviewPage;
import com.gregtechceu.gtceu.api.gui.fancy.FancyUIProvider;
import com.gregtechceu.gtceu.api.machine.feature.multiblock.IMaintenanceMachine;
import com.gregtechceu.gtceu.api.machine.feature.multiblock.LDLib2FancyPartUIProvider;
import com.gregtechceu.gtceu.api.machine.multiblock.part.TieredPartMachine;

import com.lowdragmc.lowdraglib2.gui.ui.UI;

import net.minecraft.world.entity.player.Player;

public class AutoMaintenanceHatchPartMachine extends TieredPartMachine
                                             implements IMaintenanceMachine, MachineUIProvider,
                                             LDLib2FancyPartUIProvider {

    public AutoMaintenanceHatchPartMachine(BlockEntityCreationInfo info) {
        super(info, GTValues.HV);
    }

    @Override
    public boolean canCreateUI(Player player, MachineUIHolder holder) {
        return holder.getMachine() == this;
    }

    @Override
    public UI createUI(Player player, MachineUIHolder holder) {
        FancyUIProvider page = createLDLib2FancyPage(player, holder);
        return UI.of(new FancyMachineUI(page, player.getInventory(), holder,
                page.getPageWidth(), page.getPageHeight()));
    }

    @Override
    public FancyUIProvider createLDLib2FancyPage(Player player, MachineUIHolder holder) {
        return new FancyPreviewPage(this, player, holder, null,
                this::attachLDLib2MaintenanceTooltips);
    }

    @Override
    public void setTaped(boolean ignored) {}

    @Override
    public boolean isTaped() {
        return false;
    }

    @Override
    public boolean isFullAuto() {
        return true;
    }

    @Override
    public byte startProblems() {
        return NO_PROBLEMS;
    }

    @Override
    public byte getMaintenanceProblems() {
        return NO_PROBLEMS;
    }

    @Override
    public void setMaintenanceProblems(byte problems) {}

    @Override
    public int getTimeActive() {
        return 0;
    }

    @Override
    public void setTimeActive(int time) {}
}
