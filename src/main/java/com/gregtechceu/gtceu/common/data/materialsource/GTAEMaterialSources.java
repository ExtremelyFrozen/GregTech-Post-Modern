package com.gregtechceu.gtceu.common.data.materialsource;

import com.gregtechceu.gtceu.api.multiblock.autobuild.MaterialSources.Registration;
import com.gregtechceu.gtceu.integration.ae2.autobuild.MEWirelessMaterialSource;

import com.tterrag.registrate.util.entry.RegistryEntry;

import static com.gregtechceu.gtceu.common.registry.GTRegistration.REGISTRATE;

public class GTAEMaterialSources {

    public static final RegistryEntry<Registration, Registration> WIRELESS = REGISTRATE
            .materialSource("ae2_wireless", 100, MEWirelessMaterialSource::request)
            .register();

    public static void init() {}
}
