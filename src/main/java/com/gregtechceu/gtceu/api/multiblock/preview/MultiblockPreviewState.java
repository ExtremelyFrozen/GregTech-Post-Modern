package com.gregtechceu.gtceu.api.multiblock.preview;

import it.unimi.dsi.fastutil.ints.IntAVLTreeSet;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.objects.ObjectImmutableList;
import it.unimi.dsi.fastutil.objects.ObjectList;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import it.unimi.dsi.fastutil.objects.ObjectSet;
import org.jspecify.annotations.Nullable;

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
    private final ObjectSet<String> hiddenStructures = new ObjectOpenHashSet<>();
    private final IntArrayList layers = new IntArrayList();
    private long viewRevision;
    @Nullable
    private String focusedStructure;
    @Nullable
    private Integer visibleLayer;

    public MultiblockPreviewState(MultiblockPreviewSnapshot snapshot) {
        this.snapshot = snapshot;
        updateLayers();
    }

    public MultiblockPreviewSnapshot snapshot() {
        return snapshot;
    }

    /**
     * Replaces server-authoritative content while retaining navigation only when it is still valid.
     */
    public void replaceSnapshot(MultiblockPreviewSnapshot snapshot) {
        this.snapshot = snapshot;
        hiddenStructures.removeIf(name -> snapshot.structures().stream()
                .noneMatch(structure -> structure.name().equals(name)));
        if (focusedStructure != null && snapshot.structures().stream()
                .noneMatch(structure -> structure.name().equals(focusedStructure))) {
            focusedStructure = null;
        }
        updateLayers();
        viewRevision++;
    }

    /** View changes stay local to the open window and never change build selection or material requirements. */
    public boolean isStructureVisible(String structureName) {
        return !hiddenStructures.contains(structureName);
    }

    public void setStructureVisible(String structureName, boolean visible) {
        if (snapshot.structures().stream().noneMatch(structure -> structure.name().equals(structureName))) {
            throw new IllegalArgumentException("Unknown preview structure: " + structureName);
        }
        if (visible) hiddenStructures.remove(structureName);
        else hiddenStructures.add(structureName);
        if (!visible && structureName.equals(focusedStructure)) focusedStructure = null;
        updateLayers();
        viewRevision++;
    }

    public long viewRevision() {
        return viewRevision;
    }

    private void updateLayers() {
        IntAVLTreeSet available = new IntAVLTreeSet();
        for (var cell : snapshot.cells()) {
            if (hasVisibleContributor(cell)) available.add(cell.relativePos().getY());
        }
        layers.clear();
        layers.addAll(available);
        if (visibleLayer != null && !layers.contains(visibleLayer.intValue())) visibleLayer = null;
    }

    private boolean hasVisibleContributor(MultiblockPreviewSnapshot.Cell cell) {
        return cell.contributions().stream().anyMatch(contribution -> isStructureVisible(contribution.structureName()));
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
        viewRevision++;
    }

    /**
     * Cycles through all structures in definition order and then returns to the merged view.
     */
    public void focusNext() {
        boolean next = focusedStructure == null;
        for (var structure : snapshot.structures()) {
            if (next && isStructureVisible(structure.name())) {
                focus(structure.name());
                return;
            }
            if (structure.name().equals(focusedStructure)) next = true;
        }
        focus(null);
    }

    @Nullable
    public Integer visibleLayer() {
        return visibleLayer;
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
     * Returns cells with at least one visible contributor in the active layer. Shared cells remain visible when
     * another visible structure needs them; focus only changes emphasis.
     */
    public ObjectList<MultiblockPreviewSnapshot.Cell> visibleCells() {
        if (visibleLayer == null && hiddenStructures.isEmpty()) {
            return snapshot.cells();
        }
        return snapshot.cells().stream()
                .filter(this::hasVisibleContributor)
                .filter(cell -> visibleLayer == null || cell.relativePos().getY() == visibleLayer)
                .collect(ObjectImmutableList.toList());
    }

    /**
     * Whether a cell belongs to the focused structure and should be rendered at full emphasis.
     */
    public boolean isFocused(MultiblockPreviewSnapshot.Cell cell) {
        return focusedStructure == null || cell.contributorNames().contains(focusedStructure);
    }
}
