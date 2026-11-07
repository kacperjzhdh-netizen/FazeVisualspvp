package pl.nicevisuals.module;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

public class ReachDisplay extends Module {
    private double reach;
    private long lastHit;

    public ReachDisplay() { super("Reach", Category.COMBAT); }

    public void registerHit(double distance) { reach = distance; lastHit = System.currentTimeMillis(); }

    @Override
    public void onHud(DrawContext ctx, MinecraftClient mc) {
        if (System.currentTimeMillis() - lastHit > 3000) return;
        ctx.drawCenteredTextWithShadow(mc.textRenderer, String.format("%.2f blocks", reach),
                ctx.getScaledWindowWidth() / 2, ctx.getScaledWindowHeight() / 2 + 36, 0xFFB8C2E0);
    }
}
