package pl.nicevisuals.module;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import pl.nicevisuals.setting.NumSetting;

import java.util.ArrayDeque;

public class CpsHud extends Module {
    private final NumSetting x = add(new NumSetting("X", 6, 0, 400));
    private final NumSetting y = add(new NumSetting("Y", 18, 0, 300));
    private final ArrayDeque<Long> clicks = new ArrayDeque<>();
    private boolean wasDown;

    public CpsHud() { super("CPS", Category.HUD); }

    @Override
    public void onTick(MinecraftClient mc) {
        boolean down = mc.options.attackKey.isPressed();
        if (down && !wasDown) clicks.addLast(System.currentTimeMillis());
        wasDown = down;
    }

    @Override
    public void onHud(DrawContext ctx, MinecraftClient mc) {
        long now = System.currentTimeMillis();
        while (!clicks.isEmpty() && now - clicks.peekFirst() > 1000) clicks.pollFirst();
        ctx.drawTextWithShadow(mc.textRenderer, "CPS: " + clicks.size(), x.asInt(), y.asInt(), 0xFF4DA3FF);
    }
}
