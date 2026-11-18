package pl.nicevisuals.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import org.joml.Matrix4f;

/** Batched thick anti-alias-friendly lines (real quads, not pixel steps) for the HUD. */
public final class Fx {
    private static BufferBuilder buf;
    private static Matrix4f mat;
    private static int count;

    private Fx() {}

    public static void begin(DrawContext ctx) {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
        mat = ctx.getMatrices().peek().getPositionMatrix();
        buf = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
        count = 0;
    }

    public static void end() {
        if (buf != null && count > 0) BufferRenderer.drawWithGlobalProgram(buf.end());
        buf = null;
        RenderSystem.disableBlend();
    }

    /** One thick line with slightly extended ends so polylines have no gaps. */
    public static void line(float x1, float y1, float x2, float y2, float w, int argb) {
        if (buf == null) return;
        float dx = x2 - x1, dy = y2 - y1;
        float len = (float) Math.sqrt(dx * dx + dy * dy);
        if (len < 0.001f) { dx = 1; dy = 0; len = 1; }
        dx /= len; dy /= len;
        float h = w / 2f;
        float ex = dx * h, ey = dy * h;
        float nx = -dy * h, ny = dx * h;
        float ax = x1 - ex, ay = y1 - ey, bx = x2 + ex, by = y2 + ey;
        buf.vertex(mat, ax + nx, ay + ny, 0).color(argb);
        buf.vertex(mat, ax - nx, ay - ny, 0).color(argb);
        buf.vertex(mat, bx - nx, by - ny, 0).color(argb);
        buf.vertex(mat, bx + nx, by + ny, 0).color(argb);
        count++;
    }

    /** Line with a soft outer glow (wide faint pass under the solid one). */
    public static void glowLine(float x1, float y1, float x2, float y2, float w, int rgb, float alpha) {
        line(x1, y1, x2, y2, w * 2.4f, Draw.alpha(0xFF000000 | rgb, alpha * 0.18f));
        line(x1, y1, x2, y2, w * 1.5f, Draw.alpha(0xFF000000 | rgb, alpha * 0.35f));
        line(x1, y1, x2, y2, w, Draw.alpha(0xFF000000 | rgb, alpha));
    }
}
