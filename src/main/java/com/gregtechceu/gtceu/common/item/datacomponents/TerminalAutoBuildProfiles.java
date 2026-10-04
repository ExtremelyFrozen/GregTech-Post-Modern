package com.gregtechceu.gtceu.common.item.datacomponents;

import com.gregtechceu.gtceu.api.machine.MultiblockMachineDefinition;
import com.gregtechceu.gtceu.api.multiblock.autobuild.AutoBuildOptions;
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
import org.jspecify.annotations.NullMarked;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Stores independent automatic-build profiles by multiblock machine definition.
 *
 * <p>
 * The codec recognizes the former single-structure format only to reset it safely; legacy values are not
 * migrated into the new profile model.
 * </p>
 */
@NullMarked
public record TerminalAutoBuildProfiles(Map<ResourceLocation, TerminalMachineProfile> profiles) {

    private static final int MAX_MACHINES = 256;
    private static final int MAX_STRUCTURES = 128;
    private static final int MAX_REPETITIONS = 64;
    private static final int MAX_TIER_CHOICES = 128;
    private static final int MAX_STRUCTURE_NAME = 128;
    private static final int MAX_RESOURCE_LOCATION = 256;

    public static final TerminalAutoBuildProfiles EMPTY = new TerminalAutoBuildProfiles(Map.of());

    private static final Codec<TerminalAutoBuildProfiles> CURRENT_CODEC = RecordCodecBuilder.create(instance -> instance
            .group(
                    Codec.unboundedMap(ResourceLocation.CODEC, TerminalMachineProfile.CODEC).fieldOf("profiles")
                            .forGetter(TerminalAutoBuildProfiles::profiles))
            .apply(instance, TerminalAutoBuildProfiles::new));

    private static final Codec<LegacyConfig> LEGACY_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.optionalFieldOf("structure_name", "main").forGetter(LegacyConfig::structureName),
            AutoBuildOptions.CODEC.optionalFieldOf("options", AutoBuildOptions.DEFAULT)
                    .forGetter(LegacyConfig::options))
            .apply(instance, LegacyConfig::new));

    private static final Decoder<TerminalAutoBuildProfiles> DECODER = new Decoder<>() {

        @Override
        public <T> DataResult<Pair<TerminalAutoBuildProfiles, T>> decode(DynamicOps<T> ops, T input) {
            return ops.getMap(input).flatMap(map -> {
                if (map.get("profiles") != null) {
                    return CURRENT_CODEC.decode(ops, input);
                }
                if (map.get("structure_name") != null || map.get("options") != null) {
                    return LEGACY_CODEC.decode(ops, input).map(pair -> Pair.of(EMPTY, pair.getSecond()));
                }
                return DataResult.error(() -> "Terminal auto-build data is neither current nor legacy format");
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
        LinkedHashMap<ResourceLocation, TerminalMachineProfile> ordered = new LinkedHashMap<>();
        profiles.forEach((definitionId, profile) -> {
            if (definitionId.toString().length() > MAX_RESOURCE_LOCATION) {
                throw new IllegalArgumentException("Terminal machine definition ID is unbounded");
            }
            ordered.put(definitionId, profile);
        });
        profiles = Collections.unmodifiableMap(ordered);
    }

    public TerminalMachineProfile getOrCreate(MultiblockMachineDefinition definition) {
        return profiles.getOrDefault(definition.getId(), TerminalMachineProfile.defaults(definition));
    }

    public TerminalAutoBuildProfiles withProfile(ResourceLocation definitionId, TerminalMachineProfile profile) {
        LinkedHashMap<ResourceLocation, TerminalMachineProfile> updated = new LinkedHashMap<>(profiles);
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
        LinkedHashMap<ResourceLocation, TerminalMachineProfile> profiles = new LinkedHashMap<>();
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
            structure.repetitions().forEach(buffer::writeVarInt);
            writeBoundedSize(buffer, structure.tierChoices().size(), MAX_TIER_CHOICES, "terminal tier choices");
            structure.tierChoices().forEach((group, choice) -> {
                writeResourceLocation(buffer, group);
                writeResourceLocation(buffer, choice);
            });
        });
    }

    private static TerminalMachineProfile decodeMachine(RegistryFriendlyByteBuf buffer) {
        AutoBuildSharedOptions shared = new AutoBuildSharedOptions(buffer.readBoolean(), buffer.readBoolean(),
                buffer.readBoolean());
        int structuresSize = readBoundedSize(buffer, MAX_STRUCTURES, "terminal structures");
        LinkedHashMap<String, TerminalStructureProfile> structures = new LinkedHashMap<>();
        for (int structureIndex = 0; structureIndex < structuresSize; structureIndex++) {
            String name = buffer.readUtf(MAX_STRUCTURE_NAME);
            boolean selected = buffer.readBoolean();
            AutoBuildMode mode = buffer.readEnum(AutoBuildMode.class);
            boolean flip = buffer.readBoolean();
            int repetitionsSize = readBoundedSize(buffer, MAX_REPETITIONS, "terminal repetitions");
            List<Integer> repetitions = new ArrayList<>(repetitionsSize);
            for (int repetitionIndex = 0; repetitionIndex < repetitionsSize; repetitionIndex++) {
                repetitions.add(buffer.readVarInt());
            }
            int tiersSize = readBoundedSize(buffer, MAX_TIER_CHOICES, "terminal tier choices");
            LinkedHashMap<ResourceLocation, ResourceLocation> tiers = new LinkedHashMap<>();
            for (int tierIndex = 0; tierIndex < tiersSize; tierIndex++) {
                ResourceLocation group = readResourceLocation(buffer);
                ResourceLocation choice = readResourceLocation(buffer);
                if (tiers.put(group, choice) != null) {
                    throw new IllegalArgumentException("Duplicate terminal tier choice: " + group);
                }
            }
            if (structures.put(name, new TerminalStructureProfile(selected, mode, repetitions, flip, tiers)) != null) {
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

    private record LegacyConfig(String structureName, AutoBuildOptions options) {}
}
