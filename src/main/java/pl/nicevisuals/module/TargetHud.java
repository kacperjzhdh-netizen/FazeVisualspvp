package pl.nicevisuals.module;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.PlayerSkinDrawer;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import pl.nicevisuals.gui.Draw;
import pl.nicevisuals.setting.BoolSetting;
import pl.nicevisuals.setting.ModeSetting;
import pl.nicevisuals.setting.NumSetting;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/** Head, name, bold health bar, the target's gear, and particles on the head when you hit. */
public class TargetHud extends Module {
    private static final EquipmentSlot[] GEAR = {
            EquipmentSlot.MAINHAND, EquipmentSlot.HEAD, EquipmentSlot.CHEST,
            EquipmentSlot.LEGS, EquipmentSlot.FEET, EquipmentSlot.OFFHAND };
    private static final long LIFE_MS = 550, FLINCH_MS = 160;

    private static final class Particle {
        float x, y, vx, vy; long born; int size;
    }

    private final NumSetting x = add(new NumSetting("X", 200, 0, 800));
    private final NumSetting y = add(new NumSetting("Y", 120, 0, 600));
    private final ModeSetting color = add(new ModeSetting("Color", 0, "Red", "Pink", "Blue", "Health"));
    private final BoolSetting showGear = add(new BoolSetting("Show gear", true));
    private final BoolSetting particles = add(new BoolSetting("Hit particles", true));
    private final NumSetting seconds = add(new NumSetting("Hide after s", 4, 1, 15));
    private final List<Particle> parts = new ArrayList<>();
    private LivingEntity target;
    private long lastHit;
    private boolean burst;
    private float shownHealth, trail;
    private long shownAt;

    public TargetHud() { super("Target HUD", Category.COMBAT); }

    public void setTarget(LivingEntity e) {
        long t0 = System.currentTimeMillis();
        if (e != target) { shownHealth = e.getHealth(); trail = shownHealth; }
        if (target == null || e != target || t0 - lastHit > seconds.value * 1000) shownAt = t0;
        target = e;
        lastHit = t0;
        burst = true;
    }

    @Override
    public void onHud(DrawContext ctx, MinecraftClient mc) {
        if (target == null || !target.isAlive()) return;
        long now = System.currentTimeMillis();
        long left = (long) (seconds.value * 1000) - (now - lastHit);
        if (left <= 0) return;
        float fade = Math.min(1f, Math.min((now - shownAt) / 180f, left / 300f));
        float hp = Math.max(0, target.getHealth());
        float max = Math.max(1, target.getMaxHealth());
        float absorb = target.getAbsorptionAmount();

        int rgb = switch (color.index) { case 1 -> 0xE040FB; case 2 -> 0x4DA3FF;
            case 3 -> java.awt.Color.HSBtoRGB(Math.min(1f, hp / max) * 0.33f, 0.8f, 1f) & 0xFFFFFF;
            default -> 0xFF2B2B; };
        int accent = 0xFF000000 | rgb;
        boolean gear = showGear.value;
        int w = 200, h = gear ? 70 : 48, px = x.asInt(), py = y.asInt();
        boolean wasPixel = Draw.pixel;
        Draw.pixel = false;

        // glass frame
        Draw.rrectV(ctx, px, py, w, h, 11, Draw.alpha(accent, 0.8f * fade), Draw.alpha(accent, 0.22f * fade));
        Draw.rrectV(ctx, px + 1, py + 1, w - 2, h - 2, 10, Draw.alpha(0xFF262C48, 0.68f * fade), Draw.alpha(0xFF10142A, 0.62f * fade));
        Draw.rrectV(ctx, px + 3, py + 3, w - 6, h / 3, 8, Draw.alpha(0xFFFFFFFF, 0.16f * fade), 0x00FFFFFF);

        // head (shakes + flashes when hit)
        long since = now - lastHit;
        boolean flinch = since < FLINCH_MS;
        int hs = 34, hx = px + 7, hy = py + 7;
        if (flinch) { hx += ThreadLocalRandom.current().nextInt(-1, 2); hy += ThreadLocalRandom.current().nextInt(-1, 2); }
        Draw.rrect(ctx, hx - 1, hy - 1, hs + 2, hs + 2, 6, Draw.alpha(accent, 0.6f * fade));
        if (target instanceof AbstractClientPlayerEntity pl) {
            PlayerSkinDrawer.draw(ctx, pl.getSkinTextures(), hx, hy, hs);
        } else {
            ctx.fill(hx, hy, hx + hs, hy + hs, 0xFF2A2A2A);
            ctx.drawCenteredTextWithShadow(mc.textRenderer, target.getName().getString().substring(0, 1), hx + hs / 2, hy + 13, 0xFFFFFFFF);
        }
        if (flinch) ctx.fill(hx, hy, hx + hs, hy + hs, Draw.alpha(0x88FF2020, 1f - since / (float) FLINCH_MS));

        // name + distance
        int tx = px + 48;
        ctx.drawTextWithShadow(mc.textRenderer, Text.literal(target.getName().getString()).formatted(Formatting.BOLD),
                tx, py + 8, Draw.alpha(0xFFFFFFFF, fade));
        if (mc.player != null) {
            String d = String.format("%.1fm", mc.player.distanceTo(target));
            ctx.drawTextWithShadow(mc.textRenderer, d, px + w - 8 - mc.textRenderer.getWidth(d), py + 8, Draw.alpha(0xFFB8C0E0, fade));
        }

        // health bar with a white damage trail that catches up slowly
        shownHealth += (hp - shownHealth) * 0.25f;
        if (trail < shownHealth) trail = shownHealth; else trail += (shownHealth - trail) * 0.05f;
        int bx = tx, by = py + 22, bw = w - 56, bh = 12;
        Draw.rrect(ctx, bx, by, bw, bh, 5, Draw.alpha(0xFF000000, 0.55f * fade));
        int trailW = Math.max(0, Math.min(bw - 2, Math.round((bw - 2) * (trail / max))));
        if (trailW > 0) Draw.rrect(ctx, bx + 1, by + 1, trailW, bh - 2, 4, Draw.alpha(0xFFFFFFFF, 0.45f * fade));
        int fill = Math.max(0, Math.min(bw - 2, Math.round((bw - 2) * (shownHealth / max))));
        if (fill > 0) Draw.rrectGrad(ctx, bx + 1, by + 1, fill, bh - 2, 4, Draw.alpha(accent, fade), Draw.alpha(Draw.mix(accent, 0xFFFFFFFF, 0.35f), fade));
        String hpText = Math.round(hp) + " / " + Math.round(max) + (absorb > 0 ? "  +" + Math.round(absorb) : "");
        ctx.drawCenteredTextWithShadow(mc.textRenderer, hpText, bx + bw / 2, by + 2,
                absorb > 0 ? Draw.alpha(0xFFFFD44D, fade) : Draw.alpha(0xFFFFFFFF, fade));
        int armor = target.getArmor();
        String ar = armor + "";
        ctx.drawTextWithShadow(mc.textRenderer, ar, px + w - 8 - mc.textRenderer.getWidth(ar), by + 2, Draw.alpha(0xFFA9C4FF, fade));

        // gear: main hand, armor, off hand
        if (gear) {
            int gx = px + 8, gy = py + 48;
            for (int i = 0; i < GEAR.length; i++) {
                ItemStack st = target.getEquippedStack(GEAR[i]);
                Draw.rrect(ctx, gx + i * 21 - 1, gy - 1, 18, 18, 5, Draw.alpha(0xFFFFFFFF, 0.14f * fade));
                if (st.isEmpty()) continue;
                ctx.drawItem(st, gx + i * 21, gy);
                ctx.drawStackOverlay(mc.textRenderer, st, gx + i * 21, gy);
            }
        }

        // hit particles bursting from the head
        if (particles.value) {
            if (burst) {
                ThreadLocalRandom r = ThreadLocalRandom.current();
                for (int i = 0; i < 14; i++) {
                    Particle p = new Particle();
                    p.x = px + 7 + hs / 2f; p.y = py + 7 + hs / 2f;
                    double a = r.nextDouble(0, Math.PI * 2), sp = r.nextDouble(25, 80);
                    p.vx = (float) (Math.cos(a) * sp); p.vy = (float) (Math.sin(a) * sp) - 20f;
                    p.born = now; p.size = r.nextInt(2, 5);
                    parts.add(p);
                }
            }
            Iterator<Particle> it = parts.iterator();
            while (it.hasNext()) {
                Particle p = it.next();
                float t = (now - p.born) / (float) LIFE_MS;
                if (t >= 1f) { it.remove(); continue; }
                float sec = (now - p.born) / 1000f;
                int cx = Math.round(p.x + p.vx * sec), cy = Math.round(p.y + p.vy * sec + 60f * sec * sec);
                int c = Draw.alpha(Draw.mix(0xFFFFFFFF, accent, t), 1f - t);
                ctx.fill(cx, cy, cx + p.size, cy + p.size, c);
            }
        }
        burst = false;
        Draw.pixel = wasPixel;
    }
}
