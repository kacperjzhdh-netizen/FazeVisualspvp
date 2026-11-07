package pl.nicevisuals.module;

import com.google.gson.*;
import net.fabricmc.loader.api.FabricLoader;
import pl.nicevisuals.setting.BoolSetting;
import pl.nicevisuals.setting.ModeSetting;
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
    public final TargetEsp targetEsp = new TargetEsp();
    public final ComboCounter combo = new ComboCounter();
    public final ReachDisplay reach = new ReachDisplay();
    private final Path file = FabricLoader.getInstance().getConfigDir().resolve("nicevisuals.json");

    public ModuleManager() {
        modules.add(hitMarker);
        modules.add(targetHud);
        modules.add(targetEsp);
        modules.add(combo);
        modules.add(reach);
        modules.add(new FpsHud());
        modules.add(new CpsHud());
        modules.add(new CoordsHud());
        modules.add(new ArmorHud());
        modules.add(new Keystrokes());
        modules.add(new PingHud());
        modules.add(new SpeedHud());
        modules.add(new ClockHud());
        modules.add(new PotionHud());
        modules.add(new Fullbright());
        Interface ui = new Interface();
        modules.add(ui);
        ui.setEnabled(true);
    }

    public List<Module> in(Category c) {
        List<Module> out = new ArrayList<>();
        for (Module m : modules) if (m.category == c) out.add(m);
        return out;
    }

    public JsonObject toJson() {
        JsonObject root = new JsonObject();
        for (Module m : modules) {
            JsonObject o = new JsonObject();
            o.addProperty("enabled", m.isEnabled());
            for (Setting s : m.settings) {
                if (s instanceof BoolSetting b) o.addProperty(s.name, b.value);
                else if (s instanceof NumSetting n) o.addProperty(s.name, n.value);
                else if (s instanceof ModeSetting md) o.addProperty(s.name, md.index);
            }
            root.add(m.name, o);
        }
        return root;
    }

    public void fromJson(JsonObject root) {
        for (Module m : modules) {
            if (!root.has(m.name)) continue;
            JsonObject o = root.getAsJsonObject(m.name);
            for (Setting s : m.settings) {
                if (!o.has(s.name)) continue;
                if (s instanceof BoolSetting b) b.value = o.get(s.name).getAsBoolean();
                else if (s instanceof NumSetting n) n.value = o.get(s.name).getAsDouble();
                else if (s instanceof ModeSetting md) md.index = o.get(s.name).getAsInt();
            }
            if (o.has("enabled")) m.setEnabled(o.get("enabled").getAsBoolean());
        }
    }

    public void save() {
        try {
            Files.writeString(file, new GsonBuilder().setPrettyPrinting().create().toJson(toJson()));
        } catch (Exception ignored) {}
    }

    public void load() {
        try {
            if (Files.exists(file)) fromJson(JsonParser.parseString(Files.readString(file)).getAsJsonObject());
        } catch (Exception ignored) {}
    }
}
