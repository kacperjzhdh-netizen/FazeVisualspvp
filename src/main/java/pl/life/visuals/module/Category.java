package pl.life.visuals.module;

public enum Category {
    COMBAT("Combat"),
    HUD("HUD"),
    ANIMATIONS("Animations"),
    SOUND("Sound"),
    WORLD("World"),
    CLIENT("Client");

    public final String label;

    Category(String label) {
        this.label = label;
    }
}
