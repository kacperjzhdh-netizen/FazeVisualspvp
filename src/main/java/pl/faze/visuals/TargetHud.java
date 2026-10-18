package pl.faze.visuals;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.PlayerSkinDrawer;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;

/** Kwadratowy panel informacji o celu. Wszystkie dane pochodza z encji gracza znanej klientowi. */
public final class TargetHud {
    private TargetHud() {}

    public static final int SIZE = 120;

    private static int lastId = -1;
    private static float shownHp, trailHp;
    private static long lastNanos = System.nanoTime();
    private static float appear = 0f;

    public static int pxX(Config c, int sw) {
        return (int) (c.hudX * sw);
    }

    public static int pxY(Config c, int sh) {
        return (int) (c.hudY * sh);
    }

    /** Rysuje panel dla danego gracza w pozycji (px, py) w skali z konfiguracji. */
    public static void draw(DrawContext ctx, PlayerEntity p, int px, int py, boolean preview) {
        Config c = Config.get();
        MinecraftClient mc = MinecraftClient.getInstance();
        TextRenderer tr = mc.textRenderer;

        long n = System.nanoTime();
        float dt = Math.min(0.1f, (n - lastNanos) / 1_000_000_000f);
        lastNanos = n;

        float max = Math.max(1f, p.getMaxHealth());
        float hp = Math.max(0f, p.getHealth());
        float abs = p.getAbsorptionAmount();
        if (p.getId() != lastId) {
            lastId = p.getId();
            shownHp = hp;
            trailHp = hp;
            appear = 0f;
        }
        appear = UiUtil.approach(appear, 1f, 14f, dt);
        shownHp = UiUtil.approach(shownHp, hp, 14f, dt);
        // "duch" obrazen: zostaje z tylu i powoli dogania
        if (trailHp < shownHp) trailHp = shownHp;
        else trailHp = UiUtil.approach(trailHp, shownHp, 3.5f, dt);

        float s = c.hudScale * (0.92f + 0.08f * appear);
        ctx.getMatrices().push();
        ctx.getMatrices().translate(px, py, 0);
        ctx.getMatrices().scale(s, s, 1f);

        // tlo
        UiUtil.rrect(ctx, 0, 0, SIZE, SIZE, 6, UiUtil.argb(c.hudAccentColor, 0.9f));
        UiUtil.rrect(ctx, 1, 1, SIZE - 2, SIZE - 2, 5, UiUtil.argb(c.hudBgColor, c.hudBgAlpha));

        int x0 = 7;
        int textX = x0;
        if (c.hudShowHead && p instanceof AbstractClientPlayerEntity ap) {
            UiUtil.rrect(ctx, x0 - 1, 6, 30, 30, 4, UiUtil.argb(0x000000, 0.45f));
            PlayerSkinDrawer.draw(ctx, ap.getSkinTextures(), x0, 7, 28);
            textX = x0 + 34;
        }

        // nick (przyciety do szerokosci)
        String name = p.getName().getString();
        int maxW = SIZE - textX - 6;
        if (tr.getWidth(name) > maxW) name = tr.trimToWidth(name, maxW - tr.getWidth("..")) + "..";
        ctx.drawText(tr, name, textX, 9, 0xFFFFFFFF, true);

        // HP: liczby
        String hpStr = String.format("%.1f / %.1f", hp, max);
        if (abs > 0.01f) hpStr += String.format(" +%.1f", abs);
        ctx.drawText(tr, hpStr, textX, 21, 0xFFB8B8C8, false);

        // pasek HP
        int bx = x0, by = 40, bw = SIZE - 2 * x0, bh = 9;
        UiUtil.rrect(ctx, bx, by, bw, bh, 3, UiUtil.argb(0x000000, 0.55f));
        int trailW = Math.round(bw * Math.min(1f, trailHp / max));
        int fillW = Math.round(bw * Math.min(1f, shownHp / max));
        if (trailW > 0) UiUtil.rrect(ctx, bx, by, trailW, bh, 3, UiUtil.argb(c.hudTrailColor, 0.9f));
        if (fillW > 0) UiUtil.rrect(ctx, bx, by, fillW, bh, 3, 0xFF000000 | c.hudBarColor);
        if (fillW > 2) ctx.fill(bx + 2, by + 1, bx + fillW - 2, by + 2, UiUtil.argb(0xFFFFFF, 0.25f));

        // zbroja
        if (c.hudShowArmor) {
            ctx.drawText(tr, "Armor " + p.getArmor(), x0, 54, 0xFFB8B8C8, false);
            EquipmentSlot[] slots = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
            for (int i = 0; i < 4; i++) {
                int sx = x0 + i * 27;
                slot(ctx, tr, p.getEquippedStack(slots[i]), sx, 65);
            }
        }

        // rece
        if (c.hudShowHands) {
            int hy = 91;
            slot(ctx, tr, p.getMainHandStack(), x0, hy);
            slot(ctx, tr, p.getOffHandStack(), x0 + 27 * 2, hy);
            ctx.getMatrices().push();
            ctx.getMatrices().translate(x0, hy + 19, 0);
            ctx.getMatrices().scale(0.7f, 0.7f, 1f);
            ctx.drawText(tr, "Main", 0, 0, 0xFF9A9AB0, false);
            ctx.getMatrices().pop();
            ctx.getMatrices().push();
            ctx.getMatrices().translate(x0 + 54, hy + 19, 0);
            ctx.getMatrices().scale(0.7f, 0.7f, 1f);
            ctx.drawText(tr, "Off", 0, 0, 0xFF9A9AB0, false);
            ctx.getMatrices().pop();

            // nazwa przedmiotu w glownej rece
            ItemStack main = p.getMainHandStack();
            String label = main.isEmpty() ? "-" : main.getName().getString();
            int lw = SIZE - (x0 + 27 * 3) - 4;
            ctx.getMatrices().push();
            ctx.getMatrices().translate(x0 + 27 * 3 - 4, hy + 4, 0);
            ctx.getMatrices().scale(0.8f, 0.8f, 1f);
            int lwS = (int) (lw / 0.8f);
            if (tr.getWidth(label) > lwS) label = tr.trimToWidth(label, lwS);
            ctx.drawText(tr, label, 0, 0, 0xFFD0D0E0, false);
            ctx.getMatrices().pop();
        }

        if (preview) {
            ctx.drawText(tr, "podglad", SIZE - 6 - tr.getWidth("podglad"), SIZE - 10, 0x889A9AB0, false);
        }
        ctx.getMatrices().pop();
    }

    private static void slot(DrawContext ctx, TextRenderer tr, ItemStack st, int x, int y) {
        UiUtil.rrect(ctx, x - 1, y - 1, 20, 20, 3, UiUtil.argb(0x000000, 0.40f));
        if (!st.isEmpty()) {
            ctx.drawItem(st, x + 1, y + 1);
            ctx.drawStackOverlay(tr, st, x + 1, y + 1);
        } else {
            ctx.fill(x + 6, y + 9, x + 12, y + 10, UiUtil.argb(0xFFFFFF, 0.2f));
        }
    }
}
