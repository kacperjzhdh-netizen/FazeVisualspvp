package pl.nicevisuals.setting;

public class NumSetting extends Setting {
    public double value;
    public final double min, max;
    public NumSetting(String name, double value, double min, double max) {
        super(name); this.value = value; this.min = min; this.max = max;
    }
    public int asInt() { return (int) Math.round(value); }
}
