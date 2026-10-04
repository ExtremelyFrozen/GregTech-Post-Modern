package com.gregtechceu.gtceu.api.multiblock.autobuild.batch;

/**
 * Options that must remain identical for every structure in one batch.
 *
 * @param replaceMode whether occupied target positions may be replaced
 * @param noHatchMode whether the resolver should minimize optional hatches
 * @param useME       whether a requested ME source is mandatory for build entries
 */
public record AutoBuildSharedOptions(boolean replaceMode, boolean noHatchMode, boolean useME) {

    public static final AutoBuildSharedOptions DEFAULT = new AutoBuildSharedOptions(false, true, false);
}
