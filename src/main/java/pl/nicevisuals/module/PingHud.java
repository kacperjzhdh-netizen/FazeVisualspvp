package pl.nicevisuals.module;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.PlayerListEntry;
import pl.nicevisuals.setting.NumSetting;

public class PingHud extends Module {
    private final NumSetting x = add(new NumSetting("X", 6, 0, 800));
    private final NumSetting y = add(new NumSetting("Y", 42, 0, 600));

    public PingHud() { super("Ping", Category.HUD); }

    @Override
    public void onHud(DrawContext ctx, MinecraftClient mc) {
        if (mc.player == null || mc.getNetworkHandler() == null) return;
        PlayerListEntry e = mc.getNetworkHandler().getPlayerListEntry(mc.player.getUuid());
        String s = e == null ? "Ping: -" : "Ping: " + e.getLatency() + " ms";
        ctx.drawTextWithShadow(mc.textRenderer, s, x.asInt(), y.asInt(), 0xFF4DA3FF);
    }
}
