package pl.nicevisuals.module;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import pl.nicevisuals.setting.NumSetting;

public class SpeedHud extends Module {
    private final NumSetting x = add(new NumSetting("X", 6, 0, 800));
    private final NumSetting y = add(new NumSetting("Y", 54, 0, 600));
    private double px, pz, speed;

    public SpeedHud() { super("Speed", Category.HUD); }

    @Override
    public void onTick(MinecraftClient mc) {
        if (mc.player == null) return;
        double dx = mc.player.getX() - px, dz = mc.player.getZ() - pz;
        px = mc.player.getX(); pz = mc.player.getZ();
        speed += (Math.hypot(dx, dz) * 20 - speed) * 0.3;
    }

    @Override
    public void onHud(DrawContext ctx, MinecraftClient mc) {
        ctx.drawTextWithShadow(mc.textRenderer, String.format("Speed: %.1f b/s", speed), x.asInt(), y.asInt(), 0xFF4DA3FF);
    }
}
