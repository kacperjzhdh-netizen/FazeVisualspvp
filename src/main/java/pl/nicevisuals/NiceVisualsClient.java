package pl.nicevisuals;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.ActionResult;
import org.lwjgl.glfw.GLFW;
import pl.nicevisuals.gui.ClickGui;
import pl.nicevisuals.module.Module;
import pl.nicevisuals.module.ModuleManager;

public class NiceVisualsClient implements ClientModInitializer {
    public static ModuleManager modules;
    private static KeyBinding openGui;

    @Override
    public void onInitializeClient() {
        modules = new ModuleManager();
        modules.load();

        openGui = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.nicevisuals.gui", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_X, "category.nicevisuals"));

        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            while (openGui.wasPressed()) {
                if (mc.currentScreen == null) mc.setScreen(new ClickGui());
            }
            for (Module m : modules.modules) if (m.isEnabled()) m.onTick(mc);
        });

        HudRenderCallback.EVENT.register((ctx, tickCounter) -> {
            MinecraftClient mc = MinecraftClient.getInstance();
            if (mc.options.hudHidden || mc.player == null) return;
            for (Module m : modules.modules) if (m.isEnabled()) m.onHud(ctx, mc);
        });

        AttackEntityCallback.EVENT.register((player, world, hand, entity, hit) -> {
            if (world.isClient && entity instanceof LivingEntity le) {
                if (modules.hitMarker.isEnabled()) modules.hitMarker.registerHit();
                if (modules.targetHud.isEnabled()) modules.targetHud.setTarget(le);
                if (modules.targetEsp.isEnabled()) modules.targetEsp.setTarget(le);
            }
            return ActionResult.PASS;
        });
    }
}
