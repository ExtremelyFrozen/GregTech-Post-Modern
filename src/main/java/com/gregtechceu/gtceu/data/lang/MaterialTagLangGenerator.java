package com.gregtechceu.gtceu.data.lang;

import com.gregtechceu.gtceu.GTCEu;
import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.data.chemical.material.ItemMaterialData;
import com.gregtechceu.gtceu.api.data.chemical.material.MarkerMaterials;
import com.gregtechceu.gtceu.api.data.chemical.material.Material;
import com.gregtechceu.gtceu.api.data.chemical.material.properties.FluidProperty;
import com.gregtechceu.gtceu.api.data.chemical.material.properties.PropertyKey;
import com.gregtechceu.gtceu.api.data.chemical.material.stack.MaterialEntry;
import com.gregtechceu.gtceu.api.data.tag.TagPrefix;
import com.gregtechceu.gtceu.api.fluids.GTFluid;
import com.gregtechceu.gtceu.api.fluids.store.FluidStorageKey;
import com.gregtechceu.gtceu.api.registry.GTRegistries;
import com.gregtechceu.gtceu.common.data.GTMaterials;
import com.gregtechceu.gtceu.data.recipe.CustomTags;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.material.Fluid;

import com.tterrag.registrate.providers.RegistrateLangProvider;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import it.unimi.dsi.fastutil.objects.ObjectSet;

/** Generates names for the same registered material entries used by the dynamic tag loader. */
public final class MaterialTagLangGenerator {

    private static final Object2ObjectMap<String, String> ITEM_PARENT_TAGS = new Object2ObjectOpenHashMap<>();

    static {
        ITEM_PARENT_TAGS.put("andesites", "Andesite");
        ITEM_PARENT_TAGS.put("basalts", "Basalt");
        ITEM_PARENT_TAGS.put("blackstones", "Blackstone");
        ITEM_PARENT_TAGS.put("bolts", "Bolts");
        ITEM_PARENT_TAGS.put("calcites", "Calcite");
        ITEM_PARENT_TAGS.put("chipped_gems", "Chipped Gems");
        ITEM_PARENT_TAGS.put("coral_blocks", "Coral Blocks");
        ITEM_PARENT_TAGS.put("coral_fans", "Coral Fans");
        ITEM_PARENT_TAGS.put("coral_plants", "Coral Plants");
        ITEM_PARENT_TAGS.put("corals", "Corals");
        ITEM_PARENT_TAGS.put("crushed_ores", "Crushed Ores");
        ITEM_PARENT_TAGS.put("deepslates", "Deepslate");
        ITEM_PARENT_TAGS.put("dense_plates", "Dense Plates");
        ITEM_PARENT_TAGS.put("diorites", "Diorite");
        ITEM_PARENT_TAGS.put("double_plates", "Double Plates");
        ITEM_PARENT_TAGS.put("doughs", "Doughs");
        ITEM_PARENT_TAGS.put("endstones", "End Stone");
        ITEM_PARENT_TAGS.put("exquisite_gems", "Exquisite Gems");
        ITEM_PARENT_TAGS.put("fine_wires", "Fine Wires");
        ITEM_PARENT_TAGS.put("flawed_gems", "Flawed Gems");
        ITEM_PARENT_TAGS.put("flawless_gems", "Flawless Gems");
        ITEM_PARENT_TAGS.put("foils", "Foils");
        ITEM_PARENT_TAGS.put("frames", "Frames");
        ITEM_PARENT_TAGS.put("gears", "Gears");
        ITEM_PARENT_TAGS.put("grains", "Grains");
        ITEM_PARENT_TAGS.put("granites", "Granite");
        ITEM_PARENT_TAGS.put("hot_ingots", "Hot Ingots");
        ITEM_PARENT_TAGS.put("impure_dusts", "Impure Dusts");
        ITEM_PARENT_TAGS.put("ingots", "Ingots");
        ITEM_PARENT_TAGS.put("lenses", "Lenses");
        ITEM_PARENT_TAGS.put("marbles", "Marble");
        ITEM_PARENT_TAGS.put("nuggets", "Nuggets");
        ITEM_PARENT_TAGS.put("ores", "Ores");
        ITEM_PARENT_TAGS.put("ores_in_ground/andesite", "Ores in Andesite");
        ITEM_PARENT_TAGS.put("ores_in_ground/basalt", "Ores in Basalt");
        ITEM_PARENT_TAGS.put("ores_in_ground/blackstone", "Ores in Blackstone");
        ITEM_PARENT_TAGS.put("ores_in_ground/deepslate", "Ores in Deepslate");
        ITEM_PARENT_TAGS.put("ores_in_ground/diorite", "Ores in Diorite");
        ITEM_PARENT_TAGS.put("ores_in_ground/endstone", "Ores in End Stone");
        ITEM_PARENT_TAGS.put("ores_in_ground/granite", "Ores in Granite");
        ITEM_PARENT_TAGS.put("ores_in_ground/gravel", "Ores in Gravel");
        ITEM_PARENT_TAGS.put("ores_in_ground/marble", "Ores in Marble");
        ITEM_PARENT_TAGS.put("ores_in_ground/netherrack", "Ores in Netherrack");
        ITEM_PARENT_TAGS.put("ores_in_ground/red_granite", "Ores in Red Granite");
        ITEM_PARENT_TAGS.put("ores_in_ground/red_sand", "Ores in Red Sand");
        ITEM_PARENT_TAGS.put("ores_in_ground/sand", "Ores in Sand");
        ITEM_PARENT_TAGS.put("ores_in_ground/stone", "Ores in Stone");
        ITEM_PARENT_TAGS.put("ores_in_ground/tuff", "Ores in Tuff");
        ITEM_PARENT_TAGS.put("pistons", "Pistons");
        ITEM_PARENT_TAGS.put("pure_dusts", "Purified Dusts");
        ITEM_PARENT_TAGS.put("purified_ores", "Purified Ores");
        ITEM_PARENT_TAGS.put("red_granites", "Red Granite");
        ITEM_PARENT_TAGS.put("refined_ores", "Refined Ores");
        ITEM_PARENT_TAGS.put("rings", "Rings");
        ITEM_PARENT_TAGS.put("rods", "Rods");
        ITEM_PARENT_TAGS.put("rotors", "Rotors");
        ITEM_PARENT_TAGS.put("rounds", "Rounds");
        ITEM_PARENT_TAGS.put("screws", "Screws");
        ITEM_PARENT_TAGS.put("siftables", "Siftable Materials");
        ITEM_PARENT_TAGS.put("silicon", "Silicon");
        ITEM_PARENT_TAGS.put("small_dusts", "Small Piles of Dust");
        ITEM_PARENT_TAGS.put("small_gears", "Small Gears");
        ITEM_PARENT_TAGS.put("small_springs", "Small Springs");
        ITEM_PARENT_TAGS.put("springs", "Springs");
        ITEM_PARENT_TAGS.put("storage_blocks", "Storage Blocks");
        ITEM_PARENT_TAGS.put("surface_rocks", "Surface Rocks");
        ITEM_PARENT_TAGS.put("tiny_dusts", "Tiny Piles of Dust");
        ITEM_PARENT_TAGS.put("treated_wood", "Treated Wood");
    }

    private MaterialTagLangGenerator() {}

    public static void generate(RegistrateLangProvider provider, String modId) {
        if (!modId.equals(GTCEu.MOD_ID)) return;

        Object2ObjectMap<String, String> translations = new Object2ObjectOpenHashMap<>();
        addParentTags(translations);

        ObjectSet<MaterialEntry> entries = new ObjectOpenHashSet<>(
                ItemMaterialData.MATERIAL_ENTRY_ITEM_MAP.keySet());
        for (TagPrefix prefix : GTRegistries.TAG_PREFIXES) {
            prefix.getIgnored().forEach((material, items) -> {
                if (!items.isEmpty()) entries.add(new MaterialEntry(prefix, material));
            });
        }
        for (Material material : MarkerMaterials.Color.VALUES) {
            entries.add(new MaterialEntry(TagPrefix.dye, material));
        }
        // These tags are registered outside the generated material-item table.
        entries.add(new MaterialEntry(TagPrefix.ingot, GTMaterials.Clay));
        entries.add(new MaterialEntry(TagPrefix.lens, MarkerMaterials.Color.White));

        for (MaterialEntry entry : entries) {
            Material material = entry.material();
            if (material.isNull() || !material.getModid().equals(modId)) continue;

            TagPrefix prefix = entry.tagPrefix();
            String materialName = MaterialLangGenerator.getEnglishName(material);
            for (TagKey<Item> tag : prefix.getItemTags(material)) {
                ResourceLocation id = tag.location();
                if (id.getNamespace().equals("c")) {
                    String value = id.getPath().startsWith("ores/") ? TagPrefix.ore.langValue : prefix.langValue;
                    translations.putIfAbsent("tag.item.c." + id.getPath().replace('/', '.'),
                            value.formatted(materialName));
                } else if (id.getNamespace().equals(modId)) {
                    translations.putIfAbsent("tag.item." + modId + "." + id.getPath().replace('/', '.'),
                            prefix.langValue.formatted(materialName));
                }
            }
        }

        addToolTags(translations);
        addComponentTags(translations);
        addWashedOreTags(translations);
        addFluidTags(translations);
        translations.forEach(provider::add);
    }

    private static void addParentTags(Object2ObjectMap<String, String> translations) {
        ITEM_PARENT_TAGS.forEach((path, value) -> translations.put("tag.item.c." + path.replace('/', '.'), value));
        translations.put("tag.item.c.corals.alive", "Living Corals");
        translations.put("tag.item.c.corals.dead", "Dead Corals");
        translations.put("tag.item.c.coral_blocks.alive", "Living Coral Blocks");
        translations.put("tag.item.c.coral_blocks.dead", "Dead Coral Blocks");
        translations.put("tag.item.c.coral_fans.alive", "Living Coral Fans");
        translations.put("tag.item.c.coral_fans.dead", "Dead Coral Fans");
        translations.put("tag.item.c.coral_plants.alive", "Living Coral Plants");
        translations.put("tag.item.c.coral_plants.dead", "Dead Coral Plants");
        translations.put("tag.item.c.doughs.wheat", "Wheat Doughs");
        translations.put("tag.item.c.grains.wheat", "Wheat Grains");
        translations.put("tag.item.c.silicon.plates", "Silicon Plates");
        translations.put("tag.item.c.rods.long", "Long Rods");
        translations.put("tag.item.c.dusts", "Dusts");
        translations.put("tag.item.c.gems", "Gems");
        translations.put("tag.item.c.plates", "Plates");
        translations.put("tag.item.c.raw_materials", "Raw Materials");
        translations.put("tag.item.c.dyes", "Dyes");
    }

    private static void addToolTags(Object2ObjectMap<String, String> translations) {
        translations.put("tag.item.c.tools.butchery_knife", "Butchery Knives");
        translations.put("tag.item.c.tools.buzzsaw", "Buzzsaws");
        translations.put("tag.item.c.tools.chainsaw", "Chainsaws");
        translations.put("tag.item.c.tools.crowbar", "Crowbars");
        translations.put("tag.item.c.tools.drill", "Drills");
        translations.put("tag.item.c.tools.file", "Files");
        translations.put("tag.item.c.tools.hammer", "Hammers");
        translations.put("tag.item.c.tools.igniter", "Igniters");
        translations.put("tag.item.c.tools.knife", "Knives");
        translations.put("tag.item.c.tools.mallet", "Soft Mallets");
        translations.put("tag.item.c.tools.mining_hammer", "Mining Hammers");
        translations.put("tag.item.c.tools.mortar", "Mortars");
        translations.put("tag.item.c.tools.plunger", "Plungers");
        translations.put("tag.item.c.tools.saw", "Saws");
        translations.put("tag.item.c.tools.screwdriver", "Screwdrivers");
        translations.put("tag.item.c.tools.scythe", "Scythes");
        translations.put("tag.item.c.tools.spade", "Spades");
        translations.put("tag.item.c.tools.wire_cutter", "Wire Cutters");
        translations.put("tag.item.gtpm.crafting_tools.hammer", "Crafting Hammers");
        translations.put("tag.item.gtpm.crafting_tools.mallet", "Crafting Mallets");
        translations.put("tag.item.gtpm.crafting_tools.mortar", "Crafting Mortars");
        translations.put("tag.item.gtpm.crafting_tools.screwdriver", "Crafting Screwdrivers");
        translations.put("tag.item.gtpm.crafting_tools.wire_cutter", "Crafting Wire Cutters");
        translations.put("tag.item.gtpm.crafting_tools.crowbar", "Crafting Crowbars");
        translations.put("tag.item.gtpm.crafting_tools.file", "Crafting Files");
        translations.put("tag.item.gtpm.crafting_tools.knife", "Crafting Knives");
        translations.put("tag.item.gtpm.crafting_tools.saw", "Crafting Saws");
        translations.put("tag.item.gtpm.crafting_tools.wrench", "Crafting Wrenches");
    }

    private static void addComponentTags(Object2ObjectMap<String, String> translations) {
        translations.put("tag.item.gtpm.batteries", "Batteries");
        translations.put("tag.item.gtpm.circuits", "Circuits");
        for (int tier = 0; tier < CustomTags.CIRCUITS_ARRAY.length; tier++) {
            String path = CustomTags.CIRCUITS_ARRAY[tier].location().getPath().replace('/', '.');
            translations.put("tag.item.gtpm." + path, GTValues.VN[tier] + " Circuits");
        }
        for (int tier = 0; tier < CustomTags.BATTERIES_ARRAY.length; tier++) {
            String path = CustomTags.BATTERIES_ARRAY[tier].location().getPath().replace('/', '.');
            translations.put("tag.item.gtpm." + path, GTValues.VN[tier] + " Batteries");
        }
        translations.put("tag.item.gtpm.capacitors", "Capacitors");
        translations.put("tag.item.gtpm.conveyor_modules", "Conveyor Modules");
        translations.put("tag.item.gtpm.diodes", "Diodes");
        translations.put("tag.item.gtpm.electric_motors", "Electric Motors");
        translations.put("tag.item.gtpm.electric_pistons", "Electric Pistons");
        translations.put("tag.item.gtpm.electric_pumps", "Electric Pumps");
        translations.put("tag.item.gtpm.emitters", "Emitters");
        translations.put("tag.item.gtpm.field_generators", "Field Generators");
        translations.put("tag.item.gtpm.fluid_regulators", "Fluid Regulators");
        translations.put("tag.item.gtpm.inductors", "Inductors");
        translations.put("tag.item.gtpm.ppe_armor", "Personal Protective Equipment");
        translations.put("tag.item.gtpm.resistors", "Resistors");
        translations.put("tag.item.gtpm.robot_arms", "Robot Arms");
        translations.put("tag.item.gtpm.rubber_logs", "Rubber Logs");
        translations.put("tag.item.gtpm.sensors", "Sensors");
        translations.put("tag.item.gtpm.skip_item_detector", "Items Ignored by Item Detectors");
        translations.put("tag.item.gtpm.step_boots", "Step-Assist Boots");
        translations.put("tag.item.gtpm.transistors", "Transistors");
    }

    private static void addWashedOreTags(Object2ObjectMap<String, String> translations) {
        ItemMaterialData.MATERIAL_ENTRY_ITEM_MAP.forEach((entry, items) -> {
            Material material = entry.material();
            if (entry.tagPrefix() != TagPrefix.crushed ||
                    !material.getModid().equals("gtpm") || !material.hasProperty(PropertyKey.ORE))
                return;

            Material washedIn = material.getProperty(PropertyKey.ORE).getWashedIn().first();
            if (washedIn.isNull()) return;
            translations.put("tag.item.gtpm.chemical_bath_washable", "Chemical Bath Washable Ores");
            translations.put("tag.item.gtpm.chemical_bath_washable." + washedIn.getName(),
                    "Ores Washable in " + MaterialLangGenerator.getEnglishName(washedIn));
        });
    }

    private static void addFluidTags(Object2ObjectMap<String, String> translations) {
        for (Material material : GTRegistries.MATERIALS) {
            if (!material.getModid().equals("gtpm") || !material.hasProperty(PropertyKey.FLUID)) continue;
            FluidProperty property = material.getProperty(PropertyKey.FLUID);
            for (FluidStorageKey key : FluidStorageKey.allKeys()) {
                Fluid fluid = property.get(key);
                if (fluid == null) continue;
                ResourceLocation id = BuiltInRegistries.FLUID.getKey(fluid);
                String name = MaterialLangGenerator.getEnglishName(material);
                if (fluid instanceof GTFluid) {
                    var description = (TranslatableContents) fluid.getFluidType().getDescription().getContents();
                    String template = switch (description.getKey()) {
                        case "gtpm.fluid.generic" -> "%s";
                        case "gtpm.fluid.liquid_generic" -> "Liquid %s";
                        case "gtpm.fluid.gas_generic" -> "%s Gas";
                        case "gtpm.fluid.gas_vapor" -> "%s Vapor";
                        case "gtpm.fluid.molten" -> "Molten %s";
                        case "gtpm.fluid.plasma" -> "%s Plasma";
                        default -> throw new IllegalArgumentException(
                                "Unknown material fluid name: " + description.getKey());
                    };
                    name = template.formatted(name);
                }
                translations.put("tag.fluid.c." + id.getPath().replace('/', '.'), name);
            }
        }

        translations.put("tag.fluid.c.liquid", "Liquid Fluids");
        translations.put("tag.fluid.c.molten", "Molten Fluids");
        translations.put("tag.fluid.c.plasmatic", "Plasma Fluids");
        translations.put("tag.fluid.gtpm.lighter_fluids", "Lighter Fluids");
        translations.put("tag.fluid.gtpm.hpca_coolants", "HPCA Coolants");
    }
}
