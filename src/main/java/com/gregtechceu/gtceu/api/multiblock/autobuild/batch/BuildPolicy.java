package com.gregtechceu.gtceu.api.multiblock.autobuild.batch;

import com.gregtechceu.gtceu.api.sync_system.FieldCodecs;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * Options that must remain identical for every structure in one batch.
 *
 * @param replaceMode whether occupied target positions may be replaced
 * @param noHatchMode whether all hatch candidates are filtered, retaining original non-hatch alternatives
 * @param useME       whether a requested ME source is mandatory for build entries
 */
public record BuildPolicy(boolean replaceMode, boolean noHatchMode, boolean useME) {

    public static final BuildPolicy DEFAULT = new BuildPolicy(false, true, false);

    public static final Codec<BuildPolicy> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.BOOL.fieldOf("replace").forGetter(BuildPolicy::replaceMode),
            Codec.BOOL.fieldOf("no_hatch").forGetter(BuildPolicy::noHatchMode),
            Codec.BOOL.fieldOf("use_me").forGetter(BuildPolicy::useME))
            .apply(instance, BuildPolicy::new));

    static {
        FieldCodecs.register(BuildPolicy.class, CODEC);
    }
}
