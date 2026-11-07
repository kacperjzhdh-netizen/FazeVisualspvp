package pl.nicevisuals.setting;

public class ModeSetting extends Setting {
    public final String[] modes;
    public int index;
    private final int def;
    public ModeSetting(String name, int index, String... modes) {
        super(name); this.modes = modes; this.index = index; this.def = index;
    }
    public String get() { return modes[Math.max(0, Math.min(index, modes.length - 1))]; }
    @Override public void reset() { index = def; }
}
