package pl.life.visuals.module;

import java.util.ArrayList;
import java.util.List;

public final class Module {
    public final String id, name, description;
    public final Category category;
    public final boolean defaultEnabled;
    /** true = nie da sie wylaczyc (np. Click GUI). */
    public boolean locked;
    public boolean enabled;
    public final List<Setting> settings = new ArrayList<>();
    public Setting hudX, hudY;

    // stan animacji interfejsu
    public float toggleAnim, expandAnim, hover;
    public boolean expanded;

    public Module(String id, String name, Category category, String description, boolean enabled) {
        this.id = id;
        this.name = name;
        this.category = category;
        this.description = description;
        this.defaultEnabled = enabled;
        this.enabled = enabled;
        this.toggleAnim = enabled ? 1f : 0f;
    }

    private Setting add(Setting s) {
        settings.add(s);
        return s;
    }

    public Setting bool(String id, String label, boolean def) { return add(Setting.bool(id, label, def)); }
    public Setting num(String id, String label, float min, float max, float step, float def) { return add(Setting.num(id, label, min, max, step, def)); }
    public Setting color(String id, String label, int rgb) { return add(Setting.color(id, label, rgb)); }
    public Setting mode(String id, String label, String def, String... options) { return add(Setting.mode(id, label, def, options)); }

    /** Dodaje ukryte ustawienia pozycji (ulamek ekranu), element HUD mozna wtedy przeciagac. */
    public Module hud(float x, float y) {
        hudX = add(Setting.hidden("posX", x));
        hudY = add(Setting.hidden("posY", y));
        return this;
    }

    public boolean isHud() { return hudX != null; }

    public boolean hasVisibleSettings() {
        for (Setting s : settings) if (s.type != Setting.Type.HIDDEN) return true;
        return false;
    }

    public void toggle() {
        if (!locked) enabled = !enabled;
    }

    public void resetAll() {
        enabled = defaultEnabled;
        for (Setting s : settings) s.reset();
    }
}
