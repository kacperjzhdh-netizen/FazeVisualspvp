package pl.nicevisuals.gui;

import com.google.gson.JsonObject;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
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
import java.util.Locale;
import java.util.Map;

/** Dark glass settings screen: sidebar, search, module cards, toggles, sliders, dropdowns, toast. */
public class ClickGui extends Screen {
    private static final Identifier LOGO = Identifier.of("nicevisuals", "textures/gui/logo.png");
    private static final Identifier FONT = Identifier.of("nicevisuals", "ui");
    private static final Identifier FONT_B = Identifier.of("nicevisuals", "ui_bold");
    private static final float S = 1.2f;
    private static final int TXT = 0xFFEDEFFA, DIM = 0xFF8E96B5, ACC_A = 0xFF7C5CFF, ACC_B = 0xFF4DA3FF;

    private static final Map<String, String> DESC = Map.ofEntries(
            Map.entry("Hit Marker", "Cross flash when you hit someone."),
            Map.entry("Target HUD", "Head, name, health and gear of your target."),
            Map.entry("Target ESP", "Brackets, ring or orbit on your target."),
            Map.entry("Combo", "Counts your hits in a row."),
            Map.entry("Reach", "Distance of your last hit."),
            Map.entry("FPS", "Show frames per second."),
            Map.entry("CPS", "Show clicks per second."),
            Map.entry("Coords", "Show your XYZ position."),
            Map.entry("Armor HUD", "Show worn armor and durability."),
            Map.entry("Keystrokes", "Show WASD and jump keys."),
            Map.entry("Ping", "Show your latency."),
            Map.entry("Speed", "Show your speed in blocks/s."),
            Map.entry("Clock", "Show the current time."),
            Map.entry("Potion HUD", "Show active potion effects."),
            Map.entry("Fullbright", "See clearly in the dark."),
            Map.entry("Interface", "Switch the menu look: Glass or Pixel."),
            Map.entry("Direction", "Show where you are facing."),
            Map.entry("Memory", "Show RAM used by the game."),
            Map.entry("Biome", "Show the biome you are in."),
            Map.entry("Server", "Show the server address."),
            Map.entry("World Time", "Show the in-game time of day."),
            Map.entry("Item Counter", "Totems, gapples and pearls you carry."),
            Map.entry("Hurt Flash", "Red edge flash when you take damage."),
            Map.entry("Low Health", "Pulsing red edges at low health."));

    private enum Tab {
        COMBAT("Combat", "Targeting and hit feedback.", Category.COMBAT),
        HUD("HUD", "Information on your screen.", Category.HUD),
        WORLD("World", "How the world looks.", Category.WORLD),
        CLIENT("Client", "Menu and mod settings.", Category.CLIENT),
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
    private String query = "";
    private boolean searchFocus;

    public ClickGui() {
        super(Text.literal("Nice Visuals"));
        snapshot = NiceVisualsClient.modules.toJson();
    }

    @Override public boolean shouldPause() { return false; }

    // ---- text helpers (smooth TrueType font) ----
    private Text t(String s) { return Text.literal(s).setStyle(Style.EMPTY.withFont(FONT)); }
    private Text tb(String s) { return Text.literal(s).setStyle(Style.EMPTY.withFont(FONT_B)); }
    private void txt(DrawContext ctx, String s, int x, int y, int color) { ctx.drawText(textRenderer, t(s), x, y, color, false); }
    private void txtB(DrawContext ctx, String s, int x, int y, int color) { ctx.drawText(textRenderer, tb(s), x, y, color, false); }
    private void txtC(DrawContext ctx, String s, int cx, int y, int color) {
        ctx.drawText(textRenderer, t(s), cx - textRenderer.getWidth(t(s)) / 2, y, color, false);
    }

    private boolean vis(int y, int h) { return y >= clipY1 && y + h <= clipY2; }

    private float smooth(Object key, float target) {
        float a = anim.getOrDefault(key, target);
        a += (target - a) * 0.3f;
        anim.put(key, a);
        return a;
    }

    private void toggle(DrawContext ctx, Object key, int x, int y, boolean on) {
        float a = smooth(key, on ? 1f : 0f);
        Draw.rrectGrad(ctx, x, y, 28, 14, 7, Draw.mix(0xFF2A2F48, ACC_A, a), Draw.mix(0xFF2A2F48, ACC_B, a));
        Draw.rrect(ctx, x + 2 + Math.round(14 * a), y + 2, 10, 10, 5, 0xFFFFFFFF);
    }

    private List<Module> shown() {
        List<Module> out = new ArrayList<>();
        if (!query.isEmpty()) {
            String q = query.toLowerCase(Locale.ROOT);
            for (Module m : NiceVisualsClient.modules.modules)
                if (m.name.toLowerCase(Locale.ROOT).contains(q)) out.add(m);
        } else if (tab.cat != null) {
            out.addAll(NiceVisualsClient.modules.in(tab.cat));
        }
        return out;
    }

    @Override
    public void render(DrawContext ctx, int mx, int my, float delta) {
        renderBackground(ctx, mx, my, delta);
        hits.clear();
        mx = Math.round(mx / S);
        my = Math.round(my / S);
        int vw = Math.round(width / S), vh = Math.round(height / S);
        ctx.getMatrices().push();
        ctx.getMatrices().scale(S, S, 1f);

        int W = Math.min(vw - 16, 560), H = Math.min(vh - 16, 330);
        int x0 = (vw - W) / 2, y0 = (vh - H) / 2;
        int sb = 122;

        // window
        Draw.rrectV(ctx, x0, y0, W, H, 16, 0x48FFFFFF, 0x14FFFFFF);
        Draw.rrectV(ctx, x0 + 1, y0 + 1, W - 2, H - 2, 15, 0xD40E1224, 0xD4080A17);
        Draw.rrectV(ctx, x0 + 3, y0 + 3, W - 6, 46, 13, 0x16FFFFFF, 0x00FFFFFF);
        ctx.fill(x0 + sb, y0 + 10, x0 + sb + 1, y0 + H - 10, 0x16FFFFFF);

        // sidebar: logo + tabs
        ctx.drawTexture(RenderLayer::getGuiTextured, LOGO, x0 + 16, y0 + 14, 0f, 0f, 22, 22, 22, 22);
        txtB(ctx, "Nice Visuals", x0 + 44, y0 + 19, TXT);
        int ty = y0 + 54;
        for (Tab tb : Tab.values()) {
            if (tb == Tab.CLIENT) { txt(ctx, "GENERAL", x0 + 18, ty + 8, 0xFF6C7494); ty += 26; }
            int tx = x0 + 10, tw = sb - 20;
            boolean sel = tb == tab && query.isEmpty();
            boolean hover = mx >= tx && mx < tx + tw && my >= ty && my < ty + 24;
            if (sel) Draw.rrect(ctx, tx, ty, tw, 24, 9, 0x3A7C5CFF);
            else if (hover) Draw.rrect(ctx, tx, ty, tw, 24, 9, 0x12FFFFFF);
            txt(ctx, tb.title, tx + 14, ty + 8, sel ? TXT : DIM);
            if (sel) Draw.rrect(ctx, tx + tw - 14, ty + 9, 6, 6, 3, 0xFFA77BFF);
            final Tab target = tb;
            hits.add(new Hit(tx, ty, tw, 24, false, () -> { tab = target; query = ""; scroll = 0; openDrop = null; }, null));
            ty += 28;
        }
        txt(ctx, "v1.0.0", x0 + 18, y0 + H - 22, 0xFF6C7494);

        // content header
        int cx = x0 + sb + 14, cr = x0 + W - 14, cw = cr - cx;
        boolean searching = !query.isEmpty();
        txtB(ctx, searching ? "Search results" : tab.title, cx, y0 + 14, TXT);
        txt(ctx, searching ? shown().size() + " modules found" : tab.sub, cx, y0 + 27, DIM);

        // search box
        int sbx = cr - 150, sby = y0 + 12, sbw = 150, sbh = 24;
        Draw.rrect(ctx, sbx, sby, sbw, sbh, 12, searchFocus ? 0x667C5CFF : 0x30FFFFFF);
        Draw.rrect(ctx, sbx + 1, sby + 1, sbw - 2, sbh - 2, 11, 0xF0080A16);
        String shownQ = query.isEmpty() && !searchFocus ? "Search modules" : query + (searchFocus && (System.currentTimeMillis() / 500) % 2 == 0 ? "|" : "");
        txt(ctx, shownQ, sbx + 12, sby + 8, query.isEmpty() ? DIM : TXT);
        hits.add(new Hit(sbx, sby, sbw, sbh, false, () -> searchFocus = true, null));

        // module cards
        clipY1 = y0 + 46;
        clipY2 = y0 + H - 38;
        int y = clipY1 - Math.round(scroll);
        int top = y;
        int rx = cr - 12;
        if (tab == Tab.ABOUT && !searching) {
            txtB(ctx, "NiceVisuals v1.0.0", cx, y + 6, TXT);
            txt(ctx, "Press X in game to open this menu.", cx, y + 22, DIM);
            txt(ctx, "Settings are saved in config/nicevisuals.json", cx, y + 36, DIM);
            txt(ctx, "Made by NiceCode.", cx, y + 50, DIM);
            y += 64;
        } else {
            for (Module m : shown()) {
                if (vis(y, 32)) {
                    boolean hov = mx >= cx && mx < cr && my >= y && my < y + 32;
                    Draw.rrect(ctx, cx, y, cw, 32, 10, hov ? 0x1EFFFFFF : 0x12FFFFFF);
                    txt(ctx, m.name, cx + 14, y + 6, TXT);
                    txt(ctx, DESC.getOrDefault(m.name, ""), cx + 14, y + 18, DIM);
                    toggle(ctx, m, rx - 28, y + 9, m.isEnabled());
                    hits.add(new Hit(cx, y, cw, 32, true, m::toggle, null));
                }
                y += 34;
                for (Setting s : m.settings) {
                    if (vis(y, 22)) {
                        txt(ctx, s.name, cx + 26, y + 7, DIM);
                        if (s instanceof BoolSetting b) {
                            toggle(ctx, b, rx - 28, y + 4, b.value);
                            hits.add(new Hit(cx, y, cw, 22, true, () -> b.value = !b.value, null));
                        } else if (s instanceof NumSetting n) {
                            int sx = rx - 126, swid = 92;
                            Draw.rrect(ctx, sx, y + 9, swid, 4, 2, 0x30FFFFFF);
                            float f = (float) ((n.value - n.min) / (n.max - n.min));
                            int fw = Math.max(4, Math.round(swid * f));
                            Draw.rrectGrad(ctx, sx, y + 9, fw, 4, 2, ACC_A, ACC_B);
                            Draw.rrect(ctx, sx + fw - 4, y + 6, 8, 10, 4, 0xFFFFFFFF);
                            txt(ctx, String.valueOf(n.asInt()), rx - 26, y + 7, TXT);
                            hits.add(new Hit(sx - 4, y + 3, swid + 8, 16, true, null, n));
                        } else if (s instanceof ModeSetting md) {
                            int bx = rx - 76, bw = 76;
                            Draw.rrect(ctx, bx, y + 3, bw, 16, 8, 0x26FFFFFF);
                            txt(ctx, md.get(), bx + 10, y + 7, TXT);
                            txt(ctx, openDrop == md ? "^" : "v", bx + bw - 14, y + 7, DIM);
                            final int by = y + 19;
                            hits.add(new Hit(bx, y + 3, bw, 16, true, () -> {
                                if (openDrop == md) openDrop = null;
                                else { openDrop = md; dropX = bx; dropY = by; dropW = bw; }
                            }, null));
                        }
                    }
                    y += 24;
                }
                y += 6;
            }
        }
        maxScroll = Math.max(0, (y - top) - (clipY2 - clipY1));
        scroll = Math.max(0, Math.min(scroll, maxScroll));

        // bottom buttons
        int by = y0 + H - 34, bw = 64, bh = 24, gap = 6;
        int bx = cr - (bw * 3 + gap * 2);
        button(ctx, mx, my, bx, by, bw, bh, "Reset", false, this::doReset);
        button(ctx, mx, my, bx + bw + gap, by, bw, bh, "Cancel", false, this::doCancel);
        button(ctx, mx, my, bx + (bw + gap) * 2, by, bw, bh, "Apply", true, this::doApply);

        // overlays
        ctx.getMatrices().push();
        ctx.getMatrices().translate(0, 0, 300);
        if (openDrop != null) {
            int oh = openDrop.modes.length * 18 + 6;
            Draw.rrect(ctx, dropX, dropY, dropW, oh, 8, 0x44FFFFFF);
            Draw.rrect(ctx, dropX + 1, dropY + 1, dropW - 2, oh - 2, 7, 0xF2101428);
            for (int i = 0; i < openDrop.modes.length; i++) {
                int oy = dropY + 3 + i * 18;
                boolean sel = i == openDrop.index;
                boolean hov = mx >= dropX && mx < dropX + dropW && my >= oy && my < oy + 18;
                if (sel) Draw.rrectGrad(ctx, dropX + 3, oy, dropW - 6, 18, 7, ACC_A, ACC_B);
                else if (hov) Draw.rrect(ctx, dropX + 3, oy, dropW - 6, 18, 7, 0x20FFFFFF);
                txt(ctx, openDrop.modes[i], dropX + 10, oy + 5, sel ? 0xFFFFFFFF : DIM);
            }
        }
        drawToast(ctx, vw);
        ctx.getMatrices().pop();
        ctx.getMatrices().pop();
    }

    private void button(DrawContext ctx, int mx, int my, int x, int y, int w, int h, String label, boolean primary, Runnable r) {
        boolean hover = mx >= x && mx < x + w && my >= y && my < y + h;
        if (primary) Draw.rrectGrad(ctx, x, y, w, h, 10, hover ? 0xFF907BFF : ACC_A, hover ? 0xFF6DB6FF : ACC_B);
        else {
            Draw.rrect(ctx, x, y, w, h, 10, 0x30FFFFFF);
            Draw.rrect(ctx, x + 1, y + 1, w - 2, h - 2, 9, hover ? 0xF0262C48 : 0xF0141830);
        }
        txtC(ctx, label, x + w / 2, y + 8, 0xFFFFFFFF);
        hits.add(new Hit(x, y, w, h, false, r, null));
    }

    private void drawToast(DrawContext ctx, int vw) {
        long age = System.currentTimeMillis() - toastAt;
        if (toastAt == 0 || age > 2800) return;
        float a = age < 250 ? age / 250f : (age > 2300 ? (2800 - age) / 500f : 1f);
        int w = 176, h = 38, x = vw - w - 10, y = 10 - Math.round((1 - Math.min(1f, age / 250f)) * 10);
        Draw.rrect(ctx, x, y, w, h, 12, Draw.alpha(0x50FFFFFF, a));
        Draw.rrect(ctx, x + 1, y + 1, w - 2, h - 2, 11, Draw.alpha(0xF00E1224, a));
        Draw.rrect(ctx, x + 9, y + 9, 20, 20, 10, Draw.alpha(0xFF3DDC97, a));
        int ck = Draw.alpha(0xFFFFFFFF, a);
        ctx.fill(x + 14, y + 19, x + 16, y + 21, ck);
        ctx.fill(x + 16, y + 21, x + 18, y + 23, ck);
        for (int i = 0; i < 5; i++) ctx.fill(x + 18 + i, y + 21 - i, x + 20 + i, y + 23 - i, ck);
        ctx.drawText(textRenderer, tb("Settings Saved!"), x + 38, y + 9, Draw.alpha(TXT, a), false);
        ctx.drawText(textRenderer, t("Your changes have been applied."), x + 38, y + 21, Draw.alpha(DIM, a), false);
    }

    private void doApply() { NiceVisualsClient.modules.save(); toastAt = System.currentTimeMillis(); }

    private void doCancel() { NiceVisualsClient.modules.fromJson(snapshot); close(); }

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
        mx /= S;
        my /= S;
        if (openDrop != null) {
            for (int i = 0; i < openDrop.modes.length; i++) {
                int oy = dropY + 3 + i * 18;
                if (mx >= dropX && mx < dropX + dropW && my >= oy && my < oy + 18) { openDrop.index = i; break; }
            }
            openDrop = null;
            return true;
        }
        searchFocus = false;
        for (Hit h : hits) {
            if (mx < h.x() || mx >= h.x() + h.w() || my < h.y() || my >= h.y() + h.h()) continue;
            if (h.scrolled() && (my < clipY1 || my >= clipY2)) continue;
            if (h.slider() != null) { dragging = h.slider(); dragX = h.x() + 4; dragW = h.w() - 8; applyDrag(mx); }
            else h.action().run();
            return true;
        }
        return super.mouseClicked(mx * S, my * S, button);
    }

    private void applyDrag(double mx) {
        double f = Math.max(0, Math.min(1, (mx - dragX) / dragW));
        dragging.value = dragging.min + f * (dragging.max - dragging.min);
    }

    @Override
    public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
        if (dragging != null) { applyDrag(mx / S); return true; }
        return super.mouseDragged(mx, my, button, dx, dy);
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        dragging = null;
        return super.mouseReleased(mx, my, button);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double hAmount, double vAmount) {
        scroll = Math.max(0, Math.min(maxScroll, scroll - (float) vAmount * 18f));
        openDrop = null;
        return true;
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        if (searchFocus && chr >= 32 && chr != 127 && query.length() < 24) {
            query += chr;
            scroll = 0;
            return true;
        }
        return super.charTyped(chr, modifiers);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (searchFocus) {
            if (keyCode == 259 && !query.isEmpty()) { query = query.substring(0, query.length() - 1); scroll = 0; return true; }
            if (keyCode == 256 || keyCode == 257) { searchFocus = false; return true; }
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void removed() {
        NiceVisualsClient.modules.save();
        super.removed();
    }
}
