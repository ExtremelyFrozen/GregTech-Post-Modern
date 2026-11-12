package com.gregtechceu.gtceu.client.renderer;

import com.gregtechceu.gtceu.config.ConfigHolder;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

import java.util.Arrays;

/** A client-owned issue selection, shared by Jade and full structure diagnosis. */
public final class StructureIssueOverlay {

    private static Level world;
    private static BlockPos controller;
    private static long[] positions = new long[0];
    private static long expires;
    private static boolean grouped;

    private StructureIssueOverlay() {}

    public static void showIssue(BlockPos anchor, long position) {
        if (grouped && anchor.equals(controller)) return;
        show(anchor, new long[] { position }, false);
    }

    public static void toggleGroup(BlockPos anchor, long[] selected) {
        if (anchor.equals(controller) && grouped && Arrays.equals(positions, selected)) clear();
        else show(anchor, selected.clone(), true);
    }

    private static void show(BlockPos anchor, long[] selected, boolean group) {
        Level level = Minecraft.getInstance().level;
        if (level == null) return;
        world = level;
        controller = anchor.immutable();
        positions = selected;
        grouped = group;
        expires = level.getGameTime() + ConfigHolder.INSTANCE.client.inWorldPreviewDuration * 20L;
    }

    public static void clearIssue(BlockPos anchor) {
        if (!grouped || !anchor.equals(controller)) clear();
    }

    public static void clear() {
        world = null;
        controller = null;
        positions = new long[0];
        grouped = false;
    }

    public static void render(RenderLevelStageEvent event) {
        var minecraft = Minecraft.getInstance();
        if (controller == null) return;
        if (world != minecraft.level || minecraft.player == null || world.getGameTime() > expires ||
                minecraft.player.distanceToSqr(controller.getCenter()) > 192 * 192) {
            clear();
            return;
        }
        var pose = event.getPoseStack();
        var camera = event.getCamera().getPosition();
        var buffers = minecraft.renderBuffers().bufferSource();
        var lines = buffers.getBuffer(RenderType.lines());
        pose.pushPose();
        pose.translate(-camera.x, -camera.y, -camera.z);
        for (long position : positions) {
            BlockPos block = BlockPos.of(position);
            LevelRenderer.renderLineBox(pose, lines, block.getX() - 0.003, block.getY() - 0.003, block.getZ() - 0.003,
                    block.getX() + 1.003, block.getY() + 1.003, block.getZ() + 1.003, 1, 0.25f, 0.25f, 1);
        }
        pose.popPose();
        buffers.endBatch(RenderType.lines());
    }
}
