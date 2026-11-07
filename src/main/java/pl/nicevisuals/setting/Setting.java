package pl.nicevisuals.setting;

public abstract class Setting {
    public final String name;
    protected Setting(String name) { this.name = name; }
    public abstract void reset();
}
