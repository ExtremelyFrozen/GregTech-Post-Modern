package com.gregtechceu.gtceu.api.multiblock.preview;

import it.unimi.dsi.fastutil.ints.IntAVLTreeSet;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.objects.ObjectImmutableList;
import it.unimi.dsi.fastutil.objects.ObjectList;
import lombok.Getter;
import org.jspecify.annotations.Nullable;

/**
 * Window-local navigation state for a multiblock preview.
 *
 * <p>
 * Focus and layer filters are intentionally kept outside {@link StructurePreview}; changing either must
 * never alter persisted terminal configuration or the material summary.
 * </p>
 */
public final class PreviewVisibility {

    @Getter
    private StructurePreview preview;
    private final IntArrayList layers = new IntArrayList();
    @Getter
    private long viewRevision;
    @Getter
    @Nullable
    private String focusedStructure;
    @Getter
    @Nullable
    private Integer visibleLayer;
    @Getter
    private boolean highlightHatches;

    public PreviewVisibility(StructurePreview preview) {
        this.preview = preview;
        updateLayers();
    }

    /** Only changes the window overlay; structure selection and material allocation remain untouched. */
    public void setHighlightHatches(boolean highlightHatches) {
        this.highlightHatches = highlightHatches;
        viewRevision++;
    }

    /**
     * Replaces server-authoritative content while retaining navigation only when it is still valid.
     */
    public void setPreview(StructurePreview preview) {
        this.preview = preview;
        if (focusedStructure != null && preview.structures().stream()
                .noneMatch(structure -> structure.name().equals(focusedStructure))) {
            focusedStructure = null;
        }
        updateLayers();
        viewRevision++;
    }

    private void updateLayers() {
        IntAVLTreeSet available = new IntAVLTreeSet();
        for (var cell : preview.cells()) {
            available.add(cell.relativePos().getY());
        }
        layers.clear();
        layers.addAll(available);
        if (visibleLayer != null && !layers.contains(visibleLayer.intValue())) visibleLayer = null;
    }

    /**
     * Focuses one selected structure; passing {@code null} displays all structures at equal emphasis.
     */
    public void setFocusedStructure(@Nullable String structureName) {
        if (structureName != null && preview.structures().stream()
                .noneMatch(structure -> structure.name().equals(structureName))) {
            throw new IllegalArgumentException("Unknown preview structure: " + structureName);
        }
        focusedStructure = structureName;
        viewRevision++;
    }

    /**
     * Cycles through all structures in definition order and then returns to the merged view.
     */
    public void focusNext() {
        boolean next = focusedStructure == null;
        for (var structure : preview.structures()) {
            if (next) {
                setFocusedStructure(structure.name());
                return;
            }
            if (structure.name().equals(focusedStructure)) next = true;
        }
        setFocusedStructure(null);
    }

    /**
     * Cycles through controller-relative Y layers and then returns to the all-layer view.
     */
    public void layerNext() {
        int index = visibleLayer == null ? -1 : layers.indexOf(visibleLayer.intValue());
        visibleLayer = index + 1 < layers.size() ? layers.getInt(index + 1) : null;
        viewRevision++;
    }

    /**
     * Returns cells in the active layer; focus only changes emphasis.
     */
    public ObjectList<StructurePreview.Cell> visibleCells() {
        if (visibleLayer == null) {
            return preview.cells();
        }
        return preview.cells().stream()
                .filter(cell -> cell.relativePos().getY() == visibleLayer)
                .collect(ObjectImmutableList.toList());
    }

    /**
     * Whether a cell belongs to the focused structure and should be rendered at full emphasis.
     */
    public boolean isFocused(StructurePreview.Cell cell) {
        return focusedStructure == null || cell.contributorNames().contains(focusedStructure);
    }
}
