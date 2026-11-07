package pl.nicevisuals.gui;

import net.minecraft.client.gui.DrawContext;

/** Small helper for smooth (rounded) shapes built from scanlines. */
public final class Draw {
    private Draw() {}

    public static void rrect(DrawContext ctx, int x, int y, int w, int h, int r, int color) {
        r = Math.min(r, Math.min(w, h) / 2);
        for (int i = 0; i < h; i++) {
            int inset = 0;
            int j = i < r ? i : (i >= h - r ? h - 1 - i : -1);
            if (j >= 0) {
                double dy = r - j - 0.5;
                inset = r - (int) Math.round(Math.sqrt(Math.max(0, r * r - dy * dy)));
            }
            ctx.fill(x + inset, y + i, x + w - inset, y + i + 1, color);
        }
    }
}
