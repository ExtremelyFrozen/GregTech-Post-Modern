package com.gregtechceu.gtceu.common.item.datacomponents;

import com.gregtechceu.gtceu.api.machine.MultiblockMachineDefinition;
import com.gregtechceu.gtceu.api.machine.multiblock.MultiblockControllerMachine;
import com.gregtechceu.gtceu.api.multiblock.autobuild.AutoBuildMaterialSource;
import com.gregtechceu.gtceu.api.multiblock.autobuild.batch.AutoBuildBatchRequest;
import com.gregtechceu.gtceu.api.multiblock.autobuild.batch.AutoBuildMode;
import com.gregtechceu.gtceu.api.multiblock.autobuild.batch.AutoBuildSharedOptions;
import com.gregtechceu.gtceu.api.multiblock.autobuild.batch.AutoBuildStructureOptions;
import com.gregtechceu.gtceu.api.multiblock.autobuild.plan.MultiblockPlanResolver;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import org.jspecify.annotations.NullMarked;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Persists shared and per-structure terminal settings for one multiblock definition. */
@NullMarked
public record TerminalMachineProfile(AutoBuildSharedOptions sharedOptions,
                                     Map<String, TerminalStructureProfile> structures) {

    private static final int MAX_STRUCTURES = 128;
    private static final int MAX_STRUCTURE_NAME = 128;

    private static final Codec<AutoBuildSharedOptions> SHARED_CODEC = RecordCodecBuilder.create(instance -> instance
            .group(
                    Codec.BOOL.fieldOf("replace").forGetter(AutoBuildSharedOptions::replaceMode),
                    Codec.BOOL.fieldOf("no_hatch").forGetter(AutoBuildSharedOptions::noHatchMode),
                    Codec.BOOL.fieldOf("use_me").forGetter(AutoBuildSharedOptions::useME))
            .apply(instance, AutoBuildSharedOptions::new));

    public static final Codec<TerminalMachineProfile> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            SHARED_CODEC.fieldOf("shared").forGetter(TerminalMachineProfile::sharedOptions),
            Codec.unboundedMap(Codec.STRING, TerminalStructureProfile.CODEC).fieldOf("structures")
                    .forGetter(TerminalMachineProfile::structures))
            .apply(instance, TerminalMachineProfile::new));

    public TerminalMachineProfile {
        if (structures.size() > MAX_STRUCTURES) {
            throw new IllegalArgumentException("Terminal structures exceed " + MAX_STRUCTURES);
        }
        LinkedHashMap<String, TerminalStructureProfile> ordered = new LinkedHashMap<>();
        structures.forEach((name, profile) -> {
            if (name.isBlank() || name.length() > MAX_STRUCTURE_NAME) {
                throw new IllegalArgumentException("Terminal structure name must be non-blank and bounded");
            }
            ordered.put(name, profile);
        });
        structures = Collections.unmodifiableMap(ordered);
    }

    /** Creates the first-use profile, selecting main and its explicitly declared dependencies. */
    public static TerminalMachineProfile defaults(MultiblockMachineDefinition definition) {
        LinkedHashMap<String, TerminalStructureProfile> structures = new LinkedHashMap<>();
        List<String> required = definition.getRequiredStructures(MultiblockControllerMachine.DEFAULT_STRUCTURE);
        MultiblockPlanResolver resolver = new MultiblockPlanResolver();
        for (String name : definition.getStructureOrder()) {
            boolean selected = MultiblockControllerMachine.DEFAULT_STRUCTURE.equals(name) || required.contains(name);
            AutoBuildStructureOptions options = resolver.defaultStructureOptions(definition, name);
            if (options.mode() != AutoBuildMode.BUILD || options.flipMode()) {
                throw new IllegalStateException("Default terminal structure options must use unflipped BUILD mode");
            }
            structures.put(name, new TerminalStructureProfile(selected, options.mode(), options.repetitions(),
                    options.flipMode(), options.tierChoices()));
        }
        return new TerminalMachineProfile(AutoBuildSharedOptions.DEFAULT, structures);
    }

    public TerminalMachineProfile withSharedOptions(AutoBuildSharedOptions value) {
        return new TerminalMachineProfile(value, structures);
    }

    public TerminalMachineProfile withStructure(String name, TerminalStructureProfile value) {
        LinkedHashMap<String, TerminalStructureProfile> updated = new LinkedHashMap<>(structures);
        updated.put(name, value);
        return new TerminalMachineProfile(sharedOptions, updated);
    }

    /** Creates an ordered batch request containing only structures selected in this profile. */
    public AutoBuildBatchRequest createRequest(MultiblockMachineDefinition definition,
                                               List<AutoBuildMaterialSource> materialSources) {
        List<AutoBuildStructureOptions> selected = new ArrayList<>();
        for (String name : definition.getStructureOrder()) {
            TerminalStructureProfile profile = structures.get(name);
            if (profile == null) {
                throw new IllegalArgumentException("Missing terminal structure profile: " + name);
            }
            if (profile.selected()) {
                selected.add(new AutoBuildStructureOptions(name, profile.repetitions(), profile.tierChoices(),
                        profile.flipMode(), profile.mode()));
            }
        }
        return new AutoBuildBatchRequest(selected, sharedOptions, materialSources);
    }
}
