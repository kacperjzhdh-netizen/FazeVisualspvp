package pl.nicevisuals.gui;

import net.minecraft.client.gui.DrawContext;

/** Helpers for smooth (anti-aliased) rounded shapes built from scanlines. */
public final class Draw {
    /** When true all rounded shapes become square (pixel look). */
    public static boolean pixel;

    private Draw() {}

    /** Horizontal inset (in pixels, fractional) of row/column i of a shape that is len long. */
    private static float inset(int i, int len, int r) {
        int j = i < r ? i : (i >= len - r ? len - 1 - i : -1);
        if (j < 0) return 0;
        double d = r - j - 0.5;
        return (float) (r - Math.sqrt(Math.max(0, r * r - d * d)));
    }

    private static int radius(int r, int w, int h) { return pixel ? 0 : Math.max(0, Math.min(r, Math.min(w, h) / 2)); }

    public static void rrect(DrawContext ctx, int x, int y, int w, int h, int r, int color) {
        rrectV(ctx, x, y, w, h, r, color, color);
    }

    /** Vertical gradient (top -> bottom) rounded rectangle with soft edges. */
    public static void rrectV(DrawContext ctx, int x, int y, int w, int h, int r, int top, int bottom) {
        if (w <= 0 || h <= 0) return;
        r = radius(r, w, h);
        for (int i = 0; i < h; i++) {
            float in = inset(i, h, r);
            int l = (int) Math.ceil(in);
            int c = mix(top, bottom, h <= 1 ? 0 : i / (float) (h - 1));
            ctx.fill(x + l, y + i, x + w - l, y + i + 1, c);
            float cov = l - in;
            if (l > 0 && cov > 0.02f) {
                int e = alpha(c, cov);
                ctx.fill(x + l - 1, y + i, x + l, y + i + 1, e);
                ctx.fill(x + w - l, y + i, x + w - l + 1, y + i + 1, e);
            }
        }
    }

    /** Horizontal gradient (left -> right) rounded rectangle with soft edges. */
    public static void rrectGrad(DrawContext ctx, int x, int y, int w, int h, int r, int c1, int c2) {
        if (w <= 0 || h <= 0) return;
        r = radius(r, w, h);
        for (int i = 0; i < w; i++) {
            float in = inset(i, w, r);
            int l = (int) Math.ceil(in);
            int c = mix(c1, c2, w <= 1 ? 0 : i / (float) (w - 1));
            ctx.fill(x + i, y + l, x + i + 1, y + h - l, c);
            float cov = l - in;
            if (l > 0 && cov > 0.02f) {
                int e = alpha(c, cov);
                ctx.fill(x + i, y + l - 1, x + i + 1, y + l, e);
                ctx.fill(x + i, y + h - l, x + i + 1, y + h - l + 1, e);
            }
        }
    }

    public static int mix(int a, int b, float t) {
        int out = 0;
        for (int s = 24; s >= 0; s -= 8) {
            int ca = (a >> s) & 255, cb = (b >> s) & 255;
            out |= (Math.round(ca + (cb - ca) * t) & 255) << s;
        }
        return out;
    }

    public static int alpha(int color, float a) {
        int al = Math.round(((color >>> 24) & 255) * Math.max(0, Math.min(1, a)));
        return (al << 24) | (color & 0xFFFFFF);
    }
}
