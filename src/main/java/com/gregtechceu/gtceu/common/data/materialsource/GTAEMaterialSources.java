package com.gregtechceu.gtceu.common.data.materialsource;

import com.gregtechceu.gtceu.api.multiblock.autobuild.MaterialSources.Registration;
import com.gregtechceu.gtceu.integration.ae2.autobuild.MEWirelessMaterialSource;

import net.minecraft.network.chat.Component;

import com.tterrag.registrate.util.entry.RegistryEntry;

import static com.gregtechceu.gtceu.common.registry.GTRegistration.REGISTRATE;

public class GTAEMaterialSources {

    public static final RegistryEntry<Registration, Registration> WIRELESS = REGISTRATE
            .materialSource("ae2_wireless", 0, Component.translatable("gtpm.multiblock.configuration.me"),
                    MEWirelessMaterialSource::request)
            .register();

    public static void init() {}
}
