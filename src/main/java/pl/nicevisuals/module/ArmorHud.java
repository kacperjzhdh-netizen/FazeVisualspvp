package pl.nicevisuals.module;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;
import pl.nicevisuals.gui.Draw;
import pl.nicevisuals.setting.NumSetting;

public class ArmorHud extends Module {
    private static final EquipmentSlot[] SLOTS = {
            EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET };
    private final NumSetting x = add(new NumSetting("X", 6, 0, 800));
    private final NumSetting y = add(new NumSetting("Y", 60, 0, 600));

    public ArmorHud() { super("Armor HUD", Category.HUD); }

    @Override
    public void onHud(DrawContext ctx, MinecraftClient mc) {
        if (mc.player == null) return;
        int px = x.asInt(), py = y.asInt();
        Draw.rrect(ctx, px - 3, py - 3, 24, 4 * 18 + 4, 6, 0x990B1A33);
        for (int i = 0; i < SLOTS.length; i++) {
            ItemStack s = mc.player.getEquippedStack(SLOTS[i]);
            if (s.isEmpty()) continue;
            ctx.drawItem(s, px, py + i * 18);
            ctx.drawStackOverlay(mc.textRenderer, s, px, py + i * 18);
        }
    }
}
