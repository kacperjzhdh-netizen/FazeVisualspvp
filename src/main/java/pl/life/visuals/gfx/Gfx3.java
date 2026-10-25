package pl.life.visuals.gfx;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

/** Pomocnik rysowania przezroczystej geometrii w swiecie (jedna partia trojkatow). */
public final class Gfx3 {
    private Gfx3() {}

    private static BufferBuilder b;
    private static Matrix4f m;
    public static double ox, oy, oz;
    /** Wektory kamery: prawo, gora, przod. */
    public static double rx, ry, rz, ux, uy, uz, fx, fy, fz;
    private static float pxScale;
    private static int count;

    public static void begin(MatrixStack ms, Camera cam, float fovDeg, int fbHeight) {
        Vec3d p = cam.getPos();
        ox = p.x;
        oy = p.y;
        oz = p.z;
        double yaw = Math.toRadians(cam.getYaw()), pitch = Math.toRadians(cam.getPitch());
        double cy = Math.cos(yaw), sy = Math.sin(yaw), cp = Math.cos(pitch), sp = Math.sin(pitch);
        fx = -sy * cp;
        fy = -sp;
        fz = cy * cp;
        rx = -cy;
        ry = 0;
        rz = -sy;
        // gora = prawo x przod
        ux = ry * fz - rz * fy;
        uy = rz * fx - rx * fz;
        uz = rx * fy - ry * fx;
        pxScale = (float) (2.0 * Math.tan(Math.toRadians(fovDeg) / 2.0) / Math.max(1, fbHeight));
        m = ms.peek().getPositionMatrix();
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(false);
        b = Tessellator.getInstance().begin(VertexFormat.DrawMode.TRIANGLES, VertexFormats.POSITION_COLOR);
        count = 0;
    }

    public static void end() {
        if (count > 0) {
            BufferRenderer.drawWithGlobalProgram(b.end());
        } else {
            // pusty bufor nie moze zostac zakonczony; wypelniamy niewidocznym trojkatem
            b.vertex(m, 0, 0, 0).color(0);
            b.vertex(m, 0, 0, 0).color(0);
            b.vertex(m, 0, 0, 0).color(0);
            BufferRenderer.drawWithGlobalProgram(b.end());
        }
        RenderSystem.depthMask(true);
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
        b = null;
    }

    public static int triangles() {
        return count;
    }

    public static void v(double x, double y, double z, int argb) {
        b.vertex(m, (float) (x - ox), (float) (y - oy), (float) (z - oz)).color(argb);
    }

    public static void tri(double x1, double y1, double z1, int c1, double x2, double y2, double z2, int c2, double x3, double y3, double z3, int c3) {
        v(x1, y1, z1, c1);
        v(x2, y2, z2, c2);
        v(x3, y3, z3, c3);
        count++;
    }

    public static void quad(double x1, double y1, double z1, int c1, double x2, double y2, double z2, int c2,
                            double x3, double y3, double z3, int c3, double x4, double y4, double z4, int c4) {
        tri(x1, y1, z1, c1, x2, y2, z2, c2, x3, y3, z3, c3);
        tri(x1, y1, z1, c1, x3, y3, z3, c3, x4, y4, z4, c4);
    }

    public static double dist(double x, double y, double z) {
        double dx = x - ox, dy = y - oy, dz = z - oz;
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    /** Miekka linia o stalej grubosci w pikselach (zawsze zwrocona do kamery). */
    public static void line(double x1, double y1, double z1, double x2, double y2, double z2, float px, int color) {
        double dx = x2 - x1, dy = y2 - y1, dz = z2 - z1;
        double mx = (x1 + x2) / 2 - ox, my = (y1 + y2) / 2 - oy, mz = (z1 + z2) / 2 - oz;
        double cx = dy * mz - dz * my, cy = dz * mx - dx * mz, cz = dx * my - dy * mx;
        double len = Math.sqrt(cx * cx + cy * cy + cz * cz);
        if (len < 1e-9) return;
        double dist = Math.sqrt(mx * mx + my * my + mz * mz);
        double w = px * dist * pxScale / 2.0;
        double sx = cx / len * w, sy = cy / len * w, sz = cz / len * w;
        int clear = color & 0xFFFFFF;
        // trzy pasy: rozmycie - rdzen - rozmycie
        double hx = sx * 0.5, hy = sy * 0.5, hz = sz * 0.5;
        quad(x1 - sx, y1 - sy, z1 - sz, clear, x2 - sx, y2 - sy, z2 - sz, clear, x2 - hx, y2 - hy, z2 - hz, color, x1 - hx, y1 - hy, z1 - hz, color);
        quad(x1 - hx, y1 - hy, z1 - hz, color, x2 - hx, y2 - hy, z2 - hz, color, x2 + hx, y2 + hy, z2 + hz, color, x1 + hx, y1 + hy, z1 + hz, color);
        quad(x1 + hx, y1 + hy, z1 + hz, color, x2 + hx, y2 + hy, z2 + hz, color, x2 + sx, y2 + sy, z2 + sz, clear, x1 + sx, y1 + sy, z1 + sz, clear);
    }

    public static void boxEdges(Box bx, float px, int color) {
        double x1 = bx.minX, y1 = bx.minY, z1 = bx.minZ, x2 = bx.maxX, y2 = bx.maxY, z2 = bx.maxZ;
        line(x1, y1, z1, x2, y1, z1, px, color);
        line(x2, y1, z1, x2, y1, z2, px, color);
        line(x2, y1, z2, x1, y1, z2, px, color);
        line(x1, y1, z2, x1, y1, z1, px, color);
        line(x1, y2, z1, x2, y2, z1, px, color);
        line(x2, y2, z1, x2, y2, z2, px, color);
        line(x2, y2, z2, x1, y2, z2, px, color);
        line(x1, y2, z2, x1, y2, z1, px, color);
        line(x1, y1, z1, x1, y2, z1, px, color);
        line(x2, y1, z1, x2, y2, z1, px, color);
        line(x2, y1, z2, x2, y2, z2, px, color);
        line(x1, y1, z2, x1, y2, z2, px, color);
    }

    public static void boxFill(Box bx, int color) {
        double x1 = bx.minX, y1 = bx.minY, z1 = bx.minZ, x2 = bx.maxX, y2 = bx.maxY, z2 = bx.maxZ;
        quad(x1, y1, z1, color, x2, y1, z1, color, x2, y1, z2, color, x1, y1, z2, color);
        quad(x1, y2, z1, color, x1, y2, z2, color, x2, y2, z2, color, x2, y2, z1, color);
        quad(x1, y1, z1, color, x1, y2, z1, color, x2, y2, z1, color, x2, y1, z1, color);
        quad(x1, y1, z2, color, x2, y1, z2, color, x2, y2, z2, color, x1, y2, z2, color);
        quad(x1, y1, z1, color, x1, y1, z2, color, x1, y2, z2, color, x1, y2, z1, color);
        quad(x2, y1, z1, color, x2, y2, z1, color, x2, y2, z2, color, x2, y1, z2, color);
    }

    /** Plaski pierscien na poziomej plaszczyznie. */
    public static void ringFlat(double cx, double y, double cz, double rIn, double rOut, int segs, int colIn, int colOut) {
        for (int i = 0; i < segs; i++) {
            double a1 = Math.PI * 2 * i / segs, a2 = Math.PI * 2 * (i + 1) / segs;
            double c1 = Math.cos(a1), s1 = Math.sin(a1), c2 = Math.cos(a2), s2 = Math.sin(a2);
            quad(cx + c1 * rIn, y, cz + s1 * rIn, colIn, cx + c1 * rOut, y, cz + s1 * rOut, colOut,
                    cx + c2 * rOut, y, cz + s2 * rOut, colOut, cx + c2 * rIn, y, cz + s2 * rIn, colIn);
        }
    }

    public static void discFlat(double cx, double y, double cz, double r, int segs, int colCenter, int colEdge) {
        for (int i = 0; i < segs; i++) {
            double a1 = Math.PI * 2 * i / segs, a2 = Math.PI * 2 * (i + 1) / segs;
            tri(cx, y, cz, colCenter, cx + Math.cos(a1) * r, y, cz + Math.sin(a1) * r, colEdge,
                    cx + Math.cos(a2) * r, y, cz + Math.sin(a2) * r, colEdge);
        }
    }

    /** Okrag zwrocony do kamery (billboard). */
    public static void discBillboard(double cx, double cy, double cz, double r, int segs, int colCenter, int colEdge) {
        for (int i = 0; i < segs; i++) {
            double a1 = Math.PI * 2 * i / segs, a2 = Math.PI * 2 * (i + 1) / segs;
            double c1 = Math.cos(a1) * r, s1 = Math.sin(a1) * r, c2 = Math.cos(a2) * r, s2 = Math.sin(a2) * r;
            tri(cx, cy, cz, colCenter,
                    cx + rx * c1 + ux * s1, cy + ry * c1 + uy * s1, cz + rz * c1 + uz * s1, colEdge,
                    cx + rx * c2 + ux * s2, cy + ry * c2 + uy * s2, cz + rz * c2 + uz * s2, colEdge);
        }
    }

    public static void ringBillboard(double cx, double cy, double cz, double rIn, double rOut, int segs, int colIn, int colOut) {
        for (int i = 0; i < segs; i++) {
            double a1 = Math.PI * 2 * i / segs, a2 = Math.PI * 2 * (i + 1) / segs;
            double c1 = Math.cos(a1), s1 = Math.sin(a1), c2 = Math.cos(a2), s2 = Math.sin(a2);
            quad(cx + (rx * c1 + ux * s1) * rIn, cy + (ry * c1 + uy * s1) * rIn, cz + (rz * c1 + uz * s1) * rIn, colIn,
                    cx + (rx * c1 + ux * s1) * rOut, cy + (ry * c1 + uy * s1) * rOut, cz + (rz * c1 + uz * s1) * rOut, colOut,
                    cx + (rx * c2 + ux * s2) * rOut, cy + (ry * c2 + uy * s2) * rOut, cz + (rz * c2 + uz * s2) * rOut, colOut,
                    cx + (rx * c2 + ux * s2) * rIn, cy + (ry * c2 + uy * s2) * rIn, cz + (rz * c2 + uz * s2) * rIn, colIn);
        }
    }

    /** Stozek (apex nad srodkiem podstawy). */
    public static void cone(double cx, double baseY, double cz, double r, double height, int segs, int colApex, int colBase) {
        for (int i = 0; i < segs; i++) {
            double a1 = Math.PI * 2 * i / segs, a2 = Math.PI * 2 * (i + 1) / segs;
            tri(cx, baseY + height, cz, colApex,
                    cx + Math.cos(a1) * r, baseY, cz + Math.sin(a1) * r, colBase,
                    cx + Math.cos(a2) * r, baseY, cz + Math.sin(a2) * r, colBase);
        }
    }

    /** Wstega miedzy kolejnymi punktami sladu (zwrocona do kamery), z gradientem koloru/alfa. */
    public static void ribbon(double[] xs, double[] ys, double[] zs, int n, float widthWorld, int colHead, int colTail) {
        if (n < 2) return;
        double[] lx = new double[n], ly = new double[n], lz = new double[n];
        double[] rxs = new double[n], rys = new double[n], rzs = new double[n];
        for (int i = 0; i < n; i++) {
            int a = Math.max(0, i - 1), c = Math.min(n - 1, i + 1);
            double tx = xs[c] - xs[a], ty = ys[c] - ys[a], tz = zs[c] - zs[a];
            double vx = xs[i] - ox, vy = ys[i] - oy, vz = zs[i] - oz;
            double sx = ty * vz - tz * vy, sy = tz * vx - tx * vz, sz = tx * vy - ty * vx;
            double l = Math.sqrt(sx * sx + sy * sy + sz * sz);
            if (l < 1e-9) {
                sx = rx;
                sy = ry;
                sz = rz;
                l = 1;
            }
            double k = widthWorld / 2.0 / l;
            lx[i] = xs[i] - sx * k;
            ly[i] = ys[i] - sy * k;
            lz[i] = zs[i] - sz * k;
            rxs[i] = xs[i] + sx * k;
            rys[i] = ys[i] + sy * k;
            rzs[i] = zs[i] + sz * k;
        }
        for (int i = 0; i < n - 1; i++) {
            float t1 = i / (float) (n - 1), t2 = (i + 1) / (float) (n - 1);
            int c1 = Render2D.lerp(colHead, colTail, t1), c2 = Render2D.lerp(colHead, colTail, t2);
            quad(lx[i], ly[i], lz[i], c1, rxs[i], rys[i], rzs[i], c1, rxs[i + 1], rys[i + 1], rzs[i + 1], c2, lx[i + 1], ly[i + 1], lz[i + 1], c2);
        }
    }
}
