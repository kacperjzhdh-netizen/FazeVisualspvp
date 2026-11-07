package pl.nicevisuals.module;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import pl.nicevisuals.setting.NumSetting;

public class CoordsHud extends Module {
    private final NumSetting x = add(new NumSetting("X", 6, 0, 400));
    private final NumSetting y = add(new NumSetting("Y", 30, 0, 300));

    public CoordsHud() { super("Coords", Category.HUD); }

    @Override
    public void onHud(DrawContext ctx, MinecraftClient mc) {
        if (mc.player == null) return;
        String s = String.format("XYZ: %.1f / %.1f / %.1f", mc.player.getX(), mc.player.getY(), mc.player.getZ());
        ctx.drawTextWithShadow(mc.textRenderer, s, x.asInt(), y.asInt(), 0xFF4DA3FF);
    }
}
