package com.gregtechceu.gtceu.common.item.datacomponents;

import com.gregtechceu.gtceu.api.machine.MultiblockMachineDefinition;
import com.gregtechceu.gtceu.api.machine.multiblock.MultiblockControllerMachine;
import com.gregtechceu.gtceu.api.multiblock.autobuild.MaterialRequest;
import com.gregtechceu.gtceu.api.multiblock.autobuild.batch.BatchRequest;
import com.gregtechceu.gtceu.api.multiblock.autobuild.batch.BuildPolicy;
import com.gregtechceu.gtceu.api.multiblock.autobuild.batch.StructureOperation;
import com.gregtechceu.gtceu.api.multiblock.autobuild.batch.StructureRequest;
import com.gregtechceu.gtceu.api.multiblock.autobuild.plan.BatchPlanner;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectList;
import org.jspecify.annotations.NullMarked;

/** Persists shared and per-structure terminal settings for one multiblock definition. */
@NullMarked
public record TerminalMachine(BuildPolicy sharedOptions,
                              Object2ObjectMap<String, TerminalStructure> structures) {

    public static final StreamCodec<RegistryFriendlyByteBuf, TerminalMachine> STREAM_CODEC = StreamCodec
            .of(TerminalMachine::encode, TerminalMachine::decode);

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

    /** Creates the first-use machine, selecting main and its explicitly declared dependencies. */
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

    /** Creates an ordered batch request containing only structures selected in this machine. */
    public BatchRequest createRequest(MultiblockMachineDefinition definition,
                                      ObjectList<MaterialRequest.Result> materialSources) {
        ObjectList<StructureRequest> selected = new ObjectArrayList<>();
        for (String name : definition.getStructureOrder()) {
            TerminalStructure structure = structures.get(name);
            if (structure == null) {
                throw new IllegalArgumentException("Missing terminal structure structure: " + name);
            }
            if (structure.selected()) {
                selected.add(new StructureRequest(name, structure.repetitions(), structure.tierChoices(),
                        structure.flipMode(), structure.mode(), structure.choices()));
            }
        }
        return new BatchRequest(selected, sharedOptions, materialSources);
    }

    private static void encode(RegistryFriendlyByteBuf buffer, TerminalMachine machine) {
        BuildPolicy shared = machine.sharedOptions();
        buffer.writeBoolean(shared.replaceMode());
        buffer.writeBoolean(shared.noHatchMode());
        buffer.writeBoolean(shared.useME());
        buffer.writeVarInt(machine.structures().size());
        machine.structures().forEach((name, structure) -> {
            buffer.writeUtf(name);
            buffer.writeBoolean(structure.selected());
            buffer.writeEnum(structure.mode());
            buffer.writeBoolean(structure.flipMode());
            buffer.writeVarInt(structure.repetitions().size());
            for (int repetition : structure.repetitions()) buffer.writeVarInt(repetition);
            buffer.writeVarInt(structure.tierChoices().size());
            structure.tierChoices().forEach((group, choice) -> {
                buffer.writeUtf(group);
                buffer.writeResourceLocation(choice);
            });
            buffer.writeVarInt(structure.choices().size());
            structure.choices().forEach((path, branch) -> {
                buffer.writeUtf(path);
                buffer.writeUtf(branch);
            });
        });
    }

    private static TerminalMachine decode(RegistryFriendlyByteBuf buffer) {
        BuildPolicy shared = new BuildPolicy(buffer.readBoolean(), buffer.readBoolean(),
                buffer.readBoolean());
        int structuresSize = buffer.readVarInt();
        Object2ObjectMap<String, TerminalStructure> structures = new Object2ObjectLinkedOpenHashMap<>();
        for (int structureIndex = 0; structureIndex < structuresSize; structureIndex++) {
            String name = buffer.readUtf();
            boolean selected = buffer.readBoolean();
            StructureOperation mode = buffer.readEnum(StructureOperation.class);
            boolean flip = buffer.readBoolean();
            int repetitionsSize = buffer.readVarInt();
            IntList repetitions = new IntArrayList(repetitionsSize);
            for (int repetitionIndex = 0; repetitionIndex < repetitionsSize; repetitionIndex++) {
                repetitions.add(buffer.readVarInt());
            }
            int tiersSize = buffer.readVarInt();
            Object2ObjectMap<String, ResourceLocation> tiers = new Object2ObjectLinkedOpenHashMap<>();
            for (int tierIndex = 0; tierIndex < tiersSize; tierIndex++) {
                String group = buffer.readUtf();
                tiers.put(group, buffer.readResourceLocation());
            }
            int choiceCount = buffer.readVarInt();
            Object2ObjectMap<String, String> choices = new Object2ObjectLinkedOpenHashMap<>();
            for (int choiceIndex = 0; choiceIndex < choiceCount; choiceIndex++) {
                choices.put(buffer.readUtf(), buffer.readUtf());
            }
            structures.put(name, new TerminalStructure(selected, mode, repetitions, flip, tiers, choices));
        }
        return new TerminalMachine(shared, structures);
    }
}
