package pl.nicevisuals.module;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import pl.nicevisuals.gui.Draw;
import pl.nicevisuals.gui.Fx;
import pl.nicevisuals.setting.BoolSetting;
import pl.nicevisuals.setting.ModeSetting;
import pl.nicevisuals.setting.NumSetting;

/** Marks the last entity you hit: corner brackets, a ring at the feet, or orbiting trails. */
public class TargetEsp extends Module {
    // Camera data captured every frame by the world-render hook in NiceVisualsClient.
    public static final Matrix4f PROJ = new Matrix4f(), VIEW = new Matrix4f();
    public static Vec3d cam = Vec3d.ZERO;
    public static float tickDelta;
    public static boolean valid;

    private final ModeSetting style = add(new ModeSetting("Style", 0, "Runes", "Brackets", "Ring", "Orbit"));
    private final ModeSetting color = add(new ModeSetting("Color", 1, "Pink", "Red", "Blue", "White", "Gold"));
    private final NumSetting thick = add(new NumSetting("Thickness", 8, 1, 20));
    private final NumSetting seconds = add(new NumSetting("Hide after s", 5, 1, 20));
    private final BoolSetting glow = add(new BoolSetting("Glow outline", false));
    private LivingEntity target;
    private long lastHit;

    public TargetEsp() { super("Target ESP", Category.COMBAT); }

    public void setTarget(LivingEntity e) {
        if (target != null && target != e) target.setGlowing(false);
        target = e;
        lastHit = System.currentTimeMillis();
    }

    private boolean expired() {
        return target == null || !target.isAlive() || System.currentTimeMillis() - lastHit > seconds.value * 1000;
    }

    @Override
    public void onTick(MinecraftClient mc) {
        if (target == null) return;
        if (expired()) { target.setGlowing(false); target = null; }
        else target.setGlowing(glow.value);
    }

    @Override
    public void onDisable() {
        if (target != null) { target.setGlowing(false); target = null; }
    }

    private int rgb() {
        return switch (color.index) { case 1 -> 0xFF3B3B; case 2 -> 0x4DA3FF; case 3 -> 0xFFFFFF; case 4 -> 0xFFC857; default -> 0xE040FB; };
    }

    private float[] project(Vec3d p, int sw, int sh) {
        Vector4f v = new Vector4f((float) (p.x - cam.x), (float) (p.y - cam.y), (float) (p.z - cam.z), 1f);
        v.mul(VIEW);
        v.mul(PROJ);
        if (v.w <= 0.01f) return null;
        return new float[]{(v.x / v.w * 0.5f + 0.5f) * sw, (0.5f - v.y / v.w * 0.5f) * sh};
    }

    private void seg(Vec3d a, Vec3d b, int sw, int sh, float w, int rgb, float alpha) {
        float[] p = project(a, sw, sh), q = project(b, sw, sh);
        if (p == null || q == null) return;
        Fx.glowLine(p[0], p[1], q[0], q[1], w, rgb, alpha);
    }

    private void circle(double cx, double y, double cz, double r, double rot, int n, int sw, int sh, float w, int rgb, float a) {
        Vec3d prev = null;
        for (int i = 0; i <= n; i++) {
            double ang = rot + i * Math.PI * 2 / n;
            Vec3d cur = new Vec3d(cx + Math.cos(ang) * r, y, cz + Math.sin(ang) * r);
            if (prev != null) seg(prev, cur, sw, sh, w, rgb, a);
            prev = cur;
        }
    }

    @Override
    public void onHud(DrawContext ctx, MinecraftClient mc) {
        if (!valid || expired()) return;
        int sw = ctx.getScaledWindowWidth(), sh = ctx.getScaledWindowHeight();
        LivingEntity e = target;
        double ex = MathHelper.lerp(tickDelta, e.prevX, e.getX());
        double ey = MathHelper.lerp(tickDelta, e.prevY, e.getY());
        double ez = MathHelper.lerp(tickDelta, e.prevZ, e.getZ());
        double hw = e.getWidth() / 2.0, h = e.getHeight();
        double age = System.currentTimeMillis() % 100000L / 1000.0;
        float remain = (float) (seconds.value - (System.currentTimeMillis() - lastHit) / 1000.0);
        float fade = Math.min(1f, remain / 0.5f);
        int rgb = rgb();
        float w = (float) thick.value * 0.85f;

        Fx.begin(ctx);
        switch (style.index) {
            case 0 -> { // magic circle on the ground + orbiting slashes around the body
                double R = Math.max(0.9, hw + 0.7), y = ey + 0.03;
                circle(ex, y, ez, R, 0, 56, sw, sh, w * 0.8f, rgb, fade);
                circle(ex, y, ez, R * 0.82, 0, 56, sw, sh, w * 0.55f, rgb, fade * 0.9f);
                for (int tri = 0; tri < 2; tri++) {
                    double rot = age * (tri == 0 ? 0.9 : -0.9) + tri * Math.PI / 3;
                    Vec3d[] v = new Vec3d[3];
                    for (int k = 0; k < 3; k++) {
                        double ang = rot + k * Math.PI * 2 / 3;
                        v[k] = new Vec3d(ex + Math.cos(ang) * R * 0.82, y, ez + Math.sin(ang) * R * 0.82);
                    }
                    for (int k = 0; k < 3; k++) seg(v[k], v[(k + 1) % 3], sw, sh, w * 0.6f, rgb, fade);
                }
                for (int k = 0; k < 16; k++) { // rune ticks between the two rings
                    double ang = -age * 0.5 + k * Math.PI * 2 / 16;
                    double a1 = R * 0.84, a2 = R * (k % 2 == 0 ? 0.97 : 0.92);
                    seg(new Vec3d(ex + Math.cos(ang) * a1, y, ez + Math.sin(ang) * a1),
                            new Vec3d(ex + Math.cos(ang) * a2, y, ez + Math.sin(ang) * a2), sw, sh, w * 0.5f, rgb, fade);
                }
                for (int arm = 0; arm < 3; arm++) { // rising spiral slashes
                    Vec3d prev = null;
                    for (int k = 0; k < 22; k++) {
                        double ang = age * 2.4 + arm * Math.PI * 2 / 3 - k * 0.12;
                        double ph = ((age * 0.5 + arm * 0.33 - k * 0.012) % 1.0 + 1.0) % 1.0;
                        double hh = h * (0.12 + 0.76 * ph);
                        Vec3d cur = new Vec3d(ex + Math.cos(ang) * (hw + 0.45), ey + hh, ez + Math.sin(ang) * (hw + 0.45));
                        if (prev != null && Math.abs(cur.y - prev.y) < h * 0.2)
                            seg(prev, cur, sw, sh, w * 0.7f * (1f - k / 24f), rgb, fade * (1f - k / 22f));
                        prev = cur;
                    }
                }
            }
            case 1 -> { // thick corner brackets around the projected bounding box
                float minX = Float.MAX_VALUE, minY = Float.MAX_VALUE, maxX = -Float.MAX_VALUE, maxY = -Float.MAX_VALUE;
                int ok = 0;
                for (int i = 0; i < 8; i++) {
                    float[] p = project(new Vec3d(ex + ((i & 1) == 0 ? -hw : hw), ey + ((i & 2) == 0 ? 0 : h),
                            ez + ((i & 4) == 0 ? -hw : hw)), sw, sh);
                    if (p == null) continue;
                    ok++;
                    minX = Math.min(minX, p[0]); maxX = Math.max(maxX, p[0]);
                    minY = Math.min(minY, p[1]); maxY = Math.max(maxY, p[1]);
                }
                if (ok < 8) break;
                float pad = 6 + w;
                float x1 = minX - pad, y1 = minY - pad, x2 = maxX + pad, y2 = maxY + pad;
                float len = Math.max(10f, Math.min(x2 - x1, y2 - y1) * 0.34f);
                float[][] c = {{x1, y1, 1, 1}, {x2, y1, -1, 1}, {x1, y2, 1, -1}, {x2, y2, -1, -1}};
                for (float[] k : c) {
                    Fx.glowLine(k[0], k[1], k[0] + k[2] * len, k[1], w, rgb, fade);
                    Fx.glowLine(k[0], k[1], k[0], k[1] + k[3] * len, w, rgb, fade);
                }
            }
            case 2 -> { // glowing ring sliding up and down the body
                double yOff = (Math.sin(age * 2.2) * 0.5 + 0.5) * h;
                for (int band = 0; band < 3; band++)
                    circle(ex, ey + yOff - band * 0.06, ez, hw + 0.7, 0, 48, sw, sh, w * (1f - band * 0.2f), rgb, fade * (1f - band * 0.3f));
            }
            default -> { // two orbs with long thick trails
                for (int orb = 0; orb < 2; orb++) {
                    Vec3d prev = null;
                    for (int k = 0; k < 26; k++) {
                        double ang = age * 3.0 + orb * Math.PI - k * 0.1;
                        double hh = h * (0.5 + 0.38 * Math.sin(age * 2.0 + orb * 1.7 - k * 0.07));
                        Vec3d cur = new Vec3d(ex + Math.cos(ang) * (hw + 0.7), ey + hh, ez + Math.sin(ang) * (hw + 0.7));
                        if (prev != null) seg(prev, cur, sw, sh, w * (1.2f - k / 26f), rgb, fade * (1f - k / 26f));
                        prev = cur;
                    }
                }
            }
        }
        Fx.end();
    }
}
