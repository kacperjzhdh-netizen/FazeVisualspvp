package pl.faze.visuals;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

/** Ustawienia moda, zapisywane w config/fazevisuals.json. Kolory to RGB (0xRRGGBB). */
public class Config {
    // --- Visuals ---
    public boolean targetEsp = true;
    public int targetEspColor = 0xFF5577;
    public boolean boxEsp = true;
    public int boxColor = 0x55FFFF;
    public float boxThickness = 2.0f;
    public boolean glow = true;
    public int glowColor = 0xFF3355;
    public boolean highlight = true;
    public int highlightColor = 0xFF3355;
    public float highlightAlpha = 0.25f;
    public boolean damageIndicator = true;
    public int damageColor = 0xFFAA00;

    // --- Particles ---
    public boolean pearlTrail = true;
    public int pearlColor = 0xAA55FF;
    public float pearlDensity = 3f;
    public boolean hitParticles = true;
    public int hitColor = 0xFF5555;
    public float hitAmount = 12f;
    public boolean critParticles = true;
    public int critColor = 0x55FFFF;
    public float critAmount = 16f;
    public boolean totemVisuals = true;
    public int totemColor = 0xFFD700;

    // --- Target HUD ---
    public boolean hudEnabled = true;
    public float hudX = 0.68f;   // ulamek szerokosci ekranu (lewy gorny rog)
    public float hudY = 0.40f;   // ulamek wysokosci ekranu
    public float hudScale = 1.0f;
    public int hudBarColor = 0x55FF55;
    public int hudTrailColor = 0xFF5555;
    public int hudBgColor = 0x12121C;
    public int hudAccentColor = 0x8A6BFF;
    public float hudBgAlpha = 0.80f;
    public boolean hudShowHead = true;
    public boolean hudShowArmor = true;
    public boolean hudShowHands = true;

    // --- Settings ---
    /** 0 = patrze na gracza, 1 = atakuje, 2 = oba */
    public int targetMode = 2;
    public float targetTimeout = 5f;
    public boolean menuAnimation = true;
    public float menuScale = 1.0f;

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static Config instance = new Config();

    public static Config get() {
        return instance;
    }

    private static Path path() {
        return FabricLoader.getInstance().getConfigDir().resolve("fazevisuals.json");
    }

    public static void load() {
        Path p = path();
        if (Files.exists(p)) {
            try (Reader r = Files.newBufferedReader(p)) {
                Config c = GSON.fromJson(r, Config.class);
                if (c != null) instance = c;
            } catch (Exception e) {
                FazeVisualsClient.LOGGER.warn("Nie udalo sie wczytac konfiguracji, uzywam domyslnej", e);
            }
        }
        instance.sanitize();
    }

    public static void save() {
        instance.sanitize();
        try {
            Files.createDirectories(path().getParent());
            try (Writer w = Files.newBufferedWriter(path())) {
                GSON.toJson(instance, w);
            }
        } catch (IOException e) {
            FazeVisualsClient.LOGGER.warn("Nie udalo sie zapisac konfiguracji", e);
        }
    }

    public static void reset() {
        instance = new Config();
        save();
    }

    private void sanitize() {
        boxThickness = clamp(boxThickness, 1f, 8f);
        highlightAlpha = clamp(highlightAlpha, 0f, 1f);
        pearlDensity = clamp(pearlDensity, 1f, 10f);
        hitAmount = clamp(hitAmount, 1f, 40f);
        critAmount = clamp(critAmount, 1f, 60f);
        hudScale = clamp(hudScale, 0.5f, 2.0f);
        hudBgAlpha = clamp(hudBgAlpha, 0.1f, 1f);
        hudX = clamp(hudX, 0f, 1f);
        hudY = clamp(hudY, 0f, 1f);
        targetTimeout = clamp(targetTimeout, 1f, 15f);
        targetMode = Math.max(0, Math.min(2, targetMode));
        menuScale = clamp(menuScale, 0.6f, 1.5f);
    }

    private static float clamp(float v, float lo, float hi) {
        if (Float.isNaN(v)) return lo;
        return Math.max(lo, Math.min(hi, v));
    }
}
