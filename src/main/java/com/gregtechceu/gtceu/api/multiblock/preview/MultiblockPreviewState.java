package com.gregtechceu.gtceu.api.multiblock.preview;

import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * Window-local navigation state for a multiblock preview.
 *
 * <p>
 * Focus and layer filters are intentionally kept outside {@link MultiblockPreviewSnapshot}; changing either must
 * never alter persisted terminal configuration or the material summary.
 * </p>
 */
public final class MultiblockPreviewState {

    private MultiblockPreviewSnapshot snapshot;
    @Nullable
    private String focusedStructure;
    @Nullable
    private Integer visibleLayer;

    public MultiblockPreviewState(MultiblockPreviewSnapshot snapshot) {
        this.snapshot = snapshot;
    }

    public MultiblockPreviewSnapshot snapshot() {
        return snapshot;
    }

    /**
     * Replaces server-authoritative content while retaining navigation only when it is still valid.
     */
    public void replaceSnapshot(MultiblockPreviewSnapshot snapshot) {
        this.snapshot = snapshot;
        if (focusedStructure != null && snapshot.structures().stream()
                .noneMatch(structure -> structure.name().equals(focusedStructure))) {
            focusedStructure = null;
        }
        if (visibleLayer != null && snapshot.cells().stream()
                .noneMatch(cell -> cell.relativePos().getY() == visibleLayer)) {
            visibleLayer = null;
        }
    }

    @Nullable
    public String focusedStructure() {
        return focusedStructure;
    }

    /**
     * Focuses one selected structure; passing {@code null} displays all structures at equal emphasis.
     */
    public void focus(@Nullable String structureName) {
        if (structureName != null && snapshot.structures().stream()
                .noneMatch(structure -> structure.name().equals(structureName))) {
            throw new IllegalArgumentException("Unknown preview structure: " + structureName);
        }
        focusedStructure = structureName;
    }

    /**
     * Cycles through all structures in definition order and then returns to the merged view.
     */
    public void focusNext() {
        if (snapshot.structures().isEmpty()) {
            focusedStructure = null;
            return;
        }
        if (focusedStructure == null) {
            focusedStructure = snapshot.structures().getFirst().name();
            return;
        }
        for (int index = 0; index < snapshot.structures().size(); index++) {
            if (snapshot.structures().get(index).name().equals(focusedStructure)) {
                focusedStructure = index + 1 < snapshot.structures().size() ?
                        snapshot.structures().get(index + 1).name() : null;
                return;
            }
        }
        focusedStructure = null;
    }

    @Nullable
    public Integer visibleLayer() {
        return visibleLayer;
    }

    /**
     * Cycles through controller-relative Y layers and then returns to the all-layer view.
     */
    public void layerNext() {
        List<Integer> layers = snapshot.cells().stream()
                .map(cell -> cell.relativePos().getY())
                .distinct()
                .sorted()
                .toList();
        if (layers.isEmpty()) {
            visibleLayer = null;
            return;
        }
        if (visibleLayer == null) {
            visibleLayer = layers.getFirst();
            return;
        }
        int index = layers.indexOf(visibleLayer);
        if (index >= 0 && index + 1 < layers.size()) {
            visibleLayer = layers.get(index + 1);
            return;
        }
        visibleLayer = null;
    }

    /**
     * Returns cells in the active layer. Focus changes emphasis in the widget, so it does not remove other cells.
     */
    public List<MultiblockPreviewSnapshot.Cell> visibleCells() {
        if (visibleLayer == null) {
            return snapshot.cells();
        }
        return snapshot.cells().stream()
                .filter(cell -> cell.relativePos().getY() == visibleLayer)
                .toList();
    }

    /**
     * Whether a cell belongs to the focused structure and should be rendered at full emphasis.
     */
    public boolean isFocused(MultiblockPreviewSnapshot.Cell cell) {
        return focusedStructure == null || cell.contributorNames().contains(focusedStructure);
    }
}
