package pl.nicevisuals.module;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.effect.StatusEffectInstance;
import pl.nicevisuals.setting.NumSetting;

public class PotionHud extends Module {
    private final NumSetting x = add(new NumSetting("X", 6, 0, 800));
    private final NumSetting y = add(new NumSetting("Y", 220, 0, 600));

    public PotionHud() { super("Potion HUD", Category.HUD); }

    @Override
    public void onHud(DrawContext ctx, MinecraftClient mc) {
        if (mc.player == null) return;
        int yy = y.asInt();
        for (StatusEffectInstance e : mc.player.getStatusEffects()) {
            int sec = e.getDuration() / 20;
            String s = e.getEffectType().value().getName().getString() + " " + (e.getAmplifier() + 1)
                    + "  " + (sec / 60) + ":" + String.format("%02d", sec % 60);
            ctx.drawTextWithShadow(mc.textRenderer, s, x.asInt(), yy, 0xFF4DA3FF);
            yy += 11;
        }
    }
}
