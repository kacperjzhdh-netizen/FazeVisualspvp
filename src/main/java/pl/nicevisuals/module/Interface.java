package pl.nicevisuals.module;

import pl.nicevisuals.gui.Draw;
import pl.nicevisuals.setting.ModeSetting;
import pl.nicevisuals.setting.NumSetting;

/** Holds the menu appearance settings (edited from the Client tab of the menu). */
public class Interface extends Module {
    public final ModeSetting theme = add(new ModeSetting("Theme", 1, "Midnight", "Glass", "Frost"));
    public final NumSetting hue = add(new NumSetting("Accent hue", 265, 0, 360));
    public final NumSetting glass = add(new NumSetting("Glass strength", 85, 0, 100));
    public final ModeSetting particles = add(new ModeSetting("Particles", 3, "None", "Squares", "Dots", "Snow"));
    public final NumSetting amount = add(new NumSetting("Amount", 48, 0, 150));
    public final NumSetting speed = add(new NumSetting("Speed", 9, 1, 30));
    public final NumSetting size = add(new NumSetting("Size", 10, 3, 30));

    public Interface() { super("Interface", Category.CLIENT); }

    @Override public void onEnable() { Draw.flat = false; Draw.pixel = false; }
}
