package pl.life.visuals.module;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.loader.api.FabricLoader;
import pl.life.visuals.LifeVisualsClient;

import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.Deque;

/** Zapis/odczyt konfiguracji (config/lifevisuals.json) oraz historia zmian dla /life undo. */
public final class ConfigStore {
    private ConfigStore() {}

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Deque<String> UNDO = new ArrayDeque<>();
    private static boolean dirty;
    private static long lastSave;

    public static String licenseKey = "";
    public static String discordClientId = "";

    /** Ostatnie powiadomienie (Dynamic Island). */
    public static String notifyText = "";
    public static long notifyTime;

    public static void notify(String text) {
        notifyText = text;
        notifyTime = System.currentTimeMillis();
    }

    private static Path path() {
        return FabricLoader.getInstance().getConfigDir().resolve("lifevisuals.json");
    }

    private static JsonObject modulesJson() {
        JsonObject mods = new JsonObject();
        for (Module m : Modules.ALL) {
            JsonObject o = new JsonObject();
            o.addProperty("enabled", m.enabled);
            JsonObject s = new JsonObject();
            for (Setting st : m.settings) s.add(st.id, st.toJson());
            o.add("settings", s);
            mods.add(m.id, o);
        }
        return mods;
    }

    private static void applyModules(JsonObject mods) {
        for (Module m : Modules.ALL) {
            if (!mods.has(m.id) || !mods.get(m.id).isJsonObject()) continue;
            JsonObject o = mods.getAsJsonObject(m.id);
            if (o.has("enabled") && !m.locked) {
                try {
                    m.enabled = o.get("enabled").getAsBoolean();
                } catch (Exception ignored) {
                }
            }
            if (o.has("settings") && o.get("settings").isJsonObject()) {
                JsonObject s = o.getAsJsonObject("settings");
                for (Setting st : m.settings) if (s.has(st.id)) st.fromJson(s.get(st.id));
            }
        }
    }

    // ------------------------------------------------------------ undo
    /** Zapamietuje stan przed zmiana. */
    public static void checkpoint() {
        String snap = modulesJson().toString();
        if (!UNDO.isEmpty() && UNDO.peek().equals(snap)) return;
        UNDO.push(snap);
        while (UNDO.size() > 60) UNDO.removeLast();
    }

    public static boolean undo() {
        if (UNDO.isEmpty()) return false;
        try {
            applyModules(JsonParser.parseString(UNDO.pop()).getAsJsonObject());
            syncAnimations();
            markDirty();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public static int undoDepth() {
        return UNDO.size();
    }

    private static void syncAnimations() {
        for (Module m : Modules.ALL) {
            m.toggleAnim = m.enabled ? 1f : 0f;
            for (Setting s : m.settings) if (s.type == Setting.Type.BOOL) s.anim = s.b ? 1f : 0f;
        }
    }

    public static void toggle(Module m) {
        if (m.locked) return;
        checkpoint();
        m.toggle();
        notify(m.name + (m.enabled ? " enabled" : " disabled"));
        markDirty();
    }

    public static void resetAll() {
        checkpoint();
        for (Module m : Modules.ALL) m.resetAll();
        Modules.CLICK_GUI.enabled = true;
        syncAnimations();
        markDirty();
    }

    // ------------------------------------------------------------ plik
    public static void markDirty() {
        dirty = true;
    }

    /** Wywolywane co tick: zapis najwyzej raz na 2 sekundy. */
    public static void tick() {
        if (dirty && System.currentTimeMillis() - lastSave > 2000) save();
    }

    public static void load() {
        Path p = path();
        if (!Files.exists(p)) return;
        try (Reader r = Files.newBufferedReader(p)) {
            JsonObject root = JsonParser.parseReader(r).getAsJsonObject();
            if (root.has("modules")) applyModules(root.getAsJsonObject("modules"));
            if (root.has("licenseKey")) licenseKey = root.get("licenseKey").getAsString();
            if (root.has("discordClientId")) discordClientId = root.get("discordClientId").getAsString();
            syncAnimations();
        } catch (Exception e) {
            LifeVisualsClient.LOGGER.warn("Could not read config, using defaults", e);
        }
    }

    public static void save() {
        dirty = false;
        lastSave = System.currentTimeMillis();
        try {
            JsonObject root = new JsonObject();
            root.addProperty("licenseKey", licenseKey);
            root.addProperty("discordClientId", discordClientId);
            root.add("modules", modulesJson());
            Files.createDirectories(path().getParent());
            try (Writer w = Files.newBufferedWriter(path())) {
                GSON.toJson(root, w);
            }
        } catch (Exception e) {
            LifeVisualsClient.LOGGER.warn("Could not save config", e);
        }
    }
}
