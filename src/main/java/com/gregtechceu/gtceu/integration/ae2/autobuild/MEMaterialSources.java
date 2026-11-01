package com.gregtechceu.gtceu.integration.ae2.autobuild;

import com.gregtechceu.gtceu.api.multiblock.autobuild.MaterialSources;

public final class MEMaterialSources {

    private MEMaterialSources() {}

    public static void init() {
        MaterialSources.registerMESource(MEWirelessMaterialSource::create);
    }
}
