package com.gregtechceu.gtceu.common.machine.multiblock.part.monitor;

import com.gregtechceu.gtceu.api.blockentity.BlockEntityCreationInfo;
import com.gregtechceu.gtceu.api.capability.IMonitorComponent;
import com.gregtechceu.gtceu.api.gui.factory.MachineUIHolder;
import com.gregtechceu.gtceu.api.gui.fancy.FancyPreviewPage;
import com.gregtechceu.gtceu.api.gui.fancy.IFancyUIProvider;
import com.gregtechceu.gtceu.api.machine.feature.multiblock.FancyPartUIProvider;
import com.gregtechceu.gtceu.api.machine.multiblock.part.MultiblockPartMachine;

import net.minecraft.world.entity.player.Player;

public abstract class MonitorComponentPartMachine extends MultiblockPartMachine
                                                  implements IMonitorComponent, FancyPartUIProvider {

    public MonitorComponentPartMachine(BlockEntityCreationInfo info) {
        super(info);
    }

    /** Creates the holder-scoped default preview used by the central monitor. */
    @Override
    public IFancyUIProvider createFancyPage(Player player, MachineUIHolder holder) {
        return new FancyPreviewPage(this, player, holder, null);
    }
}
