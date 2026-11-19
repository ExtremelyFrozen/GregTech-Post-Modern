package com.gregtechceu.gtceu.client.renderer.preview;

import com.gregtechceu.gtceu.GTCEu;
import com.gregtechceu.gtceu.api.multiblock.MultiblockPreviewLevel;

import com.lowdragmc.lowdraglib2.client.scene.ISceneBlockRenderHook;
import com.lowdragmc.lowdraglib2.client.scene.ImmediateWorldSceneRenderer;
import com.lowdragmc.lowdraglib2.client.scene.WorldSceneRenderer;
import com.lowdragmc.lowdraglib2.math.PositionedRect;

import net.minecraft.client.Minecraft;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import it.unimi.dsi.fastutil.longs.LongList;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.function.Consumer;

/** Cached preview geometry and CPU picking; all state belongs to the render thread. */
@OnlyIn(Dist.CLIENT)
@NullMarked
public final class PreviewRenderer extends ImmediateWorldSceneRenderer {

    private final Consumer<WorldSceneRenderer> afterRender;
    private final PoseStack pose = new PoseStack();
    private final Matrix4f transform = new Matrix4f();
    private final Matrix4f previousTransform = new Matrix4f().zero();
    private final Matrix4f inverse = new Matrix4f();
    private final int[] viewport = new int[4];
    private final Vector3f near = new Vector3f();
    private final Vector3f far = new Vector3f();
    private @Nullable PreviewMesh mesh;
    private @Nullable BlockHitResult hit;
    private AABB bounds = new AABB(0, 0, 0, 0, 0, 0);
    private boolean traceDirty = true;
    private int mouseX;
    private int mouseY;

    public PreviewRenderer(MultiblockPreviewLevel level, Consumer<WorldSceneRenderer> afterRender) {
        super(level);
        this.afterRender = afterRender;
        setOnLookingAt(ignored -> {});
    }

    public void setBlocks(LongList positions, AABB bounds, @Nullable ISceneBlockRenderHook hook) {
        if (mesh != null) mesh.close();
        mesh = new PreviewMesh((MultiblockPreviewLevel) world, positions, hook, 1);
        this.bounds = bounds.inflate(0.01);
        traceDirty = true;
    }

    @Override
    protected void setupCamera(PositionedRect rectangle) {
        super.setupCamera(rectangle);
        transform.set(RenderSystem.getProjectionMatrix()).mul(RenderSystem.getModelViewMatrix());
        if (!transform.equals(previousTransform)) {
            previousTransform.set(transform);
            transform.invert(inverse);
            traceDirty = true;
        }
        if (viewport[0] != rectangle.position.x || viewport[1] != rectangle.position.y ||
                viewport[2] != rectangle.size.width || viewport[3] != rectangle.size.height) {
            viewport[0] = rectangle.position.x;
            viewport[1] = rectangle.position.y;
            viewport[2] = rectangle.size.width;
            viewport[3] = rectangle.size.height;
            traceDirty = true;
        }
    }

    @Override
    public Vector3f unProject(int mouseX, int mouseY, boolean checkDepth) {
        if (traceDirty || this.mouseX != mouseX || this.mouseY != mouseY) {
            this.mouseX = mouseX;
            this.mouseY = mouseY;
            inverse.unprojectInv(mouseX, mouseY, 0, viewport, near);
            inverse.unprojectInv(mouseX, mouseY, 1, viewport, far);
            traceDirty = true;
        }
        return far;
    }

    @Override
    public @Nullable BlockHitResult rayTrace(Vector3f hitPos) {
        if (!traceDirty) return hit;
        traceDirty = false;
        Vec3 from = new Vec3(near);
        Vec3 to = new Vec3(far);
        var entry = bounds.clip(from, to);
        boolean inside = bounds.contains(from);
        if (!inside && entry.isEmpty()) {
            hit = null;
            return null;
        }
        Vec3 direction = to.subtract(from).normalize().scale(0.001);
        var exit = bounds.clip(to, from);
        hit = world.clip(new ClipContext((inside ? from : entry.orElseThrow()).subtract(direction),
                exit.orElse(to).add(direction), clipBlock, clipFluid, cameraEntity));
        return hit;
    }

    @Override
    protected void drawWorld() {
        if (mesh != null) {
            try {
                if (mesh.compile(RenderSystem.getModelViewMatrix())) {
                    mesh.render(RenderSystem.getModelViewMatrix(), RenderSystem.getProjectionMatrix(),
                            pose, Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(false));
                }
            } catch (RuntimeException exception) {
                GTCEu.LOGGER.error("Could not render multiblock preview", exception);
                mesh.close();
                mesh = null;
            }
        }
        afterRender.accept(this);
    }

    @Override
    public void releaseResource() {
        if (mesh != null) {
            mesh.close();
            mesh = null;
        }
    }
}
