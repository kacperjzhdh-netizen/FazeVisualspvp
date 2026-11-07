package pl.nicevisuals.gui;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import pl.nicevisuals.NiceVisualsClient;
import pl.nicevisuals.module.Category;
import pl.nicevisuals.module.Module;
import pl.nicevisuals.setting.BoolSetting;
import pl.nicevisuals.setting.NumSetting;
import pl.nicevisuals.setting.Setting;

import java.util.HashSet;
import java.util.Set;

public class ClickGui extends Screen {
    private static final int PW = 120, HEAD = 20, ROW = 18, SROW = 14, GAP = 10;
    private static final int BG = 0xE60B1A33, HEADER = 0xFF1B4F9C, ROW_BG = 0xFF10244A,
            ON = 0xFF4DA3FF, TEXT = 0xFFEAF3FF, DIM = 0xFF8FB4E8;

    private final Set<Module> expanded = new HashSet<>();
    private NumSetting dragging;
    private int dragX, dragW;

    public ClickGui() { super(Text.literal("Nice Visuals")); }

    @Override
    public boolean shouldPause() { return false; }

    private int panelX(int i) {
        int total = Category.values().length * (PW + GAP) - GAP;
        return (width - total) / 2 + i * (PW + GAP);
    }

    @Override
    public void render(DrawContext ctx, int mx, int my, float delta) {
        ctx.fill(0, 0, width, height, 0x88000814);
        Category[] cats = Category.values();
        for (int i = 0; i < cats.length; i++) {
            int x = panelX(i), y = 30;
            var mods = NiceVisualsClient.modules.in(cats[i]);
            int h = HEAD;
            for (Module m : mods) h += rowHeight(m);
            Draw.rrect(ctx, x, y, PW, h + 4, 8, BG);
            Draw.rrect(ctx, x, y, PW, HEAD, 8, HEADER);
            ctx.drawCenteredTextWithShadow(textRenderer, cats[i].label, x + PW / 2, y + 6, TEXT);
            int cy = y + HEAD + 2;
            for (Module m : mods) {
                boolean hover = mx >= x && mx < x + PW && my >= cy && my < cy + ROW;
                Draw.rrect(ctx, x + 3, cy, PW - 6, ROW - 2, 4, hover ? 0xFF173566 : ROW_BG);
                ctx.drawTextWithShadow(textRenderer, m.name, x + 8, cy + 5, m.isEnabled() ? ON : DIM);
                if (!m.settings.isEmpty())
                    ctx.drawTextWithShadow(textRenderer, expanded.contains(m) ? "-" : "+", x + PW - 14, cy + 5, DIM);
                cy += ROW;
                if (expanded.contains(m)) {
                    for (Setting s : m.settings) {
                        drawSetting(ctx, s, x + 6, cy, PW - 12);
                        cy += SROW;
                    }
                }
            }
        }
        ctx.drawCenteredTextWithShadow(textRenderer, "Nice Visuals  |  LMB: on/off  RMB: settings",
                width / 2, height - 16, DIM);
    }

    private void drawSetting(DrawContext ctx, Setting s, int x, int y, int w) {
        if (s instanceof BoolSetting b) {
            ctx.drawTextWithShadow(textRenderer, s.name, x + 2, y + 3, TEXT);
            Draw.rrect(ctx, x + w - 18, y + 2, 16, 9, 4, b.value ? ON : 0xFF1B2B4D);
        } else if (s instanceof NumSetting n) {
            Draw.rrect(ctx, x, y + 1, w, SROW - 2, 3, 0xFF1B2B4D);
            double f = (n.value - n.min) / (n.max - n.min);
            int fw = Math.max(3, (int) (w * f));
            Draw.rrect(ctx, x, y + 1, fw, SROW - 2, 3, 0xFF2F6FC4);
            ctx.drawTextWithShadow(textRenderer, s.name + ": " + (int) Math.round(n.value), x + 3, y + 3, TEXT);
        }
    }

    private int rowHeight(Module m) {
        return ROW + (expanded.contains(m) ? m.settings.size() * SROW : 0);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        Category[] cats = Category.values();
        for (int i = 0; i < cats.length; i++) {
            int x = panelX(i), cy = 30 + HEAD + 2;
            for (Module m : NiceVisualsClient.modules.in(cats[i])) {
                if (mx >= x && mx < x + PW && my >= cy && my < cy + ROW) {
                    if (button == 0) m.toggle();
                    else if (button == 1 && !m.settings.isEmpty()) {
                        if (!expanded.remove(m)) expanded.add(m);
                    }
                    NiceVisualsClient.modules.save();
                    return true;
                }
                cy += ROW;
                if (expanded.contains(m)) {
                    for (Setting s : m.settings) {
                        int sx = x + 6, sw = PW - 12;
                        if (button == 0 && mx >= sx && mx < sx + sw && my >= cy && my < cy + SROW) {
                            if (s instanceof BoolSetting b) { b.value = !b.value; NiceVisualsClient.modules.save(); }
                            else if (s instanceof NumSetting n) { dragging = n; dragX = sx; dragW = sw; applyDrag(mx); }
                            return true;
                        }
                        cy += SROW;
                    }
                }
            }
        }
        return super.mouseClicked(mx, my, button);
    }

    private void applyDrag(double mx) {
        double f = Math.max(0, Math.min(1, (mx - dragX) / dragW));
        dragging.value = dragging.min + f * (dragging.max - dragging.min);
    }

    @Override
    public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
        if (dragging != null) { applyDrag(mx); return true; }
        return super.mouseDragged(mx, my, button, dx, dy);
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        if (dragging != null) { dragging = null; NiceVisualsClient.modules.save(); }
        return super.mouseReleased(mx, my, button);
    }

    @Override
    public void close() {
        NiceVisualsClient.modules.save();
        super.close();
    }
}
