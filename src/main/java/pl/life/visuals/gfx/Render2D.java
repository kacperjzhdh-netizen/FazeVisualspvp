package pl.life.visuals.gfx;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.joml.Matrix4f;
import pl.life.visuals.module.Modules;

/**
 * Gladkie rysowanie 2D: zaokraglone prostokaty z antyaliasingiem (przezroczysta obwodka),
 * luki, linie, gradienty oraz tekst fontem TTF. Wszystkie wspolrzedne sa w biezacym ukladzie macierzy;
 * po {@link #begin} jednostka = 1 piksel ekranu (niezalezna od GUI scale).
 */
public final class Render2D {
    private Render2D() {}

    public static final Identifier FONT = Identifier.of("lifevisuals", "ui");
    public static final Identifier FONT_BOLD = Identifier.of("lifevisuals", "ui_bold");
    private static final float BASE = 18f;
    private static final float TTF_CENTER = 0.7f;

    /** Szerokosc rozmycia krawedzi w aktualnych jednostkach (1 piksel ekranu). */
    public static float fringe = 1f;

    // ------------------------------------------------------------ uklad wspolrzednych
    public static float gs() {
        return (float) MinecraftClient.getInstance().getWindow().getScaleFactor();
    }

    public static int fbW() {
        return MinecraftClient.getInstance().getWindow().getFramebufferWidth();
    }

    public static int fbH() {
        return MinecraftClient.getInstance().getWindow().getFramebufferHeight();
    }

    /** Przechodzi na uklad w pikselach ekranu. */
    public static void begin(DrawContext ctx) {
        ctx.draw();
        MatrixStack ms = ctx.getMatrices();
        ms.push();
        float s = 1f / gs();
        ms.scale(s, s, 1f);
        fringe = 1f;
    }

    public static void end(DrawContext ctx) {
        ctx.draw();
        ctx.getMatrices().pop();
        fringe = 1f;
    }

    // ------------------------------------------------------------ kolory
    public static int argb(int rgb, float a) {
        int al = Math.max(0, Math.min(255, (int) (a * 255f + 0.5f)));
        return (al << 24) | (rgb & 0xFFFFFF);
    }

    public static int mulAlpha(int argb, float m) {
        int a = Math.max(0, Math.min(255, (int) (((argb >>> 24) & 255) * m + 0.5f)));
        return (a << 24) | (argb & 0xFFFFFF);
    }

    public static int lerp(int c1, int c2, float t) {
        t = Math.max(0f, Math.min(1f, t));
        int a = (int) (((c1 >>> 24) & 255) * (1 - t) + ((c2 >>> 24) & 255) * t);
        int r = (int) (((c1 >> 16) & 255) * (1 - t) + ((c2 >> 16) & 255) * t);
        int g = (int) (((c1 >> 8) & 255) * (1 - t) + ((c2 >> 8) & 255) * t);
        int b = (int) ((c1 & 255) * (1 - t) + (c2 & 255) * t);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    public static float ease(float t) {
        t = Math.max(0f, Math.min(1f, t));
        return 1f - (1f - t) * (1f - t) * (1f - t);
    }

    public static float approach(float cur, float target, float speed, float dt) {
        float k = 1f - (float) Math.exp(-speed * dt);
        return cur + (target - cur) * k;
    }

    // ------------------------------------------------------------ bufor
    private static BufferBuilder start() {
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableDepthTest();
        RenderSystem.disableCull();
        return Tessellator.getInstance().begin(VertexFormat.DrawMode.TRIANGLES, VertexFormats.POSITION_COLOR);
    }

    private static void finish(BufferBuilder bb) {
        BufferRenderer.drawWithGlobalProgram(bb.end());
        RenderSystem.enableCull();
        RenderSystem.enableDepthTest();
    }

    private static void v(BufferBuilder b, Matrix4f m, float x, float y, int c) {
        b.vertex(m, x, y, 0f).color(c);
    }

    private static void tri(BufferBuilder b, Matrix4f m, float x1, float y1, int c1, float x2, float y2, int c2, float x3, float y3, int c3) {
        v(b, m, x1, y1, c1);
        v(b, m, x2, y2, c2);
        v(b, m, x3, y3, c3);
    }

    private static int grad(int top, int bot, float y0, float h, float py) {
        if (top == bot) return top;
        return lerp(top, bot, h <= 0 ? 0 : (py - y0) / h);
    }

    private static int segFor(float r) {
        if (r <= 1.5f) return 1;
        if (r < 6f) return 3;
        if (r < 16f) return 5;
        return 8;
    }

    /** Punkty zaokraglonego prostokata (kolejnosc: 4 rogi, kazdy seg+1 punktow). */
    private static float[] poly(float x, float y, float w, float h, float r, int seg) {
        r = Math.max(0f, Math.min(r, Math.min(w, h) / 2f));
        float[] cx = {x + r, x + w - r, x + w - r, x + r};
        float[] cy = {y + r, y + r, y + h - r, y + h - r};
        float[] a0 = {180f, 270f, 0f, 90f};
        float[] out = new float[4 * (seg + 1) * 2];
        int k = 0;
        for (int c = 0; c < 4; c++) {
            for (int i = 0; i <= seg; i++) {
                double ang = Math.toRadians(a0[c] + 90f * i / seg);
                out[k++] = cx[c] + (float) Math.cos(ang) * r;
                out[k++] = cy[c] + (float) Math.sin(ang) * r;
            }
        }
        return out;
    }

    private static void strip(BufferBuilder b, Matrix4f m, float[] a, float[] c, int colA, int colB, float y0, float h, int top, int bot, boolean useGrad) {
        int n = a.length / 2;
        for (int i = 0; i < n; i++) {
            int j = (i + 1) % n;
            int ca1 = useGrad ? mulAlphaOf(grad(top, bot, y0, h, a[i * 2 + 1]), colA) : colA;
            int ca2 = useGrad ? mulAlphaOf(grad(top, bot, y0, h, a[j * 2 + 1]), colA) : colA;
            int cb1 = useGrad ? mulAlphaOf(grad(top, bot, y0, h, c[i * 2 + 1]), colB) : colB;
            int cb2 = useGrad ? mulAlphaOf(grad(top, bot, y0, h, c[j * 2 + 1]), colB) : colB;
            tri(b, m, a[i * 2], a[i * 2 + 1], ca1, c[i * 2], c[i * 2 + 1], cb1, c[j * 2], c[j * 2 + 1], cb2);
            tri(b, m, a[i * 2], a[i * 2 + 1], ca1, c[j * 2], c[j * 2 + 1], cb2, a[j * 2], a[j * 2 + 1], ca2);
        }
    }

    /** Pomocniczo: 'mask' to 0 (przezroczysty brzeg) albo -1 (bez zmian). */
    private static int mulAlphaOf(int color, int mask) {
        return mask == 0 ? (color & 0xFFFFFF) : color;
    }

    // ------------------------------------------------------------ ksztalty
    public static void rr(DrawContext ctx, float x, float y, float w, float h, float r, int color) {
        rrGrad(ctx, x, y, w, h, r, color, color);
    }

    /** Wypelniony zaokraglony prostokat z pionowym gradientem i wygladzona krawedzia. */
    public static void rrGrad(DrawContext ctx, float x, float y, float w, float h, float r, int top, int bot) {
        if (w <= 0 || h <= 0 || (((top >>> 24) == 0) && ((bot >>> 24) == 0))) return;
        ctx.draw();
        float f = fringe;
        int seg = segFor(r);
        float[] core = poly(x + f / 2, y + f / 2, w - f, h - f, Math.max(0, r - f / 2), seg);
        float[] out = poly(x - f / 2, y - f / 2, w + f, h + f, r + f / 2, seg);
        BufferBuilder b = start();
        Matrix4f m = ctx.getMatrices().peek().getPositionMatrix();
        float mx = x + w / 2, my = y + h / 2;
        int cm = grad(top, bot, y, h, my);
        int n = core.length / 2;
        for (int i = 0; i < n; i++) {
            int j = (i + 1) % n;
            tri(b, m, mx, my, cm,
                    core[i * 2], core[i * 2 + 1], grad(top, bot, y, h, core[i * 2 + 1]),
                    core[j * 2], core[j * 2 + 1], grad(top, bot, y, h, core[j * 2 + 1]));
        }
        strip(b, m, core, out, -1, 0, y, h, top, bot, true);
        finish(b);
    }

    /** Obramowanie (kontur) zaokraglonego prostokata o grubosci t. */
    public static void rrStroke(DrawContext ctx, float x, float y, float w, float h, float r, float t, int color) {
        if (w <= 2 * t || h <= 2 * t || (color >>> 24) == 0) return;
        ctx.draw();
        float f = fringe;
        int seg = segFor(r);
        float[] oF = poly(x - f / 2, y - f / 2, w + f, h + f, r + f / 2, seg);
        float[] oC = poly(x + f / 2, y + f / 2, w - f, h - f, Math.max(0, r - f / 2), seg);
        float[] iC = poly(x + t - f / 2, y + t - f / 2, w - 2 * t + f, h - 2 * t + f, Math.max(0, r - t + f / 2), seg);
        float[] iF = poly(x + t + f / 2, y + t + f / 2, w - 2 * t - f, h - 2 * t - f, Math.max(0, r - t - f / 2), seg);
        BufferBuilder b = start();
        Matrix4f m = ctx.getMatrices().peek().getPositionMatrix();
        int clear = color & 0xFFFFFF;
        strip(b, m, oF, oC, clear, color, 0, 0, 0, 0, false);
        strip(b, m, oC, iC, color, color, 0, 0, 0, 0, false);
        strip(b, m, iC, iF, color, clear, 0, 0, 0, 0, false);
        finish(b);
    }

    public static void circle(DrawContext ctx, float cx, float cy, float r, int color) {
        rr(ctx, cx - r, cy - r, r * 2, r * 2, r, color);
    }

    /** Luk (pierscien): startDeg 0 = gora, rosnie zgodnie z ruchem wskazowek zegara. */
    public static void arc(DrawContext ctx, float cx, float cy, float r, float t, float startDeg, float sweepDeg, int color) {
        if (sweepDeg <= 0.1f || (color >>> 24) == 0) return;
        ctx.draw();
        float f = fringe;
        int steps = Math.max(3, (int) (sweepDeg / 5f));
        BufferBuilder b = start();
        Matrix4f m = ctx.getMatrices().peek().getPositionMatrix();
        int clear = color & 0xFFFFFF;
        float ro = r + t / 2, ri = r - t / 2;
        for (int i = 0; i < steps; i++) {
            double a1 = Math.toRadians(startDeg - 90 + sweepDeg * i / steps);
            double a2 = Math.toRadians(startDeg - 90 + sweepDeg * (i + 1) / steps);
            float c1 = (float) Math.cos(a1), s1 = (float) Math.sin(a1), c2 = (float) Math.cos(a2), s2 = (float) Math.sin(a2);
            // rdzen
            quadTri(b, m, cx + c1 * ro, cy + s1 * ro, color, cx + c2 * ro, cy + s2 * ro, color, cx + c2 * ri, cy + s2 * ri, color, cx + c1 * ri, cy + s1 * ri, color);
            // krawedz zewnetrzna i wewnetrzna
            quadTri(b, m, cx + c1 * (ro + f), cy + s1 * (ro + f), clear, cx + c2 * (ro + f), cy + s2 * (ro + f), clear, cx + c2 * ro, cy + s2 * ro, color, cx + c1 * ro, cy + s1 * ro, color);
            quadTri(b, m, cx + c1 * ri, cy + s1 * ri, color, cx + c2 * ri, cy + s2 * ri, color, cx + c2 * (ri - f), cy + s2 * (ri - f), clear, cx + c1 * (ri - f), cy + s1 * (ri - f), clear);
        }
        finish(b);
    }

    private static void quadTri(BufferBuilder b, Matrix4f m, float x1, float y1, int c1, float x2, float y2, int c2,
                                float x3, float y3, int c3, float x4, float y4, int c4) {
        tri(b, m, x1, y1, c1, x2, y2, c2, x3, y3, c3);
        tri(b, m, x1, y1, c1, x3, y3, c3, x4, y4, c4);
    }

    public static void line(DrawContext ctx, float x1, float y1, float x2, float y2, float t, int color) {
        if ((color >>> 24) == 0) return;
        ctx.draw();
        float dx = x2 - x1, dy = y2 - y1;
        float len = (float) Math.sqrt(dx * dx + dy * dy);
        if (len < 0.01f) return;
        float nx = -dy / len, ny = dx / len;
        float f = fringe;
        float hw = t / 2f, ow = hw + f;
        int clear = color & 0xFFFFFF;
        BufferBuilder b = start();
        Matrix4f m = ctx.getMatrices().peek().getPositionMatrix();
        quadTri(b, m, x1 + nx * hw, y1 + ny * hw, color, x2 + nx * hw, y2 + ny * hw, color, x2 - nx * hw, y2 - ny * hw, color, x1 - nx * hw, y1 - ny * hw, color);
        quadTri(b, m, x1 + nx * ow, y1 + ny * ow, clear, x2 + nx * ow, y2 + ny * ow, clear, x2 + nx * hw, y2 + ny * hw, color, x1 + nx * hw, y1 + ny * hw, color);
        quadTri(b, m, x1 - nx * hw, y1 - ny * hw, color, x2 - nx * hw, y2 - ny * hw, color, x2 - nx * ow, y2 - ny * ow, clear, x1 - nx * ow, y1 - ny * ow, clear);
        finish(b);
    }

    public static void triangle(DrawContext ctx, float x1, float y1, float x2, float y2, float x3, float y3, int color) {
        if ((color >>> 24) == 0) return;
        ctx.draw();
        BufferBuilder b = start();
        tri(b, ctx.getMatrices().peek().getPositionMatrix(), x1, y1, color, x2, y2, color, x3, y3, color);
        finish(b);
    }

    /** Prostokat z czterema kolorami naroznikow (do palety kolorow). */
    public static void gradQuad(DrawContext ctx, float x, float y, float w, float h, int tl, int tr, int br, int bl) {
        ctx.draw();
        BufferBuilder b = start();
        quadTri(b, ctx.getMatrices().peek().getPositionMatrix(), x, y, tl, x + w, y, tr, x + w, y + h, br, x, y + h, bl);
        finish(b);
    }

    /** Miekki cien / poswiata wokol prostokata. */
    public static void glow(DrawContext ctx, float x, float y, float w, float h, float r, float spread, int color) {
        for (int i = 4; i >= 1; i--) {
            float s = spread * i / 4f;
            float a = 0.09f * (5 - i) / 4f;
            rr(ctx, x - s, y - s, w + s * 2, h + s * 2, r + s, mulAlpha(color, a * (((color >>> 24) & 255) / 255f)));
        }
    }

    // ------------------------------------------------------------ obcinanie
    /** Obcina rysowanie do prostokata podanego w pikselach ekranu (lewy gorny rog). */
    public static void scissor(DrawContext ctx, int x, int y, int w, int h) {
        ctx.draw();
        RenderSystem.enableScissor(Math.max(0, x), Math.max(0, fbH() - y - h), Math.max(0, w), Math.max(0, h));
    }

    public static void endScissor(DrawContext ctx) {
        ctx.draw();
        RenderSystem.disableScissor();
    }

    // ------------------------------------------------------------ tekst
    public static boolean ttf() {
        return Modules.CG_FONT.bool();
    }

    public static Text styled(String s, boolean bold) {
        return Text.literal(s).setStyle(Style.EMPTY.withFont(bold ? FONT_BOLD : FONT));
    }

    public static float textW(String s, float px, boolean bold) {
        TextRenderer tr = MinecraftClient.getInstance().textRenderer;
        if (ttf()) return tr.getWidth(styled(s, bold)) * px / BASE;
        return tr.getWidth(s) * px / 9f;
    }

    /** Rysuje tekst; y to srodek wizualny linii. px = wysokosc tekstu w pikselach. */
    public static void text(DrawContext ctx, String s, float x, float cy, float px, int color, boolean bold, boolean shadow) {
        if (s == null || s.isEmpty() || (color >>> 24) < 5) return;
        TextRenderer tr = MinecraftClient.getInstance().textRenderer;
        boolean t = ttf();
        float f = t ? px / BASE : px / 9f;
        float y0 = cy - (t ? TTF_CENTER : 3.5f) * f;
        MatrixStack ms = ctx.getMatrices();
        ms.push();
        ms.translate(x, y0, 0f);
        ms.scale(f, f, 1f);
        if (t) ctx.drawText(tr, styled(s, bold), 0, 0, color, shadow);
        else ctx.drawText(tr, s, 0, 0, color, shadow);
        ms.pop();
    }

    public static void textC(DrawContext ctx, String s, float cx, float cy, float px, int color, boolean bold, boolean shadow) {
        text(ctx, s, cx - textW(s, px, bold) / 2f, cy, px, color, bold, shadow);
    }

    public static void textR(DrawContext ctx, String s, float rx, float cy, float px, int color, boolean bold, boolean shadow) {
        text(ctx, s, rx - textW(s, px, bold), cy, px, color, bold, shadow);
    }

    // ------------------------------------------------------------ przedmioty
    public static void item(DrawContext ctx, ItemStack stack, float x, float y, float size) {
        if (stack == null || stack.isEmpty()) return;
        MatrixStack ms = ctx.getMatrices();
        ms.push();
        ms.translate(x, y, 0f);
        float s = size / 16f;
        ms.scale(s, s, 1f);
        ctx.drawItem(stack, 0, 0);
        ms.pop();
        ctx.draw();
    }
}
