package com.gregtechceu.gtceu.data.lang;

import com.gregtechceu.gtceu.api.data.worldgen.GTOreDefinition;
import com.gregtechceu.gtceu.api.data.worldgen.bedrockfluid.BedrockFluidDefinition;
import com.gregtechceu.gtceu.common.data.GTBedrockFluids;
import com.gregtechceu.gtceu.common.data.GTOreVeins;

import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;

import com.tterrag.registrate.providers.RegistrateLangProvider;

public class IntegrationLang {

    public static void init(RegistrateLangProvider provider) {
        initRecipeViewerLang(provider);
        initWailaLikeLang(provider);
        initMinimapLang(provider);
        initOwnershipLang(provider);
    }

    /**
     * JEI, EMI
     */
    private static void initRecipeViewerLang(RegistrateLangProvider provider) {
        provider.add("gtpm.jei.multiblock_info", "Multiblock Info");
        provider.add("gtpm.multiblock.preview.material.required", "Required: %s");
        provider.add("gtpm.multiblock.preview.material.structure", "  %s: %s");
        provider.add("gtpm.multiblock.preview.material.missing", "Missing: %s");
        provider.add("gtpm.multiblock.preview.tooltip.previous_materials", "Previous materials");
        provider.add("gtpm.multiblock.preview.tooltip.next_materials", "Next materials");
        provider.add("gtpm.multiblock.preview.cell.position", "Relative position: %s, %s, %s");
        provider.add("gtpm.multiblock.preview.cell.contribution", "%s -> %s [%s], cell %s/%s/%s/%s/%s");
        provider.add("gtpm.multiblock.preview.cell.direction", "Required direction: %s");
        provider.add("gtpm.multiblock.preview.cell.conflict", "Conflicting structure targets");
        provider.add("gtpm.multiblock.preview.cell.demolition", "Demolition target");
        provider.add("gtpm.multiblock.preview.cell.action.controller", "controller");
        provider.add("gtpm.multiblock.preview.cell.action.keep", "keep");
        provider.add("gtpm.multiblock.preview.cell.action.update_direction", "update direction");
        provider.add("gtpm.multiblock.preview.cell.action.place", "place");
        provider.add("gtpm.multiblock.preview.cell.action.clear_for_air", "clear for air");
        provider.add("gtpm.multiblock.preview.cell.action.demolish_candidate", "demolish");
        provider.add("gtpm.multiblock.preview.cell.action.ignore_any", "ignore");
        provider.add("gtpm.multiblock.preview.button.structure_all", "S:*");
        provider.add("gtpm.multiblock.preview.button.structure", "S");
        provider.add("gtpm.multiblock.preview.button.layer_all", "L:*");
        provider.add("gtpm.multiblock.preview.button.layer", "L:%s");
        provider.add("gtpm.multiblock.preview.button.selected.true", "+");
        provider.add("gtpm.multiblock.preview.button.selected.false", "-");
        provider.add("gtpm.multiblock.preview.button.selected.locked", "Lock");
        provider.add("gtpm.multiblock.preview.button.mode.build", "B");
        provider.add("gtpm.multiblock.preview.button.mode.demolish", "D");
        provider.add("gtpm.multiblock.preview.button.mode.locked", "B!");
        provider.add("gtpm.multiblock.preview.button.flip.true", "F+");
        provider.add("gtpm.multiblock.preview.button.flip.false", "F-");
        provider.add("gtpm.multiblock.preview.button.repeat_unit", "U:%s");
        provider.add("gtpm.multiblock.preview.button.repeat", "R:%s");
        provider.add("gtpm.multiblock.preview.button.repeat_none", "R:-");
        provider.add("gtpm.multiblock.preview.button.no_hatch.true", "H+");
        provider.add("gtpm.multiblock.preview.button.no_hatch.false", "H-");
        provider.add("gtpm.multiblock.preview.button.replace.true", "X+");
        provider.add("gtpm.multiblock.preview.button.replace.false", "X-");
        provider.add("gtpm.multiblock.preview.button.me.true", "A+");
        provider.add("gtpm.multiblock.preview.button.me.false", "A-");
        provider.add("gtpm.multiblock.preview.button.tier_group", "T:%s");
        provider.add("gtpm.multiblock.preview.button.tier_group_none", "T:-");
        provider.add("gtpm.multiblock.preview.button.tier_value", "V:%s");
        provider.add("gtpm.multiblock.preview.button.tier_value_none", "V:-");
        provider.add("gtpm.multiblock.preview.button.unavailable", "!");
        provider.add("gtpm.multiblock.preview.tooltip.focus", "Focus one selected structure");
        provider.add("gtpm.multiblock.preview.tooltip.layer", "Cycle visible layers");
        provider.add("gtpm.multiblock.preview.button.hatches", "H");
        provider.add("gtpm.multiblock.preview.tooltip.hatches", "Highlight positions that accept hatches");
        provider.add("gtpm.multiblock.preview.cell.hatch_replaceable", "Accepts interchangeable hatches");
        provider.add("gtpm.multiblock.preview.direction.down", "Down");
        provider.add("gtpm.multiblock.preview.direction.up", "Up");
        provider.add("gtpm.multiblock.preview.direction.north", "North");
        provider.add("gtpm.multiblock.preview.direction.south", "South");
        provider.add("gtpm.multiblock.preview.direction.west", "West");
        provider.add("gtpm.multiblock.preview.direction.east", "East");
        provider.add("gtpm.multiblock.configuration.title", "Structure");
        provider.add("gtpm.multiblock.configuration.replace", "Replace");
        provider.add("gtpm.multiblock.configuration.casings", "No hatches");
        provider.add("gtpm.multiblock.configuration.me", "ME");
        provider.add("gtpm.multiblock.configuration.main", "Main structure");
        provider.add("gtpm.multiblock.configuration.required", "Required");
        provider.add("gtpm.multiblock.configuration.selected", "Included");
        provider.add("gtpm.multiblock.configuration.unselected", "Excluded");
        provider.add("gtpm.multiblock.configuration.visible", "Hide");
        provider.add("gtpm.multiblock.configuration.hidden", "Show");
        provider.add("gtpm.multiblock.configuration.world_preview",
                "Show or hide this structure at the bound controller");
        provider.add("gtpm.multiblock.configuration.mode.build", "Build");
        provider.add("gtpm.multiblock.configuration.mode.demolish", "Demolish");
        provider.add("gtpm.multiblock.configuration.flip", "Flip: %s");
        provider.add("gtpm.multiblock.configuration.repeat_axis", "Repeat along %s");
        provider.add("gtpm.multiblock.configuration.repeat_range", "Range: %s–%s");
        provider.add("gtpm.multiblock.configuration.axis.depth", "Depth");
        provider.add("gtpm.multiblock.configuration.axis.height", "Height");
        provider.add("gtpm.multiblock.configuration.axis.width", "Width");
        provider.add("gtpm.multiblock.configuration.variant", "Variant");
        provider.add("gtpm.multiblock.configuration.variant_index", "Variant %s / %s");
        provider.add("gtpm.multiblock.configuration.hatch_tier", "Hatch tier");
        provider.add("gtpm.multiblock.configuration.toggle", "%s: %s");
        provider.add("gtpm.multiblock.configuration.on", "ON");
        provider.add("gtpm.multiblock.configuration.off", "OFF");
        provider.add("gtpm.multiblock.configuration.fullscreen", "Full screen");
        provider.add("gtpm.multiblock.configuration.compact", "Compact view");
        provider.add("gtpm.multiblock.configuration.select", "Tiers and dimensions");
        provider.add("gtpm.multiblock.configuration.edit", "Choose blocks");
        provider.add("gtpm.multiblock.configuration.dimension", "%s: %s");
        provider.add("gtpm.multiblock.ability.import_items", "Item input");
        provider.add("gtpm.multiblock.ability.export_items", "Item output");
        provider.add("gtpm.multiblock.ability.import_fluids", "Fluid input");
        provider.add("gtpm.multiblock.ability.export_fluids", "Fluid output");
        provider.add("gtpm.multiblock.ability.import_fluids_1x", "Fluid input (1 tank)");
        provider.add("gtpm.multiblock.ability.export_fluids_1x", "Fluid output (1 tank)");
        provider.add("gtpm.multiblock.ability.import_fluids_4x", "Fluid input (4 tanks)");
        provider.add("gtpm.multiblock.ability.export_fluids_4x", "Fluid output (4 tanks)");
        provider.add("gtpm.multiblock.ability.import_fluids_9x", "Fluid input (9 tanks)");
        provider.add("gtpm.multiblock.ability.export_fluids_9x", "Fluid output (9 tanks)");
        provider.add("gtpm.multiblock.ability.input_energy", "Energy input");
        provider.add("gtpm.multiblock.ability.output_energy", "Energy output");
        provider.add("gtpm.multiblock.ability.substation_input_energy", "Substation energy input");
        provider.add("gtpm.multiblock.ability.substation_output_energy", "Substation energy output");
        provider.add("gtpm.multiblock.ability.rotor_holder", "Rotor holder");
        provider.add("gtpm.multiblock.ability.pump_fluid_hatch", "Pump fluid hatch");
        provider.add("gtpm.multiblock.ability.steam", "Steam input");
        provider.add("gtpm.multiblock.ability.steam_import_items", "Steam item input");
        provider.add("gtpm.multiblock.ability.steam_export_items", "Steam item output");
        provider.add("gtpm.multiblock.ability.coke_oven_hatch", "Coke oven hatch");
        provider.add("gtpm.multiblock.ability.maintenance", "Maintenance");
        provider.add("gtpm.multiblock.ability.muffler", "Muffler");
        provider.add("gtpm.multiblock.ability.tank_valve", "Tank valve");
        provider.add("gtpm.multiblock.ability.passthrough_hatch", "Passthrough");
        provider.add("gtpm.multiblock.ability.parallel_hatch", "Parallel hatch");
        provider.add("gtpm.multiblock.ability.input_laser", "Laser input");
        provider.add("gtpm.multiblock.ability.output_laser", "Laser output");
        provider.add("gtpm.multiblock.ability.computation_data_reception", "Computation reception");
        provider.add("gtpm.multiblock.ability.computation_data_transmission", "Computation transmission");
        provider.add("gtpm.multiblock.ability.optical_data_reception", "Optical data reception");
        provider.add("gtpm.multiblock.ability.optical_data_transmission", "Optical data transmission");
        provider.add("gtpm.multiblock.ability.data_access", "Data access");
        provider.add("gtpm.multiblock.ability.hpca_component", "HPCA component");
        provider.add("gtpm.multiblock.ability.object_holder", "Object holder");
        provider.add("gtpm.multiblock.category.heating_coils", "Heating coils");
        provider.add("gtpm.multiblock.category.cleanroom_filters", "Cleanroom filters");
        provider.add("gtpm.multiblock.category.power_substation_batteries", "Substation batteries");
        provider.add("gtpm.multiblock.category.lamps", "Lamps");
        provider.add("gtpm.multiblock.category.borderless_lamps", "Borderless lamps");
        provider.add("gtpm.multiblock.category.muffler_hatches", "Muffler hatches");
        provider.add("gtpm.multiblock.category.rotor_holder", "Rotor holders");
        provider.add("gtpm.multiblock.category.glass", "Glass");
        provider.add("gtpm.multiblock.category.machine_casings", "Machine casings");
        provider.add("gtpm.multiblock.category.hermetic_casings", "Hermetic casings");
        provider.add("gtpm.multiblock.category.machine_hulls", "Machine hulls");
        provider.add("gtpm.multiblock.category.computer_casings", "Computer casings");
        provider.add("gtpm.multiblock.category.computer_heat_vents", "Computer heat vents");
        provider.add("gtpm.multiblock.preview.tooltip.structure", "Select the structure to configure");
        provider.add("gtpm.multiblock.preview.tooltip.selected", "Include or exclude this structure");
        provider.add("gtpm.multiblock.preview.tooltip.selected_locked",
                "Required by a selected build structure and cannot be removed");
        provider.add("gtpm.multiblock.preview.tooltip.mode", "Switch between build and demolition");
        provider.add("gtpm.multiblock.preview.tooltip.mode_locked",
                "Required by a selected build structure and locked to build mode");
        provider.add("gtpm.multiblock.preview.tooltip.flip", "Flip this structure");
        provider.add("gtpm.multiblock.preview.tooltip.repeat_unit", "Select a repeatable pattern unit");
        provider.add("gtpm.multiblock.preview.tooltip.repeat", "Change this unit's repetition count");
        provider.add("gtpm.multiblock.preview.tooltip.no_hatch",
                "Exclude all hatches and fill with original non-hatch candidates; skip hatch-only positions");
        provider.add("gtpm.multiblock.preview.tooltip.replace", "Allow replacing occupied targets");
        provider.add("gtpm.multiblock.preview.tooltip.me",
                "Real execution uses ME first, then player inventory; XEI does not query the network");
        provider.add("gtpm.multiblock.preview.tooltip.tier_group", "Select a tiered block group");
        provider.add("gtpm.multiblock.preview.tooltip.tier_value", "Select an exact legal tier");
        provider.add("gtpm.multiblock.preview.tooltip.tier_group_selected", "Tier group: %s");
        provider.add("gtpm.multiblock.preview.tooltip.tier_value_selected", "Exact tier candidate: %s (%s)");
        provider.add("gtpm.multiblock.preview.diagnostic.missing_material", "Missing material");
        provider.add("gtpm.jei.ore_processing_diagram", "Ore Processing Diagram");
        provider.add("gtpm.jei.ore_vein_diagram", "Ore Vein Diagram");
        provider.add("gtpm.jei.programmed_circuit", "Programmed Circuit Page");
        provider.add("gtpm.jei.bedrock_fluid_diagram", "Bedrock Fluid Diagram");
        provider.add("gtpm.jei.bedrock_ore_diagram", "Bedrock Ore Diagram");
        provider.add("gtpm.jei.bedrock_vein_diagram.yield", "Yield: %s");
        provider.add("gtpm.jei.bedrock_vein_diagram.depleted", "Depleted: %s");
        provider.add("gtpm.jei.ore_vein_diagram.chance", "§eChance: %s§r");
        provider.add("gtpm.jei.ore_vein_diagram.spawn_range", "Spawn Range:");
        provider.add("gtpm.jei.ore_vein_diagram.weight", "Weight: %s");
        provider.add("gtpm.jei.ore_vein_diagram.dimensions", "Dimensions:");
        for (ResourceKey<GTOreDefinition> key : GTOreVeins.ALL_KEYS) {
            ResourceLocation id = key.location();
            String name = id.getPath();
            provider.add(id.toLanguageKey("ore_vein"), RegistrateLangProvider.toEnglishName(name));
        }
        for (ResourceKey<BedrockFluidDefinition> key : GTBedrockFluids.ALL_KEYS) {
            ResourceLocation id = key.location();
            String name = id.getPath();
            provider.add(id.toLanguageKey("bedrock_fluid"), RegistrateLangProvider.toEnglishName(name));
        }
    }

    /**
     * Jade
     */
    private static void initWailaLikeLang(RegistrateLangProvider provider) {
        provider.add("gtpm.top.working_disabled", "Working Disabled");
        provider.add("gtpm.top.energy_consumption", "Using");
        provider.add("gtpm.top.energy_production", "Producing");
        provider.add("gtpm.top.transform_up", "§cStep Up§r %s");
        provider.add("gtpm.top.transform_down", "§aStep Down§r %s");
        provider.add("gtpm.top.transform_input", "§6Input:§r %s");
        provider.add("gtpm.top.transform_output", "§9Output:§r %s");
        provider.add("gtpm.top.convert_eu", "Converting §eEU§r -> §cFE§r");
        provider.add("gtpm.top.convert_fe", "Converting §cFE§r -> §eEU§r");
        provider.add("gtpm.top.fuel_min_consume", "Needs");
        provider.add("gtpm.top.fuel_none", "No fuel");
        provider.add("gtpm.top.invalid_structure", "Structure Incomplete");
        provider.add("gtpm.top.valid_structure", "Structure Formed");
        provider.add("gtpm.top.obstructed_structure", "Structure Obstructed");
        provider.add("gtpm.top.maintenance_fixed", "Maintenance Fine");
        provider.add("gtpm.top.maintenance_broken", "Needs Maintenance");
        provider.add("gtpm.top.maintenance.wrench", "Pipe is loose");
        provider.add("gtpm.top.maintenance.screwdriver", "Screws are loose");
        provider.add("gtpm.top.maintenance.soft_mallet", "Something is stuck");
        provider.add("gtpm.top.maintenance.hard_hammer", "Plating is dented");
        provider.add("gtpm.top.maintenance.wire_cutter", "Wires burned out");
        provider.add("gtpm.top.maintenance.crowbar", "That doesn't belong there");
        provider.add("gtpm.top.primitive_pump_production", "Production: %s mB/s");
        provider.add("gtpm.top.filter.label", "Filter:");
        provider.add("gtpm.top.link_cover.color", "Color:");
        provider.add("gtpm.top.mode.export", "Exporting");
        provider.add("gtpm.top.mode.import", "Importing");
        provider.add("gtpm.top.unit.items", "Items");
        provider.add("gtpm.top.unit.fluid_milibuckets", "L");
        provider.add("gtpm.top.unit.fluid_buckets", "kL");
        provider.add("gtpm.top.recipe_output", "Recipe Outputs:");
        provider.add("gtpm.top.item_auto_output", "Item Output: %s");
        provider.add("gtpm.top.fluid_auto_output", "Fluid Output: %s");
        provider.add("gtpm.top.auto_output", "Auto Output");
        provider.add("gtpm.top.allow_output_input", "Allow Input");
        provider.add("gtpm.top.cable_voltage", "Voltage: ");
        provider.add("gtpm.top.cable_amperage", "Amperage: ");
        provider.add("gtpm.top.cable_overloaded", "§4OVERHEATING: %s%%§r");
        provider.add("gtpm.top.exhaust_vent_direction", "Exhaust Vent: %s");
        provider.add("gtpm.top.exhaust_vent_blocked", "Blocked");
        provider.add("gtpm.top.machine_mode", "Machine Mode: ");
        provider.add("gtpm.top.stained", "Colored: %s");
        provider.add("gtpm.top.buffer_not_bound", "Buffer Not Currently Bound");
        provider.add("gtpm.top.buffer_bound_pos", "Bound To - X: %s, Y: %s, Z: %s");
        provider.add("gtpm.top.proxies_bound", "Buffer Proxies Bound: %s");

        provider.add("gtpm.jade.energy_stored", "%d / %d EU");
        provider.add("gtpm.jade.progress_computation", "%s / %s CWU");
        provider.add("gtpm.jade.progress_sec", "%s / %s s");
        provider.add("gtpm.jade.progress_tick", "%s / %s t");
        provider.add("gtpm.jade.cleaned_this_second", "Cleaned hazard: %s/s");
        provider.add("gtpm.jade.fluid_use", "%s mB/t");
        provider.add("gtpm.jade.amperage_use", "%s A");
        provider.add("gtpm.jade.at", " @ ");
        provider.add("gtpm.jade.remaining_charge_time", "Until charged: %s");
        provider.add("gtpm.jade.remaining_discharge_time", "Until empty: %s");
        provider.add("gtpm.jade.changes_eu_sec", "%s EU/s");
        provider.add("gtpm.jade.seconds", "%s seconds");
        provider.add("gtpm.jade.minutes", "%s minutes");
        provider.add("gtpm.jade.hours", "%s hours");
        provider.add("gtpm.jade.days", "%s days");
        provider.add("gtpm.jade.years", "%s years");

        provider.add("gtpm.top.energy_stored", " / %d EU");
        provider.add("gtpm.top.progress_computation", " / %s CWU");
        provider.add("gtpm.top.progress_sec", " / %s s");
        provider.add("gtpm.top.progress_tick", " / %s t");

        provider.add("gtpm.top.ldp_endpoint.is_formed", "§aPipeline Formed§r");
        provider.add("gtpm.top.ldp_endpoint.not_formed", "§cPipeline Incomplete§r");
        provider.add("gtpm.top.ldp_endpoint.io_type", "IO Type: %s");
        provider.add("gtpm.top.ldp_endpoint.output_direction", "Output Direction: %s");
    }

    private static void initMinimapLang(RegistrateLangProvider provider) {
        provider.add("gtpm.minimap.ore_vein.depleted", "Depleted");

        provider.add("message.gtpm.new_veins.amount", "Prospected %d new veins!");
        provider.add("message.gtpm.new_veins.name", "Prospected %s!");
        provider.add("button.gtpm.mark_as_depleted.name", "Mark as Depleted");
        provider.add("button.gtpm.toggle_waypoint.name", "Toggle Waypoint");

        provider.add("gtpm.journeymap.options.layers", "Prospection layers");
        provider.add("gtpm.journeymap.options.layers.ore_veins", "Show Ore Veins");
        provider.add("gtpm.journeymap.options.layers.bedrock_fluids", "Show Bedrock Fluid Veins");
        provider.add("gtpm.journeymap.options.layers.hide_depleted", "Hide Depleted Veins");
    }

    private static void initOwnershipLang(RegistrateLangProvider provider) {
        provider.add("gtpm.ownership.name.player", "Player");
        provider.add("gtpm.ownership.name.ftb", "FTB Teams");
        provider.add("gtpm.ownership.name.argonauts", "Argonauts Guild");
    }
}
