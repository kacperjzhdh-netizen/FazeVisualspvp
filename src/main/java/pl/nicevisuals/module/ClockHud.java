package pl.nicevisuals.module;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import pl.nicevisuals.setting.NumSetting;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class ClockHud extends Module {
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("HH:mm:ss");
    private final NumSetting x = add(new NumSetting("X", 6, 0, 800));
    private final NumSetting y = add(new NumSetting("Y", 66, 0, 600));

    public ClockHud() { super("Clock", Category.HUD); }

    @Override
    public void onHud(DrawContext ctx, MinecraftClient mc) {
        ctx.drawTextWithShadow(mc.textRenderer, LocalTime.now().format(FMT), x.asInt(), y.asInt(), 0xFF4DA3FF);
    }
}
