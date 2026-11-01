package com.gregtechceu.gtceu.common.item.datacomponents;

import com.gregtechceu.gtceu.api.multiblock.autobuild.batch.AutoBuildMode;

import net.minecraft.resources.ResourceLocation;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import org.jspecify.annotations.NullMarked;

/** Persists all structure-local automatic-build choices for one named pattern. */
@NullMarked
public record TerminalStructureProfile(boolean selected, AutoBuildMode mode, IntList repetitions,
                                       boolean flipMode,
                                       Object2ObjectMap<String, ResourceLocation> tierChoices,
                                       Object2ObjectMap<String, String> choices) {

    private static final int MAX_REPETITIONS = 64;
    private static final int MAX_TIER_CHOICES = 128;
    private static final int MAX_RESOURCE_LOCATION = 256;
    private static final int MAX_CHOICES = 128;
    private static final int MAX_CHOICE_PATH = 2048;

    private static final Codec<AutoBuildMode> MODE_CODEC = Codec.STRING.comapFlatMap(name -> {
        try {
            return DataResult.success(AutoBuildMode.valueOf(name));
        } catch (IllegalArgumentException exception) {
            return DataResult.error(() -> "Unknown automatic-build mode: " + name);
        }
    }, AutoBuildMode::name);

    public static final Codec<TerminalStructureProfile> CODEC = RecordCodecBuilder
            .<TerminalStructureProfile>create(instance -> instance.group(
            Codec.BOOL.fieldOf("selected").forGetter(TerminalStructureProfile::selected),
            MODE_CODEC.fieldOf("mode").forGetter(TerminalStructureProfile::mode),
            Codec.INT.listOf().<IntList>xmap(IntArrayList::new, value -> value).fieldOf("repetitions")
                    .forGetter(TerminalStructureProfile::repetitions),
            Codec.BOOL.fieldOf("flip").forGetter(TerminalStructureProfile::flipMode),
            Codec.unboundedMap(Codec.STRING, ResourceLocation.CODEC)
                    .<Object2ObjectMap<String, ResourceLocation>>xmap(Object2ObjectLinkedOpenHashMap::new,
                            value -> value)
                    .fieldOf("tiers")
                    .forGetter(TerminalStructureProfile::tierChoices),
            Codec.unboundedMap(Codec.STRING, Codec.STRING)
                    .<Object2ObjectMap<String, String>>xmap(Object2ObjectLinkedOpenHashMap::new, value -> value)
                    .fieldOf("choices")
                    .forGetter(TerminalStructureProfile::choices))
            .apply(instance, TerminalStructureProfile::new))
            .comapFlatMap(TerminalStructureProfile::validateDecoded, value -> value);

    private static DataResult<TerminalStructureProfile> validateDecoded(TerminalStructureProfile value) {
        if (value.repetitions.size() > MAX_REPETITIONS ||
                value.repetitions.intStream().anyMatch(repetition -> repetition < 0)) {
            return DataResult.error(() -> "Terminal repetitions must be bounded and non-negative");
        }
        if (value.tierChoices.size() > MAX_TIER_CHOICES) {
            return DataResult.error(() -> "Terminal tier choices exceed " + MAX_TIER_CHOICES);
        }
        for (var entry : value.tierChoices.object2ObjectEntrySet()) {
            String group = entry.getKey();
            ResourceLocation choice = entry.getValue();
            if (group.isBlank() || group.length() > MAX_RESOURCE_LOCATION ||
                    choice.toString().length() > MAX_RESOURCE_LOCATION) {
                return DataResult.error(() -> "Terminal tier IDs must be bounded");
            }
        }
        if (value.choices.size() > MAX_CHOICES) {
            return DataResult.error(() -> "Too many terminal branch choices");
        }
        for (var entry : value.choices.object2ObjectEntrySet()) {
            String path = entry.getKey();
            String branch = entry.getValue();
            if (path.isBlank() || path.length() > MAX_CHOICE_PATH || branch.isBlank() ||
                    branch.length() > MAX_RESOURCE_LOCATION) {
                return DataResult.error(() -> "Terminal choice paths and branches must be non-blank and bounded");
            }
        }
        return DataResult.success(value);
    }

    public TerminalStructureProfile withSelected(boolean value) {
        return new TerminalStructureProfile(value, mode, repetitions, flipMode, tierChoices, choices);
    }

    public TerminalStructureProfile withMode(AutoBuildMode value) {
        return new TerminalStructureProfile(selected, value, repetitions, flipMode, tierChoices, choices);
    }

    public TerminalStructureProfile withRepetitions(IntList value) {
        return new TerminalStructureProfile(selected, mode, value, flipMode, tierChoices, choices);
    }

    public TerminalStructureProfile withFlipMode(boolean value) {
        return new TerminalStructureProfile(selected, mode, repetitions, value, tierChoices, choices);
    }

    public TerminalStructureProfile withTierChoice(String group, ResourceLocation choice) {
        Object2ObjectMap<String, ResourceLocation> updated = new Object2ObjectLinkedOpenHashMap<>(
                tierChoices);
        updated.put(group, choice);
        return new TerminalStructureProfile(selected, mode, repetitions, flipMode, updated, choices);
    }

    public TerminalStructureProfile withChoice(String path, String branch) {
        Object2ObjectMap<String, String> updated = new Object2ObjectLinkedOpenHashMap<>(choices);
        updated.put(path, branch);
        return new TerminalStructureProfile(selected, mode, repetitions, flipMode, tierChoices, updated);
    }
}
