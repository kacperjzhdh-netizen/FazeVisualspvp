package pl.nicevisuals.gui;

import net.minecraft.client.gui.DrawContext;

/**
 * Smooth (anti-aliased) rounded shapes built from scanlines.
 * All coordinates are in "layout units"; {@link #unit} converts them to drawing pixels. The menu sets
 * unit so that one drawing pixel equals one real screen pixel, which makes the soft edges truly smooth.
 * HUD code leaves unit at 1 (plain GUI pixels).
 */
public final class Draw {
    /** When true all rounded shapes become square (pixel look). */
    public static boolean pixel;
    /** When true gradients collapse to solid colors and corners are small. */
    public static boolean flat;
    /** Drawing pixels per layout unit. */
    public static float unit = 1f;

    private Draw() {}

    private static int u(float v) { return Math.round(v * unit); }

    public static void fill(DrawContext ctx, int x1, int y1, int x2, int y2, int color) {
        ctx.fill(u(x1), u(y1), u(x2), u(y2), color);
    }

    /** Fractional inset of scanline i of a shape len pixels long with corner radius r. */
    private static float inset(int i, int len, int r) {
        int j = i < r ? i : (i >= len - r ? len - 1 - i : -1);
        if (j < 0) return 0;
        double d = r - j - 0.5;
        return (float) (r - Math.sqrt(Math.max(0, r * r - d * d)));
    }

    private static int radius(float r, int w, int h) {
        if (pixel) return 0;
        float rr = flat ? Math.min(r, 6 * unit) : r;
        return Math.max(0, Math.min(Math.round(rr), Math.min(w, h) / 2));
    }

    public static void rrect(DrawContext ctx, int x, int y, int w, int h, int r, int color) {
        rrectV(ctx, x, y, w, h, r, color, color);
    }

    /** Vertical gradient (top to bottom) rounded rectangle. */
    public static void rrectV(DrawContext ctx, int x, int y, int w, int h, int r, int top, int bottom) {
        int X = u(x), Y = u(y), W = u(x + w) - X, H = u(y + h) - Y;
        if (W <= 0 || H <= 0) return;
        if (flat) bottom = top;
        int R = radius(r * unit, W, H);
        for (int i = 0; i < H; i++) {
            if (i >= R && i < H - R) {               // straight middle: one gradient quad
                int endRow = H - R;
                ctx.fillGradient(X, Y + i, X + W, Y + endRow, mix(top, bottom, H <= 1 ? 0 : i / (float) (H - 1)),
                        mix(top, bottom, H <= 1 ? 0 : (endRow - 1) / (float) (H - 1)));
                i = endRow - 1;
                continue;
            }
            float in = inset(i, H, R);
            int l = (int) Math.ceil(in);
            int c = mix(top, bottom, H <= 1 ? 0 : i / (float) (H - 1));
            ctx.fill(X + l, Y + i, X + W - l, Y + i + 1, c);
            float cov = l - in;
            if (l > 0 && cov > 0.02f) {
                int e = alpha(c, cov);
                ctx.fill(X + l - 1, Y + i, X + l, Y + i + 1, e);
                ctx.fill(X + W - l, Y + i, X + W - l + 1, Y + i + 1, e);
            }
        }
    }

    /** Horizontal gradient (left to right) rounded rectangle. */
    public static void rrectGrad(DrawContext ctx, int x, int y, int w, int h, int r, int c1, int c2) {
        int X = u(x), Y = u(y), W = u(x + w) - X, H = u(y + h) - Y;
        if (W <= 0 || H <= 0) return;
        if (flat) c2 = c1;
        int R = radius(r * unit, W, H);
        int step = Math.max(1, W / 56);
        for (int i = 0; i < W; ) {
            boolean edge = i < R || i >= W - R;
            int n = edge ? 1 : Math.min(step, W - R - i);
            float in = edge ? inset(i, W, R) : 0f;
            int l = (int) Math.ceil(in);
            int c = mix(c1, c2, W <= 1 ? 0 : (i + n / 2f) / (float) (W - 1));
            ctx.fill(X + i, Y + l, X + i + n, Y + H - l, c);
            float cov = l - in;
            if (edge && l > 0 && cov > 0.02f) {
                int e = alpha(c, cov);
                ctx.fill(X + i, Y + l - 1, X + i + 1, Y + l, e);
                ctx.fill(X + i, Y + H - l, X + i + 1, Y + H - l + 1, e);
            }
            i += n;
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
