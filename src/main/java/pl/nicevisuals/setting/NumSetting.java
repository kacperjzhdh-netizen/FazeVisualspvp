package pl.nicevisuals.setting;

public class NumSetting extends Setting {
    public double value;
    public final double min, max;
    private final double def;
    public NumSetting(String name, double value, double min, double max) {
        super(name); this.value = value; this.min = min; this.max = max; this.def = value;
    }
    public int asInt() { return (int) Math.round(value); }
    @Override public void reset() { value = def; }
}
