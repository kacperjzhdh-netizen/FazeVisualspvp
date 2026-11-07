package pl.nicevisuals.module;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

public class ComboCounter extends Module {
    private int combo, prevHurt;
    private long lastHit;

    public ComboCounter() { super("Combo", Category.COMBAT); }

    public void registerHit() {
        long now = System.currentTimeMillis();
        combo = (now - lastHit > 3000) ? 1 : combo + 1;
        lastHit = now;
    }

    @Override
    public void onTick(MinecraftClient mc) {
        if (mc.player == null) return;
        if (mc.player.hurtTime > 0 && prevHurt == 0) combo = 0;
        prevHurt = mc.player.hurtTime;
        if (System.currentTimeMillis() - lastHit > 3000) combo = 0;
    }

    @Override
    public void onHud(DrawContext ctx, MinecraftClient mc) {
        if (combo < 2) return;
        ctx.drawCenteredTextWithShadow(mc.textRenderer, combo + " combo",
                ctx.getScaledWindowWidth() / 2, ctx.getScaledWindowHeight() / 2 + 24, 0xFF4DA3FF);
    }
}
