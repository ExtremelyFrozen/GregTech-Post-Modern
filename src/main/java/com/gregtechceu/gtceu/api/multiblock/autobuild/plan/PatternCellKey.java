package com.gregtechceu.gtceu.api.multiblock.autobuild.plan;

/**
 * Stable address of one expanded pattern cell.
 *
 * @param unitIndex  repeatable pattern unit index
 * @param repetition zero-based repetition within the unit
 * @param innerSlice slice within the unit
 * @param row        pattern row index
 * @param column     pattern column index
 */
public record PatternCellKey(int unitIndex, int repetition, int innerSlice, int row, int column) {

    public PatternCellKey {
        if (unitIndex < 0 || repetition < 0 || innerSlice < 0 || row < 0 || column < 0) {
            throw new IllegalArgumentException("Pattern cell indexes must be non-negative");
        }
    }
}
