package com.gregtechceu.gtceu.common.item.datacomponents;

import com.gregtechceu.gtceu.api.multiblock.autobuild.batch.StructureOperation;
import com.gregtechceu.gtceu.api.sync_system.FieldCodecs;

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
public record TerminalStructure(boolean selected, StructureOperation mode, IntList repetitions,
                                boolean flipMode,
                                Object2ObjectMap<String, ResourceLocation> tierChoices,
                                Object2ObjectMap<String, String> choices) {

    private static final Codec<StructureOperation> MODE_CODEC = Codec.STRING.comapFlatMap(name -> {
        try {
            return DataResult.success(StructureOperation.valueOf(name));
        } catch (IllegalArgumentException exception) {
            return DataResult.error(() -> "Unknown automatic-build mode: " + name);
        }
    }, StructureOperation::name);

    public static final Codec<TerminalStructure> CODEC = RecordCodecBuilder
            .<TerminalStructure>create(instance -> instance.group(
                    Codec.BOOL.fieldOf("selected").forGetter(TerminalStructure::selected),
                    MODE_CODEC.fieldOf("mode").forGetter(TerminalStructure::mode),
                    Codec.INT.listOf().<IntList>xmap(IntArrayList::new, value -> value).fieldOf("repetitions")
                            .forGetter(TerminalStructure::repetitions),
                    Codec.BOOL.fieldOf("flip").forGetter(TerminalStructure::flipMode),
                    Codec.unboundedMap(Codec.STRING, ResourceLocation.CODEC)
                            .<Object2ObjectMap<String, ResourceLocation>>xmap(Object2ObjectLinkedOpenHashMap::new,
                                    value -> value)
                            .fieldOf("tiers")
                            .forGetter(TerminalStructure::tierChoices),
                    Codec.unboundedMap(Codec.STRING, Codec.STRING)
                            .<Object2ObjectMap<String, String>>xmap(Object2ObjectLinkedOpenHashMap::new, value -> value)
                            .fieldOf("choices")
                            .forGetter(TerminalStructure::choices))
                    .apply(instance, TerminalStructure::new));

    static {
        FieldCodecs.register(TerminalStructure.class, CODEC);
    }

    public TerminalStructure withSelected(boolean value) {
        return new TerminalStructure(value, mode, repetitions, flipMode, tierChoices, choices);
    }

    public TerminalStructure withMode(StructureOperation value) {
        return new TerminalStructure(selected, value, repetitions, flipMode, tierChoices, choices);
    }

    public TerminalStructure withRepetitions(IntList value) {
        return new TerminalStructure(selected, mode, value, flipMode, tierChoices, choices);
    }

    public TerminalStructure withFlipMode(boolean value) {
        return new TerminalStructure(selected, mode, repetitions, value, tierChoices, choices);
    }

    public TerminalStructure withTierChoice(String group, ResourceLocation choice) {
        Object2ObjectMap<String, ResourceLocation> updated = new Object2ObjectLinkedOpenHashMap<>(
                tierChoices);
        updated.put(group, choice);
        return new TerminalStructure(selected, mode, repetitions, flipMode, updated, choices);
    }

    public TerminalStructure withChoice(String path, String branch) {
        Object2ObjectMap<String, String> updated = new Object2ObjectLinkedOpenHashMap<>(choices);
        updated.put(path, branch);
        return new TerminalStructure(selected, mode, repetitions, flipMode, tierChoices, updated);
    }
}
