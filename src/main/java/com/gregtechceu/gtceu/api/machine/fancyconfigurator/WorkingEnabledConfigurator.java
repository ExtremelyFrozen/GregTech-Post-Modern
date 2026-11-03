package com.gregtechceu.gtceu.api.machine.fancyconfigurator;

import com.gregtechceu.gtceu.api.capability.IControllable;
import com.gregtechceu.gtceu.api.gui.GuiTextures;
import com.gregtechceu.gtceu.api.gui.factory.MachineUIHolder;
import com.gregtechceu.gtceu.api.gui.fancy.FancyConfiguratorButton;
import com.gregtechceu.gtceu.api.machine.MetaMachine;

import net.minecraft.network.chat.Component;

import java.util.List;

/**
 * LDLib2 Fancy toggle for the common machine working-enabled state.
 *
 * <p>
 * The button sends a GTM machine action for server-side mutation and does not use LDLib2-owned business state
 * channels.
 */
public class WorkingEnabledConfigurator extends FancyConfiguratorButton.Toggle {

    /**
     * Creates a working-enabled toggle bound to the opened machine holder.
     */
    public WorkingEnabledConfigurator(IControllable controllable, MachineUIHolder holder) {
        this(Opening.capture(controllable, holder));
    }

    private WorkingEnabledConfigurator(Opening opening) {
        super(GuiTextures.BUTTON_POWER.getSubTexture(0, 0, 1, 0.5),
                GuiTextures.BUTTON_POWER.getSubTexture(0, 0.5, 1, 0.5),
                opening.controllable()::isWorkingEnabled,
                (event, pressed) -> {
                    if (opening.holder().getMachine() != opening.machine()) {
                        throw new IllegalStateException(
                                "Working-enabled configurator holder no longer resolves its opened machine.");
                    }
                    if (opening.machine().isRemote()) {
                        opening.machine().rpcToServer("setWorkingEnabled", pressed);
                        event.stopImmediatePropagation();
                        event.hasHandler = true;
                    }
                });
        setTooltipsSupplier(pressed -> List.of(Component.translatable(
                pressed ? "behaviour.soft_hammer.enabled" : "behaviour.soft_hammer.disabled")));
    }

    private record Opening(MetaMachine machine, IControllable controllable, MachineUIHolder holder) {

        private static Opening capture(IControllable controllable, MachineUIHolder holder) {
            var machine = holder.getMachine();
            if (machine != controllable) {
                throw new IllegalArgumentException(
                        "Working-enabled configurator holder must resolve the opened controllable machine.");
            }
            return new Opening(machine, controllable, holder);
        }
    }
}
