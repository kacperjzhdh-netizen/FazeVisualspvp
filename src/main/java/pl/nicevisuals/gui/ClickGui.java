package pl.nicevisuals.gui;

import com.google.gson.JsonObject;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import pl.nicevisuals.NiceVisualsClient;
import pl.nicevisuals.module.Category;
import pl.nicevisuals.module.Module;
import pl.nicevisuals.setting.BoolSetting;
import pl.nicevisuals.setting.ModeSetting;
import pl.nicevisuals.setting.NumSetting;
import pl.nicevisuals.setting.Setting;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Glass-style settings screen: sidebar, scrollable list, toggles, sliders, dropdowns, toast. */
public class ClickGui extends Screen {
    private static final Identifier LOGO = Identifier.of("nicevisuals", "textures/gui/logo.png");
    private static final int WHITE = 0xFFFFFFFF, DIM = 0xFFB8C2E0, ACCENT_A = 0xFF7B6CF6, ACCENT_B = 0xFF4DA3FF;
    private static final Map<String, String> DESC = Map.of(
            "Hit Marker", "Cross flash when you hit someone.",
            "Target HUD", "Name and health of your target.",
            "Target ESP", "Glow outline on your last target.",
            "FPS", "Show frames per second.",
            "CPS", "Show clicks per second.",
            "Coords", "Show your XYZ position.",
            "Armor HUD", "Show worn armor and durability.",
            "Keystrokes", "Show WASD and jump keys.",
            "Fullbright", "See clearly in the dark.");

    private enum Tab {
        COMBAT("Combat", "Targeting and hit feedback.", Category.COMBAT),
        HUD("HUD", "Information on your screen.", Category.HUD),
        WORLD("World", "How the world looks.", Category.WORLD),
        ABOUT("About", "NiceVisuals.", null);
        final String title, sub; final Category cat;
        Tab(String t, String s, Category c) { title = t; sub = s; cat = c; }
    }

    private record Hit(int x, int y, int w, int h, boolean scrolled, Runnable action, NumSetting slider) {}

    private final List<Hit> hits = new ArrayList<>();
    private final Map<Object, Float> anim = new HashMap<>();
    private final JsonObject snapshot;
    private Tab tab = Tab.COMBAT;
    private float scroll, maxScroll;
    private int clipY1, clipY2;
    private ModeSetting openDrop;
    private int dropX, dropY, dropW;
    private NumSetting dragging;
    private int dragX, dragW;
    private long toastAt;

    public ClickGui() {
        super(Text.literal("Nice Visuals"));
        snapshot = NiceVisualsClient.modules.toJson();
    }

    @Override public boolean shouldPause() { return false; }

    private float smooth(Object key, float target) {
        float a = anim.getOrDefault(key, target);
        a += (target - a) * 0.3f;
        anim.put(key, a);
        return a;
    }

    private void glass(DrawContext ctx, int x, int y, int w, int h, int r) {
        Draw.rrect(ctx, x, y, w, h, r, 0x38FFFFFF);
        Draw.rrect(ctx, x + 1, y + 1, w - 2, h - 2, Math.max(1, r - 1), 0x9A1A2244);
    }

    private void toggle(DrawContext ctx, Object key, int x, int y, boolean on) {
        float a = smooth(key, on ? 1f : 0f);
        Draw.rrectGrad(ctx, x, y, 28, 14, 7, Draw.mix(0xFF3A4260, ACCENT_A, a), Draw.mix(0xFF3A4260, ACCENT_B, a));
        int kx = x + 2 + Math.round(14 * a);
        Draw.rrect(ctx, kx, y + 2, 10, 10, 5, WHITE);
    }

    @Override
    public void render(DrawContext ctx, int mx, int my, float delta) {
        renderBackground(ctx, mx, my, delta);
        hits.clear();

        int W = Math.min(width - 16, 440), H = Math.min(height - 16, 250);
        int x0 = (width - W) / 2, y0 = (height - H) / 2;
        int sw = 96;

        // sidebar
        glass(ctx, x0, y0, sw, H, 10);
        ctx.drawTexture(RenderLayer::getGuiTextured, LOGO, x0 + 9, y0 + 9, 0f, 0f, 20, 20, 20, 20);
        ctx.drawTextWithShadow(textRenderer, Text.literal("Visuals").formatted(Formatting.BOLD), x0 + 35, y0 + 15, WHITE);
        int ty = y0 + 40;
        for (Tab t : Tab.values()) {
            int tx = x0 + 6, tw = sw - 12;
            boolean sel = t == tab, hover = mx >= tx && mx < tx + tw && my >= ty && my < ty + 22;
            if (sel) Draw.rrectGrad(ctx, tx, ty, tw, 22, 7, ACCENT_A, 0xFF5B8CF0);
            else if (hover) Draw.rrect(ctx, tx, ty, tw, 22, 7, 0x22FFFFFF);
            ctx.drawTextWithShadow(textRenderer, t.title, tx + 12, ty + 7, sel ? WHITE : DIM);
            final Tab target = t;
            hits.add(new Hit(tx, ty, tw, 22, false, () -> { tab = target; scroll = 0; openDrop = null; }, null));
            ty += 26;
        }
        ctx.drawTextWithShadow(textRenderer, "v1.0.0", x0 + 12, y0 + H - 16, 0xFF8A94B8);

        // content
        int cx = x0 + sw + 8, cw = W - sw - 8, ch = H - 30;
        glass(ctx, cx, y0, cw, ch, 10);
        ctx.drawTextWithShadow(textRenderer, Text.literal(tab.title).formatted(Formatting.BOLD), cx + 14, y0 + 9, WHITE);
        ctx.drawTextWithShadow(textRenderer, tab.sub, cx + 14, y0 + 21, DIM);
        ctx.fill(cx + 14, y0 + 35, cx + cw - 14, y0 + 36, 0x30FFFFFF);

        clipY1 = y0 + 40;
        clipY2 = y0 + ch - 6;
        ctx.enableScissor(cx, clipY1, cx + cw, clipY2);
        int y = clipY1 - Math.round(scroll);
        int top = y;
        int rx = cx + cw - 14; // right edge for controls
        if (tab == Tab.ABOUT) {
            ctx.drawTextWithShadow(textRenderer, "NiceVisuals v1.0.0", cx + 14, y + 4, WHITE);
            ctx.drawTextWithShadow(textRenderer, "Press X in game to open this menu.", cx + 14, y + 18, DIM);
            ctx.drawTextWithShadow(textRenderer, "Settings are saved in config/nicevisuals.json", cx + 14, y + 30, DIM);
            ctx.drawTextWithShadow(textRenderer, "Made by NiceCode.", cx + 14, y + 42, DIM);
            y += 60;
        } else {
            for (Module m : NiceVisualsClient.modules.in(tab.cat)) {
                ctx.drawTextWithShadow(textRenderer, m.name, cx + 14, y + 4, WHITE);
                ctx.drawTextWithShadow(textRenderer, DESC.getOrDefault(m.name, ""), cx + 14, y + 15, 0xFF9AA5C8);
                toggle(ctx, m, rx - 28, y + 7, m.isEnabled());
                hits.add(new Hit(cx + 10, y, cw - 20, 28, true, m::toggle, null));
                y += 30;
                for (Setting s : m.settings) {
                    ctx.drawTextWithShadow(textRenderer, s.name, cx + 26, y + 7, DIM);
                    if (s instanceof BoolSetting b) {
                        toggle(ctx, b, rx - 28, y + 4, b.value);
                        hits.add(new Hit(cx + 10, y, cw - 20, 22, true, () -> b.value = !b.value, null));
                    } else if (s instanceof NumSetting n) {
                        int sx = rx - 120, swid = 88;
                        Draw.rrect(ctx, sx, y + 9, swid, 4, 2, 0x44FFFFFF);
                        float f = (float) ((n.value - n.min) / (n.max - n.min));
                        int fw = Math.max(4, Math.round(swid * f));
                        Draw.rrectGrad(ctx, sx, y + 9, fw, 4, 2, ACCENT_A, ACCENT_B);
                        Draw.rrect(ctx, sx + fw - 4, y + 6, 8, 10, 4, WHITE);
                        ctx.drawTextWithShadow(textRenderer, String.valueOf(n.asInt()), rx - 26, y + 7, WHITE);
                        hits.add(new Hit(sx - 4, y + 3, swid + 8, 16, true, null, n));
                    } else if (s instanceof ModeSetting md) {
                        int bx = rx - 72, bw = 72;
                        Draw.rrect(ctx, bx, y + 3, bw, 16, 6, 0x33FFFFFF);
                        ctx.drawTextWithShadow(textRenderer, md.get(), bx + 7, y + 7, WHITE);
                        ctx.drawTextWithShadow(textRenderer, openDrop == md ? "^" : "v", bx + bw - 12, y + 7, DIM);
                        final int by = y + 19;
                        hits.add(new Hit(bx, y + 3, bw, 16, true, () -> {
                            if (openDrop == md) openDrop = null;
                            else { openDrop = md; dropX = bx; dropY = by; dropW = bw; }
                        }, null));
                    }
                    y += 24;
                }
                ctx.fill(cx + 14, y + 1, cx + cw - 14, y + 2, 0x18FFFFFF);
                y += 5;
            }
        }
        maxScroll = Math.max(0, (y - top) - (clipY2 - clipY1));
        scroll = Math.max(0, Math.min(scroll, maxScroll));
        ctx.disableScissor();

        // bottom buttons: Reset, Cancel, Apply
        int by = y0 + ch + 4, bw = 62, bh = 22, gap = 6;
        int bx = cx + cw - (bw * 3 + gap * 2);
        button(ctx, mx, my, bx, by, bw, bh, "Reset", false, this::doReset);
        button(ctx, mx, my, bx + bw + gap, by, bw, bh, "Cancel", false, this::doCancel);
        button(ctx, mx, my, bx + (bw + gap) * 2, by, bw, bh, "Apply", true, this::doApply);

        // overlays (drawn above everything else)
        ctx.getMatrices().push();
        ctx.getMatrices().translate(0, 0, 300);
        if (openDrop != null) {
            int oh = openDrop.modes.length * 16 + 4;
            glass(ctx, dropX, dropY, dropW, oh, 6);
            for (int i = 0; i < openDrop.modes.length; i++) {
                int oy = dropY + 2 + i * 16;
                boolean sel = i == openDrop.index;
                boolean hov = mx >= dropX && mx < dropX + dropW && my >= oy && my < oy + 16;
                if (sel) Draw.rrectGrad(ctx, dropX + 2, oy, dropW - 4, 16, 5, ACCENT_A, 0xFF5B8CF0);
                else if (hov) Draw.rrect(ctx, dropX + 2, oy, dropW - 4, 16, 5, 0x22FFFFFF);
                ctx.drawTextWithShadow(textRenderer, openDrop.modes[i], dropX + 8, oy + 4, sel ? WHITE : DIM);
            }
        }
        drawToast(ctx);
        ctx.getMatrices().pop();
    }

    private void button(DrawContext ctx, int mx, int my, int x, int y, int w, int h, String label, boolean primary, Runnable r) {
        boolean hover = mx >= x && mx < x + w && my >= y && my < y + h;
        if (primary) Draw.rrectGrad(ctx, x, y, w, h, 7, hover ? 0xFF8F82FF : ACCENT_A, hover ? 0xFF6DB6FF : ACCENT_B);
        else { Draw.rrect(ctx, x, y, w, h, 7, 0x38FFFFFF); Draw.rrect(ctx, x + 1, y + 1, w - 2, h - 2, 6, hover ? 0xB0303A60 : 0x9A1A2244); }
        ctx.drawCenteredTextWithShadow(textRenderer, label, x + w / 2, y + 7, WHITE);
        hits.add(new Hit(x, y, w, h, false, r, null));
    }

    private void drawToast(DrawContext ctx) {
        long age = System.currentTimeMillis() - toastAt;
        if (toastAt == 0 || age > 2800) return;
        float a = age < 250 ? age / 250f : (age > 2300 ? (2800 - age) / 500f : 1f);
        int w = 170, h = 36, x = width - w - 10, y = 10 - Math.round((1 - Math.min(1f, age / 250f)) * 10);
        Draw.rrect(ctx, x, y, w, h, 8, Draw.alpha(0x55FFFFFF, a));
        Draw.rrect(ctx, x + 1, y + 1, w - 2, h - 2, 7, Draw.alpha(0xEE1A2244, a));
        Draw.rrect(ctx, x + 8, y + 8, 20, 20, 10, Draw.alpha(0xFF3DDC97, a));
        int ck = Draw.alpha(WHITE, a);
        ctx.fill(x + 13, y + 18, x + 15, y + 20, ck);
        ctx.fill(x + 15, y + 20, x + 17, y + 22, ck);
        for (int i = 0; i < 5; i++) ctx.fill(x + 17 + i, y + 20 - i, x + 19 + i, y + 22 - i, ck);
        ctx.drawTextWithShadow(textRenderer, Text.literal("Settings Saved!").formatted(Formatting.BOLD), x + 36, y + 9, Draw.alpha(WHITE, a));
        ctx.drawTextWithShadow(textRenderer, "Your changes have been applied.", x + 36, y + 21, Draw.alpha(DIM, a));
    }

    private void doApply() {
        NiceVisualsClient.modules.save();
        toastAt = System.currentTimeMillis();
    }

    private void doCancel() {
        NiceVisualsClient.modules.fromJson(snapshot);
        close();
    }

    private void doReset() {
        if (tab.cat == null) return;
        for (Module m : NiceVisualsClient.modules.in(tab.cat)) {
            m.setEnabled(false);
            for (Setting s : m.settings) s.reset();
        }
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (button != 0) return super.mouseClicked(mx, my, button);
        if (openDrop != null) {
            for (int i = 0; i < openDrop.modes.length; i++) {
                int oy = dropY + 2 + i * 16;
                if (mx >= dropX && mx < dropX + dropW && my >= oy && my < oy + 16) { openDrop.index = i; break; }
            }
            openDrop = null;
            return true;
        }
        for (Hit h : hits) {
            if (mx < h.x() || mx >= h.x() + h.w() || my < h.y() || my >= h.y() + h.h()) continue;
            if (h.scrolled() && (my < clipY1 || my >= clipY2)) continue;
            if (h.slider() != null) { dragging = h.slider(); dragX = h.x() + 4; dragW = h.w() - 8; applyDrag(mx); }
            else h.action().run();
            return true;
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
        dragging = null;
        return super.mouseReleased(mx, my, button);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double hAmount, double vAmount) {
        scroll = Math.max(0, Math.min(maxScroll, scroll - (float) vAmount * 14f));
        openDrop = null;
        return true;
    }

    @Override
    public void removed() {
        NiceVisualsClient.modules.save();
        super.removed();
    }
}
