package pl.faze.visuals;

import net.minecraft.client.gui.DrawContext;

/** Male pomocnicze funkcje rysowania 2D (zaokraglone prostokaty, kolory). */
public final class UiUtil {
    private UiUtil() {}

    public static int argb(int rgb, float alpha) {
        int a = Math.max(0, Math.min(255, (int) (alpha * 255f)));
        return (a << 24) | (rgb & 0xFFFFFF);
    }

    public static int mix(int c1, int c2, float t) {
        t = Math.max(0f, Math.min(1f, t));
        int r = (int) (((c1 >> 16) & 255) * (1 - t) + ((c2 >> 16) & 255) * t);
        int g = (int) (((c1 >> 8) & 255) * (1 - t) + ((c2 >> 8) & 255) * t);
        int b = (int) ((c1 & 255) * (1 - t) + (c2 & 255) * t);
        return (r << 16) | (g << 8) | b;
    }

    /** Wypelniony prostokat z zaokraglonymi rogami (rzedy poziome, bez nakladania sie). */
    public static void rrect(DrawContext ctx, int x, int y, int w, int h, int r, int color) {
        if (w <= 0 || h <= 0) return;
        r = Math.max(0, Math.min(r, Math.min(w, h) / 2));
        if (r == 0) {
            ctx.fill(x, y, x + w, y + h, color);
            return;
        }
        for (int i = 0; i < r; i++) {
            double dy = r - i - 0.5;
            int inset = (int) Math.round(r - Math.sqrt(Math.max(0, (double) r * r - dy * dy)));
            ctx.fill(x + inset, y + i, x + w - inset, y + i + 1, color);
            ctx.fill(x + inset, y + h - i - 1, x + w - inset, y + h - i, color);
        }
        ctx.fill(x, y + r, x + w, y + h - r, color);
    }

    /** Obramowanie 1px dookola zaokraglonego prostokata (rysowane jako dwa prostokaty). */
    public static void rrectOutline(DrawContext ctx, int x, int y, int w, int h, int r, int color) {
        rrect(ctx, x, y, w, h, r, color);
    }

    public static float easeOut(float t) {
        t = Math.max(0f, Math.min(1f, t));
        return 1f - (1f - t) * (1f - t) * (1f - t);
    }

    public static float approach(float cur, float target, float speed, float dt) {
        float k = 1f - (float) Math.exp(-speed * dt);
        return cur + (target - cur) * k;
    }
}
