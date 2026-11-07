package pl.nicevisuals.module;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import pl.nicevisuals.setting.NumSetting;

public class HitMarker extends Module {
    private final NumSetting size = add(new NumSetting("Size", 5, 2, 12));
    private final NumSetting duration = add(new NumSetting("Duration ms", 250, 100, 1000));
    private long lastHit;

    public HitMarker() { super("Hit Marker", Category.COMBAT); }

    public void registerHit() { lastHit = System.currentTimeMillis(); }

    @Override
    public void onHud(DrawContext ctx, MinecraftClient mc) {
        long age = System.currentTimeMillis() - lastHit;
        if (age > duration.value) return;
        int alpha = (int) (255 * (1.0 - age / duration.value));
        int color = (alpha << 24) | 0x4DA3FF;
        int cx = ctx.getScaledWindowWidth() / 2;
        int cy = ctx.getScaledWindowHeight() / 2;
        int gap = 3, len = size.asInt();
        for (int i = 0; i < len; i++) {
            int d = gap + i;
            ctx.fill(cx + d, cy + d, cx + d + 1, cy + d + 1, color);
            ctx.fill(cx - d - 1, cy + d, cx - d, cy + d + 1, color);
            ctx.fill(cx + d, cy - d - 1, cx + d + 1, cy - d, color);
            ctx.fill(cx - d - 1, cy - d - 1, cx - d, cy - d, color);
        }
    }
}
