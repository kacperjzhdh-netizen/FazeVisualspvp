package pl.nicevisuals.module;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import pl.nicevisuals.setting.NumSetting;

public class FpsHud extends Module {
    private final NumSetting x = add(new NumSetting("X", 6, 0, 400));
    private final NumSetting y = add(new NumSetting("Y", 6, 0, 300));

    public FpsHud() { super("FPS", Category.HUD); }

    @Override
    public void onHud(DrawContext ctx, MinecraftClient mc) {
        ctx.drawTextWithShadow(mc.textRenderer, "FPS: " + mc.getCurrentFps(), x.asInt(), y.asInt(), 0xFF4DA3FF);
    }
}
