package pl.faze.visuals;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;
import java.util.function.Supplier;

/** Menu FazeVisuals (klawisz P): kategorie, przelaczniki ON/OFF, suwaki, wybor kolorow. */
public class FazeMenuScreen extends Screen {
    private static final int W = 420, H = 272, SIDE = 104;
    private static final int ROW_H = 22, ROW_GAP = 2, TOP = 42;
    private static final int ACCENT = 0x8A6BFF;
    private static final int[] PRESETS = {0xFF5555, 0xFFAA00, 0xFFFF55, 0x55FF55, 0x55FFFF, 0x5577FF, 0xAA55FF, 0xFF55FF, 0xFFFFFF, 0x202020};

    private final String[] tabs = {"Visuals", "Target HUD", "Particles", "Settings"};
    private final List<List<Row>> pages = new ArrayList<>();
    private int tab = 0;
    private float tabAnim = 0f;
    private int scroll = 0;
    private final long openTime = System.currentTimeMillis();
    private long lastNanos = System.nanoTime();
    private float scale = 1f;

    private Row activeRow;

    // picker
    private String pickerTitle;
    private IntSupplier pickerGet;
    private IntConsumer pickerSet;
    private float ph, ps, pv;
    private int pdrag; // 0 brak, 1 SV, 2 hue
    private static final int PK_W = 190, PK_H = 160;

    // przeciaganie HUD
    private boolean hudDrag;
    private int dragOffX, dragOffY;

    public FazeMenuScreen() {
        super(Text.literal("FazeVisuals"));
        build();
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    public void removed() {
        Config.save();
    }

    // ------------------------------------------------------------------ strony
    private void build() {
        pages.clear();
        Config c = Config.get();
        List<Row> v = new ArrayList<>();
        v.add(new ToggleRow("Target ESP", () -> Config.get().targetEsp, b -> Config.get().targetEsp = b,
                () -> Config.get().targetEspColor, i -> Config.get().targetEspColor = i));
        v.add(new ToggleRow("Box ESP", () -> Config.get().boxEsp, b -> Config.get().boxEsp = b,
                () -> Config.get().boxColor, i -> Config.get().boxColor = i));
        v.add(new SliderRow("Box thickness", 1f, 8f, 0.5f, () -> Config.get().boxThickness, f -> Config.get().boxThickness = f, "%.1f"));
        v.add(new ToggleRow("Glow", () -> Config.get().glow, b -> Config.get().glow = b,
                () -> Config.get().glowColor, i -> Config.get().glowColor = i));
        v.add(new ToggleRow("Target Highlight", () -> Config.get().highlight, b -> Config.get().highlight = b,
                () -> Config.get().highlightColor, i -> Config.get().highlightColor = i));
        v.add(new SliderRow("Highlight opacity", 0f, 1f, 0.05f, () -> Config.get().highlightAlpha, f -> Config.get().highlightAlpha = f, "%.2f"));
        v.add(new ToggleRow("Damage Indicator", () -> Config.get().damageIndicator, b -> Config.get().damageIndicator = b,
                () -> Config.get().damageColor, i -> Config.get().damageColor = i));
        pages.add(v);

        List<Row> h = new ArrayList<>();
        h.add(new ToggleRow("Target HUD", () -> Config.get().hudEnabled, b -> Config.get().hudEnabled = b, null, null));
        h.add(new SliderRow("HUD size", 0.5f, 2f, 0.05f, () -> Config.get().hudScale, f -> Config.get().hudScale = f, "%.2fx"));
        h.add(new ColorRow("HP bar color", () -> Config.get().hudBarColor, i -> Config.get().hudBarColor = i));
        h.add(new ColorRow("HP trail color", () -> Config.get().hudTrailColor, i -> Config.get().hudTrailColor = i));
        h.add(new ColorRow("Background color", () -> Config.get().hudBgColor, i -> Config.get().hudBgColor = i));
        h.add(new ColorRow("Accent color", () -> Config.get().hudAccentColor, i -> Config.get().hudAccentColor = i));
        h.add(new SliderRow("Background opacity", 0.1f, 1f, 0.05f, () -> Config.get().hudBgAlpha, f -> Config.get().hudBgAlpha = f, "%.2f"));
        h.add(new ToggleRow("Show head", () -> Config.get().hudShowHead, b -> Config.get().hudShowHead = b, null, null));
        h.add(new ToggleRow("Show armor", () -> Config.get().hudShowArmor, b -> Config.get().hudShowArmor = b, null, null));
        h.add(new ToggleRow("Show hands", () -> Config.get().hudShowHands, b -> Config.get().hudShowHands = b, null, null));
        h.add(new ButtonRow("Reset HUD position", () -> {
            Config.get().hudX = 0.68f;
            Config.get().hudY = 0.40f;
        }));
        pages.add(h);

        List<Row> p = new ArrayList<>();
        p.add(new ToggleRow("Ender Pearl Trails", () -> Config.get().pearlTrail, b -> Config.get().pearlTrail = b,
                () -> Config.get().pearlColor, i -> Config.get().pearlColor = i));
        p.add(new SliderRow("Trail density", 1f, 10f, 1f, () -> Config.get().pearlDensity, f -> Config.get().pearlDensity = f, "%.0f"));
        p.add(new ToggleRow("Hit Particles", () -> Config.get().hitParticles, b -> Config.get().hitParticles = b,
                () -> Config.get().hitColor, i -> Config.get().hitColor = i));
        p.add(new SliderRow("Hit amount", 1f, 40f, 1f, () -> Config.get().hitAmount, f -> Config.get().hitAmount = f, "%.0f"));
        p.add(new ToggleRow("Critical Hit Particles", () -> Config.get().critParticles, b -> Config.get().critParticles = b,
                () -> Config.get().critColor, i -> Config.get().critColor = i));
        p.add(new SliderRow("Crit amount", 1f, 60f, 1f, () -> Config.get().critAmount, f -> Config.get().critAmount = f, "%.0f"));
        p.add(new ToggleRow("Totem Pop Visuals", () -> Config.get().totemVisuals, b -> Config.get().totemVisuals = b,
                () -> Config.get().totemColor, i -> Config.get().totemColor = i));
        pages.add(p);

        List<Row> s = new ArrayList<>();
        s.add(new CycleRow("Target mode", new String[]{"Looking at", "Attacking", "Both"},
                () -> Config.get().targetMode, i -> Config.get().targetMode = i));
        s.add(new SliderRow("Target timeout (s)", 1f, 15f, 1f, () -> Config.get().targetTimeout, f -> Config.get().targetTimeout = f, "%.0f"));
        s.add(new ToggleRow("Menu animation", () -> Config.get().menuAnimation, b -> Config.get().menuAnimation = b, null, null));
        s.add(new SliderRow("Menu scale", 0.6f, 1.5f, 0.05f, () -> Config.get().menuScale, f -> Config.get().menuScale = f, "%.2fx"));
        s.add(new ButtonRow("Reset all settings", () -> {
            Config.reset();
            build();
        }));
        pages.add(s);
    }

    // ------------------------------------------------------------------ geometria
    private float computeScale() {
        float s = Config.get().menuScale;
        s = Math.min(s, (width - 8f) / W);
        s = Math.min(s, (height - 8f) / H);
        return Math.max(0.3f, s);
    }

    private double lx(double mx) {
        return (mx - width / 2.0) / scale + W / 2.0;
    }

    private double ly(double my) {
        return (my - height / 2.0) / scale + H / 2.0;
    }

    private int contentX() {
        return SIDE + 8;
    }

    private int contentW() {
        return W - SIDE - 18;
    }

    private int fit() {
        return (H - 10 - TOP + ROW_GAP) / (ROW_H + ROW_GAP);
    }

    private int maxScroll() {
        return Math.max(0, pages.get(tab).size() - fit());
    }

    // ------------------------------------------------------------------ render
    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        long nn = System.nanoTime();
        float dt = Math.min(0.1f, (nn - lastNanos) / 1_000_000_000f);
        lastNanos = nn;
        scale = computeScale();

        ctx.fill(0, 0, width, height, 0x88000000);

        float t = Config.get().menuAnimation ? UiUtil.easeOut((System.currentTimeMillis() - openTime) / 260f) : 1f;
        float anim = 0.88f + 0.12f * t;
        double mx = lx(mouseX), my = ly(mouseY);
        tabAnim = UiUtil.approach(tabAnim, tab, 18f, dt);
        scroll = Math.max(0, Math.min(scroll, maxScroll()));

        ctx.getMatrices().push();
        ctx.getMatrices().translate(width / 2f, height / 2f, 0f);
        ctx.getMatrices().scale(scale * anim, scale * anim, 1f);
        ctx.getMatrices().translate(-W / 2f, -H / 2f, 0f);

        // okno
        UiUtil.rrect(ctx, -1, -1, W + 2, H + 2, 9, UiUtil.argb(ACCENT, 0.55f * t));
        UiUtil.rrect(ctx, 0, 0, W, H, 8, UiUtil.argb(0x101018, 0.96f));
        UiUtil.rrect(ctx, 0, 0, SIDE, H, 8, UiUtil.argb(0x0A0A10, 1f));
        ctx.fill(SIDE - 8, 0, SIDE, H, UiUtil.argb(0x0A0A10, 1f));

        // logo
        ctx.getMatrices().push();
        ctx.getMatrices().translate(12, 12, 0);
        ctx.getMatrices().scale(1.25f, 1.25f, 1f);
        ctx.drawText(textRenderer, "Faze", 0, 0, 0xFFFFFFFF, true);
        ctx.drawText(textRenderer, "Visuals", textRenderer.getWidth("Faze"), 0, 0xFF000000 | ACCENT, true);
        ctx.getMatrices().pop();
        ctx.drawText(textRenderer, "PvP visuals  -  P to close", 12, 26, 0xFF6C6C80, false);

        // zakladki
        int ty = TOP;
        UiUtil.rrect(ctx, 6, Math.round(ty + tabAnim * 28), SIDE - 12, 24, 5, UiUtil.argb(ACCENT, 0.28f));
        UiUtil.rrect(ctx, 6, Math.round(ty + tabAnim * 28) + 4, 3, 16, 1, 0xFF000000 | ACCENT);
        for (int i = 0; i < tabs.length; i++) {
            boolean hov = mx >= 6 && mx <= SIDE - 6 && my >= ty + i * 28 && my < ty + i * 28 + 24;
            int col = i == tab ? 0xFFFFFFFF : hov ? 0xFFD0D0E0 : 0xFF8A8AA0;
            ctx.drawText(textRenderer, tabs[i], 18, ty + i * 28 + 8, col, false);
        }

        // naglowek + wiersze
        ctx.getMatrices().push();
        ctx.getMatrices().translate(SIDE + 10, 14, 0);
        ctx.getMatrices().scale(1.3f, 1.3f, 1f);
        ctx.drawText(textRenderer, tabs[tab], 0, 0, 0xFFFFFFFF, true);
        ctx.getMatrices().pop();
        if (tab == 1) {
            String hint = "Drag the preview to move the HUD";
            ctx.drawText(textRenderer, hint, W - 12 - textRenderer.getWidth(hint), 18, 0xFF6C6C80, false);
        }

        List<Row> rows = pages.get(tab);
        int y = TOP;
        for (int i = scroll; i < rows.size(); i++) {
            if (y + ROW_H > H - 10) break;
            rows.get(i).draw(ctx, textRenderer, contentX(), y, contentW(), (int) mx, (int) my, dt);
            y += ROW_H + ROW_GAP;
        }
        if (maxScroll() > 0) {
            int trackY = TOP, trackH = H - 10 - TOP;
            int barH = Math.max(14, trackH * fit() / rows.size());
            int barY = trackY + (trackH - barH) * scroll / maxScroll();
            UiUtil.rrect(ctx, W - 8, barY, 3, barH, 1, UiUtil.argb(ACCENT, 0.6f));
        }

        // picker
        if (pickerGet != null) drawPicker(ctx, (int) mx, (int) my);

        ctx.getMatrices().pop();

        // podglad HUD (prawdziwe dane wlasnego gracza), przeciagany myszka
        if (tab == 1 && pickerGet == null) {
            MinecraftClient mc = MinecraftClient.getInstance();
            if (mc.player != null) {
                Config c = Config.get();
                int px = TargetHud.pxX(c, width), py = TargetHud.pxY(c, height);
                int sz = Math.round(TargetHud.SIZE * c.hudScale);
                UiUtil.rrect(ctx, px - 2, py - 2, sz + 4, sz + 4, 7, UiUtil.argb(0xFFFFFF, hudDrag ? 0.5f : 0.2f));
                TargetHud.draw(ctx, mc.player, px, py, true);
            }
        }
    }

    private void drawPicker(DrawContext ctx, int mx, int my) {
        int px = (W - PK_W) / 2, py = (H - PK_H) / 2;
        ctx.fill(0, 0, W, H, UiUtil.argb(0x000000, 0.55f));
        UiUtil.rrect(ctx, px - 1, py - 1, PK_W + 2, PK_H + 2, 7, UiUtil.argb(ACCENT, 0.8f));
        UiUtil.rrect(ctx, px, py, PK_W, PK_H, 6, 0xFF16161F);
        ctx.drawText(textRenderer, pickerTitle, px + 10, py + 8, 0xFFFFFFFF, false);
        int cur = MathHelper.hsvToRgb(ph, ps, pv) & 0xFFFFFF;
        UiUtil.rrect(ctx, px + PK_W - 36, py + 6, 26, 12, 3, 0xFF000000 | cur);
        // kwadrat S/V
        int sx = px + 15, sy = py + 26, sw = 160, sh = 70;
        for (int i = 0; i < sw; i++) {
            int top = 0xFF000000 | (MathHelper.hsvToRgb(ph, i / (float) (sw - 1), 1f) & 0xFFFFFF);
            ctx.fillGradient(sx + i, sy, sx + i + 1, sy + sh, top, 0xFF000000);
        }
        int kx = sx + Math.round(ps * (sw - 1)), ky = sy + Math.round((1f - pv) * (sh - 1));
        ctx.fill(kx - 3, ky - 1, kx + 4, ky + 2, 0xFFFFFFFF);
        ctx.fill(kx - 1, ky - 3, kx + 2, ky + 4, 0xFFFFFFFF);
        ctx.fill(kx - 2, ky, kx + 3, ky + 1, 0xFF000000);
        ctx.fill(kx, ky - 2, kx + 1, ky + 3, 0xFF000000);
        // hue
        int hy = py + 104;
        for (int i = 0; i < sw; i++) {
            ctx.fill(sx + i, hy, sx + i + 1, hy + 10, 0xFF000000 | (MathHelper.hsvToRgb(i / (float) (sw - 1), 1f, 1f) & 0xFFFFFF));
        }
        int hk = sx + Math.round(ph * (sw - 1));
        ctx.fill(hk - 1, hy - 2, hk + 2, hy + 12, 0xFFFFFFFF);
        // presety
        for (int i = 0; i < PRESETS.length; i++) {
            int cx = sx + i * 16;
            UiUtil.rrect(ctx, cx, py + 124, 13, 13, 3, 0xFF000000 | PRESETS[i]);
        }
        ctx.drawText(textRenderer, String.format("#%06X", cur), px + 15, py + 144, 0xFF8A8AA0, false);
        ctx.drawText(textRenderer, "click outside to close", px + PK_W - 15 - textRenderer.getWidth("click outside to close"), py + 144, 0xFF55556A, false);
    }

    // ------------------------------------------------------------------ picker logika
    private void openPicker(String title, IntSupplier get, IntConsumer set) {
        pickerTitle = title;
        pickerGet = get;
        pickerSet = set;
        float[] hsv = rgbToHsv(get.getAsInt());
        ph = hsv[0];
        ps = hsv[1];
        pv = hsv[2];
    }

    private void closePicker() {
        pickerGet = null;
        pickerSet = null;
        pdrag = 0;
        Config.save();
    }

    private void applyPicker() {
        if (pickerSet != null) pickerSet.accept(MathHelper.hsvToRgb(ph, ps, pv) & 0xFFFFFF);
    }

    private static float[] rgbToHsv(int rgb) {
        float r = ((rgb >> 16) & 255) / 255f, g = ((rgb >> 8) & 255) / 255f, b = (rgb & 255) / 255f;
        float max = Math.max(r, Math.max(g, b)), min = Math.min(r, Math.min(g, b));
        float d = max - min, h;
        if (d == 0) h = 0;
        else if (max == r) h = ((g - b) / d) % 6f;
        else if (max == g) h = (b - r) / d + 2f;
        else h = (r - g) / d + 4f;
        h /= 6f;
        if (h < 0) h += 1f;
        return new float[]{h, max == 0 ? 0 : d / max, max};
    }

    private void pickerDrag(double mx, double my) {
        int px = (W - PK_W) / 2, py = (H - PK_H) / 2;
        int sx = px + 15, sy = py + 26, sw = 160, sh = 70;
        if (pdrag == 1) {
            ps = (float) MathHelper.clamp((mx - sx) / (sw - 1), 0, 1);
            pv = 1f - (float) MathHelper.clamp((my - sy) / (sh - 1), 0, 1);
            applyPicker();
        } else if (pdrag == 2) {
            ph = (float) MathHelper.clamp((mx - sx) / (sw - 1), 0, 1);
            applyPicker();
        }
    }

    // ------------------------------------------------------------------ wejscie
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) return true;
        double mx = lx(mouseX), my = ly(mouseY);

        if (pickerGet != null) {
            int px = (W - PK_W) / 2, py = (H - PK_H) / 2;
            if (mx < px || mx > px + PK_W || my < py || my > py + PK_H) {
                closePicker();
                return true;
            }
            int sx = px + 15, sy = py + 26, sw = 160, sh = 70;
            if (mx >= sx && mx <= sx + sw && my >= sy && my <= sy + sh) {
                pdrag = 1;
                pickerDrag(mx, my);
            } else if (mx >= sx && mx <= sx + sw && my >= py + 104 && my <= py + 114) {
                pdrag = 2;
                pickerDrag(mx, my);
            } else if (my >= py + 124 && my <= py + 137) {
                int idx = (int) ((mx - sx) / 16);
                if (idx >= 0 && idx < PRESETS.length && (mx - sx) % 16 <= 13) {
                    float[] hsv = rgbToHsv(PRESETS[idx]);
                    ph = hsv[0];
                    ps = hsv[1];
                    pv = hsv[2];
                    applyPicker();
                }
            }
            return true;
        }

        // przeciaganie HUD (podglad na zakladce Target HUD)
        if (tab == 1 && MinecraftClient.getInstance().player != null) {
            Config c = Config.get();
            int px = TargetHud.pxX(c, width), py = TargetHud.pxY(c, height);
            int sz = Math.round(TargetHud.SIZE * c.hudScale);
            if (mouseX >= px && mouseX <= px + sz && mouseY >= py && mouseY <= py + sz) {
                hudDrag = true;
                dragOffX = (int) mouseX - px;
                dragOffY = (int) mouseY - py;
                return true;
            }
        }

        // zakladki
        for (int i = 0; i < tabs.length; i++) {
            if (mx >= 6 && mx <= SIDE - 6 && my >= TOP + i * 28 && my < TOP + i * 28 + 24) {
                if (tab != i) {
                    tab = i;
                    scroll = 0;
                }
                return true;
            }
        }

        // wiersze
        List<Row> rows = pages.get(tab);
        int y = TOP;
        for (int i = scroll; i < rows.size(); i++) {
            if (y + ROW_H > H - 10) break;
            Row r = rows.get(i);
            if (mx >= contentX() && mx <= contentX() + contentW() && my >= y && my < y + ROW_H) {
                if (r.click(this, contentX(), y, contentW(), (int) mx, (int) my)) activeRow = r;
                return true;
            }
            y += ROW_H + ROW_GAP;
        }
        return true;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dx, double dy) {
        double mx = lx(mouseX), my = ly(mouseY);
        if (pickerGet != null && pdrag != 0) {
            pickerDrag(mx, my);
            return true;
        }
        if (hudDrag) {
            Config c = Config.get();
            int sz = Math.round(TargetHud.SIZE * c.hudScale);
            int px = MathHelper.clamp((int) mouseX - dragOffX, 0, Math.max(0, width - sz));
            int py = MathHelper.clamp((int) mouseY - dragOffY, 0, Math.max(0, height - sz));
            c.hudX = px / (float) width;
            c.hudY = py / (float) height;
            return true;
        }
        if (activeRow != null) {
            int idx = pages.get(tab).indexOf(activeRow);
            if (idx >= 0) activeRow.drag(contentX(), contentW(), (int) mx);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dx, dy);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        pdrag = 0;
        if (hudDrag || activeRow != null) Config.save();
        hudDrag = false;
        if (activeRow != null) activeRow.release();
        activeRow = null;
        return true;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (pickerGet != null) return true;
        if (verticalAmount > 0) scroll--;
        else if (verticalAmount < 0) scroll++;
        scroll = Math.max(0, Math.min(scroll, maxScroll()));
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (FazeVisualsClient.menuKey != null && FazeVisualsClient.menuKey.matchesKey(keyCode, scanCode)) {
            if (pickerGet != null) closePicker();
            close();
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_ESCAPE && pickerGet != null) {
            closePicker();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    // ------------------------------------------------------------------ wiersze
    private abstract static class Row {
        final String label;
        float hover;

        Row(String label) {
            this.label = label;
        }

        abstract void draw(DrawContext ctx, TextRenderer tr, int x, int y, int w, int mx, int my, float dt);

        boolean click(FazeMenuScreen s, int x, int y, int w, int mx, int my) {
            return false;
        }

        void drag(int x, int w, int mx) {}

        void release() {}

        void background(DrawContext ctx, int x, int y, int w, int mx, int my, float dt) {
            boolean hov = mx >= x && mx <= x + w && my >= y && my < y + ROW_H;
            hover = UiUtil.approach(hover, hov ? 1f : 0f, 16f, dt);
            UiUtil.rrect(ctx, x, y, w, ROW_H, 4, UiUtil.argb(UiUtil.mix(0x1A1A26, 0x262636, hover), 1f));
        }
    }

    private static final class ToggleRow extends Row {
        final BooleanSupplier get;
        final Consumer<Boolean> set;
        final IntSupplier colGet;
        final IntConsumer colSet;
        float knob = -1f;

        ToggleRow(String l, BooleanSupplier g, Consumer<Boolean> s, IntSupplier cg, IntConsumer cs) {
            super(l);
            get = g;
            set = s;
            colGet = cg;
            colSet = cs;
        }

        @Override
        void draw(DrawContext ctx, TextRenderer tr, int x, int y, int w, int mx, int my, float dt) {
            background(ctx, x, y, w, mx, my, dt);
            boolean on = get.getAsBoolean();
            if (knob < 0) knob = on ? 1f : 0f;
            knob = UiUtil.approach(knob, on ? 1f : 0f, 20f, dt);
            ctx.drawText(tr, label, x + 8, y + 7, 0xFFE6E6F0, false);
            int tx = x + w - 36, ty = y + 5;
            UiUtil.rrect(ctx, tx, ty, 28, 12, 6, UiUtil.argb(UiUtil.mix(0x3A3A4C, ACCENT, knob), 1f));
            int kx = tx + 2 + Math.round(knob * 16);
            UiUtil.rrect(ctx, kx, ty + 2, 8, 8, 4, 0xFFFFFFFF);
            String st = on ? "ON" : "OFF";
            ctx.drawText(tr, st, tx - 6 - tr.getWidth(st) - (colGet != null ? 24 : 0), y + 7, on ? 0xFF000000 | ACCENT : 0xFF6C6C80, false);
            if (colGet != null) {
                UiUtil.rrect(ctx, x + w - 36 - 22, y + 5, 16, 12, 3, 0xFF000000 | (colGet.getAsInt() & 0xFFFFFF));
            }
        }

        @Override
        boolean click(FazeMenuScreen s, int x, int y, int w, int mx, int my) {
            if (colGet != null && mx >= x + w - 36 - 24 && mx <= x + w - 36 - 4) {
                s.openPicker(label, colGet, colSet);
                return false;
            }
            set.accept(!get.getAsBoolean());
            Config.save();
            return false;
        }
    }

    private static final class SliderRow extends Row {
        final float min, max, step;
        final Supplier<Float> get;
        final Consumer<Float> set;
        final String fmt;

        SliderRow(String l, float min, float max, float step, Supplier<Float> g, Consumer<Float> s, String fmt) {
            super(l);
            this.min = min;
            this.max = max;
            this.step = step;
            get = g;
            set = s;
            this.fmt = fmt;
        }

        private int tx(int x, int w) {
            return x + w - 118;
        }

        @Override
        void draw(DrawContext ctx, TextRenderer tr, int x, int y, int w, int mx, int my, float dt) {
            background(ctx, x, y, w, mx, my, dt);
            ctx.drawText(tr, label, x + 8, y + 7, 0xFFE6E6F0, false);
            float v = get.get();
            float f = (v - min) / (max - min);
            int tx = tx(x, w), tw = 74;
            UiUtil.rrect(ctx, tx, y + 9, tw, 4, 2, 0xFF3A3A4C);
            UiUtil.rrect(ctx, tx, y + 9, Math.max(4, Math.round(tw * f)), 4, 2, 0xFF000000 | ACCENT);
            UiUtil.rrect(ctx, tx + Math.round(tw * f) - 3, y + 6, 7, 10, 3, 0xFFFFFFFF);
            String val = String.format(fmt, v);
            ctx.drawText(tr, val, x + w - 8 - tr.getWidth(val), y + 7, 0xFFB8B8C8, false);
        }

        @Override
        boolean click(FazeMenuScreen s, int x, int y, int w, int mx, int my) {
            drag(x, w, mx);
            return true;
        }

        @Override
        void drag(int x, int w, int mx) {
            int tx = tx(x, w), tw = 74;
            float f = MathHelper.clamp((mx - tx) / (float) tw, 0f, 1f);
            float v = min + f * (max - min);
            v = Math.round(v / step) * step;
            set.accept(MathHelper.clamp(v, min, max));
        }
    }

    private static final class ColorRow extends Row {
        final IntSupplier get;
        final IntConsumer set;

        ColorRow(String l, IntSupplier g, IntConsumer s) {
            super(l);
            get = g;
            set = s;
        }

        @Override
        void draw(DrawContext ctx, TextRenderer tr, int x, int y, int w, int mx, int my, float dt) {
            background(ctx, x, y, w, mx, my, dt);
            ctx.drawText(tr, label, x + 8, y + 7, 0xFFE6E6F0, false);
            int c = get.getAsInt() & 0xFFFFFF;
            String hex = String.format("#%06X", c);
            ctx.drawText(tr, hex, x + w - 52 - tr.getWidth(hex), y + 7, 0xFF8A8AA0, false);
            UiUtil.rrect(ctx, x + w - 44, y + 4, 36, 14, 4, 0xFFFFFFFF);
            UiUtil.rrect(ctx, x + w - 43, y + 5, 34, 12, 3, 0xFF000000 | c);
        }

        @Override
        boolean click(FazeMenuScreen s, int x, int y, int w, int mx, int my) {
            s.openPicker(label, get, set);
            return false;
        }
    }

    private static final class CycleRow extends Row {
        final String[] options;
        final IntSupplier get;
        final IntConsumer set;

        CycleRow(String l, String[] o, IntSupplier g, IntConsumer s) {
            super(l);
            options = o;
            get = g;
            set = s;
        }

        @Override
        void draw(DrawContext ctx, TextRenderer tr, int x, int y, int w, int mx, int my, float dt) {
            background(ctx, x, y, w, mx, my, dt);
            ctx.drawText(tr, label, x + 8, y + 7, 0xFFE6E6F0, false);
            String o = options[get.getAsInt() % options.length];
            int bw = 78;
            UiUtil.rrect(ctx, x + w - bw - 8, y + 3, bw, 16, 4, UiUtil.argb(ACCENT, 0.35f));
            ctx.drawText(tr, o, x + w - 8 - bw / 2 - tr.getWidth(o) / 2, y + 7, 0xFFFFFFFF, false);
        }

        @Override
        boolean click(FazeMenuScreen s, int x, int y, int w, int mx, int my) {
            set.accept((get.getAsInt() + 1) % options.length);
            Config.save();
            return false;
        }
    }

    private static final class ButtonRow extends Row {
        final Runnable action;

        ButtonRow(String l, Runnable a) {
            super(l);
            action = a;
        }

        @Override
        void draw(DrawContext ctx, TextRenderer tr, int x, int y, int w, int mx, int my, float dt) {
            background(ctx, x, y, w, mx, my, dt);
            UiUtil.rrect(ctx, x + 4, y + 3, w - 8, ROW_H - 6, 4, UiUtil.argb(ACCENT, 0.25f + 0.25f * hover));
            ctx.drawText(tr, label, x + w / 2 - tr.getWidth(label) / 2, y + 7, 0xFFFFFFFF, false);
        }

        @Override
        boolean click(FazeMenuScreen s, int x, int y, int w, int mx, int my) {
            action.run();
            Config.save();
            return false;
        }
    }
}
