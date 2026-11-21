package pl.nicevisuals.module;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.screen.multiplayer.AddServerScreen;
import net.minecraft.client.gui.screen.multiplayer.ConnectScreen;
import net.minecraft.client.gui.screen.multiplayer.DirectConnectScreen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.gui.screen.option.OptionsScreen;
import net.minecraft.client.gui.screen.world.SelectWorldScreen;
import pl.nicevisuals.gui.Draw;
import pl.nicevisuals.setting.BoolSetting;
import pl.nicevisuals.setting.NumSetting;

import java.awt.Color;

/** Colour glow layered over Minecraft's menus (title, multiplayer, add server, ...). */
public class MenuStyle extends Module {
    private final NumSetting strength = add(new NumSetting("Strength", 40, 5, 100));
    private final NumSetting hue = add(new NumSetting("Hue", 140, 0, 360));
    private final BoolSetting glow = add(new BoolSetting("Moving glow", true));

    public MenuStyle() { super("Menu Style", Category.CLIENT); }

    public static boolean applies(Screen s) {
        return s instanceof TitleScreen || s instanceof MultiplayerScreen || s instanceof AddServerScreen
                || s instanceof DirectConnectScreen || s instanceof SelectWorldScreen || s instanceof OptionsScreen
                || s instanceof ConnectScreen;
    }

    private int col(float h, float s, float b, float a) {
        return Draw.alpha(0xFF000000 | (Color.HSBtoRGB((((h % 360f) + 360f) % 360f) / 360f, s, b) & 0xFFFFFF), a);
    }

    public void draw(DrawContext ctx, int w, int h) {
        float k = (float) (strength.value / 100.0), hu = (float) hue.value;
        ctx.fillGradient(0, 0, w, h, col(hu, 0.7f, 0.9f, 0.30f * k), col(hu + 50, 0.8f, 0.35f, 0.42f * k));
        if (!glow.value) return;
        double t = System.currentTimeMillis() % 200000L / 1000.0;
        for (int i = 0; i < 4; i++) {
            float cx = (float) (w * (0.5 + 0.38 * Math.sin(t * 0.21 + i * 1.7)));
            float cy = (float) (h * (0.5 + 0.34 * Math.cos(t * 0.17 + i * 2.3)));
            int r = Math.max(w, h) / 5;
            for (int l = 3; l >= 1; l--) {
                int rr = r * l / 3;
                Draw.rrect(ctx, Math.round(cx - rr), Math.round(cy - rr), rr * 2, rr * 2, rr,
                        col(hu + i * 30, 0.6f, 1f, 0.06f * k));
            }
        }
    }
}
