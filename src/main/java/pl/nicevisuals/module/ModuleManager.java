package pl.nicevisuals.module;

import com.google.gson.*;
import net.fabricmc.loader.api.FabricLoader;
import pl.nicevisuals.setting.BoolSetting;
import pl.nicevisuals.setting.NumSetting;
import pl.nicevisuals.setting.Setting;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class ModuleManager {
    public final List<Module> modules = new ArrayList<>();
    public final HitMarker hitMarker = new HitMarker();
    public final TargetHud targetHud = new TargetHud();
    private final Path file = FabricLoader.getInstance().getConfigDir().resolve("nicevisuals.json");

    public ModuleManager() {
        modules.add(hitMarker);
        modules.add(targetHud);
        modules.add(new FpsHud());
        modules.add(new CpsHud());
        modules.add(new CoordsHud());
        modules.add(new Fullbright());
    }

    public List<Module> in(Category c) {
        List<Module> out = new ArrayList<>();
        for (Module m : modules) if (m.category == c) out.add(m);
        return out;
    }

    public void save() {
        try {
            JsonObject root = new JsonObject();
            for (Module m : modules) {
                JsonObject o = new JsonObject();
                o.addProperty("enabled", m.isEnabled());
                for (Setting s : m.settings) {
                    if (s instanceof BoolSetting b) o.addProperty(s.name, b.value);
                    else if (s instanceof NumSetting n) o.addProperty(s.name, n.value);
                }
                root.add(m.name, o);
            }
            Files.writeString(file, new GsonBuilder().setPrettyPrinting().create().toJson(root));
        } catch (Exception ignored) {}
    }

    public void load() {
        try {
            if (!Files.exists(file)) return;
            JsonObject root = JsonParser.parseString(Files.readString(file)).getAsJsonObject();
            for (Module m : modules) {
                if (!root.has(m.name)) continue;
                JsonObject o = root.getAsJsonObject(m.name);
                for (Setting s : m.settings) {
                    if (!o.has(s.name)) continue;
                    if (s instanceof BoolSetting b) b.value = o.get(s.name).getAsBoolean();
                    else if (s instanceof NumSetting n) n.value = o.get(s.name).getAsDouble();
                }
                if (o.has("enabled")) m.setEnabled(o.get("enabled").getAsBoolean());
            }
        } catch (Exception ignored) {}
    }
}
