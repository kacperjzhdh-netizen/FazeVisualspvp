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
import pl.nicevisuals.module.Interface;
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
import java.util.function.Function;

/** Themeable settings screen: top tab bar, two-column module cards, appearance editor, particles. */
public class ClickGui extends Screen {
    private static final Identifier LOGO = Identifier.of("nicevisuals", "textures/gui/logo.png");
    private static final Identifier FONT = Identifier.of("nicevisuals", "ui");
    private static final Identifier FONT_B = Identifier.of("nicevisuals", "ui_bold");
    private static final float S = 1.2f;

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
            Map.entry("Direction", "Show where you are facing."),
            Map.entry("Memory", "Show RAM used by the game."),
            Map.entry("Biome", "Show the biome you are in."),
            Map.entry("Server", "Show the server address."),
            Map.entry("World Time", "Show the in-game time of day."),
            Map.entry("Item Counter", "Totems, gapples and pearls you carry."),
            Map.entry("Hurt Flash", "Red edge flash when you take damage."),
            Map.entry("Low Health", "Pulsing red edges at low health."),
            Map.entry("Hit Sound", "Play a sound every time you hit."),
            Map.entry("Kill Counter", "Counts your kills."),
            Map.entry("Status", "Show sprinting, sneaking or swimming."),
            Map.entry("Day Counter", "Show the world day number."),
            Map.entry("Light Level", "Show the light level where you stand."),
            Map.entry("Time Changer", "Force a fixed time of day (client only)."),
            Map.entry("Weather", "Remove rain and thunder (client only)."),
            Map.entry("Hit Effect", "Particles where you hit."),
            Map.entry("Kill Effect", "Particle pillar and sound on a kill."),
            Map.entry("Trails", "Particle trail behind you."),
            Map.entry("Menu Style", "Colour glow over title, multiplayer and add-server screens."),
            Map.entry("Sky", "Pick the colour of the sky."),
            Map.entry("Jump Particles", "Particle ring when you jump."),
            Map.entry("Discord RPC", "Show server and play time on your Discord."));

    private enum Tab {
        COMBAT("Combat", "Targeting and hit feedback.", Category.COMBAT),
        HUD("HUD", "Information on your screen.", Category.HUD),
        WORLD("World", "How the world looks.", Category.WORLD),
        CLIENT("Client", "Menu appearance.", Category.CLIENT),
        ABOUT("About", "LeafVisuals.", null);
        final String title, sub; final Category cat;
        Tab(String t, String s, Category c) { title = t; sub = s; cat = c; }
    }

    private record Hit(int x, int y, int w, int h, boolean scrolled, Runnable action, NumSetting slider, int sx, int sw) {}
    private interface RowDraw { void draw(DrawContext ctx, int x, int y, int w); }
    private record Row(int h, RowDraw draw) {}
    private static final class Card {
        final List<Row> rows = new ArrayList<>();
        int height() { int h = 16; for (Row r : rows) h += r.h(); return h; }
    }
    private static final class P { float x, y, size, phase; }

    private final List<Hit> hits = new ArrayList<>();
    private final Map<Object, Float> anim = new HashMap<>();
    private final List<P> ps = new ArrayList<>();
    private final JsonObject snapshot;
    private Tab tab = Tab.COMBAT;
    private float scroll, maxScroll, listScroll, listMax;
    private Module selected;
    private int listRight;
    private int clipY1, clipY2, mx, my;
    private ModeSetting openDrop;
    private int dropX, dropY, dropW;
    private NumSetting dragging;
    private int dragX, dragW;
    private long toastAt, lastNs;
    private String query = "";
    private boolean searchFocus;
    private Interface ui;

    // palette (recomputed every frame from the theme)
    private int txt, dim, cardFill, cardHover, border, field, off, accA, accB, win0, win1;
    private boolean light;

    private final net.minecraft.client.gui.screen.Screen parent;

    public ClickGui() { this(null); }

    public ClickGui(net.minecraft.client.gui.screen.Screen parent) {
        super(Text.literal("LeafVisuals"));
        this.parent = parent;
        snapshot = NiceVisualsClient.modules.toJson();
    }

    @Override public boolean shouldPause() { return false; }

    @Override public void close() { client.setScreen(parent); }

    // ---- text ----
    private Text t(String s) { return Text.literal(s).setStyle(Style.EMPTY.withFont(FONT)); }
    private Text tb(String s) { return Text.literal(s).setStyle(Style.EMPTY.withFont(FONT_B)); }
    private float m = 1f; // drawing pixels per layout unit (set in render)
    private static final float SC = 0.8f, SPACING = 0.35f;

    private static String up(char ch) { return String.valueOf(Character.isLetter(ch) ? Character.toUpperCase(ch) : ch); }

    /** Width of a string in small caps (letters are drawn as scaled-down capitals). */
    private float scW(String s, boolean bold) {
        float w = 0;
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            Text tx = bold ? tb(up(ch)) : t(up(ch));
            w += textRenderer.getWidth(tx) * (Character.isLetter(ch) ? SC : 1f) + SPACING;
        }
        return w;
    }

    private void sc(DrawContext c, String s, float x, float y, int col, boolean bold) {
        var ms = c.getMatrices();
        float cx = x;
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            boolean letter = Character.isLetter(ch);
            float k = letter ? SC : 1f;
            Text tx = bold ? tb(up(ch)) : t(up(ch));
            ms.push();
            ms.translate(cx * m, (y + (letter ? (1f - SC) * 7.2f : 0f)) * m, 0);
            ms.scale(m * k, m * k, 1f);
            c.drawText(textRenderer, tx, 0, 0, col, false);
            ms.pop();
            cx += textRenderer.getWidth(tx) * k + SPACING;
        }
    }

    private String fit(String s, int max) {
        if (s == null) return "";
        while (s.length() > 1 && scW(s, true) > max) s = s.substring(0, s.length() - 1);
        return s;
    }

    private void txt(DrawContext c, String s, int x, int y, int col) { sc(c, s, x, y, col, false); }
    private void txtB(DrawContext c, String s, int x, int y, int col) { sc(c, s, x, y, col, true); }
    private void txtR(DrawContext c, String s, int rx, int y, int col) { sc(c, s, rx - scW(s, false), y, col, false); }
    private void txtC(DrawContext c, String s, int cx, int y, int col) { sc(c, s, cx - scW(s, false) / 2f, y, col, false); }
    private void logo(DrawContext c, int x, int y, int size) {
        var ms = c.getMatrices();
        ms.push();
        ms.translate(x * m, y * m, 0);
        ms.scale(m, m, 1f);
        c.drawTexture(RenderLayer::getGuiTextured, LOGO, 0, 0, 0f, 0f, size, size, size, size);
        ms.pop();
    }

    private static int hsb(float hue, float s, float b) {
        return 0xFF000000 | (java.awt.Color.HSBtoRGB(((hue % 360f) + 360f) % 360f / 360f, s, b) & 0xFFFFFF);
    }

    private void palette() {
        float hue = (float) ui.hue.value;
        accA = hsb(hue, 0.62f, 1f);
        accB = hsb(hue + 35f, 0.55f, 1f);
        int theme = ui.theme.index;
        if (theme == 4) { accA = 0xFFFF8A2B; accB = 0xFFFF3D1E; }
        light = theme == 2;
        if (theme == 0) {                       // Midnight: opaque dark
            win0 = 0xFF0C0E1A; win1 = 0xFF080A13;
            txt = 0xFFEDEFFA; dim = 0xFF8A92B2; cardFill = 0x16FFFFFF; cardHover = 0x22FFFFFF;
            border = 0x20FFFFFF; field = 0xFF080A13; off = 0xFF2A2F48;
        } else if (theme == 1) {                // Glass: translucent, tinted by the accent
            int a = Math.max(0x30, 232 - (int) (ui.glass.value * 1.55));
            win0 = (a << 24) | (Draw.mix(0xFF1A1236, accA, 0.30f) & 0xFFFFFF);
            win1 = (a << 24) | (Draw.mix(0xFF0B0A1E, accA, 0.12f) & 0xFFFFFF);
            txt = 0xFFF4F1FF; dim = 0xFFB9B2D8; cardFill = 0x1CFFFFFF; cardHover = 0x2AFFFFFF;
            border = 0x50FFFFFF; field = 0x66100A24; off = 0xFF39345A;
        } else if (theme == 4) {                // Ember: near-black with orange-red accents
            win0 = 0xF2121010; win1 = 0xF20A0808;
            txt = 0xFFF3EEEA; dim = 0xFF9C8F89; cardFill = 0x1CFFFFFF; cardHover = 0x2CFFFFFF;
            border = 0x26FFFFFF; field = 0xFF0B0909; off = 0xFF3A302C;
        } else if (theme == 3) {                // Shader: very clear, neon-tinted, glowing edges
            win0 = 0x55000000 | (Draw.mix(0xFF06241A, accA, 0.38f) & 0xFFFFFF);
            win1 = 0x40000000 | (Draw.mix(0xFF04120E, accB, 0.22f) & 0xFFFFFF);
            txt = 0xFFF2FFF8; dim = 0xFFA9D6C2; cardFill = 0x22FFFFFF; cardHover = 0x33FFFFFF;
            border = Draw.alpha(accA, 0.75f); field = 0x55061410; off = 0xFF2C4A3E;
        } else {                                // Frost: light
            win0 = 0xEEF3F6FF; win1 = 0xEEE5EAFA;
            txt = 0xFF1B2140; dim = 0xFF5F688E; cardFill = 0x1A1B2140; cardHover = 0x281B2140;
            border = 0x301B2140; field = 0xFFFFFFFF; off = 0xFFBFC6E0;
        }
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
        Draw.rrectGrad(ctx, x, y, 28, 14, 7, Draw.mix(off, accA, a), Draw.mix(off, accB, a));
        Draw.rrect(ctx, x + 2 + Math.round(14 * a), y + 2, 10, 10, 5, 0xFFFFFFFF);
    }

    private List<Module> shown() {
        List<Module> out = new ArrayList<>();
        if (!query.isEmpty()) {
            String q = query.toLowerCase(Locale.ROOT);
            for (Module m : NiceVisualsClient.modules.modules)
                if (m != ui && m.name.toLowerCase(Locale.ROOT).contains(q)) out.add(m);
        } else if (tab.cat != null) {
            for (Module m : NiceVisualsClient.modules.in(tab.cat)) if (m != ui) out.add(m);
        }
        return out;
    }

    // ---- row builders ----
    private Row titleRow(String s) { return new Row(20, (c, x, y, w) -> txtB(c, s, x, y + 2, txt)); }

    private Row sliderRow(NumSetting n, String label, Function<Double, String> fmt) {
        return new Row(30, (c, x, y, w) -> {
            txt(c, label, x, y + 1, dim);
            txtR(c, fmt.apply(n.value), x + w, y + 1, txt);
            Draw.rrect(c, x, y + 16, w, 5, 2, light ? 0x30000000 : 0x30FFFFFF);
            float f = (float) ((n.value - n.min) / (n.max - n.min));
            int fw = Math.max(5, Math.round(w * f));
            Draw.rrectGrad(c, x, y + 16, fw, 5, 2, accA, accB);
            Draw.rrect(c, x + fw - 5, y + 13, 10, 11, 5, 0xFFFFFFFF);
            hits.add(new Hit(x - 2, y + 10, w + 4, 16, true, null, n, x, w));
        });
    }

    private Row boolRow(BoolSetting b) {
        return new Row(24, (c, x, y, w) -> {
            txt(c, b.name, x + 6, y + 7, dim);
            toggle(c, b, x + w - 28, y + 4, b.value);
            hits.add(new Hit(x, y, w, 22, true, () -> b.value = !b.value, null, 0, 0));
        });
    }

    private Row modeRow(ModeSetting md) {
        return new Row(24, (c, x, y, w) -> {
            txt(c, md.name, x + 6, y + 7, dim);
            int bw = 80, bx = x + w - bw;
            Draw.rrect(c, bx, y + 3, bw, 17, 8, field);
            txt(c, md.get(), bx + 10, y + 7, txt);
            txt(c, openDrop == md ? "^" : "v", bx + bw - 14, y + 7, dim);
            final int by = y + 20;
            hits.add(new Hit(bx, y + 3, bw, 17, true, () -> {
                if (openDrop == md) openDrop = null;
                else { openDrop = md; dropX = bx; dropY = by; dropW = bw; }
            }, null, 0, 0));
        });
    }

    private Card moduleCard(Module m) {
        Card c = new Card();
        c.rows.add(new Row(32, (ctx, x, y, w) -> {
            txt(ctx, m.name, x, y + 4, txt);
            txt(ctx, DESC.getOrDefault(m.name, ""), x, y + 17, dim);
            toggle(ctx, m, x + w - 28, y + 8, m.isEnabled());
            hits.add(new Hit(x - 12, y, w + 24, 32, true, m::toggle, null, 0, 0));
        }));
        for (Setting s : m.settings) {
            if (s instanceof BoolSetting b) c.rows.add(boolRow(b));
            else if (s instanceof NumSetting n) c.rows.add(sliderRow(n, s.name, v -> String.valueOf(n.asInt())));
            else if (s instanceof ModeSetting md) c.rows.add(modeRow(md));
        }
        return c;
    }

    private Card themeCard() {
        Card c = new Card();
        c.rows.add(titleRow("GUI Theme"));
        c.rows.add(new Row(66, (ctx, x, y, w) -> {
            String[] names = ui.theme.modes;
            int tw = (w - 18) / 4;
            int[] order = {4, 3, 1, 2};
            for (int oi = 0; oi < 4; oi++) {
                int i = order[oi];
                int tx = x + oi * (tw + 6);
                boolean sel = ui.theme.index == i;
                Draw.rrect(ctx, tx, y, tw, 62, 9, sel ? accA : border);
                Draw.rrect(ctx, tx + 1, y + 1, tw - 2, 60, 8, light ? 0xFFF3F5FC : 0xFF10122A);
                int pv = i == 0 ? 0xFF0C0E1A : i == 1 ? Draw.mix(0xFF2A1B52, accA, 0.35f) : i == 3 ? Draw.mix(0xFF06241A, accA, 0.55f) : i == 4 ? 0xFF2A130A : 0xFFF2F5FF;
                Draw.rrect(ctx, tx + 6, y + 6, tw - 12, 28, 6, pv);
                Draw.rrect(ctx, tx + tw / 2 - 3, y + 17, 6, 6, 3, i == 2 ? 0xFF1B2140 : i == 4 ? 0xFFFF6A1F : accB);
                txtC(ctx, names[i], tx + tw / 2, y + 40, txt);
                final int idx = i;
                hits.add(new Hit(tx, y, tw, 62, true, () -> ui.theme.index = idx, null, 0, 0));
            }
        }));
        return c;
    }

    private Card accentCard() {
        Card c = new Card();
        c.rows.add(titleRow("Accent color"));
        c.rows.add(new Row(24, (ctx, x, y, w) -> {
            for (int i = 0; i < w; i++) Draw.fill(ctx, x + i, y + 6, x + i + 1, y + 14, hsb(360f * i / (w - 1), 0.75f, 1f));
            int kx = x + Math.round((float) (ui.hue.value / 360.0) * (w - 1));
            Draw.rrect(ctx, kx - 6, y + 4, 12, 12, 6, 0xFFFFFFFF);
            Draw.rrect(ctx, kx - 4, y + 6, 8, 8, 4, accA);
            hits.add(new Hit(x - 2, y, w + 4, 20, true, null, ui.hue, x, w));
        }));
        c.rows.add(new Row(24, (ctx, x, y, w) -> {
            float[] hues = {350, 20, 50, 130, 185, 220, 265, 310};
            int step = (w - 14) / (hues.length - 1);
            for (int i = 0; i < hues.length; i++) {
                int dx = x + 7 + i * step;
                boolean sel = Math.abs(ui.hue.value - hues[i]) < 1;
                if (sel) Draw.rrect(ctx, dx - 9, y + 1, 18, 18, 9, 0xFFFFFFFF);
                Draw.rrect(ctx, dx - 7, y + 3, 14, 14, 7, hsb(hues[i], 0.65f, 1f));
                final float hv = hues[i];
                hits.add(new Hit(dx - 9, y + 1, 18, 18, true, () -> ui.hue.value = hv, null, 0, 0));
            }
        }));
        return c;
    }

    private Card glassCard() {
        Card c = new Card();
        c.rows.add(titleRow("Glass"));
        c.rows.add(sliderRow(ui.glass, "Glass strength", v -> Math.round(v) + "%"));
        return c;
    }

    private Card particlesCard() {
        Card c = new Card();
        c.rows.add(titleRow("Background particles"));
        c.rows.add(new Row(28, (ctx, x, y, w) -> {
            Draw.rrect(ctx, x, y + 1, w, 24, 12, field);
            int n = ui.particles.modes.length, sw = (w - 4) / n;
            for (int i = 0; i < n; i++) {
                int sx = x + 2 + i * sw;
                boolean sel = ui.particles.index == i;
                if (sel) Draw.rrectGrad(ctx, sx, y + 3, sw, 20, 10, accA, accB);
                txtC(ctx, ui.particles.modes[i], sx + sw / 2, y + 9, sel ? 0xFFFFFFFF : dim);
                final int idx = i;
                hits.add(new Hit(sx, y + 3, sw, 20, true, () -> ui.particles.index = idx, null, 0, 0));
            }
        }));
        c.rows.add(sliderRow(ui.amount, "Amount", v -> String.valueOf(Math.round(v))));
        c.rows.add(sliderRow(ui.speed, "Speed", v -> String.format(Locale.ROOT, "%.1fx", v / 10.0)));
        c.rows.add(sliderRow(ui.size, "Size", v -> String.format(Locale.ROOT, "%.1fx", v / 10.0)));
        return c;
    }

    private Card aboutCard() {
        Card c = new Card();
        c.rows.add(titleRow("LeafVisuals v1.0.0"));
        c.rows.add(new Row(14, (ctx, x, y, w) -> txt(ctx, "Press X in game to open this menu.", x, y, dim)));
        c.rows.add(new Row(14, (ctx, x, y, w) -> txt(ctx, "Settings are saved in config/nicevisuals.json", x, y, dim)));
        c.rows.add(new Row(14, (ctx, x, y, w) -> txt(ctx, "Made by LeafVisuals.", x, y, dim)));
        return c;
    }

    private List<Card> buildCards() {
        List<Card> out = new ArrayList<>();
        if (query.isEmpty() && tab == Tab.CLIENT) {
            out.add(themeCard()); out.add(accentCard()); out.add(glassCard()); out.add(particlesCard());
            for (Module m : shown()) out.add(moduleCard(m));
        } else if (query.isEmpty() && tab == Tab.ABOUT) {
            out.add(aboutCard());
        } else {
            for (Module m : shown()) out.add(moduleCard(m));
        }
        return out;
    }

    // ---- particles ----
    private void drawParticles(DrawContext ctx, int x0, int y0, int W, int H) {
        int mode = ui.particles.index;
        int n = mode == 0 ? 0 : ui.amount.asInt();
        while (ps.size() < n) {
            P p = new P();
            p.x = (float) Math.random(); p.y = (float) Math.random();
            p.size = 1.5f + (float) Math.random() * 2.2f; p.phase = (float) (Math.random() * 6.28);
            ps.add(p);
        }
        while (ps.size() > n) ps.remove(ps.size() - 1);
        if (n == 0) return;
        long now = System.nanoTime();
        float dt = lastNs == 0 ? 0f : Math.min(0.1f, (now - lastNs) / 1e9f);
        lastNs = now;
        float sp = (float) (ui.speed.value / 10.0), sz = (float) (ui.size.value / 10.0);
        float time = (now / 1_000_000_000L % 100000L) + (now % 1_000_000_000L) / 1e9f;
        for (P p : ps) {
            switch (mode) {
                case 3 -> { p.y += (0.04f + p.size * 0.02f) * sp * dt; p.x += (float) Math.sin(time + p.phase) * 0.012f * sp * dt; }
                case 2 -> { p.y -= 0.03f * sp * dt; p.x += 0.012f * sp * dt; }
                default -> { p.y -= 0.045f * sp * dt; p.x += (float) Math.sin(time * 0.6 + p.phase) * 0.01f * sp * dt; }
            }
            if (p.y > 1f) p.y -= 1f; else if (p.y < 0f) p.y += 1f;
            if (p.x > 1f) p.x -= 1f; else if (p.x < 0f) p.x += 1f;
            int px = x0 + 10 + Math.round(p.x * (W - 20)), py = y0 + 10 + Math.round(p.y * (H - 20));
            int s = Math.max(1, Math.round(p.size * sz));
            float a = 0.25f + 0.5f * (0.5f + 0.5f * (float) Math.sin(time * 1.3 + p.phase));
            int col = mode == 3 ? Draw.alpha(light ? 0xFF9AA6D8 : 0xFFFFFFFF, a) : Draw.alpha(Draw.mix(accA, 0xFFFFFFFF, 0.3f), a);
            if (mode == 1) Draw.fill(ctx, px, py, px + s, py + s, col);
            else Draw.rrect(ctx, px, py, s, s, s / 2 + 1, col);
        }
    }

    // ---- render ----
    @Override
    public void render(DrawContext ctx, int rmx, int rmy, float delta) {
        ui = NiceVisualsClient.modules.ui;
        palette();
        renderBackground(ctx, rmx, rmy, delta);
        hits.clear();
        mx = Math.round(rmx / S);
        my = Math.round(rmy / S);
        int vw = Math.round(width / S), vh = Math.round(height / S);
        float gs = (float) client.getWindow().getScaleFactor();
        m = S * gs;
        Draw.unit = m;
        ctx.getMatrices().push();
        ctx.getMatrices().scale(1f / gs, 1f / gs, 1f);

        int W = Math.min(vw - 16, 660), H = Math.min(vh - 16, 360);
        int x0 = (vw - W) / 2, y0 = (vh - H) / 2;

        // soft shadow, window + particles
        for (int i = 6; i >= 1; i--) Draw.rrect(ctx, x0 - i * 2, y0 - i * 2 + 4, W + i * 4, H + i * 4, 16 + i * 2, light ? 0x07102040 : ui.theme.index == 3 ? Draw.alpha(accA, 0.06f) : 0x0B000000);
        Draw.rrectV(ctx, x0, y0, W, H, 16, border, Draw.alpha(border, 0.4f));
        Draw.rrectV(ctx, x0 + 1, y0 + 1, W - 2, H - 2, 15, win0, win1);
        if (ui.theme.index == 1 || ui.theme.index == 3) Draw.rrectV(ctx, x0 + 3, y0 + 3, W - 6, 40, 13, 0x1CFFFFFF, 0x00FFFFFF);
        drawParticles(ctx, x0, y0, W, H);

        // sidebar: logo, categories, status
        int SB = 150;
        Draw.rrect(ctx, x0 + 8, y0 + 8, SB - 8, H - 16, 13, field);
        logo(ctx, x0 + 20, y0 + 18, 24);
        txtB(ctx, "LeafVisuals", x0 + 50, y0 + 19, txt);
        txt(ctx, "Client", x0 + 50, y0 + 31, dim);
        Draw.fill(ctx, x0 + 20, y0 + 52, x0 + SB - 4, y0 + 53, border);
        txt(ctx, "Categories", x0 + 22, y0 + 62, dim);
        Tab[] tabs = Tab.values();
        int ty = y0 + 78;
        for (Tab tb : tabs) {
            boolean sel = tb == tab && query.isEmpty();
            boolean hov = mx >= x0 + 14 && mx < x0 + SB - 4 && my >= ty && my < ty + 28;
            if (sel) Draw.rrectGrad(ctx, x0 + 14, ty, SB - 18, 28, 10, accA, accB);
            else if (hov) Draw.rrect(ctx, x0 + 14, ty, SB - 18, 28, 10, cardFill);
            Draw.rrect(ctx, x0 + 24, ty + 10, 8, 8, 4, sel ? 0xFFFFFFFF : Draw.alpha(accA, 0.8f));
            txt(ctx, tb.title, x0 + 40, ty + 10, sel ? 0xFFFFFFFF : txt);
            final Tab target = tb;
            hits.add(new Hit(x0 + 14, ty, SB - 18, 28, false, () -> { tab = target; query = ""; scroll = 0; listScroll = 0; selected = null; openDrop = null; }, null, 0, 0));
            ty += 32;
        }
        // profile: nick + where you play
        String nick = client.getSession().getUsername();
        var se = client.getCurrentServerEntry();
        String where = se != null ? se.address : client.world != null ? "Singleplayer" : "Main menu";
        int pY = y0 + H - 108;
        Draw.rrect(ctx, x0 + 14, pY, SB - 28, 60, 12, border);
        Draw.rrect(ctx, x0 + 15, pY + 1, SB - 30, 58, 11, cardFill);
        txt(ctx, "Player", x0 + 24, pY + 8, dim);
        txtB(ctx, fit(nick, SB - 48), x0 + 24, pY + 19, txt);
        txt(ctx, "Playing on", x0 + 24, pY + 34, dim);
        txtB(ctx, fit(where, SB - 48), x0 + 24, pY + 45, accA);
        int on = 0;
        for (Module m : NiceVisualsClient.modules.modules) if (m.isEnabled() && m != ui) on++;
        String chipText = on + (on == 1 ? " module on" : " modules on");
        int chipW = SB - 28, chipY = y0 + H - 38;
        Draw.rrect(ctx, x0 + 14, chipY, chipW, 24, 12, 0x5538D98A);
        Draw.rrect(ctx, x0 + 15, chipY + 1, chipW - 2, 22, 11, cardFill);
        Draw.rrect(ctx, x0 + 24, chipY + 9, 6, 6, 3, 0xFF38D98A);
        txt(ctx, chipText, x0 + 38, chipY + 8, txt);

        // content header: title, search, close
        int cx = x0 + SB + 14, cr = x0 + W - 16, cw = cr - cx;
        boolean searching = !query.isEmpty();
        txtB(ctx, searching ? "Search results" : tab.title, cx, y0 + 16, txt);
        txt(ctx, searching ? shown().size() + " modules found" : tab.sub, cx, y0 + 29, dim);
        int closeX = cr - 24;
        Draw.rrect(ctx, closeX, y0 + 12, 24, 24, 12, cardFill);
        txtC(ctx, "×", closeX + 12, y0 + 17, txt);
        hits.add(new Hit(closeX, y0 + 12, 24, 24, false, this::close, null, 0, 0));
        int sbw = Math.min(150, cw - 140), sbx = closeX - 8 - sbw, sby = y0 + 12, sbh = 24;
        Draw.rrect(ctx, sbx, sby, sbw, sbh, 12, searchFocus ? accA : border);
        Draw.rrect(ctx, sbx + 1, sby + 1, sbw - 2, sbh - 2, 11, field);
        String shownQ = query.isEmpty() && !searchFocus ? "Search modules"
                : query + (searchFocus && (System.currentTimeMillis() / 500) % 2 == 0 ? "|" : "");
        txt(ctx, shownQ, sbx + 12, sby + 8, query.isEmpty() ? dim : txt);
        hits.add(new Hit(sbx, sby, sbw, sbh, false, () -> searchFocus = true, null, 0, 0));

        // content: module list + settings of the selected module (or a card grid)
        clipY1 = y0 + 50;
        clipY2 = y0 + H - 40;
        boolean detail = !query.isEmpty() || (tab.cat != null && tab != Tab.CLIENT);
        int gx = cx, gw = cw;
        List<Card> cards;
        listRight = 0;
        if (detail) {
            List<Module> mods = shown();
            if (selected == null || !mods.contains(selected)) selected = mods.isEmpty() ? null : mods.get(0);
            int lw = Math.min(190, cw / 2 - 10);
            listRight = cx + lw;
            int ly = clipY1 - Math.round(listScroll);
            for (Module md : mods) {
                if (ly >= clipY1 && ly + 26 <= clipY2) {
                    boolean mon = md.isEnabled(), sel = md == selected;
                    boolean hov = mx >= cx && mx < cx + lw && my >= ly && my < ly + 26;
                    if (mon) Draw.rrectGrad(ctx, cx, ly, lw, 26, 9, accA, accB);
                    else {
                        Draw.rrect(ctx, cx, ly, lw, 26, 9, sel ? accA : border);
                        Draw.rrect(ctx, cx + 1, ly + 1, lw - 2, 24, 8, hov ? cardHover : cardFill);
                    }
                    txt(ctx, md.name, cx + 12, ly + 9, mon ? 0xFFFFFFFF : (sel ? txt : dim));
                    Draw.rrect(ctx, cx + lw - 26, ly + 8, 10, 10, 5, mon ? 0xCCFFFFFF : sel ? accA : off);
                    Draw.rrect(ctx, cx + lw - 23, ly + 11, 4, 4, 2, mon ? accB : cardFill);
                    final Module mm = md;
                    hits.add(new Hit(cx, ly, lw - 30, 26, true, () -> { selected = mm; mm.toggle(); }, null, 0, 0));
                    hits.add(new Hit(cx + lw - 30, ly, 30, 26, true, () -> selected = mm, null, 0, 0));
                }
                ly += 31;
            }
            listMax = Math.max(0, mods.size() * 31 - (clipY2 - clipY1));
            listScroll = Math.max(0, Math.min(listScroll, listMax));
            gx = cx + lw + 12;
            gw = cr - gx;
            cards = new ArrayList<>();
            if (selected != null) {
                Card full = moduleCard(selected);
                Card head = new Card();
                head.rows.add(full.rows.get(0));
                cards.add(head);
                List<Row> rest = full.rows.subList(1, full.rows.size());
                if (!rest.isEmpty()) {
                    if (gw >= 380 && rest.size() > 1) {
                        int half = (rest.size() + 1) / 2;
                        Card c1 = new Card(), c2 = new Card();
                        c1.rows.addAll(rest.subList(0, half));
                        c2.rows.addAll(rest.subList(half, rest.size()));
                        cards.add(c1); cards.add(c2);
                    } else {
                        Card c1 = new Card();
                        c1.rows.addAll(rest);
                        cards.add(c1);
                    }
                }
            }
        } else cards = buildCards();
        int cols = gw < 380 ? 1 : 2, gap = 10;
        int colW = (gw - gap * (cols - 1)) / cols;
        int[] colH = new int[cols];
        int top = clipY1 - Math.round(scroll);
        for (Card card : cards) {
            int col = 0;
            for (int i = 1; i < cols; i++) if (colH[i] < colH[col]) col = i;
            int cardX = gx + col * (colW + gap), cardY = top + colH[col], ch = card.height();
            int y1 = Math.max(cardY, clipY1), y2 = Math.min(cardY + ch, clipY2);
            if (y2 > y1) {
                boolean full = y1 == cardY && y2 == cardY + ch;
                if (full) Draw.rrect(ctx, cardX, cardY, colW, ch, 12, border);
                Draw.rrect(ctx, cardX + (full ? 1 : 0), y1 + (full ? 1 : 0), colW - (full ? 2 : 0), y2 - y1 - (full ? 2 : 0), 11, cardFill);
            }
            int ry = cardY + 8;
            for (Row r : card.rows) {
                if (vis(ry, r.h())) r.draw().draw(ctx, cardX + 12, ry, colW - 24);
                ry += r.h();
            }
            colH[col] += ch + gap;
        }
        int total = 0;
        for (int h : colH) total = Math.max(total, h);
        maxScroll = Math.max(0, total - (clipY2 - clipY1));
        scroll = Math.max(0, Math.min(scroll, maxScroll));

        // footer buttons
        int by = y0 + H - 34, bw = 64, bh = 24, bgap = 6;
        int bx = cr - (bw * 3 + bgap * 2);
        button(ctx, bx, by, bw, bh, "Reset", false, this::doReset);
        button(ctx, bx + bw + bgap, by, bw, bh, "Cancel", false, this::doCancel);
        button(ctx, bx + (bw + bgap) * 2, by, bw, bh, "Apply", true, this::doApply);

        // overlays
        ctx.getMatrices().push();
        ctx.getMatrices().translate(0, 0, 300);
        if (openDrop != null) {
            int oh = openDrop.modes.length * 18 + 6;
            Draw.rrect(ctx, dropX, dropY, dropW, oh, 8, border);
            Draw.rrect(ctx, dropX + 1, dropY + 1, dropW - 2, oh - 2, 7, light ? 0xFFFFFFFF : 0xFF12162B);
            for (int i = 0; i < openDrop.modes.length; i++) {
                int oy = dropY + 3 + i * 18;
                boolean sel = i == openDrop.index;
                boolean hov = mx >= dropX && mx < dropX + dropW && my >= oy && my < oy + 18;
                if (sel) Draw.rrectGrad(ctx, dropX + 3, oy, dropW - 6, 18, 7, accA, accB);
                else if (hov) Draw.rrect(ctx, dropX + 3, oy, dropW - 6, 18, 7, cardHover);
                txt(ctx, openDrop.modes[i], dropX + 10, oy + 5, sel ? 0xFFFFFFFF : dim);
            }
        }
        drawToast(ctx, vw);
        ctx.getMatrices().pop();
        ctx.getMatrices().pop();
        Draw.unit = 1f;
    }

    private void button(DrawContext ctx, int x, int y, int w, int h, String label, boolean primary, Runnable r) {
        boolean hover = mx >= x && mx < x + w && my >= y && my < y + h;
        if (primary) Draw.rrectGrad(ctx, x, y, w, h, 12, accA, accB);
        else {
            Draw.rrect(ctx, x, y, w, h, 12, border);
            Draw.rrect(ctx, x + 1, y + 1, w - 2, h - 2, 11, hover ? cardHover : field);
        }
        txtC(ctx, label, x + w / 2, y + 8, primary ? 0xFFFFFFFF : txt);
        hits.add(new Hit(x, y, w, h, false, r, null, 0, 0));
    }

    private void drawToast(DrawContext ctx, int vw) {
        long age = System.currentTimeMillis() - toastAt;
        if (toastAt == 0 || age > 2800) return;
        float a = age < 250 ? age / 250f : (age > 2300 ? (2800 - age) / 500f : 1f);
        int w = 176, h = 38, x = vw - w - 10, y = 10 - Math.round((1 - Math.min(1f, age / 250f)) * 10);
        Draw.rrect(ctx, x, y, w, h, 12, Draw.alpha(border, a));
        Draw.rrect(ctx, x + 1, y + 1, w - 2, h - 2, 11, Draw.alpha(light ? 0xFFFFFFFF : 0xF2101428, a));
        Draw.rrect(ctx, x + 9, y + 9, 20, 20, 10, Draw.alpha(0xFF3DDC97, a));
        int ck = Draw.alpha(0xFFFFFFFF, a);
        Draw.fill(ctx, x + 14, y + 19, x + 16, y + 21, ck);
        Draw.fill(ctx, x + 16, y + 21, x + 18, y + 23, ck);
        for (int i = 0; i < 5; i++) Draw.fill(ctx, x + 18 + i, y + 21 - i, x + 20 + i, y + 23 - i, ck);
        txtB(ctx, "Settings Saved!", x + 38, y + 9, Draw.alpha(txt, a));
        txt(ctx, "Your changes have been applied.", x + 38, y + 21, Draw.alpha(dim, a));
    }

    private void doApply() { NiceVisualsClient.modules.save(); toastAt = System.currentTimeMillis(); }
    private void doCancel() { NiceVisualsClient.modules.fromJson(snapshot); close(); }

    private void doReset() {
        if (tab == Tab.ABOUT) return;
        for (Module m : NiceVisualsClient.modules.in(tab.cat)) {
            if (m != ui) m.setEnabled(false);
            for (Setting s : m.settings) s.reset();
        }
    }

    // ---- input ----
    @Override
    public boolean mouseClicked(double x, double y, int button) {
        if (button != 0) return super.mouseClicked(x, y, button);
        x /= S;
        y /= S;
        if (openDrop != null) {
            for (int i = 0; i < openDrop.modes.length; i++) {
                int oy = dropY + 3 + i * 18;
                if (x >= dropX && x < dropX + dropW && y >= oy && y < oy + 18) { openDrop.index = i; break; }
            }
            openDrop = null;
            return true;
        }
        searchFocus = false;
        for (int i = hits.size() - 1; i >= 0; i--) {
            Hit h = hits.get(i);
            if (x < h.x() || x >= h.x() + h.w() || y < h.y() || y >= h.y() + h.h()) continue;
            if (h.scrolled() && (y < clipY1 || y >= clipY2)) continue;
            if (h.slider() != null) { dragging = h.slider(); dragX = h.sx(); dragW = h.sw(); applyDrag(x); }
            else h.action().run();
            return true;
        }
        return super.mouseClicked(x * S, y * S, button);
    }

    private void applyDrag(double x) {
        double f = Math.max(0, Math.min(1, (x - dragX) / dragW));
        dragging.value = dragging.min + f * (dragging.max - dragging.min);
    }

    @Override
    public boolean mouseDragged(double x, double y, int button, double dx, double dy) {
        if (dragging != null) { applyDrag(x / S); return true; }
        return super.mouseDragged(x, y, button, dx, dy);
    }

    @Override
    public boolean mouseReleased(double x, double y, int button) {
        dragging = null;
        return super.mouseReleased(x, y, button);
    }

    @Override
    public boolean mouseScrolled(double x, double y, double hAmount, double vAmount) {
        if (listRight > 0 && mx < listRight) listScroll = Math.max(0, Math.min(listMax, listScroll - (float) vAmount * 20f));
        else scroll = Math.max(0, Math.min(maxScroll, scroll - (float) vAmount * 20f));
        openDrop = null;
        return true;
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        if (searchFocus && chr >= 32 && chr != 127 && query.length() < 24) { query += chr; scroll = 0; return true; }
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
