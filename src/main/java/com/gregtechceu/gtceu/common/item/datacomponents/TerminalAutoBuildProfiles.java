package com.gregtechceu.gtceu.common.item.datacomponents;

import com.gregtechceu.gtceu.api.machine.MultiblockMachineDefinition;
import com.gregtechceu.gtceu.api.multiblock.autobuild.batch.AutoBuildMode;
import com.gregtechceu.gtceu.api.multiblock.autobuild.batch.AutoBuildSharedOptions;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectMaps;
import org.jspecify.annotations.NullMarked;

/** Stores independent automatic-build profiles by multiblock machine definition. */
@NullMarked
public record TerminalAutoBuildProfiles(Object2ObjectMap<ResourceLocation, TerminalMachineProfile> profiles) {

    private static final int MAX_MACHINES = 256;
    private static final int MAX_STRUCTURES = 128;
    private static final int MAX_REPETITIONS = 64;
    private static final int MAX_TIER_CHOICES = 128;
    private static final int MAX_STRUCTURE_NAME = 128;
    private static final int MAX_RESOURCE_LOCATION = 256;
    private static final int MAX_CHOICES = 128;
    private static final int MAX_CHOICE_PATH = 2048;

    public static final TerminalAutoBuildProfiles EMPTY = new TerminalAutoBuildProfiles(Object2ObjectMaps.emptyMap());

    public static final Codec<TerminalAutoBuildProfiles> CODEC = RecordCodecBuilder
            .<TerminalAutoBuildProfiles>create(instance -> instance
            .group(
                    Codec.unboundedMap(ResourceLocation.CODEC, TerminalMachineProfile.CODEC)
                            .<Object2ObjectMap<ResourceLocation, TerminalMachineProfile>>xmap(
                                    Object2ObjectLinkedOpenHashMap::new, value -> value)
                            .fieldOf("profiles")
                            .forGetter(TerminalAutoBuildProfiles::profiles))
            .apply(instance, TerminalAutoBuildProfiles::new))
            .comapFlatMap(TerminalAutoBuildProfiles::validateDecoded, value -> value);

    public static final StreamCodec<RegistryFriendlyByteBuf, TerminalAutoBuildProfiles> STREAM_CODEC = StreamCodec
            .of(TerminalAutoBuildProfiles::encode, TerminalAutoBuildProfiles::decode);

    private static DataResult<TerminalAutoBuildProfiles> validateDecoded(TerminalAutoBuildProfiles value) {
        if (value.profiles.size() > MAX_MACHINES) {
            return DataResult.error(() -> "Terminal machine profiles exceed " + MAX_MACHINES);
        }
        for (ResourceLocation definitionId : value.profiles.keySet()) {
            if (definitionId.toString().length() > MAX_RESOURCE_LOCATION) {
                return DataResult.error(() -> "Terminal machine definition ID is unbounded");
            }
        }
        return DataResult.success(value);
    }

    public TerminalMachineProfile getOrCreate(MultiblockMachineDefinition definition) {
        TerminalMachineProfile profile = profiles.get(definition.getId());
        return profile == null ? TerminalMachineProfile.defaults(definition) : profile;
    }

    public TerminalAutoBuildProfiles withProfile(ResourceLocation definitionId, TerminalMachineProfile profile) {
        Object2ObjectMap<ResourceLocation, TerminalMachineProfile> updated = new Object2ObjectLinkedOpenHashMap<>(
                profiles);
        updated.put(definitionId, profile);
        return new TerminalAutoBuildProfiles(updated);
    }

    private static void encode(RegistryFriendlyByteBuf buffer, TerminalAutoBuildProfiles value) {
        buffer.writeVarInt(value.profiles.size());
        value.profiles.forEach((definitionId, profile) -> {
            writeResourceLocation(buffer, definitionId);
            encodeMachine(buffer, profile);
        });
    }

    private static TerminalAutoBuildProfiles decode(RegistryFriendlyByteBuf buffer) {
        int size = readBoundedSize(buffer, MAX_MACHINES, "terminal machine profiles");
        Object2ObjectMap<ResourceLocation, TerminalMachineProfile> profiles = new Object2ObjectLinkedOpenHashMap<>();
        for (int index = 0; index < size; index++) {
            ResourceLocation id = readResourceLocation(buffer);
            if (profiles.put(id, decodeMachine(buffer)) != null) {
                throw new IllegalArgumentException("Duplicate terminal machine profile: " + id);
            }
        }
        return new TerminalAutoBuildProfiles(profiles);
    }

    private static void encodeMachine(RegistryFriendlyByteBuf buffer, TerminalMachineProfile profile) {
        AutoBuildSharedOptions shared = profile.sharedOptions();
        buffer.writeBoolean(shared.replaceMode());
        buffer.writeBoolean(shared.noHatchMode());
        buffer.writeBoolean(shared.useME());
        buffer.writeVarInt(profile.structures().size());
        profile.structures().forEach((name, structure) -> {
            buffer.writeUtf(name, MAX_STRUCTURE_NAME);
            buffer.writeBoolean(structure.selected());
            buffer.writeEnum(structure.mode());
            buffer.writeBoolean(structure.flipMode());
            buffer.writeVarInt(structure.repetitions().size());
            for (int repetition : structure.repetitions()) buffer.writeVarInt(repetition);
            buffer.writeVarInt(structure.tierChoices().size());
            structure.tierChoices().forEach((group, choice) -> {
                buffer.writeUtf(group, MAX_RESOURCE_LOCATION);
                writeResourceLocation(buffer, choice);
            });
            buffer.writeVarInt(structure.choices().size());
            structure.choices().forEach((path, branch) -> {
                buffer.writeUtf(path, MAX_CHOICE_PATH);
                buffer.writeUtf(branch, MAX_RESOURCE_LOCATION);
            });
        });
    }

    private static TerminalMachineProfile decodeMachine(RegistryFriendlyByteBuf buffer) {
        AutoBuildSharedOptions shared = new AutoBuildSharedOptions(buffer.readBoolean(), buffer.readBoolean(),
                buffer.readBoolean());
        int structuresSize = readBoundedSize(buffer, MAX_STRUCTURES, "terminal structures");
        Object2ObjectMap<String, TerminalStructureProfile> structures = new Object2ObjectLinkedOpenHashMap<>();
        for (int structureIndex = 0; structureIndex < structuresSize; structureIndex++) {
            String name = buffer.readUtf(MAX_STRUCTURE_NAME);
            if (name.isBlank()) {
                throw new IllegalArgumentException("Terminal structure name must be non-blank");
            }
            boolean selected = buffer.readBoolean();
            AutoBuildMode mode = buffer.readEnum(AutoBuildMode.class);
            boolean flip = buffer.readBoolean();
            int repetitionsSize = readBoundedSize(buffer, MAX_REPETITIONS, "terminal repetitions");
            IntList repetitions = new IntArrayList(repetitionsSize);
            for (int repetitionIndex = 0; repetitionIndex < repetitionsSize; repetitionIndex++) {
                int repetition = buffer.readVarInt();
                if (repetition < 0) {
                    throw new IllegalArgumentException("Terminal repetitions must be non-negative");
                }
                repetitions.add(repetition);
            }
            int tiersSize = readBoundedSize(buffer, MAX_TIER_CHOICES, "terminal tier choices");
            Object2ObjectMap<String, ResourceLocation> tiers = new Object2ObjectLinkedOpenHashMap<>();
            for (int tierIndex = 0; tierIndex < tiersSize; tierIndex++) {
                String group = buffer.readUtf(MAX_RESOURCE_LOCATION);
                if (group.isBlank()) {
                    throw new IllegalArgumentException("Terminal tier IDs must be non-blank");
                }
                ResourceLocation choice = readResourceLocation(buffer);
                if (tiers.put(group, choice) != null) {
                    throw new IllegalArgumentException("Duplicate terminal tier choice: " + group);
                }
            }
            int choiceCount = readBoundedSize(buffer, MAX_CHOICES, "terminal branch choices");
            Object2ObjectMap<String, String> choices = new Object2ObjectLinkedOpenHashMap<>();
            for (int choiceIndex = 0; choiceIndex < choiceCount; choiceIndex++) {
                String path = buffer.readUtf(MAX_CHOICE_PATH);
                String branch = buffer.readUtf(MAX_RESOURCE_LOCATION);
                if (path.isBlank() || branch.isBlank()) {
                    throw new IllegalArgumentException("Terminal choice paths and branches must be non-blank");
                }
                if (choices.put(path, branch) != null) {
                    throw new IllegalArgumentException("Duplicate terminal branch choice: " + path);
                }
            }
            if (structures.put(name, new TerminalStructureProfile(selected, mode, repetitions, flip, tiers, choices)) !=
                    null) {
                throw new IllegalArgumentException("Duplicate terminal structure profile: " + name);
            }
        }
        return new TerminalMachineProfile(shared, structures);
    }

    private static int readBoundedSize(RegistryFriendlyByteBuf buffer, int maximum, String name) {
        int size = buffer.readVarInt();
        if (size < 0 || size > maximum) {
            throw new IllegalArgumentException(name + " exceeds wire limit " + maximum);
        }
        return size;
    }

    private static void writeResourceLocation(RegistryFriendlyByteBuf buffer, ResourceLocation value) {
        buffer.writeUtf(value.toString(), MAX_RESOURCE_LOCATION);
    }

    private static ResourceLocation readResourceLocation(RegistryFriendlyByteBuf buffer) {
        return ResourceLocation.parse(buffer.readUtf(MAX_RESOURCE_LOCATION));
    }
}
