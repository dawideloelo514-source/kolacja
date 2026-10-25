package com.sharedwaypoints;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.util.math.Vec3d;

/**
 * Rysuje waypointy na ekranie (HUD). W stylu MARKER: romb + nazwa + dystans, także przy krawędzi
 * ekranu, gdy cel jest poza polem widzenia. W stylu BEAM: sama etykieta nad kolumną światła.
 */
public final class HudMarkers {
    private HudMarkers() {}

    public static void render(DrawContext ctx, RenderTickCounter tickCounter) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.world == null || mc.player == null || mc.options.hudHidden) return;
        if (WaypointStore.all().isEmpty()) return;

        Camera cam = mc.gameRenderer.getCamera();
        if (!cam.isReady()) return;

        boolean beamStyle = Config.data.style == Config.Style.BEAM;
        String dim = mc.world.getRegistryKey().getValue().toString();
        TextRenderer tr = mc.textRenderer;

        int screenW = ctx.getScaledWindowWidth();
        int screenH = ctx.getScaledWindowHeight();
        double tanHalf = Math.tan(Math.toRadians(mc.options.getFov().getValue()) / 2.0);
        double aspect = (double) screenW / screenH;

        // Baza kamery: f = przód, r = prawo, u = góra (konwencja Minecrafta: yaw 0 = +Z).
        double yaw = Math.toRadians(cam.getYaw());
        double pitch = Math.toRadians(cam.getPitch());
        double sy = Math.sin(yaw), cy = Math.cos(yaw), sp = Math.sin(pitch), cp = Math.cos(pitch);
        double fx = -sy * cp, fy = -sp, fz = cy * cp;
        double rx = -cy, rz = -sy;
        double ux = -sy * sp, uy = cp, uz = cy * sp;

        Vec3d cpos = cam.getPos();
        double pulse = Math.sin(System.currentTimeMillis() / 250.0);

        for (Waypoint w : WaypointStore.all()) {
            if (!w.visible || !dim.equals(w.dim)) continue;

            double dx = w.x + 0.5 - cpos.x;
            double dy = w.y + 1.8 - cpos.y; // znacznik wisi nad blokiem
            double dz = w.z + 0.5 - cpos.z;
            double dist = Math.sqrt(dx * dx + (w.y + 0.5 - cpos.y) * (w.y + 0.5 - cpos.y) + dz * dz);

            double depth = dx * fx + dy * fy + dz * fz;
            double rc = dx * rx + dz * rz;
            double uc = dx * ux + dy * uy + dz * uz;

            double nx, ny;
            if (depth > 0.05) {
                nx = rc / depth / (tanHalf * aspect);
                ny = uc / depth / tanHalf;
            } else {
                double len = Math.hypot(rc, uc);
                if (len < 1e-6) {
                    nx = 0;
                    ny = -1;
                } else {
                    nx = rc / len * 100.0;
                    ny = uc / len * 100.0;
                }
            }

            boolean onScreen = depth > 0.05 && Math.abs(nx) <= 0.95 && Math.abs(ny) <= 0.95;
            if (!onScreen) {
                if (beamStyle) continue;
                double m = Math.max(Math.abs(nx), Math.abs(ny));
                double k = 0.92 / m;
                nx *= k;
                ny *= k;
            }

            int px = (int) Math.round((nx * 0.5 + 0.5) * screenW);
            int py = (int) Math.round((0.5 - ny * 0.5) * screenH);
            int rgb = 0xFF000000 | w.color;

            int s = beamStyle ? 3 : 5 + (int) Math.round(pulse * 0.8);
            if (!onScreen) s = 4;

            drawDiamond(ctx, px, py, s + 1, 0xC0000000);
            drawDiamond(ctx, px, py, s, rgb);
            drawDiamond(ctx, px, py, Math.max(1, s - 3), 0xFFFFFFFF);

            if (onScreen) {
                String name = w.displayName();
                int tw = tr.getWidth(name);
                int ly = py - s - 14;
                ctx.fill(px - tw / 2 - 3, ly - 2, px + tw / 2 + 3, ly + 10, 0x90000000);
                ctx.fill(px - tw / 2 - 3, ly + 10, px + tw / 2 + 3, ly + 11, rgb);
                ctx.drawTextWithShadow(tr, name, px - tw / 2, ly, 0xFFFFFFFF);
            }
            if (Config.data.showDistance) {
                String d = formatDistance(dist);
                ctx.drawTextWithShadow(tr, d, px - tr.getWidth(d) / 2, py + s + 4, 0xFFDDDDDD);
            }
        }
    }

    private static void drawDiamond(DrawContext ctx, int cx, int cy, int r, int argb) {
        for (int dy = -r; dy <= r; dy++) {
            int hw = r - Math.abs(dy);
            ctx.fill(cx - hw, cy + dy, cx + hw + 1, cy + dy + 1, argb);
        }
    }

    private static String formatDistance(double d) {
        if (d >= 1000) return String.format(java.util.Locale.ROOT, "%.1f km", d / 1000.0);
        return Math.round(d) + " m";
    }
}
