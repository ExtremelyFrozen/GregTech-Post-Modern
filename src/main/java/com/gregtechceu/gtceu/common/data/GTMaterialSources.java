package com.gregtechceu.gtceu.common.data;

import com.gregtechceu.gtceu.GTCEu;
import com.gregtechceu.gtceu.api.multiblock.autobuild.MaterialSources.Registration;
import com.gregtechceu.gtceu.api.multiblock.autobuild.PlayerInventoryMaterialSource;
import com.gregtechceu.gtceu.common.data.materialsource.GTAEMaterialSources;

import com.tterrag.registrate.util.entry.RegistryEntry;

import static com.gregtechceu.gtceu.common.registry.GTRegistration.REGISTRATE;

public class GTMaterialSources {

    public static final RegistryEntry<Registration, Registration> PLAYER_INVENTORY = REGISTRATE
            .materialSource("player_inventory", 100, null, PlayerInventoryMaterialSource::request).register();

    public static void init() {
        if (GTCEu.MODS.AE2.isLoaded()) {
            GTAEMaterialSources.init();
        }
    }
}
