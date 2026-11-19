package pl.nicevisuals.gui;

import net.minecraft.client.gui.DrawContext;

/** Thick lines for the HUD built from touching squares (uses only DrawContext.fill, so it works everywhere). */
public final class Fx {
    private static DrawContext ctx;

    private Fx() {}

    public static void begin(DrawContext c) { ctx = c; }

    public static void end() { ctx = null; }

    private static void stamp(float x1, float y1, float x2, float y2, float size, int argb) {
        float dx = x2 - x1, dy = y2 - y1;
        float len = (float) Math.sqrt(dx * dx + dy * dy);
        float step = Math.max(1f, size * 0.9f);
        int n = Math.max(1, (int) Math.ceil(len / step));
        float h = size / 2f;
        for (int i = 0; i <= n; i++) {
            float t = i / (float) n;
            int x = Math.round(x1 + dx * t - h), y = Math.round(y1 + dy * t - h);
            int s = Math.max(1, Math.round(size));
            ctx.fill(x, y, x + s, y + s, argb);
        }
    }

    public static void line(float x1, float y1, float x2, float y2, float w, int argb) {
        if (ctx == null) return;
        stamp(x1, y1, x2, y2, w, argb);
    }

    /** Line with a soft faint halo underneath. */
    public static void glowLine(float x1, float y1, float x2, float y2, float w, int rgb, float alpha) {
        if (ctx == null) return;
        stamp(x1, y1, x2, y2, w * 1.8f, Draw.alpha(0xFF000000 | rgb, alpha * 0.22f));
        stamp(x1, y1, x2, y2, w, Draw.alpha(0xFF000000 | rgb, alpha));
    }
}
