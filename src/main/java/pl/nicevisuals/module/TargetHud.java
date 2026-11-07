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
    private final ModeSetting color = add(new ModeSetting("Color", 0, "Red", "Pink", "Blue"));
    private final BoolSetting showGear = add(new BoolSetting("Show gear", true));
    private final BoolSetting particles = add(new BoolSetting("Hit particles", true));
    private final NumSetting seconds = add(new NumSetting("Hide after s", 4, 1, 15));
    private final List<Particle> parts = new ArrayList<>();
    private LivingEntity target;
    private long lastHit;
    private boolean burst;
    private float shownHealth;

    public TargetHud() { super("Target HUD", Category.COMBAT); }

    public void setTarget(LivingEntity e) {
        if (e != target) shownHealth = e.getHealth();
        target = e;
        lastHit = System.currentTimeMillis();
        burst = true;
    }

    @Override
    public void onHud(DrawContext ctx, MinecraftClient mc) {
        if (target == null || !target.isAlive()) return;
        long now = System.currentTimeMillis();
        if (now - lastHit > seconds.value * 1000) return;

        int rgb = switch (color.index) { case 1 -> 0xE040FB; case 2 -> 0x4DA3FF; default -> 0xFF2B2B; };
        int accent = 0xFF000000 | rgb;
        boolean gear = showGear.value;
        int w = 176, h = gear ? 60 : 40, px = x.asInt(), py = y.asInt();
        boolean wasPixel = Draw.pixel;
        Draw.pixel = false; // this HUD is always glass

        // frosted glass frame: tinted edge, translucent body, top sheen
        Draw.rrectV(ctx, px, py, w, h, 9, Draw.alpha(accent, 0.75f), Draw.alpha(accent, 0.25f));
        Draw.rrectV(ctx, px + 1, py + 1, w - 2, h - 2, 8, 0xA6262C48, 0xA010142A);
        Draw.rrectV(ctx, px + 3, py + 3, w - 6, h / 3, 6, 0x2EFFFFFF, 0x00FFFFFF);

        // head (shakes + flashes when hit)
        long since = now - lastHit;
        boolean flinch = since < FLINCH_MS;
        int hs = 28, hx = px + 5, hy = py + 5;
        if (flinch) { hx += ThreadLocalRandom.current().nextInt(-1, 2); hy += ThreadLocalRandom.current().nextInt(-1, 2); }
        if (target instanceof AbstractClientPlayerEntity pl) {
            PlayerSkinDrawer.draw(ctx, pl.getSkinTextures(), hx, hy, hs);
        } else {
            ctx.fill(hx, hy, hx + hs, hy + hs, 0xFF2A2A2A);
            ctx.drawCenteredTextWithShadow(mc.textRenderer, target.getName().getString().substring(0, 1), hx + hs / 2, hy + 10, 0xFFFFFFFF);
        }
        if (flinch) ctx.fill(hx, hy, hx + hs, hy + hs, Draw.alpha(0x88FF2020, 1f - since / (float) FLINCH_MS));

        // name
        ctx.drawTextWithShadow(mc.textRenderer, Text.literal(target.getName().getString()).formatted(Formatting.BOLD),
                px + 38, py + 6, 0xFFFFFFFF);

        // health bar
        float hp = Math.max(0, target.getHealth());
        float max = Math.max(1, target.getMaxHealth());
        shownHealth += (hp - shownHealth) * 0.2f;
        int bx = px + 38, by = py + 19, bw = w - 44, bh = 12;
        Draw.rrect(ctx, bx, by, bw, bh, 5, 0x99000000);
        int fill = Math.max(0, Math.min(bw - 2, Math.round((bw - 2) * (shownHealth / max))));
        if (fill > 0) Draw.rrectGrad(ctx, bx + 1, by + 1, fill, bh - 2, 4, accent, Draw.mix(accent, 0xFFFFFFFF, 0.35f));
        ctx.drawCenteredTextWithShadow(mc.textRenderer, Text.literal(String.valueOf(Math.round(hp))).formatted(Formatting.BOLD),
                bx + bw / 2, by + 2, 0xFFFFFFFF);

        // gear: main hand, armor, off hand
        if (gear) {
            int gx = px + 6, gy = py + 38;
            for (int i = 0; i < GEAR.length; i++) {
                ItemStack s = target.getEquippedStack(GEAR[i]);
                Draw.rrect(ctx, gx + i * 19 - 1, gy - 1, 18, 18, 5, 0x30FFFFFF);
                if (s.isEmpty()) continue;
                ctx.drawItem(s, gx + i * 19, gy);
                ctx.drawStackOverlay(mc.textRenderer, s, gx + i * 19, gy);
            }
        }

        // hit particles bursting from the head
        if (particles.value) {
            if (burst) {
                ThreadLocalRandom r = ThreadLocalRandom.current();
                for (int i = 0; i < 12; i++) {
                    Particle p = new Particle();
                    p.x = px + 5 + hs / 2f; p.y = py + 5 + hs / 2f;
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
