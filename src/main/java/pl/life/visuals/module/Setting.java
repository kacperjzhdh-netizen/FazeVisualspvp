package pl.life.visuals.module;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

/** Pojedyncze ustawienie modulu (przelacznik, suwak, kolor, tryb). */
public final class Setting {
    public enum Type { BOOL, NUM, COLOR, MODE, HIDDEN }

    public final Type type;
    public final String id, label;
    public final float min, max, step;
    public final String[] options;
    private final float defNum;
    private final int defInt;
    private final boolean defBool;

    public boolean b;
    public float n;
    public int color;
    public int mode;

    /** Stan animacji interfejsu (nie zapisywany). */
    public float anim;

    private Setting(Type type, String id, String label, float min, float max, float step, String[] options,
                    boolean b, float n, int color, int mode) {
        this.type = type;
        this.id = id;
        this.label = label;
        this.min = min;
        this.max = max;
        this.step = step;
        this.options = options;
        this.b = b;
        this.n = n;
        this.color = color;
        this.mode = mode;
        this.defBool = b;
        this.defNum = n;
        this.defInt = type == Type.COLOR ? color : mode;
        this.anim = b ? 1f : 0f;
    }

    public static Setting bool(String id, String label, boolean def) {
        return new Setting(Type.BOOL, id, label, 0, 1, 1, null, def, 0, 0, 0);
    }

    public static Setting num(String id, String label, float min, float max, float step, float def) {
        return new Setting(Type.NUM, id, label, min, max, step, null, false, def, 0, 0);
    }

    public static Setting color(String id, String label, int rgb) {
        return new Setting(Type.COLOR, id, label, 0, 0, 0, null, false, 0, rgb & 0xFFFFFF, 0);
    }

    public static Setting mode(String id, String label, String def, String... options) {
        int idx = 0;
        for (int i = 0; i < options.length; i++) if (options[i].equalsIgnoreCase(def)) idx = i;
        return new Setting(Type.MODE, id, label, 0, options.length - 1, 1, options, false, 0, 0, idx);
    }

    public static Setting hidden(String id, float def) {
        return new Setting(Type.HIDDEN, id, id, 0, 1, 0.0001f, null, false, def, 0, 0);
    }

    public boolean bool() { return b; }
    public float num() { return n; }
    public int intv() { return Math.round(n); }
    public int rgb() { return color & 0xFFFFFF; }
    public int mode() { return mode; }
    public String modeName() { return options[mode]; }
    public boolean is(String name) { return type == Type.MODE && options[mode].equalsIgnoreCase(name); }

    public void setNum(float v) {
        if (Float.isNaN(v)) v = defNum;
        v = Math.max(min, Math.min(max, v));
        if (step > 0.0005f) v = Math.round(v / step) * step;
        n = Math.max(min, Math.min(max, v));
    }

    public String display() {
        switch (type) {
            case BOOL: return b ? "ON" : "OFF";
            case NUM: {
                if (step >= 1f) return Integer.toString(Math.round(n));
                if (step >= 0.1f) return String.format("%.1f", n);
                return String.format("%.2f", n);
            }
            case COLOR: return String.format("#%06X", color & 0xFFFFFF);
            case MODE: return options[mode];
            default: return "";
        }
    }

    public void reset() {
        b = defBool;
        n = defNum;
        if (type == Type.COLOR) color = defInt;
        if (type == Type.MODE) mode = defInt;
    }

    public JsonElement toJson() {
        switch (type) {
            case BOOL: return new JsonPrimitive(b);
            case COLOR: return new JsonPrimitive(color & 0xFFFFFF);
            case MODE: return new JsonPrimitive(mode);
            default: return new JsonPrimitive(n);
        }
    }

    public void fromJson(JsonElement e) {
        try {
            if (e == null || !e.isJsonPrimitive()) return;
            switch (type) {
                case BOOL: b = e.getAsBoolean(); break;
                case COLOR: color = e.getAsInt() & 0xFFFFFF; break;
                case MODE: mode = Math.max(0, Math.min(options.length - 1, e.getAsInt())); break;
                default: setNum(e.getAsFloat());
            }
        } catch (Exception ignored) {
            // uszkodzona wartosc w pliku - zostaje domyslna
        }
    }
}
