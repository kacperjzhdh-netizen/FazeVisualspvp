package pl.nicevisuals.module;

import pl.nicevisuals.setting.ModeSetting;
import pl.nicevisuals.setting.NumSetting;

import java.awt.Color;

/** Choose the colour of the sky (client side only). */
public class Sky extends Module {
    private static final int[] PRESETS = {0xFF8A4B, 0xFF7FC8, 0x8A4DFF, 0x0B1030, 0x32D6E6, 0xC41E2A, 0x6FD36F};
    private final ModeSetting preset = add(new ModeSetting("Preset", 0,
            "Sunset", "Pink", "Purple", "Night", "Aqua", "Blood", "Mint", "Custom"));
    private final NumSetting hue = add(new NumSetting("Custom hue", 300, 0, 360));
    private final NumSetting bright = add(new NumSetting("Brightness", 100, 20, 100));

    public Sky() { super("Sky", Category.WORLD); }

    public int rgb() {
        int c = preset.index < PRESETS.length ? PRESETS[preset.index]
                : Color.HSBtoRGB((float) (hue.value / 360.0), 0.65f, 1f) & 0xFFFFFF;
        float b = (float) (bright.value / 100.0);
        int r = Math.round((c >> 16 & 255) * b), g = Math.round((c >> 8 & 255) * b), bl = Math.round((c & 255) * b);
        return r << 16 | g << 8 | bl;
    }
}
