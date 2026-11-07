package pl.nicevisuals.module;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.PlayerSkinDrawer;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import pl.nicevisuals.gui.Draw;
import pl.nicevisuals.setting.ModeSetting;
import pl.nicevisuals.setting.NumSetting;

/** Player head, name and a bold health bar with the health number inside. */
public class TargetHud extends Module {
    private final NumSetting x = add(new NumSetting("X", 200, 0, 800));
    private final NumSetting y = add(new NumSetting("Y", 120, 0, 600));
    private final ModeSetting color = add(new ModeSetting("Color", 0, "Red", "Pink", "Blue"));
    private final NumSetting seconds = add(new NumSetting("Hide after s", 4, 1, 15));
    private LivingEntity target;
    private long lastHit;
    private float shownHealth;

    public TargetHud() { super("Target HUD", Category.COMBAT); }

    public void setTarget(LivingEntity e) {
        if (e != target) shownHealth = e.getHealth();
        target = e;
        lastHit = System.currentTimeMillis();
    }

    @Override
    public void onHud(DrawContext ctx, MinecraftClient mc) {
        if (target == null || !target.isAlive()) return;
        if (System.currentTimeMillis() - lastHit > seconds.value * 1000) return;
        int rgb = switch (color.index) { case 1 -> 0xE040FB; case 2 -> 0x4DA3FF; default -> 0xFF2B2B; };
        int accent = 0xFF000000 | rgb;
        int w = 124, h = 38, px = x.asInt(), py = y.asInt();

        Draw.rrect(ctx, px, py, w, h, 4, accent);               // 1px colored frame
        Draw.rrect(ctx, px + 1, py + 1, w - 2, h - 2, 3, 0xF0101010);

        // head
        int hx = px + 5, hy = py + 5, hs = 28;
        if (target instanceof AbstractClientPlayerEntity pl) {
            PlayerSkinDrawer.draw(ctx, pl.getSkinTextures(), hx, hy, hs);
        } else {
            ctx.fill(hx, hy, hx + hs, hy + hs, 0xFF2A2A2A);
            ctx.drawCenteredTextWithShadow(mc.textRenderer, target.getName().getString().substring(0, 1), hx + hs / 2, hy + 10, 0xFFFFFFFF);
        }

        // name
        ctx.drawTextWithShadow(mc.textRenderer, Text.literal(target.getName().getString()).formatted(Formatting.BOLD),
                px + 38, py + 6, 0xFFFFFFFF);

        // health bar
        float hp = Math.max(0, target.getHealth());
        float max = Math.max(1, target.getMaxHealth());
        shownHealth += (hp - shownHealth) * 0.2f;
        int bx = px + 38, by = py + 19, bw = w - 44, bh = 12;
        Draw.rrect(ctx, bx, by, bw, bh, 2, 0xFF000000);
        int fill = Math.max(0, Math.min(bw - 2, Math.round((bw - 2) * (shownHealth / max))));
        if (fill > 0) Draw.rrect(ctx, bx + 1, by + 1, fill, bh - 2, 2, accent);
        ctx.drawCenteredTextWithShadow(mc.textRenderer, Text.literal(String.valueOf(Math.round(hp))).formatted(Formatting.BOLD),
                bx + bw / 2, by + 2, 0xFFFFFFFF);
    }
}
