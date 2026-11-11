package com.gregtechceu.gtceu.api.multiblock.index;

import com.gregtechceu.gtceu.api.multiblock.MultiblockState;
import com.gregtechceu.gtceu.api.multiblock.MultiblockWorldSavedData;

import net.minecraft.core.BlockPos;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import org.jspecify.annotations.Nullable;

import java.util.Comparator;

/** Sparse interval tree for unformed structures; even huge repeat ranges allocate one region per structure. */
public final class StructureSearchIndex {

    private final Reference2ObjectOpenHashMap<MultiblockState, Region> regions = new Reference2ObjectOpenHashMap<>();
    private @Nullable Node root;
    private boolean dirty;

    public boolean contains(MultiblockState state) {
        return regions.containsKey(state);
    }

    public void add(MultiblockState state, int[] radii) {
        int radius = Math.max(radii[0], Math.max(radii[1], radii[2]));
        Region region = new Region(state, (long) state.controllerPos.getX() - radius,
                (long) state.controllerPos.getX() + radius, (long) state.controllerPos.getY() - radius,
                (long) state.controllerPos.getY() + radius, (long) state.controllerPos.getZ() - radius,
                (long) state.controllerPos.getZ() + radius);
        if (!region.equals(regions.put(state, region))) dirty = true;
    }

    public void remove(MultiblockState state) {
        if (regions.remove(state) != null) dirty = true;
    }

    public void changed(BlockPos position, MultiblockWorldSavedData data) {
        if (dirty) {
            ObjectArrayList<Region> sorted = new ObjectArrayList<>(regions.values());
            sorted.sort(Comparator.comparingLong(Region::minimumX));
            root = build(sorted, 0, sorted.size());
            dirty = false;
        }
        visit(root, position, data);
    }

    private static @Nullable Node build(ObjectArrayList<Region> sorted, int start, int end) {
        if (start == end) return null;
        int middle = (start + end) >>> 1;
        return new Node(sorted.get(middle), build(sorted, start, middle), build(sorted, middle + 1, end));
    }

    private static void visit(@Nullable Node node, BlockPos position, MultiblockWorldSavedData data) {
        if (node == null || position.getX() > node.maximumX) return;
        Region region = node.region;
        visit(node.left, position, data);
        if (position.getX() < region.minimumX) return;
        if (position.getX() <= region.maximumX && position.getY() >= region.minimumY &&
                position.getY() <= region.maximumY && position.getZ() >= region.minimumZ &&
                position.getZ() <= region.maximumZ) {
            data.queueChangedPosition(region.state, position.asLong());
        }
        visit(node.right, position, data);
    }

    public void clear() {
        regions.clear();
        root = null;
        dirty = false;
    }

    private record Region(MultiblockState state, long minimumX, long maximumX, long minimumY, long maximumY,
                          long minimumZ, long maximumZ) {}

    private static final class Node {

        private final Region region;
        private final @Nullable Node left;
        private final @Nullable Node right;
        private final long maximumX;

        private Node(Region region, @Nullable Node left, @Nullable Node right) {
            this.region = region;
            this.left = left;
            this.right = right;
            maximumX = Math.max(region.maximumX, Math.max(left == null ? Long.MIN_VALUE : left.maximumX,
                    right == null ? Long.MIN_VALUE : right.maximumX));
        }
    }
}
