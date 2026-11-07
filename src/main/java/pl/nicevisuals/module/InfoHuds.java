package pl.nicevisuals.module;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.math.MathHelper;
import pl.nicevisuals.setting.NumSetting;

import java.util.Locale;

/** Small information modules, grouped in one file. */
public final class InfoHuds {
    private InfoHuds() {}

    private static final int BLUE = 0xFF4DA3FF;

    private abstract static class Line extends Module {
        final NumSetting x, y;
        Line(String name, int dx, int dy) {
            super(name, Category.HUD);
            x = add(new NumSetting("X", dx, 0, 800));
            y = add(new NumSetting("Y", dy, 0, 600));
        }
        abstract String text(MinecraftClient mc);
        @Override public void onHud(DrawContext ctx, MinecraftClient mc) {
            String s = text(mc);
            if (s != null) ctx.drawTextWithShadow(mc.textRenderer, s, x.asInt(), y.asInt(), BLUE);
        }
    }

    public static class Direction extends Line {
        public Direction() { super("Direction", 6, 78); }
        @Override String text(MinecraftClient mc) {
            if (mc.player == null) return null;
            String f = mc.player.getHorizontalFacing().asString();
            return "Facing: " + f.substring(0, 1).toUpperCase(Locale.ROOT) + f.substring(1)
                    + " (" + Math.round(MathHelper.wrapDegrees(mc.player.getYaw())) + ")";
        }
    }

    public static class Memory extends Line {
        public Memory() { super("Memory", 6, 90); }
        @Override String text(MinecraftClient mc) {
            Runtime r = Runtime.getRuntime();
            long used = (r.totalMemory() - r.freeMemory()) / 1048576L, max = r.maxMemory() / 1048576L;
            return "RAM: " + used + " / " + max + " MB";
        }
    }

    public static class Biome extends Line {
        public Biome() { super("Biome", 6, 102); }
        @Override String text(MinecraftClient mc) {
            if (mc.world == null || mc.player == null) return null;
            return mc.world.getBiome(mc.player.getBlockPos()).getKey()
                    .map(k -> "Biome: " + k.getValue().getPath().replace('_', ' ')).orElse(null);
        }
    }

    public static class Server extends Line {
        public Server() { super("Server", 6, 114); }
        @Override String text(MinecraftClient mc) {
            return mc.getCurrentServerEntry() == null ? "Singleplayer" : mc.getCurrentServerEntry().address;
        }
    }

    public static class WorldTime extends Line {
        public WorldTime() { super("World Time", 6, 126); }
        @Override String text(MinecraftClient mc) {
            if (mc.world == null) return null;
            long t = mc.world.getTimeOfDay() % 24000L;
            long hour = (t / 1000 + 6) % 24, min = (t % 1000) * 60 / 1000;
            return String.format("Time: %02d:%02d", hour, min);
        }
    }

    public static class ItemCounter extends Module {
        private static final Item[] ITEMS = {Items.TOTEM_OF_UNDYING, Items.GOLDEN_APPLE, Items.ENDER_PEARL, Items.END_CRYSTAL};
        private final NumSetting x = add(new NumSetting("X", 6, 0, 800));
        private final NumSetting y = add(new NumSetting("Y", 170, 0, 600));
        public ItemCounter() { super("Item Counter", Category.HUD); }
        @Override public void onHud(DrawContext ctx, MinecraftClient mc) {
            if (mc.player == null) return;
            int yy = y.asInt();
            for (Item item : ITEMS) {
                int n = 0;
                for (int i = 0; i < mc.player.getInventory().size(); i++) {
                    ItemStack s = mc.player.getInventory().getStack(i);
                    if (s.isOf(item)) n += s.getCount();
                }
                ctx.drawItem(new ItemStack(item), x.asInt(), yy);
                ctx.drawTextWithShadow(mc.textRenderer, "x" + n, x.asInt() + 20, yy + 4, n == 0 ? 0xFF8A94B8 : BLUE);
                yy += 18;
            }
        }
    }

    public static class HurtFlash extends Module {
        public HurtFlash() { super("Hurt Flash", Category.COMBAT); }
        @Override public void onHud(DrawContext ctx, MinecraftClient mc) {
            if (mc.player == null || mc.player.hurtTime <= 0) return;
            edges(ctx, Math.min(1f, mc.player.hurtTime / 10f) * 0.55f);
        }
    }

    public static class LowHealth extends Module {
        public LowHealth() { super("Low Health", Category.COMBAT); }
        @Override public void onHud(DrawContext ctx, MinecraftClient mc) {
            if (mc.player == null || mc.player.getHealth() > 6f) return;
            float pulse = (float) (Math.sin(System.currentTimeMillis() / 220.0) * 0.5 + 0.5);
            edges(ctx, 0.15f + 0.4f * pulse);
        }
    }

    static void edges(DrawContext ctx, float a) {
        int w = ctx.getScaledWindowWidth(), h = ctx.getScaledWindowHeight();
        int strong = ((int) (a * 255) << 24) | 0xFF2020, none = 0x00FF2020;
        int bh = h / 4, bw = w / 6;
        ctx.fillGradient(0, 0, w, bh, strong, none);
        ctx.fillGradient(0, h - bh, w, h, none, strong);
        for (int i = 0; i < bw; i++) {
            int c = (((int) (a * 255 * (1f - i / (float) bw))) << 24) | 0xFF2020;
            ctx.fill(i, 0, i + 1, h, c);
            ctx.fill(w - 1 - i, 0, w - i, h, c);
        }
    }
}
