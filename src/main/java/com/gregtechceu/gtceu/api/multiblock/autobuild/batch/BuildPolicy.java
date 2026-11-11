package com.gregtechceu.gtceu.api.multiblock.autobuild.batch;

import com.gregtechceu.gtceu.api.sync_system.FieldCodecs;

import net.minecraft.resources.ResourceLocation;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import it.unimi.dsi.fastutil.objects.ObjectSet;
import it.unimi.dsi.fastutil.objects.ObjectSets;

/**
 * Options that must remain identical for every structure in one batch.
 *
 * @param replaceMode whether occupied target positions may be replaced
 * @param noHatchMode whether all hatch candidates are filtered, retaining original non-hatch alternatives
 * @param sources     enabled optional material providers, identified by their registry keys
 */
public record BuildPolicy(boolean replaceMode, boolean noHatchMode, ObjectSet<ResourceLocation> sources) {

    public static final BuildPolicy DEFAULT = new BuildPolicy(false, true, ObjectSets.emptySet());

    public static final Codec<BuildPolicy> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.BOOL.fieldOf("replace").forGetter(BuildPolicy::replaceMode),
            Codec.BOOL.fieldOf("no_hatch").forGetter(BuildPolicy::noHatchMode),
            ResourceLocation.CODEC.listOf()
                    .<ObjectSet<ResourceLocation>>xmap(ObjectOpenHashSet::new, ObjectArrayList::new)
                    .optionalFieldOf("sources", ObjectSets.emptySet()).forGetter(BuildPolicy::sources))
            .apply(instance, BuildPolicy::new));

    static {
        FieldCodecs.register(BuildPolicy.class, CODEC);
    }
}
