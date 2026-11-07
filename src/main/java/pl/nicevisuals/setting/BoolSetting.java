package pl.nicevisuals.setting;

public class BoolSetting extends Setting {
    public boolean value;
    private final boolean def;
    public BoolSetting(String name, boolean value) { super(name); this.value = value; this.def = value; }
    @Override public void reset() { value = def; }
}
