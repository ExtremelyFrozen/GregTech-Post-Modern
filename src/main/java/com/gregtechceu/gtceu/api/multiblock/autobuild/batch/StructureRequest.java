package com.gregtechceu.gtceu.api.multiblock.autobuild.batch;

import com.gregtechceu.gtceu.api.sync_system.FieldCodecs;

import net.minecraft.resources.ResourceLocation;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;

/**
 * User-selected behavior for one named structure.
 *
 * @param structureName selected definition structure
 * @param repetitions   one exact repetition count per pattern unit
 * @param tierChoices   tier group name to exact block id selections
 * @param flipMode      whether the pattern transform is flipped
 * @param mode          whether this structure is built or demolished
 */
public record StructureRequest(String structureName, IntList repetitions,
                               Object2ObjectMap<String, ResourceLocation> tierChoices,
                               boolean flipMode, StructureOperation mode,
                               Object2ObjectMap<String, String> choices) {

    public static final Codec<StructureRequest> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("name").forGetter(StructureRequest::structureName),
            Codec.INT.listOf().<IntList>xmap(IntArrayList::new, value -> value).fieldOf("repetitions")
                    .forGetter(StructureRequest::repetitions),
            Codec.unboundedMap(Codec.STRING, ResourceLocation.CODEC)
                    .<Object2ObjectMap<String, ResourceLocation>>xmap(Object2ObjectLinkedOpenHashMap::new,
                            value -> value)
                    .fieldOf("tiers").forGetter(StructureRequest::tierChoices),
            Codec.BOOL.fieldOf("flip").forGetter(StructureRequest::flipMode),
            StructureOperation.CODEC.fieldOf("mode").forGetter(StructureRequest::mode),
            Codec.unboundedMap(Codec.STRING, Codec.STRING)
                    .<Object2ObjectMap<String, String>>xmap(Object2ObjectLinkedOpenHashMap::new, value -> value)
                    .fieldOf("choices").forGetter(StructureRequest::choices))
            .apply(instance, StructureRequest::new));

    static {
        FieldCodecs.register(StructureRequest.class, CODEC);
    }

    public StructureRequest withRepetitions(IntList values) {
        return new StructureRequest(structureName, values, tierChoices, flipMode, mode, choices);
    }

    public StructureRequest withTierChoice(String group, ResourceLocation id) {
        var values = new Object2ObjectLinkedOpenHashMap<>(tierChoices);
        values.put(group, id);
        return new StructureRequest(structureName, repetitions, values, flipMode, mode, choices);
    }

    public StructureRequest withFlipMode(boolean value) {
        return new StructureRequest(structureName, repetitions, tierChoices, value, mode, choices);
    }

    public StructureRequest withMode(StructureOperation value) {
        return new StructureRequest(structureName, repetitions, tierChoices, flipMode, value, choices);
    }

    public StructureRequest withChoice(String path, String branch) {
        var values = new Object2ObjectLinkedOpenHashMap<>(choices);
        values.put(path, branch);
        return new StructureRequest(structureName, repetitions, tierChoices, flipMode, mode, values);
    }
}
