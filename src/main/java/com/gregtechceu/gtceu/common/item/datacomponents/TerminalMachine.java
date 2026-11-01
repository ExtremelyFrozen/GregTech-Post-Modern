package com.gregtechceu.gtceu.common.item.datacomponents;

import com.gregtechceu.gtceu.api.machine.MultiblockMachineDefinition;
import com.gregtechceu.gtceu.api.machine.multiblock.MultiblockControllerMachine;
import com.gregtechceu.gtceu.api.multiblock.autobuild.MaterialSource;
import com.gregtechceu.gtceu.api.multiblock.autobuild.batch.BatchRequest;
import com.gregtechceu.gtceu.api.multiblock.autobuild.batch.BuildPolicy;
import com.gregtechceu.gtceu.api.multiblock.autobuild.batch.StructureRequest;
import com.gregtechceu.gtceu.api.multiblock.autobuild.plan.BatchPlanner;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectList;
import org.jspecify.annotations.NullMarked;

/** Persists shared and per-structure terminal settings for one multiblock definition. */
@NullMarked
public record TerminalMachine(BuildPolicy sharedOptions,
                                     Object2ObjectMap<String, TerminalStructure> structures) {

    private static final Codec<BuildPolicy> SHARED_CODEC = RecordCodecBuilder.create(instance -> instance
            .group(
                    Codec.BOOL.fieldOf("replace").forGetter(BuildPolicy::replaceMode),
                    Codec.BOOL.fieldOf("no_hatch").forGetter(BuildPolicy::noHatchMode),
                    Codec.BOOL.fieldOf("use_me").forGetter(BuildPolicy::useME))
            .apply(instance, BuildPolicy::new));

    public static final Codec<TerminalMachine> CODEC = RecordCodecBuilder
            .<TerminalMachine>create(instance -> instance.group(
                    SHARED_CODEC.fieldOf("shared").forGetter(TerminalMachine::sharedOptions),
                    Codec.unboundedMap(Codec.STRING, TerminalStructure.CODEC)
                            .<Object2ObjectMap<String, TerminalStructure>>xmap(
                                    Object2ObjectLinkedOpenHashMap::new,
                                    value -> value)
                            .fieldOf("structures")
                            .forGetter(TerminalMachine::structures))
                    .apply(instance, TerminalMachine::new));

    /** Creates the first-use profile, selecting main and its explicitly declared dependencies. */
    public static TerminalMachine defaults(MultiblockMachineDefinition definition) {
        Object2ObjectMap<String, TerminalStructure> structures = new Object2ObjectLinkedOpenHashMap<>();
        var required = definition.getRequiredStructures(MultiblockControllerMachine.DEFAULT_STRUCTURE);
        BatchPlanner resolver = new BatchPlanner();
        for (String name : definition.getStructureOrder()) {
            boolean selected = MultiblockControllerMachine.DEFAULT_STRUCTURE.equals(name) || required.contains(name);
            StructureRequest options = resolver.defaultStructureOptions(definition, name);
            structures.put(name, new TerminalStructure(selected, options.mode(), options.repetitions(),
                    options.flipMode(), options.tierChoices(), options.choices()));
        }
        return new TerminalMachine(BuildPolicy.DEFAULT, structures);
    }

    public TerminalMachine withSharedOptions(BuildPolicy value) {
        return new TerminalMachine(value, structures);
    }

    public TerminalMachine withStructure(String name, TerminalStructure value) {
        Object2ObjectMap<String, TerminalStructure> updated = new Object2ObjectLinkedOpenHashMap<>(structures);
        updated.put(name, value);
        return new TerminalMachine(sharedOptions, updated);
    }

    /** Creates an ordered batch request containing only structures selected in this profile. */
    public BatchRequest createRequest(MultiblockMachineDefinition definition,
                                               ObjectList<MaterialSource> materialSources) {
        ObjectList<StructureRequest> selected = new ObjectArrayList<>();
        for (String name : definition.getStructureOrder()) {
            TerminalStructure profile = structures.get(name);
            if (profile == null) {
                throw new IllegalArgumentException("Missing terminal structure profile: " + name);
            }
            if (profile.selected()) {
                selected.add(new StructureRequest(name, profile.repetitions(), profile.tierChoices(),
                        profile.flipMode(), profile.mode(), profile.choices()));
            }
        }
        return new BatchRequest(selected, sharedOptions, materialSources);
    }
}
