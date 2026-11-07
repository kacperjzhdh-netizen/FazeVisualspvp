package pl.nicevisuals.module;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.LivingEntity;
import pl.nicevisuals.gui.Draw;
import pl.nicevisuals.setting.NumSetting;

public class TargetHud extends Module {
    private final NumSetting x = add(new NumSetting("X", 200, 0, 800));
    private final NumSetting y = add(new NumSetting("Y", 140, 0, 600));
    private final NumSetting seconds = add(new NumSetting("Hide after s", 4, 1, 15));
    private LivingEntity target;
    private long lastHit;
    private float shownHealth;

    public TargetHud() { super("Target HUD", Category.COMBAT); }

    public void setTarget(LivingEntity e) { target = e; lastHit = System.currentTimeMillis(); }

    @Override
    public void onHud(DrawContext ctx, MinecraftClient mc) {
        if (target == null || !target.isAlive()) return;
        if (System.currentTimeMillis() - lastHit > seconds.value * 1000) return;
        int w = 110, h = 34, px = x.asInt(), py = y.asInt();
        Draw.rrect(ctx, px, py, w, h, 6, 0xCC0B1A33);
        ctx.drawTextWithShadow(mc.textRenderer, target.getName().getString(), px + 8, py + 6, 0xFFFFFFFF);
        float hp = Math.max(0, target.getHealth());
        float max = Math.max(1, target.getMaxHealth());
        shownHealth += (hp - shownHealth) * 0.2f;
        int barW = w - 16;
        Draw.rrect(ctx, px + 8, py + 20, barW, 6, 3, 0xFF14284D);
        int fill = Math.max(0, Math.min(barW, (int) (barW * (shownHealth / max))));
        if (fill > 0) Draw.rrect(ctx, px + 8, py + 20, Math.max(fill, 6), 6, 3, 0xFF4DA3FF);
    }
}
