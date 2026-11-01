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
                    .apply(instance, TerminalStructureProfile::new));

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
