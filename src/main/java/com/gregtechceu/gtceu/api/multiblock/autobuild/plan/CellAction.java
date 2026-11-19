package com.gregtechceu.gtceu.api.multiblock.autobuild.plan;

/**
 * Describes the exact world operation represented by a resolved pattern cell.
 */
public enum CellAction {

    CONTROLLER,
    KEEP,
    UPDATE_DIRECTION,
    PLACE,
    CLEAR_FOR_AIR,
    DEMOLISH_CANDIDATE,
    IGNORE_ANY
}
