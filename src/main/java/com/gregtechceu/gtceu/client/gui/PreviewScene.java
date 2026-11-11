package com.gregtechceu.gtceu.client.gui;

import com.gregtechceu.gtceu.api.gui.element.GTSceneElement;
import com.gregtechceu.gtceu.api.multiblock.MultiblockPreviewLevel;
import com.gregtechceu.gtceu.client.renderer.preview.PreviewRenderer;

import com.lowdragmc.lowdraglib2.client.scene.ISceneBlockRenderHook;
import com.lowdragmc.lowdraglib2.client.scene.WorldSceneRenderer;
import com.lowdragmc.lowdraglib2.utils.data.BlockPosFace;

import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import it.unimi.dsi.fastutil.longs.LongList;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/** Preview-only scene: LDLib camera controls with packed positions and cached geometry. */
@OnlyIn(Dist.CLIENT)
@NullMarked
public final class PreviewScene extends GTSceneElement {

    private final LongSet visible = new LongOpenHashSet();

    public PreviewScene(int x, int y, int width, int height) {
        super(x, y, width, height);
        setTickWorld(false);
    }

    public void setLevel(MultiblockPreviewLevel world) {
        releaseRendererResource();
        visible.clear();
        level = dummyWorld = world;
        renderer = new PreviewRenderer(world, this::renderBlockOverLay);
        world.setBlockFilter(pos -> visible.contains(pos.asLong()));
        renderer.useOrtho(useOrtho);
        renderer.setClipBlock(clipBlock);
        renderer.setClipFluid(clipFluid);
        lastClickPosFace = lastHoverPosFace = lastSelectedPosFace = null;
        lastHoverItem = null;
    }

    /** The supplied positions remain unchanged while the renderer compiles them across frames. */
    public void setBlocks(LongList positions, @Nullable ISceneBlockRenderHook hook, boolean resetCamera) {
        visible.clear();
        visible.addAll(positions);
        int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE, minZ = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE, maxZ = Integer.MIN_VALUE;
        for (int i = 0; i < positions.size(); i++) {
            long pos = positions.getLong(i);
            minX = Math.min(minX, BlockPos.getX(pos));
            minY = Math.min(minY, BlockPos.getY(pos));
            minZ = Math.min(minZ, BlockPos.getZ(pos));
            maxX = Math.max(maxX, BlockPos.getX(pos));
            maxY = Math.max(maxY, BlockPos.getY(pos));
            maxZ = Math.max(maxZ, BlockPos.getZ(pos));
        }
        var bounds = positions.isEmpty() ? new AABB(0, 0, 0, 0, 0, 0) :
                new AABB(minX, minY, minZ, maxX + 1, maxY + 1, maxZ + 1);
        center = bounds.getCenter().toVector3f();
        ((PreviewRenderer) renderer).setBlocks(positions, bounds, hook);
        if (resetCamera) {
            zoom = (float) (3.5 * Math.sqrt(Math.max(1, Math.max(bounds.getXsize(),
                    Math.max(bounds.getYsize(), bounds.getZsize())))));
            renderer.setCameraOrtho(range * zoom, range * zoom, range * zoom);
            renderer.setCameraLookAt(center, camZoom(), Math.toRadians(rotationYaw), Math.toRadians(rotationPitch));
        }
    }

    @Override
    public void renderBlockOverLay(WorldSceneRenderer renderer) {
        lastHoverPosFace = null;
        var hit = renderer.getLastTraceResult();
        if (isHover() && hit != null && hit.getType() == HitResult.Type.BLOCK &&
                visible.contains(hit.getBlockPos().asLong())) {
            lastHoverPosFace = new BlockPosFace(hit.getBlockPos(), hit.getDirection());
        }
        if (afterWorldRender != null) afterWorldRender.accept(this);
    }
}
