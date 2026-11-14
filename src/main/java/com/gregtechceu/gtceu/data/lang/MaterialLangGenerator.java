package com.gregtechceu.gtceu.data.lang;

import com.gregtechceu.gtceu.api.data.chemical.material.Material;
import com.gregtechceu.gtceu.api.registry.GTRegistries;
import com.gregtechceu.gtceu.common.data.GTMaterials;

import com.tterrag.registrate.providers.RegistrateLangProvider;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;

public class MaterialLangGenerator {

    private static final Object2ObjectMap<Material, String> NAME_OVERRIDES = new Object2ObjectOpenHashMap<>();

    static {
        NAME_OVERRIDES.put(GTMaterials.FullersEarth, "Fuller's Earth");
        NAME_OVERRIDES.put(GTMaterials.Cooperite, "Sheldonite");
        NAME_OVERRIDES.put(GTMaterials.YellowLimonite, "Yellow Limonite");
        NAME_OVERRIDES.put(GTMaterials.HSSG, "HSS-G");
        NAME_OVERRIDES.put(GTMaterials.HSSE, "HSS-E");
        NAME_OVERRIDES.put(GTMaterials.HSSS, "HSS-S");
        NAME_OVERRIDES.put(GTMaterials.RTMAlloy, "RTM Alloy");
        NAME_OVERRIDES.put(GTMaterials.HSLASteel, "HSLA Steel");
        NAME_OVERRIDES.put(GTMaterials.UUMatter, "UU-Matter");
        NAME_OVERRIDES.put(GTMaterials.PCBCoolant, "PCB Coolant");
        NAME_OVERRIDES.put(GTMaterials.TungstenSteel, "Tungstensteel");
        NAME_OVERRIDES.put(GTMaterials.Iron3Chloride, "Iron III Chloride");
        NAME_OVERRIDES.put(GTMaterials.Iron2Chloride, "Iron II Chloride");
        NAME_OVERRIDES.put(GTMaterials.HydroCrackedButadiene, "Hydro-Cracked Butadiene");
        NAME_OVERRIDES.put(GTMaterials.HydroCrackedButane, "Hydro-Cracked Butane");
        NAME_OVERRIDES.put(GTMaterials.HydroCrackedButene, "Hydro-Cracked Butene");
        NAME_OVERRIDES.put(GTMaterials.HydroCrackedEthane, "Hydro-Cracked Ethane");
        NAME_OVERRIDES.put(GTMaterials.HydroCrackedEthylene, "Hydro-Cracked Ethylene");
        NAME_OVERRIDES.put(GTMaterials.HydroCrackedPropane, "Hydro-Cracked Propane");
        NAME_OVERRIDES.put(GTMaterials.HydroCrackedPropene, "Hydro-Cracked Propene");
        NAME_OVERRIDES.put(GTMaterials.SteamCrackedButadiene, "Steam-Cracked Butadiene");
        NAME_OVERRIDES.put(GTMaterials.SteamCrackedButane, "Steam-Cracked Butane");
        NAME_OVERRIDES.put(GTMaterials.SteamCrackedButene, "Steam-Cracked Butene");
        NAME_OVERRIDES.put(GTMaterials.SteamCrackedEthane, "Steam-Cracked Ethane");
        NAME_OVERRIDES.put(GTMaterials.SteamCrackedEthylene, "Steam-Cracked Ethylene");
        NAME_OVERRIDES.put(GTMaterials.SteamCrackedPropane, "Steam-Cracked Propane");
        NAME_OVERRIDES.put(GTMaterials.SteamCrackedPropene, "Steam-Cracked Propene");
        NAME_OVERRIDES.put(GTMaterials.LightlyHydroCrackedGas, "Lightly Hydro-Cracked Gas");
        NAME_OVERRIDES.put(GTMaterials.LightlyHydroCrackedHeavyFuel, "Lightly Hydro-Cracked Heavy Fuel");
        NAME_OVERRIDES.put(GTMaterials.LightlyHydroCrackedLightFuel, "Lightly Hydro-Cracked Light Fuel");
        NAME_OVERRIDES.put(GTMaterials.LightlyHydroCrackedNaphtha, "Lightly Hydro-Cracked Naphtha");
        NAME_OVERRIDES.put(GTMaterials.LightlySteamCrackedGas, "Lightly Steam-Cracked Gas");
        NAME_OVERRIDES.put(GTMaterials.LightlySteamCrackedHeavyFuel, "Lightly Steam-Cracked Heavy Fuel");
        NAME_OVERRIDES.put(GTMaterials.LightlySteamCrackedLightFuel, "Lightly Steam-Cracked Light Fuel");
        NAME_OVERRIDES.put(GTMaterials.LightlySteamCrackedNaphtha, "Lightly Steam-Cracked Naphtha");
        NAME_OVERRIDES.put(GTMaterials.SeverelyHydroCrackedGas, "Severely Hydro-Cracked Gas");
        NAME_OVERRIDES.put(GTMaterials.SeverelyHydroCrackedHeavyFuel, "Severely Hydro-Cracked Heavy Fuel");
        NAME_OVERRIDES.put(GTMaterials.SeverelyHydroCrackedLightFuel, "Severely Hydro-Cracked Light Fuel");
        NAME_OVERRIDES.put(GTMaterials.SeverelyHydroCrackedNaphtha, "Severely Hydro-Cracked Naphtha");
        NAME_OVERRIDES.put(GTMaterials.SeverelySteamCrackedGas, "Severely Steam-Cracked Gas");
        NAME_OVERRIDES.put(GTMaterials.SeverelySteamCrackedHeavyFuel, "Severely Steam-Cracked Heavy Fuel");
        NAME_OVERRIDES.put(GTMaterials.SeverelySteamCrackedLightFuel, "Severely Steam-Cracked Light Fuel");
        NAME_OVERRIDES.put(GTMaterials.SeverelySteamCrackedNaphtha, "Severely Steam-Cracked Naphtha");
        NAME_OVERRIDES.put(GTMaterials.LPG, "LPG");
        NAME_OVERRIDES.put(GTMaterials.Zeron100, "Zeron-100");
        NAME_OVERRIDES.put(GTMaterials.IncoloyMA956, "Incoloy MA-956");
        NAME_OVERRIDES.put(GTMaterials.Stellite100, "Stellite-100");
        NAME_OVERRIDES.put(GTMaterials.HastelloyC276, "Hastelloy C-276");
    }

    /** Resolves the material name shared by the material and tag language generators. */
    public static String getEnglishName(Material material) {
        return NAME_OVERRIDES.getOrDefault(material, material.getDefaultTranslation());
    }

    public static void generate(RegistrateLangProvider provider, final String modId) {
        GTRegistries.MATERIALS.stream()
                .filter(mat -> mat.getModid().equals(modId))
                .forEach(material -> {
                    provider.add(material.getUnlocalizedName(), getEnglishName(material));
                });
        MaterialTagLangGenerator.generate(provider, modId);
    }
}
