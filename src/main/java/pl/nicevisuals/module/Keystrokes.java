package pl.nicevisuals.module;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.option.KeyBinding;
import pl.nicevisuals.gui.Draw;
import pl.nicevisuals.setting.NumSetting;

public class Keystrokes extends Module {
    private final NumSetting x = add(new NumSetting("X", 6, 0, 800));
    private final NumSetting y = add(new NumSetting("Y", 150, 0, 600));

    public Keystrokes() { super("Keystrokes", Category.HUD); }

    private void key(DrawContext ctx, MinecraftClient mc, KeyBinding k, String label, int kx, int ky, int w) {
        boolean down = k.isPressed();
        Draw.rrect(ctx, kx, ky, w, 20, 5, down ? 0xCC4DA3FF : 0x990B1A33);
        ctx.drawCenteredTextWithShadow(mc.textRenderer, label, kx + w / 2, ky + 6, 0xFFFFFFFF);
    }

    @Override
    public void onHud(DrawContext ctx, MinecraftClient mc) {
        int px = x.asInt(), py = y.asInt();
        key(ctx, mc, mc.options.forwardKey, "W", px + 23, py, 20);
        key(ctx, mc, mc.options.leftKey, "A", px, py + 23, 20);
        key(ctx, mc, mc.options.backKey, "S", px + 23, py + 23, 20);
        key(ctx, mc, mc.options.rightKey, "D", px + 46, py + 23, 20);
        key(ctx, mc, mc.options.jumpKey, "SPACE", px, py + 46, 66);
    }
}
