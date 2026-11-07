package pl.nicevisuals.module;

import net.minecraft.client.MinecraftClient;
import pl.nicevisuals.gui.Draw;
import pl.nicevisuals.setting.ModeSetting;

public class Interface extends Module {
    private final ModeSetting style = add(new ModeSetting("Style", 0, "Glass", "Pixel"));

    public Interface() { super("Interface", Category.CLIENT); }

    @Override public void onEnable() { Draw.pixel = style.index == 1; }
    @Override public void onDisable() { Draw.pixel = false; }
    @Override public void onTick(MinecraftClient mc) { Draw.pixel = style.index == 1; }
}
