package com.gregtechceu.gtceu.common.item.datacomponents;

import com.gregtechceu.gtceu.api.machine.MultiblockMachineDefinition;
import com.gregtechceu.gtceu.api.multiblock.autobuild.batch.AutoBuildMode;
import com.gregtechceu.gtceu.api.multiblock.autobuild.batch.AutoBuildSharedOptions;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Decoder;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectMaps;
import org.jspecify.annotations.NullMarked;

/**
 * Stores independent automatic-build profiles by multiblock machine definition.
 *
 * <p>
 * The codec recognizes the former single-structure format only to reset it safely; legacy values are not
 * migrated into the new profile model.
 * </p>
 */
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

    private static final Codec<TerminalAutoBuildProfiles> CURRENT_CODEC = RecordCodecBuilder.create(instance -> instance
            .group(
                    Codec.unboundedMap(ResourceLocation.CODEC, TerminalMachineProfile.CODEC)
                            .<Object2ObjectMap<ResourceLocation, TerminalMachineProfile>>xmap(
                                    Object2ObjectLinkedOpenHashMap::new, value -> value)
                            .fieldOf("profiles")
                            .forGetter(TerminalAutoBuildProfiles::profiles))
            .apply(instance, TerminalAutoBuildProfiles::new));

    private static final Decoder<TerminalAutoBuildProfiles> DECODER = new Decoder<>() {

        @Override
        public <T> DataResult<Pair<TerminalAutoBuildProfiles, T>> decode(DynamicOps<T> ops, T input) {
            return ops.getMap(input).flatMap(map -> {
                if (map.get("profiles") != null) return CURRENT_CODEC.decode(ops, input);
                T structure = map.get("structure_name");
                T options = map.get("options");
                if (structure == null && options == null) {
                    return DataResult
                            .error(() -> "Terminal data is neither current nor retired single-structure format");
                }
                DataResult<String> name = structure == null ? DataResult.success("main") :
                        Codec.STRING.parse(ops, structure);
                return name.flatMap(ignored -> options == null ? DataResult.success(Pair.of(EMPTY, ops.empty())) :
                        ops.getMap(options).map(retired -> Pair.of(EMPTY, ops.empty())));
            });
        }
    };

    public static final Codec<TerminalAutoBuildProfiles> CODEC = Codec.of(CURRENT_CODEC, DECODER);

    public static final StreamCodec<RegistryFriendlyByteBuf, TerminalAutoBuildProfiles> STREAM_CODEC = StreamCodec
            .of(TerminalAutoBuildProfiles::encode, TerminalAutoBuildProfiles::decode);

    public TerminalAutoBuildProfiles {
        if (profiles.size() > MAX_MACHINES) {
            throw new IllegalArgumentException("Terminal machine profiles exceed " + MAX_MACHINES);
        }
        Object2ObjectMap<ResourceLocation, TerminalMachineProfile> ordered = new Object2ObjectLinkedOpenHashMap<>();
        profiles.forEach((definitionId, profile) -> {
            if (definitionId.toString().length() > MAX_RESOURCE_LOCATION) {
                throw new IllegalArgumentException("Terminal machine definition ID is unbounded");
            }
            ordered.put(definitionId, profile);
        });
        profiles = Object2ObjectMaps.unmodifiable(ordered);
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
        writeBoundedSize(buffer, value.profiles.size(), MAX_MACHINES, "terminal machine profiles");
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
        writeBoundedSize(buffer, profile.structures().size(), MAX_STRUCTURES, "terminal structures");
        profile.structures().forEach((name, structure) -> {
            buffer.writeUtf(name, MAX_STRUCTURE_NAME);
            buffer.writeBoolean(structure.selected());
            buffer.writeEnum(structure.mode());
            buffer.writeBoolean(structure.flipMode());
            writeBoundedSize(buffer, structure.repetitions().size(), MAX_REPETITIONS, "terminal repetitions");
            for (int repetition : structure.repetitions()) buffer.writeVarInt(repetition);
            writeBoundedSize(buffer, structure.tierChoices().size(), MAX_TIER_CHOICES, "terminal tier choices");
            structure.tierChoices().forEach((group, choice) -> {
                buffer.writeUtf(group, MAX_RESOURCE_LOCATION);
                writeResourceLocation(buffer, choice);
            });
            writeBoundedSize(buffer, structure.choices().size(), MAX_CHOICES, "terminal branch choices");
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
            boolean selected = buffer.readBoolean();
            AutoBuildMode mode = buffer.readEnum(AutoBuildMode.class);
            boolean flip = buffer.readBoolean();
            int repetitionsSize = readBoundedSize(buffer, MAX_REPETITIONS, "terminal repetitions");
            IntList repetitions = new IntArrayList(repetitionsSize);
            for (int repetitionIndex = 0; repetitionIndex < repetitionsSize; repetitionIndex++) {
                repetitions.add(buffer.readVarInt());
            }
            int tiersSize = readBoundedSize(buffer, MAX_TIER_CHOICES, "terminal tier choices");
            Object2ObjectMap<String, ResourceLocation> tiers = new Object2ObjectLinkedOpenHashMap<>();
            for (int tierIndex = 0; tierIndex < tiersSize; tierIndex++) {
                String group = buffer.readUtf(MAX_RESOURCE_LOCATION);
                ResourceLocation choice = readResourceLocation(buffer);
                if (tiers.put(group, choice) != null) {
                    throw new IllegalArgumentException("Duplicate terminal tier choice: " + group);
                }
            }
            int choiceCount = readBoundedSize(buffer, MAX_CHOICES, "terminal branch choices");
            Object2ObjectMap<String, String> choices = new Object2ObjectLinkedOpenHashMap<>();
            for (int choiceIndex = 0; choiceIndex < choiceCount; choiceIndex++) {
                String path = buffer.readUtf(MAX_CHOICE_PATH);
                if (choices.put(path, buffer.readUtf(MAX_RESOURCE_LOCATION)) != null) {
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

    private static void writeBoundedSize(RegistryFriendlyByteBuf buffer, int size, int maximum, String name) {
        if (size < 0 || size > maximum) {
            throw new IllegalArgumentException(name + " exceeds wire limit " + maximum);
        }
        buffer.writeVarInt(size);
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
