package com.gregtechceu.gtceu.common.item.datacomponents;

import com.gregtechceu.gtceu.GTCEu;
import com.gregtechceu.gtceu.api.multiblock.autobuild.batch.AutoBuildMode;

import net.minecraft.resources.ResourceLocation;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.ints.IntLists;
import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectMaps;
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

    public static final Codec<TerminalStructureProfile> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.BOOL.fieldOf("selected").forGetter(TerminalStructureProfile::selected),
            MODE_CODEC.fieldOf("mode").forGetter(TerminalStructureProfile::mode),
            Codec.INT.listOf().<IntList>xmap(IntArrayList::new, value -> value).fieldOf("repetitions")
                    .forGetter(TerminalStructureProfile::repetitions),
            Codec.BOOL.fieldOf("flip").forGetter(TerminalStructureProfile::flipMode),
            Codec.unboundedMap(Codec.STRING.xmap(
                    group -> group.startsWith(GTCEu.MOD_ID + ":") ? group.substring(GTCEu.MOD_ID.length() + 1) : group,
                    group -> group), ResourceLocation.CODEC)
                    .<Object2ObjectMap<String, ResourceLocation>>xmap(Object2ObjectLinkedOpenHashMap::new,
                            value -> value)
                    .fieldOf("tiers")
                    .forGetter(TerminalStructureProfile::tierChoices),
            Codec.unboundedMap(Codec.STRING, Codec.STRING)
                    .<Object2ObjectMap<String, String>>xmap(Object2ObjectLinkedOpenHashMap::new, value -> value)
                    .optionalFieldOf("choices", Object2ObjectMaps.emptyMap())
                    .forGetter(TerminalStructureProfile::choices))
            .apply(instance, TerminalStructureProfile::new));

    public TerminalStructureProfile {
        repetitions = IntLists.unmodifiable(new IntArrayList(repetitions));
        if (repetitions.size() > MAX_REPETITIONS || repetitions.intStream().anyMatch(value -> value < 0)) {
            throw new IllegalArgumentException("Terminal repetitions must be bounded and non-negative");
        }
        if (tierChoices.size() > MAX_TIER_CHOICES) {
            throw new IllegalArgumentException("Terminal tier choices exceed " + MAX_TIER_CHOICES);
        }
        Object2ObjectMap<String, ResourceLocation> ordered = new Object2ObjectLinkedOpenHashMap<>();
        tierChoices.forEach((group, choice) -> {
            if (group.isBlank() || group.length() > MAX_RESOURCE_LOCATION ||
                    choice.toString().length() > MAX_RESOURCE_LOCATION) {
                throw new IllegalArgumentException("Terminal tier IDs must be bounded");
            }
            ordered.put(group, choice);
        });
        tierChoices = Object2ObjectMaps.unmodifiable(ordered);
        if (choices.size() > MAX_CHOICES) throw new IllegalArgumentException("Too many terminal branch choices");
        choices.forEach((path, branch) -> {
            if (path.isBlank() || path.length() > MAX_CHOICE_PATH || branch.isBlank() ||
                    branch.length() > MAX_RESOURCE_LOCATION) {
                throw new IllegalArgumentException("Terminal choice paths and branches must be non-blank and bounded");
            }
        });
        choices = Object2ObjectMaps.unmodifiable(new Object2ObjectLinkedOpenHashMap<>(choices));
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
