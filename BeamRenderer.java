package com.sharedwaypoints;

import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

/** Kolumna światła w świecie (styl BEAM): wewnętrzny jasny rdzeń + szersza poświata, zanikająca ku górze. */
public final class BeamRenderer {
    private BeamRenderer() {}

    public static void render(WorldRenderContext context) {
        if (Config.data.style != Config.Style.BEAM) return;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.world == null || WaypointStore.all().isEmpty()) return;

        MatrixStack matrices = context.matrixStack();
        VertexConsumerProvider consumers = context.consumers();
        if (matrices == null || consumers == null) return;

        Vec3d cam = context.camera().getPos();
        String dim = mc.world.getRegistryKey().getValue().toString();
        int bottomY = mc.world.getBottomY();
        int height = mc.world.getHeight();

        boolean drewAny = false;
        for (Waypoint w : WaypointStore.all()) {
            if (!w.visible || !dim.equals(w.dim)) continue;

            float r = ((w.color >> 16) & 0xFF) / 255f;
            float g = ((w.color >> 8) & 0xFF) / 255f;
            float b = (w.color & 0xFF) / 255f;

            // Od dna świata do góry; współrzędne względem pozycji waypointa.
            float y0 = (float) (bottomY - w.y);
            float y1 = y0 + height;

            VertexConsumer vc = consumers.getBuffer(RenderLayer.getLightning());
            matrices.push();
            matrices.translate(w.x + 0.5 - cam.x, w.y - cam.y, w.z + 0.5 - cam.z);
            Matrix4f m = matrices.peek().getPositionMatrix();

            box(vc, m, 0.10f, y0, y1, r, g, b, 0.85f); // rdzeń
            box(vc, m, 0.26f, y0, y1, r, g, b, 0.28f); // poświata

            matrices.pop();
            drewAny = true;
        }

        if (drewAny && consumers instanceof VertexConsumerProvider.Immediate immediate) {
            immediate.draw(RenderLayer.getLightning());
        }
    }

    /** Prostopadłościan o kwadratowym przekroju (promień rad), przezroczystość maleje ku górze. */
    private static void box(VertexConsumer vc, Matrix4f m, float rad, float y0, float y1,
                            float r, float g, float b, float alphaBottom) {
        wall(vc, m, -rad, -rad, rad, -rad, y0, y1, r, g, b, alphaBottom);
        wall(vc, m, rad, -rad, rad, rad, y0, y1, r, g, b, alphaBottom);
        wall(vc, m, rad, rad, -rad, rad, y0, y1, r, g, b, alphaBottom);
        wall(vc, m, -rad, rad, -rad, -rad, y0, y1, r, g, b, alphaBottom);
    }

    /** Jedna ściana rysowana dwustronnie, żeby była widoczna z każdej strony. */
    private static void wall(VertexConsumer vc, Matrix4f m, float xa, float za, float xb, float zb,
                             float y0, float y1, float r, float g, float b, float aBottom) {
        // przód
        vc.vertex(m, xa, y0, za).color(r, g, b, aBottom);
        vc.vertex(m, xb, y0, zb).color(r, g, b, aBottom);
        vc.vertex(m, xb, y1, zb).color(r, g, b, 0f);
        vc.vertex(m, xa, y1, za).color(r, g, b, 0f);
        // tył
        vc.vertex(m, xa, y1, za).color(r, g, b, 0f);
        vc.vertex(m, xb, y1, zb).color(r, g, b, 0f);
        vc.vertex(m, xb, y0, zb).color(r, g, b, aBottom);
        vc.vertex(m, xa, y0, za).color(r, g, b, aBottom);
    }
}
