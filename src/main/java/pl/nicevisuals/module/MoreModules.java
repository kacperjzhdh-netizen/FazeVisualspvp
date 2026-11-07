package pl.nicevisuals.module;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.LivingEntity;
import pl.nicevisuals.setting.NumSetting;

/** Extra small modules. */
public final class MoreModules {
    private MoreModules() {}
    private static final int BLUE = 0xFF4DA3FF;

    public static class KillCounter extends Module {
        private final NumSetting x = add(new NumSetting("X", 6, 0, 800));
        private final NumSetting y = add(new NumSetting("Y", 138, 0, 600));
        private LivingEntity target;
        private long lastHit;
        private int kills;

        public KillCounter() { super("Kill Counter", Category.COMBAT); }

        public void registerHit(LivingEntity e) { target = e; lastHit = System.currentTimeMillis(); }

        @Override public void onTick(MinecraftClient mc) {
            if (target == null) return;
            long age = System.currentTimeMillis() - lastHit;
            if (!target.isAlive() || target.getHealth() <= 0) {
                if (age < 4000) kills++;
                target = null;
            } else if (age > 5000) target = null;
        }

        @Override public void onHud(DrawContext ctx, MinecraftClient mc) {
            ctx.drawTextWithShadow(mc.textRenderer, "Kills: " + kills, x.asInt(), y.asInt(), BLUE);
        }
    }

    public static class Status extends Module {
        private final NumSetting x = add(new NumSetting("X", 6, 0, 800));
        private final NumSetting y = add(new NumSetting("Y", 150, 0, 600));
        public Status() { super("Status", Category.HUD); }
        @Override public void onHud(DrawContext ctx, MinecraftClient mc) {
            if (mc.player == null) return;
            String s = mc.player.isSwimming() ? "Swimming" : mc.player.isSneaking() ? "Sneaking"
                    : mc.player.isSprinting() ? "Sprinting" : mc.player.isOnGround() ? "Walking" : "Airborne";
            ctx.drawTextWithShadow(mc.textRenderer, s, x.asInt(), y.asInt(), BLUE);
        }
    }

    public static class DayCounter extends Module {
        private final NumSetting x = add(new NumSetting("X", 6, 0, 800));
        private final NumSetting y = add(new NumSetting("Y", 162, 0, 600));
        public DayCounter() { super("Day Counter", Category.HUD); }
        @Override public void onHud(DrawContext ctx, MinecraftClient mc) {
            if (mc.world == null) return;
            ctx.drawTextWithShadow(mc.textRenderer, "Day " + (mc.world.getTimeOfDay() / 24000L + 1), x.asInt(), y.asInt(), BLUE);
        }
    }

    public static class LightLevel extends Module {
        private final NumSetting x = add(new NumSetting("X", 6, 0, 800));
        private final NumSetting y = add(new NumSetting("Y", 174, 0, 600));
        public LightLevel() { super("Light Level", Category.HUD); }
        @Override public void onHud(DrawContext ctx, MinecraftClient mc) {
            if (mc.world == null || mc.player == null) return;
            ctx.drawTextWithShadow(mc.textRenderer, "Light: " + mc.world.getLightLevel(mc.player.getBlockPos()), x.asInt(), y.asInt(), BLUE);
        }
    }

    public static class TimeChanger extends Module {
        private final NumSetting time = add(new NumSetting("Time", 6000, 0, 24000));
        public TimeChanger() { super("Time Changer", Category.WORLD); }
        @Override public void onTick(MinecraftClient mc) {
            if (mc.world != null) mc.world.getLevelProperties().setTimeOfDay((long) time.value);
        }
    }

    public static class Weather extends Module {
        public Weather() { super("Weather", Category.WORLD); }
        @Override public void onTick(MinecraftClient mc) {
            if (mc.world == null) return;
            mc.world.setRainGradient(0f);
            mc.world.setThunderGradient(0f);
        }
    }
}
