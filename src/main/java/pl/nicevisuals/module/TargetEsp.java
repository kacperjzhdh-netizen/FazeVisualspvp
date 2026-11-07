package pl.nicevisuals.module;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import pl.nicevisuals.gui.Draw;
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

    private final ModeSetting style = add(new ModeSetting("Style", 0, "Brackets", "Ring", "Orbit"));
    private final ModeSetting color = add(new ModeSetting("Color", 0, "Pink", "Red", "Blue", "White"));
    private final NumSetting thick = add(new NumSetting("Thickness", 3, 1, 6));
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
        return switch (color.index) { case 1 -> 0xFF2B2B; case 2 -> 0x4DA3FF; case 3 -> 0xFFFFFF; default -> 0xE040FB; };
    }

    private float[] project(Vec3d p, int sw, int sh) {
        Vector4f v = new Vector4f((float) (p.x - cam.x), (float) (p.y - cam.y), (float) (p.z - cam.z), 1f);
        v.mul(VIEW);
        v.mul(PROJ);
        if (v.w <= 0.01f) return null;
        return new float[]{(v.x / v.w * 0.5f + 0.5f) * sw, (0.5f - v.y / v.w * 0.5f) * sh};
    }

    private void dot(DrawContext ctx, float[] p, int size, int argb) {
        if (p == null) return;
        int x = Math.round(p[0] - size / 2f), y = Math.round(p[1] - size / 2f);
        ctx.fill(x, y, x + size, y + size, argb);
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
        int t = thick.asInt();

        switch (style.index) {
            case 0 -> { // corner brackets around the projected bounding box
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
                if (ok < 8) return;
                int x1 = Math.round(minX) - 2, y1 = Math.round(minY) - 2, x2 = Math.round(maxX) + 2, y2 = Math.round(maxY) + 2;
                int len = Math.max(8, Math.round(Math.min(x2 - x1, y2 - y1) * 0.28f));
                int c = Draw.alpha(0xFF000000 | rgb, fade);
                int outline = Draw.alpha(0xAA000000, fade);
                for (int pass = 0; pass < 2; pass++) {
                    int o = pass == 0 ? 1 : 0, col = pass == 0 ? outline : c;
                    // top-left, top-right, bottom-left, bottom-right
                    ctx.fill(x1 - o, y1 - o, x1 + len + o, y1 + t + o, col);
                    ctx.fill(x1 - o, y1 - o, x1 + t + o, y1 + len + o, col);
                    ctx.fill(x2 - len - o, y1 - o, x2 + o, y1 + t + o, col);
                    ctx.fill(x2 - t - o, y1 - o, x2 + o, y1 + len + o, col);
                    ctx.fill(x1 - o, y2 - t - o, x1 + len + o, y2 + o, col);
                    ctx.fill(x1 - o, y2 - len - o, x1 + t + o, y2 + o, col);
                    ctx.fill(x2 - len - o, y2 - t - o, x2 + o, y2 + o, col);
                    ctx.fill(x2 - t - o, y2 - len - o, x2 + o, y2 + o, col);
                }
            }
            case 1 -> { // glowing ring sliding up and down the body
                double yOff = (Math.sin(age * 2.2) * 0.5 + 0.5) * h;
                double r = hw + 0.35;
                for (int band = 0; band < 4; band++) {
                    float a = fade * (1f - band * 0.22f);
                    for (int i = 0; i < 48; i++) {
                        double ang = i * Math.PI * 2 / 48;
                        Vec3d p = new Vec3d(ex + Math.cos(ang) * r, ey + yOff - band * 0.04, ez + Math.sin(ang) * r);
                        dot(ctx, project(p, sw, sh), t, Draw.alpha(0xFF000000 | rgb, a));
                    }
                }
            }
            default -> { // two orbs with fading trails
                for (int orb = 0; orb < 2; orb++) {
                    for (int k = 0; k < 16; k++) {
                        double ang = age * 3.0 + orb * Math.PI - k * 0.13;
                        double hh = h * (0.5 + 0.38 * Math.sin(age * 2.0 + orb * 1.7 - k * 0.09));
                        Vec3d p = new Vec3d(ex + Math.cos(ang) * (hw + 0.4), ey + hh, ez + Math.sin(ang) * (hw + 0.4));
                        float a = fade * (1f - k / 16f);
                        int size = Math.max(1, Math.round((t + 3) * (1f - k / 18f)));
                        dot(ctx, project(p, sw, sh), size, Draw.alpha(0xFF000000 | rgb, a));
                    }
                }
            }
        }
    }
}
