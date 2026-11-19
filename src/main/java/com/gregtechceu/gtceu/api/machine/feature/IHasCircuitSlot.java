package com.gregtechceu.gtceu.api.machine.feature;

import com.gregtechceu.gtceu.api.machine.fancyconfigurator.CircuitFancyConfigurator;
import com.gregtechceu.gtceu.api.machine.trait.NotifiableItemStackHandler;
import com.gregtechceu.gtceu.api.sync_system.rpc.RPCMethod;

public interface IHasCircuitSlot {

    default boolean isCircuitSlotEnabled() {
        return true;
    }

    NotifiableItemStackHandler getCircuitInventory();

    @RPCMethod
    default void configureCircuit(int configuration) {
        CircuitFancyConfigurator.writeMachineCircuitConfiguration(getCircuitInventory(), configuration);
    }
}
