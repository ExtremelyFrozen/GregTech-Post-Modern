package com.gregtechceu.gtceu.api.machine.feature.multiblock;

import com.gregtechceu.gtceu.api.gui.factory.MachineUIHolder;
import com.gregtechceu.gtceu.api.gui.fancy.IFancyUIProvider;
import com.gregtechceu.gtceu.api.machine.feature.FancyActionMachine;
import com.gregtechceu.gtceu.api.machine.feature.IMachineFeature;

import net.minecraft.world.entity.player.Player;

/**
 * Creates an opening-scoped LDLib2 Fancy home page for a machine used as a contextual multiblock part.
 *
 * <p>
 * This contract stays separate from the legacy part UI provider interfaces because their page-list return types are
 * not compatible with {@link IFancyUIProvider}. It also opts each implementation into the holder-validated
 * Fancy action handlers required by working and cover controls.
 */
public interface FancyPartUIProvider extends IMachineFeature, FancyActionMachine {

    /**
     * Creates the part page for one menu opening.
     *
     * @param player player opening the controller UI
     * @param holder holder that must resolve this part rather than the surrounding controller
     * @return a new stable page provider owned by this menu opening
     */
    IFancyUIProvider createFancyPage(Player player, MachineUIHolder holder);
}
