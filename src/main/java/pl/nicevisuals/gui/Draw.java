package pl.nicevisuals.gui;

import net.minecraft.client.gui.DrawContext;

/** Small helpers for smooth (rounded) shapes built from scanlines. */
public final class Draw {
    /** When true all rounded shapes become square (pixel look). */
    public static boolean pixel;

    private Draw() {}

    private static int inset(int i, int h, int r) {
        int j = i < r ? i : (i >= h - r ? h - 1 - i : -1);
        if (j < 0) return 0;
        double dy = r - j - 0.5;
        return r - (int) Math.round(Math.sqrt(Math.max(0, r * r - dy * dy)));
    }

    public static void rrect(DrawContext ctx, int x, int y, int w, int h, int r, int color) {
        r = pixel ? 0 : Math.min(r, Math.min(w, h) / 2);
        for (int i = 0; i < h; i++) {
            int in = inset(i, h, r);
            ctx.fill(x + in, y + i, x + w - in, y + i + 1, color);
        }
    }

    /** Horizontal gradient rounded rectangle (left color -> right color). */
    public static void rrectGrad(DrawContext ctx, int x, int y, int w, int h, int r, int c1, int c2) {
        r = pixel ? 0 : Math.min(r, Math.min(w, h) / 2);
        for (int i = 0; i < w; i++) {
            int in = inset(i, w, r);
            ctx.fill(x + i, y + in, x + i + 1, y + h - in, mix(c1, c2, w <= 1 ? 0 : i / (float) (w - 1)));
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
