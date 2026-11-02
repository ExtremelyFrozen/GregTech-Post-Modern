package com.gregtechceu.gtceu.api.multiblock.autobuild.batch;

/**
 * Options that must remain identical for every structure in one batch.
 *
 * @param replaceMode whether occupied target positions may be replaced
 * @param noHatchMode whether hatches are filtered from multi-rule positions, allowing single-rule positions
 * @param useME       whether a requested ME source is mandatory for build entries
 */
public record BuildPolicy(boolean replaceMode, boolean noHatchMode, boolean useME) {

    public static final BuildPolicy DEFAULT = new BuildPolicy(false, true, false);
}
