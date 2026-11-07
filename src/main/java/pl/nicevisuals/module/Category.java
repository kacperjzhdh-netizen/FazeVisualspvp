package pl.nicevisuals.module;

public enum Category {
    COMBAT("Combat"), HUD("HUD"), WORLD("World");
    public final String label;
    Category(String label) { this.label = label; }
}
