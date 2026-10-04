package com.gregtechceu.gtceu.common.item.datacomponents;

import com.gregtechceu.gtceu.api.multiblock.autobuild.batch.AutoBuildMode;

import net.minecraft.resources.ResourceLocation;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import org.jspecify.annotations.NullMarked;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Persists all structure-local automatic-build choices for one named pattern. */
@NullMarked
public record TerminalStructureProfile(boolean selected, AutoBuildMode mode, List<Integer> repetitions,
                                       boolean flipMode,
                                       Map<ResourceLocation, ResourceLocation> tierChoices) {

    private static final int MAX_REPETITIONS = 64;
    private static final int MAX_TIER_CHOICES = 128;
    private static final int MAX_RESOURCE_LOCATION = 256;

    private static final Codec<AutoBuildMode> MODE_CODEC = Codec.STRING.comapFlatMap(name -> {
        try {
            return DataResult.success(AutoBuildMode.valueOf(name));
        } catch (IllegalArgumentException exception) {
            return DataResult.error(() -> "Unknown automatic-build mode: " + name);
        }
    }, AutoBuildMode::name);

    public static final Codec<TerminalStructureProfile> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.BOOL.fieldOf("selected").forGetter(TerminalStructureProfile::selected),
            MODE_CODEC.fieldOf("mode").forGetter(TerminalStructureProfile::mode),
            Codec.INT.listOf().fieldOf("repetitions").forGetter(TerminalStructureProfile::repetitions),
            Codec.BOOL.fieldOf("flip").forGetter(TerminalStructureProfile::flipMode),
            Codec.unboundedMap(ResourceLocation.CODEC, ResourceLocation.CODEC).fieldOf("tiers")
                    .forGetter(TerminalStructureProfile::tierChoices))
            .apply(instance, TerminalStructureProfile::new));

    public TerminalStructureProfile {
        repetitions = List.copyOf(repetitions);
        if (repetitions.size() > MAX_REPETITIONS || repetitions.stream().anyMatch(value -> value < 0)) {
            throw new IllegalArgumentException("Terminal repetitions must be bounded and non-negative");
        }
        if (tierChoices.size() > MAX_TIER_CHOICES) {
            throw new IllegalArgumentException("Terminal tier choices exceed " + MAX_TIER_CHOICES);
        }
        LinkedHashMap<ResourceLocation, ResourceLocation> ordered = new LinkedHashMap<>();
        tierChoices.forEach((group, choice) -> {
            if (group.toString().length() > MAX_RESOURCE_LOCATION ||
                    choice.toString().length() > MAX_RESOURCE_LOCATION) {
                throw new IllegalArgumentException("Terminal tier IDs must be bounded");
            }
            ordered.put(group, choice);
        });
        tierChoices = Collections.unmodifiableMap(ordered);
    }

    public TerminalStructureProfile withSelected(boolean value) {
        return new TerminalStructureProfile(value, mode, repetitions, flipMode, tierChoices);
    }

    public TerminalStructureProfile withMode(AutoBuildMode value) {
        return new TerminalStructureProfile(selected, value, repetitions, flipMode, tierChoices);
    }

    public TerminalStructureProfile withRepetitions(List<Integer> value) {
        return new TerminalStructureProfile(selected, mode, value, flipMode, tierChoices);
    }

    public TerminalStructureProfile withFlipMode(boolean value) {
        return new TerminalStructureProfile(selected, mode, repetitions, value, tierChoices);
    }

    public TerminalStructureProfile withTierChoice(ResourceLocation group, ResourceLocation choice) {
        LinkedHashMap<ResourceLocation, ResourceLocation> updated = new LinkedHashMap<>(tierChoices);
        updated.put(group, choice);
        return new TerminalStructureProfile(selected, mode, repetitions, flipMode, updated);
    }
}
