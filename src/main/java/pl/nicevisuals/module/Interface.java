package pl.nicevisuals.module;

import net.minecraft.client.MinecraftClient;
import pl.nicevisuals.gui.Draw;
import pl.nicevisuals.setting.ModeSetting;

public class Interface extends Module {
    private final ModeSetting style = add(new ModeSetting("Style", 0, "Flat", "Glass", "Pixel"));

    public Interface() { super("Interface", Category.CLIENT); }

    private void apply() {
        Draw.flat = style.index == 0;
        Draw.pixel = style.index == 2;
    }

    @Override public void onEnable() { apply(); }
    @Override public void onDisable() { Draw.flat = false; Draw.pixel = false; }
    @Override public void onTick(MinecraftClient mc) { apply(); }
}
