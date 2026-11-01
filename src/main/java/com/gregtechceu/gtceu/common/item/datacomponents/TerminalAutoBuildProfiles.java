package com.gregtechceu.gtceu.common.item.datacomponents;

import com.gregtechceu.gtceu.api.machine.MultiblockMachineDefinition;
import com.gregtechceu.gtceu.api.multiblock.autobuild.batch.AutoBuildMode;
import com.gregtechceu.gtceu.api.multiblock.autobuild.batch.AutoBuildSharedOptions;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;

import com.mojang.serialization.Codec;
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

    public static final TerminalAutoBuildProfiles EMPTY = new TerminalAutoBuildProfiles(Object2ObjectMaps.emptyMap());

    public static final Codec<TerminalAutoBuildProfiles> CODEC = RecordCodecBuilder
            .<TerminalAutoBuildProfiles>create(instance -> instance
                    .group(
                            Codec.unboundedMap(ResourceLocation.CODEC, TerminalMachineProfile.CODEC)
                                    .<Object2ObjectMap<ResourceLocation, TerminalMachineProfile>>xmap(
                                            Object2ObjectLinkedOpenHashMap::new, value -> value)
                                    .fieldOf("profiles")
                                    .forGetter(TerminalAutoBuildProfiles::profiles))
                    .apply(instance, TerminalAutoBuildProfiles::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, TerminalAutoBuildProfiles> STREAM_CODEC = StreamCodec
            .of(TerminalAutoBuildProfiles::encode, TerminalAutoBuildProfiles::decode);

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
            buffer.writeResourceLocation(definitionId);
            encodeMachine(buffer, profile);
        });
    }

    private static TerminalAutoBuildProfiles decode(RegistryFriendlyByteBuf buffer) {
        int size = buffer.readVarInt();
        Object2ObjectMap<ResourceLocation, TerminalMachineProfile> profiles = new Object2ObjectLinkedOpenHashMap<>();
        for (int index = 0; index < size; index++) {
            ResourceLocation id = buffer.readResourceLocation();
            profiles.put(id, decodeMachine(buffer));
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

    private static TerminalMachineProfile decodeMachine(RegistryFriendlyByteBuf buffer) {
        AutoBuildSharedOptions shared = new AutoBuildSharedOptions(buffer.readBoolean(), buffer.readBoolean(),
                buffer.readBoolean());
        int structuresSize = buffer.readVarInt();
        Object2ObjectMap<String, TerminalStructureProfile> structures = new Object2ObjectLinkedOpenHashMap<>();
        for (int structureIndex = 0; structureIndex < structuresSize; structureIndex++) {
            String name = buffer.readUtf();
            boolean selected = buffer.readBoolean();
            AutoBuildMode mode = buffer.readEnum(AutoBuildMode.class);
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
            structures.put(name, new TerminalStructureProfile(selected, mode, repetitions, flip, tiers, choices));
        }
        return new TerminalMachineProfile(shared, structures);
    }
}
