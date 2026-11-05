package com.gregtechceu.gtceu.common.machine.multiblock.part;

import com.gregtechceu.gtceu.api.blockentity.BlockEntityCreationInfo;
import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.gui.factory.MachineUIHolder;
import com.gregtechceu.gtceu.api.gui.fancy.IFancyUIProvider;
import com.gregtechceu.gtceu.api.machine.feature.multiblock.FancyPartUIProvider;

import net.minecraft.world.entity.player.Player;

/**
 * Ordinary fluid hatch whose existing LDLib2 page can also be opened from a multiblock controller context.
 *
 * <p>
 * The provider marker lives on this concrete type so specialized fluid hatches cannot accidentally inherit a page
 * contract that their distinct standalone interfaces do not implement.
 */
public final class StandardFluidHatchPartMachine extends FluidHatchPartMachine
                                                 implements FancyPartUIProvider {

    /** Creates an ordinary fluid hatch with the configured direction, capacity, and tank count. */
    public StandardFluidHatchPartMachine(BlockEntityCreationInfo info, int tier, IO io, int initialCapacity,
                                         int slots) {
        super(info, tier, io, initialCapacity, slots);
    }

    /** Ordinary definitions always use the generic fluid hatch page, including addon registrations. */
    @Override
    protected boolean supportsGenericPage() {
        return true;
    }

    /** Reuses the holder-validated ordinary hatch page for one controller UI opening. */
    @Override
    public IFancyUIProvider createFancyPage(Player player, MachineUIHolder holder) {
        return createPage(player, holder);
    }
}
