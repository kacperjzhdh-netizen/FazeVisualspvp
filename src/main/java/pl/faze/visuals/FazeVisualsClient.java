package pl.faze.visuals;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.player.PlayerEntity;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FazeVisualsClient implements ClientModInitializer {
    public static final Logger LOGGER = LoggerFactory.getLogger("FazeVisuals");
    public static KeyBinding menuKey;

    @Override
    public void onInitializeClient() {
        Config.load();

        menuKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.fazevisuals.menu", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_P, "category.fazevisuals"));

        TargetTracker.init();
        WorldVisuals.init();

        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            while (menuKey.wasPressed()) {
                if (mc.currentScreen == null) mc.setScreen(new FazeMenuScreen());
            }
            TargetTracker.tick(mc);
        });

        HudRenderCallback.EVENT.register((ctx, tickCounter) -> {
            MinecraftClient mc = MinecraftClient.getInstance();
            Config c = Config.get();
            if (!c.hudEnabled || mc.player == null || mc.options.hudHidden) return;
            if (mc.currentScreen instanceof FazeMenuScreen) return; // menu rysuje wlasny podglad
            PlayerEntity t = TargetTracker.getTarget();
            if (t == null) return;
            int w = ctx.getScaledWindowWidth(), h = ctx.getScaledWindowHeight();
            TargetHud.draw(ctx, t, TargetHud.pxX(c, w), TargetHud.pxY(c, h), false);
        });

        LOGGER.info("FazeVisuals zaladowany. Menu: klawisz P");
    }
}
