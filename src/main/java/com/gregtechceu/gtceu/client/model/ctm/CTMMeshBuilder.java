package com.gregtechceu.gtceu.client.model.ctm;

import com.gregtechceu.gtceu.client.model.quad.MeshBuilder;
import com.gregtechceu.gtceu.client.model.quad.MutableQuadView;

import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;

import org.joml.Vector2f;
import org.joml.Vector3f;

import java.util.LinkedList;
import java.util.List;

import static com.gregtechceu.gtceu.client.model.quad.MutableQuadView.*;
import static com.gregtechceu.gtceu.client.util.ModelEventHelper.*;

public class CTMMeshBuilder {

    public static List<BakedQuad> buildCTMQuads(BlockAndTintGetter level, BlockPos pos, BlockState state,
                                                List<BakedQuad> quads, Direction cullFace) {
        TextureConnections connections = TextureConnections.getInstance();
        connections.fillSubmapCache(level, pos, state, cullFace);

        return buildCTMQuads(connections, quads, cullFace);
    }

    public static List<BakedQuad> buildCTMQuads(TextureConnections connections, List<BakedQuad> base,
                                                Direction cullFace) {
        List<BakedQuad> result = new LinkedList<>();
        MeshBuilder meshBuilder = MeshBuilder.getInstance();
        var emitter = meshBuilder.getEmitter();

        for (BakedQuad originalQuad : base) {
            TextureAtlasSprite originalSprite = originalQuad.getSprite();

            TextureAtlasSprite connectionSprite = CTM_SPRITE_CACHE.get(originalSprite.contents().name());
            if (connectionSprite == null) {
                result.add(originalQuad);
                continue;
            }

            for (int xQuadrant = 0; xQuadrant < 2; xQuadrant++) {
                for (int yQuadrant = 0; yQuadrant < 2; yQuadrant++) {
                    boolean defaultTexture = connections.isDefaultTexture(xQuadrant, yQuadrant);
                    TextureAtlasSprite ctmSprite = defaultTexture ? originalSprite : connectionSprite;

                    emitter.fromVanilla(originalQuad, cullFace);
                    emitter.spriteBake(null, BAKE_LOCK_UV);

                    // slice quad into the current quadrant
                    subsect(emitter, Submap.X2[xQuadrant][yQuadrant]);
                    remapUVs(emitter, connections.getSubmapFor(xQuadrant, yQuadrant));

                    emitter.spriteBake(ctmSprite, BAKE_NORMALIZED);

                    emitter.computeGeometry();
                    emitter.populateMissingNormals();

                    result.add(emitter.toBakedQuad(ctmSprite));
                    emitter.emit();
                }
            }
        }
        return result;
    }

    // these are only used within the below methods, but are stored here as consts to reduce allocations
    // because they can be reused infinitely. DO NOT USE OUTSIDE subsect()/transformUVs()!!

    // filled in first copyUv() calls
    private static final ThreadLocal<Vector2f[]> uvs = ThreadLocal.withInitial(() -> new Vector2f[4]);
    private static final ThreadLocal<Vector2f[]> uvExtremes = ThreadLocal.withInitial(() -> new Vector2f[] { new Vector2f(), new Vector2f() });
    // set in copyPos() calls
    private static final ThreadLocal<Vector3f> position = ThreadLocal.withInitial(Vector3f::new);
    private static final ThreadLocal<Vector2f[]> xy = ThreadLocal.withInitial(() -> new Vector2f[] { new Vector2f(), new Vector2f(), new Vector2f(), new Vector2f() });

    private static void remapUVs(MutableQuadView quad, ISubmap submap) {
        submap = submap.unitScale();

        Vector2f maxUV = CTMMeshBuilder.uvExtremes.get()[0];
        maxUV.set(Float.MIN_VALUE, Float.MIN_VALUE);

        // cache UVs
        Vector2f[] uvs = CTMMeshBuilder.uvs.get();
        for (int i = 0; i < 4; i++) {
            uvs[i] = quad.copyUv(i, uvs[i]);
            maxUV.max(uvs[i]);
        }
        // scale the quadrants' UVs to the quadrant's area
        scaleUVCoordinatesToQuadrant(uvs, maxUV);

        // recompute min & max UVs
        Vector2f[] uvExtremes = getUVExtremes(uvs);
        Vector2f minUV = uvExtremes[0];
        maxUV = uvExtremes[1];

        float width = maxUV.x - minUV.x;
        float height = maxUV.y - minUV.y;

        float minU = submap.getXOffset();
        float minV = submap.getYOffset();
        minU += minUV.x * submap.getWidth();
        minV += minUV.y * submap.getHeight();

        float maxU = minU + (width * submap.getWidth());
        float maxV = minV + (height * submap.getHeight());

        quad.uv(0, uvs[0].x <= minUV.x ? minU : maxU, uvs[0].y <= minUV.y ? minV : maxV);
        quad.uv(1, uvs[1].x <= minUV.x ? minU : maxU, uvs[1].y <= minUV.y ? minV : maxV);
        quad.uv(2, uvs[2].x <= minUV.x ? minU : maxU, uvs[2].y <= minUV.y ? minV : maxV);
        quad.uv(3, uvs[3].x <= minUV.x ? minU : maxU, uvs[3].y <= minUV.y ? minV : maxV);
    }

    public static MutableQuadView subsect(final MutableQuadView quad, ISubmap submap) {
        Direction normal = quad.nominalFace();
        // nominalFace should never be null here; MutableQuadView.fromVanilla updates it
        assert normal != null;

        Vector2f[] xy = CTMMeshBuilder.xy.get();
        Vector2f[] uvs = CTMMeshBuilder.uvs.get();
        Vector3f position = CTMMeshBuilder.position.get();
        for (int i = 0; i < 4; i++) {
            quad.copyPos(i, position);
            uvs[i] = quad.copyUv(i, uvs[i]);

            switch (normal.getAxis()) {
                case X -> xy[i].set(position.z, position.y);
                case Y -> xy[i].set(position.x, position.z);
                case Z -> xy[i].set(position.x, position.y);
            }
        }

        if (normal != Direction.UP) {
            submap = submap.flipY();
        }
        if (normal == Direction.EAST || normal == Direction.NORTH) {
            submap = submap.flipX();
        }

        submap = submap.unitScale();

        // Rotated model quads retain their vertex order; it need not match FaceInfo for the world-facing side.
        float edge1X = xy[1].x - xy[0].x;
        float edge1Y = xy[1].y - xy[0].y;
        float edge3X = xy[3].x - xy[0].x;
        float edge3Y = xy[3].y - xy[0].y;
        float determinant = edge1X * edge3Y - edge1Y * edge3X;
        for (int i = 0; i < 4; i++) {
            float x = Mth.clamp(xy[i].x, submap.getXOffset(), submap.getXOffset() + submap.getWidth());
            float y = Mth.clamp(xy[i].y, submap.getYOffset(), submap.getYOffset() + submap.getHeight());
            float dx = x - xy[0].x;
            float dy = y - xy[0].y;
            float along1 = (dx * edge3Y - dy * edge3X) / determinant;
            float along3 = (edge1X * dy - edge1Y * dx) / determinant;

            // Read only the original UVs so one clipped vertex cannot affect a later vertex's interpolation.
            quad.uv(i,
                    Mth.lerp(along3, Mth.lerp(along1, uvs[0].x, uvs[1].x), Mth.lerp(along1, uvs[3].x, uvs[2].x)),
                    Mth.lerp(along3, Mth.lerp(along1, uvs[0].y, uvs[1].y), Mth.lerp(along1, uvs[3].y, uvs[2].y)));
            switch (normal.getAxis()) {
                case X -> quad.pos(i, quad.x(i), y, x);
                case Y -> quad.pos(i, x, quad.y(i), y);
                case Z -> quad.pos(i, x, y, quad.z(i));
            }
        }

        return quad;
    }

    public static Vector2f[] getUVExtremes(Vector2f[] uvs) {
        Vector2f[] uvExtremes = CTMMeshBuilder.uvExtremes.get();
        uvExtremes[0].set(Float.MAX_VALUE, Float.MAX_VALUE);
        uvExtremes[1].set(Float.MIN_VALUE, Float.MIN_VALUE);

        for (int i = 0; i < 4; i++) {
            Vector2f vertexUV = uvs[i];
            uvExtremes[0].min(vertexUV);
            uvExtremes[1].max(vertexUV);
        }
        return uvExtremes;
    }

    private static void scaleUVCoordinatesToQuadrant(Vector2f[] uvs, Vector2f maxUV) {
        float minU = maxUV.x() - 0.5f > Mth.EPSILON ? 0.5f : 0.0f,
                minV = maxUV.y() - 0.5f > Mth.EPSILON ? 0.5f : 0.0f;
        float maxU = maxUV.x() - 0.5f > Mth.EPSILON ? 1.0f : 0.5f,
                maxV = maxUV.y() - 0.5f > Mth.EPSILON ? 1.0f : 0.5f;

        for (int i = 0; i < 4; i++) {
            // scale u,v to a 0-1 range
            uvs[i].set(normalize(uvs[i].x, minU, maxU), normalize(uvs[i].y, minV, maxV));
        }
    }

    /// scale {@code delta} to a 0-1 range based on {@code min} and {@code max}
    public static float normalize(float delta, float min, float max) {
        if (min == max) return 0.5f;
        return Mth.clamp(Mth.inverseLerp(delta, min, max), 0.0f, 1.0f);
    }
}
