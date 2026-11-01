package com.gregtechceu.gtceu.common.item.datacomponents;

import com.gregtechceu.gtceu.api.machine.MultiblockMachineDefinition;
import com.gregtechceu.gtceu.api.machine.multiblock.MultiblockControllerMachine;
import com.gregtechceu.gtceu.api.multiblock.autobuild.AutoBuildMaterialSource;
import com.gregtechceu.gtceu.api.multiblock.autobuild.batch.AutoBuildBatchRequest;
import com.gregtechceu.gtceu.api.multiblock.autobuild.batch.AutoBuildSharedOptions;
import com.gregtechceu.gtceu.api.multiblock.autobuild.batch.AutoBuildStructureOptions;
import com.gregtechceu.gtceu.api.multiblock.autobuild.plan.MultiblockPlanResolver;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectList;
import org.jspecify.annotations.NullMarked;

/** Persists shared and per-structure terminal settings for one multiblock definition. */
@NullMarked
public record TerminalMachineProfile(AutoBuildSharedOptions sharedOptions,
                                     Object2ObjectMap<String, TerminalStructureProfile> structures) {

    private static final int MAX_STRUCTURES = 128;
    private static final int MAX_STRUCTURE_NAME = 128;

    private static final Codec<AutoBuildSharedOptions> SHARED_CODEC = RecordCodecBuilder.create(instance -> instance
            .group(
                    Codec.BOOL.fieldOf("replace").forGetter(AutoBuildSharedOptions::replaceMode),
                    Codec.BOOL.fieldOf("no_hatch").forGetter(AutoBuildSharedOptions::noHatchMode),
                    Codec.BOOL.fieldOf("use_me").forGetter(AutoBuildSharedOptions::useME))
            .apply(instance, AutoBuildSharedOptions::new));

    public static final Codec<TerminalMachineProfile> CODEC = RecordCodecBuilder
            .<TerminalMachineProfile>create(instance -> instance.group(
            SHARED_CODEC.fieldOf("shared").forGetter(TerminalMachineProfile::sharedOptions),
            Codec.unboundedMap(Codec.STRING, TerminalStructureProfile.CODEC)
                    .<Object2ObjectMap<String, TerminalStructureProfile>>xmap(Object2ObjectLinkedOpenHashMap::new,
                            value -> value)
                    .fieldOf("structures")
                    .forGetter(TerminalMachineProfile::structures))
            .apply(instance, TerminalMachineProfile::new))
            .comapFlatMap(TerminalMachineProfile::validateDecoded, value -> value);

    private static DataResult<TerminalMachineProfile> validateDecoded(TerminalMachineProfile value) {
        if (value.structures.size() > MAX_STRUCTURES) {
            return DataResult.error(() -> "Terminal structures exceed " + MAX_STRUCTURES);
        }
        for (String name : value.structures.keySet()) {
            if (name.isBlank() || name.length() > MAX_STRUCTURE_NAME) {
                return DataResult.error(() -> "Terminal structure name must be non-blank and bounded");
            }
        }
        return DataResult.success(value);
    }

    /** Creates the first-use profile, selecting main and its explicitly declared dependencies. */
    public static TerminalMachineProfile defaults(MultiblockMachineDefinition definition) {
        Object2ObjectMap<String, TerminalStructureProfile> structures = new Object2ObjectLinkedOpenHashMap<>();
        var required = definition.getRequiredStructures(MultiblockControllerMachine.DEFAULT_STRUCTURE);
        MultiblockPlanResolver resolver = new MultiblockPlanResolver();
        for (String name : definition.getStructureOrder()) {
            boolean selected = MultiblockControllerMachine.DEFAULT_STRUCTURE.equals(name) || required.contains(name);
            AutoBuildStructureOptions options = resolver.defaultStructureOptions(definition, name);
            structures.put(name, new TerminalStructureProfile(selected, options.mode(), options.repetitions(),
                    options.flipMode(), options.tierChoices(), options.choices()));
        }
        return new TerminalMachineProfile(AutoBuildSharedOptions.DEFAULT, structures);
    }

    public TerminalMachineProfile withSharedOptions(AutoBuildSharedOptions value) {
        return new TerminalMachineProfile(value, structures);
    }

    public TerminalMachineProfile withStructure(String name, TerminalStructureProfile value) {
        Object2ObjectMap<String, TerminalStructureProfile> updated = new Object2ObjectLinkedOpenHashMap<>(structures);
        updated.put(name, value);
        return new TerminalMachineProfile(sharedOptions, updated);
    }

    /** Creates an ordered batch request containing only structures selected in this profile. */
    public AutoBuildBatchRequest createRequest(MultiblockMachineDefinition definition,
                                               ObjectList<AutoBuildMaterialSource> materialSources) {
        ObjectList<AutoBuildStructureOptions> selected = new ObjectArrayList<>();
        for (String name : definition.getStructureOrder()) {
            TerminalStructureProfile profile = structures.get(name);
            if (profile == null) {
                throw new IllegalArgumentException("Missing terminal structure profile: " + name);
            }
            if (profile.selected()) {
                selected.add(new AutoBuildStructureOptions(name, profile.repetitions(), profile.tierChoices(),
                        profile.flipMode(), profile.mode(), profile.choices()));
            }
        }
        return new AutoBuildBatchRequest(selected, sharedOptions, materialSources);
    }
}
