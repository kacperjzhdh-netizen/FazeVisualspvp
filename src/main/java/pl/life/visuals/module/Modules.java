package pl.life.visuals.module;

import java.util.ArrayList;
import java.util.List;

import static pl.life.visuals.module.Category.*;

/** Rejestr wszystkich modulow i ich ustawien. */
public final class Modules {
    private Modules() {}

    public static final List<Module> ALL = new ArrayList<>();

    private static Module m(String id, String name, Category c, String desc, boolean def) {
        Module mod = new Module(id, name, c, desc, def);
        ALL.add(mod);
        return mod;
    }

    // ============================================================ COMBAT
    public static final Module HIT_MARKER = m("hit_marker", "Hit Marker", COMBAT, "Marker at the crosshair on confirmed hits", true);
    public static final Setting HM_COLOR = HIT_MARKER.color("color", "Color", 0x66C7FF);
    public static final Setting HM_CRIT = HIT_MARKER.color("crit", "Crit color", 0xFFE066);
    public static final Setting HM_SIZE = HIT_MARKER.num("size", "Size", 6, 30, 1, 13);
    public static final Setting HM_GAP = HIT_MARKER.num("gap", "Gap", 0, 20, 1, 6);
    public static final Setting HM_THICK = HIT_MARKER.num("thick", "Thickness", 1, 6, 0.5f, 2);
    public static final Setting HM_TIME = HIT_MARKER.num("time", "Duration (ms)", 150, 900, 10, 380);

    public static final Module DAMAGE_NUMBERS = m("damage_numbers", "Damage Numbers", COMBAT, "Floating damage dealt above the target", false);
    public static final Setting DN_COLOR = DAMAGE_NUMBERS.color("color", "Color", 0xFFC857);
    public static final Setting DN_CRIT = DAMAGE_NUMBERS.color("crit", "Crit color", 0xFFF066);
    public static final Setting DN_SCALE = DAMAGE_NUMBERS.num("scale", "Scale", 0.5f, 2.5f, 0.1f, 1f);
    public static final Setting DN_TIME = DAMAGE_NUMBERS.num("time", "Duration (s)", 0.4f, 2.5f, 0.1f, 1f);

    public static final Module HIT_PARTICLES = m("hit_particles", "Hit Particles", COMBAT, "Particles on hit, extra burst on critical hits", false);
    public static final Setting HP_COLOR = HIT_PARTICLES.color("color", "Color", 0xFF5C7A);
    public static final Setting HP_AMOUNT = HIT_PARTICLES.num("amount", "Amount", 1, 40, 1, 12);
    public static final Setting HP_SIZE = HIT_PARTICLES.num("size", "Size", 0.5f, 2.5f, 0.1f, 1.2f);
    public static final Setting HP_CRIT = HIT_PARTICLES.bool("crits", "Critical burst", true);
    public static final Setting HP_CRIT_COLOR = HIT_PARTICLES.color("critcolor", "Crit color", 0x66E6FF);

    public static final Module HIT_BUBBLES = m("hit_bubbles", "Hit Bubbles", COMBAT, "Soft bubbles floating out of the target on hit", false);
    public static final Setting HB_COLOR = HIT_BUBBLES.color("color", "Color", 0x8FD8FF);
    public static final Setting HB_AMOUNT = HIT_BUBBLES.num("amount", "Amount", 1, 20, 1, 6);
    public static final Setting HB_SIZE = HIT_BUBBLES.num("size", "Size", 0.04f, 0.3f, 0.01f, 0.12f);
    public static final Setting HB_LIFE = HIT_BUBBLES.num("life", "Lifetime (s)", 0.5f, 3f, 0.1f, 1.3f);

    public static final Module TARGET_ESP = m("target_esp", "Target ESP", COMBAT, "Marks your current target (ring, diamond, glow, highlight)", true);
    public static final Setting TE_STYLE = TARGET_ESP.mode("style", "Style", "Ring", "Ring", "Diamond", "Both");
    public static final Setting TE_COLOR = TARGET_ESP.color("color", "Color", 0x4DB8FF);
    public static final Setting TE_SPEED = TARGET_ESP.num("speed", "Speed", 0.3f, 3f, 0.1f, 1f);
    public static final Setting TE_GLOW = TARGET_ESP.bool("glow", "Glow outline", true);
    public static final Setting TE_GLOW_COLOR = TARGET_ESP.color("glowcolor", "Glow color", 0x4DB8FF);
    public static final Setting TE_FILL = TARGET_ESP.bool("fill", "Highlight fill", true);
    public static final Setting TE_FILL_COLOR = TARGET_ESP.color("fillcolor", "Fill color", 0x4DB8FF);
    public static final Setting TE_FILL_ALPHA = TARGET_ESP.num("fillalpha", "Fill opacity", 0.05f, 0.8f, 0.05f, 0.2f);

    public static final Module KILL_EFFECT = m("kill_effect", "Kill Effect", COMBAT, "Visual effect when you kill a player", true);
    public static final Setting KE_STYLE = KILL_EFFECT.mode("style", "Style", "Lightning", "Burst", "Lightning", "Souls", "Rings");
    public static final Setting KE_COLOR = KILL_EFFECT.color("color", "Color", 0x8FD8FF);
    public static final Setting KE_SIZE = KILL_EFFECT.num("size", "Size", 0.5f, 3f, 0.1f, 1f);
    public static final Setting KE_SOUND = KILL_EFFECT.bool("sound", "Thunder sound", false);

    public static final Module DEATH_EFFECT = m("death_effect", "Death Effect", COMBAT, "Soft effect whenever a nearby player dies", false);
    public static final Setting DE_COLOR = DEATH_EFFECT.color("color", "Color", 0xBFD8FF);
    public static final Setting DE_SIZE = DEATH_EFFECT.num("size", "Size", 0.5f, 3f, 0.1f, 1f);
    public static final Setting DE_OTHERS = DEATH_EFFECT.bool("others", "Other players", true);

    public static final Module TOTEM_POP = m("totem_pop", "Totem Pop", COMBAT, "Ring and burst effect when a totem pops", true);
    public static final Setting TP_COLOR = TOTEM_POP.color("color", "Color", 0xFFE066);
    public static final Setting TP_AMOUNT = TOTEM_POP.num("amount", "Amount", 10, 100, 5, 40);
    public static final Setting TP_TEXT = TOTEM_POP.bool("text", "Show text", true);
    public static final Setting TP_SELF = TOTEM_POP.bool("self", "Include yourself", true);

    public static final Module HIT_COLOR = m("hit_color", "Hit Color", COMBAT, "Changes the red flash when an entity is hurt", false);
    public static final Setting HC_COLOR = HIT_COLOR.color("color", "Color", 0x3DA5FF);
    public static final Setting HC_ALPHA = HIT_COLOR.num("alpha", "Opacity", 0.2f, 1f, 0.05f, 0.7f);

    public static final Module HITBOX = m("hitbox", "Hitbox Customizer", COMBAT, "Colored hitbox outline (visual only, no gameplay change)", false);
    public static final Setting HX_PLAYERS = HITBOX.bool("players", "Players", true);
    public static final Setting HX_MOBS = HITBOX.bool("mobs", "Mobs", false);
    public static final Setting HX_COLOR = HITBOX.color("color", "Color", 0x4DB8FF);
    public static final Setting HX_TARGET = HITBOX.color("target", "Target color", 0xFFFFFF);
    public static final Setting HX_THICK = HITBOX.num("thick", "Thickness", 1, 6, 0.5f, 2);
    public static final Setting HX_EXPAND = HITBOX.num("expand", "Expand (visual)", 0, 0.5f, 0.05f, 0);
    public static final Setting HX_FILL = HITBOX.bool("fill", "Filled", false);
    public static final Setting HX_FILL_ALPHA = HITBOX.num("fillalpha", "Fill opacity", 0.05f, 0.5f, 0.05f, 0.15f);

    // ============================================================ HUD
    public static final Module HUD_STYLE = m("hud_style", "HUD Style", HUD, "Look of all HUD elements (glass / solid / outline)", true);
    public static final Setting HS_MODE = HUD_STYLE.mode("mode", "Style", "Glass", "Glass", "Solid", "Outline");
    public static final Setting HS_ACCENT = HUD_STYLE.color("accent", "Accent color", 0x4C9AFF);
    public static final Setting HS_BG = HUD_STYLE.color("bg", "Background", 0x0B1626);
    public static final Setting HS_ALPHA = HUD_STYLE.num("alpha", "Opacity", 0.2f, 1f, 0.05f, 0.78f);
    public static final Setting HS_RADIUS = HUD_STYLE.num("radius", "Corner radius", 0, 24, 1, 12);
    public static final Setting HS_SCALE = HUD_STYLE.num("scale", "Scale", 0.6f, 2f, 0.05f, 1f);

    public static final Module TARGET_HUD = m("target_hud", "Target HUD", HUD, "Square panel: head, HP, armor and held items of your target", true).hud(0.62f, 0.34f);
    public static final Setting TH_BAR = TARGET_HUD.color("bar", "HP bar color", 0x35C7FF);
    public static final Setting TH_TRAIL = TARGET_HUD.color("trail", "HP trail color", 0xFF5C7A);
    public static final Setting TH_SCALE = TARGET_HUD.num("scale", "Size", 0.5f, 2f, 0.05f, 1f);
    public static final Setting TH_HEAD = TARGET_HUD.bool("head", "Show head", true);
    public static final Setting TH_ARMOR = TARGET_HUD.bool("armor", "Show armor", true);
    public static final Setting TH_HANDS = TARGET_HUD.bool("hands", "Show hands", true);
    public static final Setting TH_DURA = TARGET_HUD.bool("dura", "Durability bars", true);

    public static final Module WATERMARK = m("watermark", "Watermark", HUD, "Client name with FPS and ping", true).hud(0.012f, 0.02f);
    public static final Setting WM_FPS = WATERMARK.bool("fps", "Show FPS", true);
    public static final Setting WM_PING = WATERMARK.bool("ping", "Show ping", true);
    public static final Setting WM_NAME = WATERMARK.bool("name", "Show player name", false);

    public static final Module ISLAND = m("dynamic_island", "Dynamic Island", HUD, "Top pill: notifications, combo and target info", true);
    public static final Setting DI_NOTIFY = ISLAND.bool("notify", "Module notifications", true);
    public static final Setting DI_TARGET = ISLAND.bool("target", "Show target", true);
    public static final Setting DI_Y = ISLAND.num("y", "Top offset", 0, 60, 1, 10);

    public static final Module POTIONS = m("potions", "Potions", HUD, "Active potion effects with timers", true).hud(0.012f, 0.30f);
    public static final Setting PO_ICONS = POTIONS.bool("icons", "Icons", true);
    public static final Setting PO_TIME = POTIONS.bool("time", "Timers", true);

    public static final Module COOLDOWNS = m("cooldowns", "Cooldowns", HUD, "Item cooldowns (pearl, apples, chorus, wind charge, shield)", true).hud(0.012f, 0.62f);
    public static final Setting CD_PEARL = COOLDOWNS.bool("pearl", "Ender pearl", true);
    public static final Setting CD_APPLE = COOLDOWNS.bool("apple", "Golden apples", true);
    public static final Setting CD_CHORUS = COOLDOWNS.bool("chorus", "Chorus fruit", true);
    public static final Setting CD_WIND = COOLDOWNS.bool("wind", "Wind charge", true);
    public static final Setting CD_SHIELD = COOLDOWNS.bool("shield", "Shield", true);

    public static final Module COMBO = m("combo", "Combo Counter", HUD, "Consecutive hits without taking damage", true).hud(0.44f, 0.60f);
    public static final Setting CO_COLOR = COMBO.color("color", "Color", 0x66C7FF);
    public static final Setting CO_RESET = COMBO.num("reset", "Reset after (s)", 1, 8, 0.5f, 3f);
    public static final Setting CO_SCALE = COMBO.num("scale", "Size", 0.6f, 2f, 0.05f, 1f);

    public static final Module COOLDOWN_RING = m("cooldown_ring", "Cooldown Ring", HUD, "Attack cooldown ring around the crosshair", false);
    public static final Setting CR_COLOR = COOLDOWN_RING.color("color", "Color", 0x4DB8FF);
    public static final Setting CR_RADIUS = COOLDOWN_RING.num("radius", "Radius", 8, 40, 1, 16);
    public static final Setting CR_THICK = COOLDOWN_RING.num("thick", "Thickness", 1, 6, 0.5f, 2.5f);
    public static final Setting CR_ONLY = COOLDOWN_RING.bool("only", "Only while charging", true);

    public static final Module CROSSHAIR = m("crosshair", "Crosshair", HUD, "Custom crosshair (replaces the vanilla one)", false);
    public static final Setting CH_STYLE = CROSSHAIR.mode("style", "Style", "Cross", "Cross", "Dot", "Circle", "Plus + Dot");
    public static final Setting CH_COLOR = CROSSHAIR.color("color", "Color", 0xFFFFFF);
    public static final Setting CH_SIZE = CROSSHAIR.num("size", "Size", 2, 24, 1, 8);
    public static final Setting CH_GAP = CROSSHAIR.num("gap", "Gap", 0, 12, 1, 3);
    public static final Setting CH_THICK = CROSSHAIR.num("thick", "Thickness", 1, 6, 0.5f, 2);
    public static final Setting CH_OUTLINE = CROSSHAIR.bool("outline", "Outline", true);

    // ============================================================ ANIMATIONS
    public static final Module SWING = m("swing", "Swing Animations", ANIMATIONS, "Custom first-person swing styles and speed", false);
    public static final Setting SW_MODE = SWING.mode("mode", "Style", "Smooth", "Smooth", "Slash", "Spin", "Poke", "Block");
    public static final Setting SW_SPEED = SWING.num("speed", "Swing speed", 0.4f, 2.5f, 0.1f, 1f);
    public static final Setting SW_POWER = SWING.num("power", "Intensity", 0.2f, 2f, 0.1f, 1f);

    public static final Module KNIFE = m("knife", "Knife", ANIMATIONS, "Holds swords like a knife: lowered, tilted and smaller", false);
    public static final Setting KN_SCALE = KNIFE.num("scale", "Item scale", 0.5f, 1.2f, 0.05f, 0.8f);
    public static final Setting KN_TILT = KNIFE.num("tilt", "Tilt", 0, 90, 5, 35);

    public static final Module HURT_CAM = m("hurt_cam", "Hurt Cam", ANIMATIONS, "Strength of the camera tilt when you take damage", false);
    public static final Setting HU_POWER = HURT_CAM.num("power", "Strength", 0, 1, 0.05f, 0.3f);

    // ============================================================ SOUND
    public static final Module HIT_SOUNDS = m("hit_sounds", "Hit Sounds", SOUND, "Custom sound on every confirmed hit", false);
    public static final Setting HSD_SOUND = HIT_SOUNDS.mode("sound", "Sound", "Bell", "Bell", "Pling", "Orb", "Chime", "Click", "Crit", "Bit");
    public static final Setting HSD_CRIT = HIT_SOUNDS.bool("crit", "Different crit sound", true);
    public static final Setting HSD_CRIT_SOUND = HIT_SOUNDS.mode("critsound", "Crit sound", "Crit", "Bell", "Pling", "Orb", "Chime", "Click", "Crit", "Bit");
    public static final Setting HSD_VOL = HIT_SOUNDS.num("volume", "Volume", 0.1f, 1f, 0.05f, 0.6f);
    public static final Setting HSD_PITCH = HIT_SOUNDS.num("pitch", "Pitch", 0.5f, 2f, 0.05f, 1.2f);

    // ============================================================ WORLD
    public static final Module JUMP_CIRCLES = m("jump_circles", "Jump Circles", WORLD, "Expanding ring on the ground when you jump", false);
    public static final Setting JC_STYLE = JUMP_CIRCLES.mode("style", "Style", "Ring", "Ring", "Filled", "Double");
    public static final Setting JC_COLOR = JUMP_CIRCLES.color("color", "Color", 0x4DB8FF);
    public static final Setting JC_SIZE = JUMP_CIRCLES.num("size", "Size", 0.8f, 3.5f, 0.1f, 1.7f);
    public static final Setting JC_TIME = JUMP_CIRCLES.num("time", "Duration (s)", 0.3f, 2f, 0.1f, 0.9f);
    public static final Setting JC_OTHERS = JUMP_CIRCLES.bool("others", "Other players", false);

    public static final Module JUMP_PARTICLES = m("jump_particles", "Jump Particles", WORLD, "Particle burst under your feet when you jump", false);
    public static final Setting JP_COLOR = JUMP_PARTICLES.color("color", "Color", 0x7FD6FF);
    public static final Setting JP_AMOUNT = JUMP_PARTICLES.num("amount", "Amount", 4, 40, 1, 14);
    public static final Setting JP_SIZE = JUMP_PARTICLES.num("size", "Size", 0.5f, 2.5f, 0.1f, 1f);
    public static final Setting JP_OTHERS = JUMP_PARTICLES.bool("others", "Other players", false);

    public static final Module AMBIENT = m("ambient", "Ambient Particles", WORLD, "Soft floating particles around you", false);
    public static final Setting AM_STYLE = AMBIENT.mode("style", "Style", "Dust", "Dust", "Fireflies", "Snow");
    public static final Setting AM_COLOR = AMBIENT.color("color", "Color", 0x9FDCFF);
    public static final Setting AM_AMOUNT = AMBIENT.num("amount", "Amount", 10, 250, 10, 70);
    public static final Setting AM_SIZE = AMBIENT.num("size", "Size", 0.01f, 0.08f, 0.005f, 0.03f);
    public static final Setting AM_RANGE = AMBIENT.num("range", "Range", 4, 24, 1, 12);

    public static final Module TRAILS = m("trails", "Trails", WORLD, "Ribbon trails behind players and thrown pearls", false);
    public static final Setting TR_COLOR = TRAILS.color("color", "Color", 0x4DB8FF);
    public static final Setting TR_COLOR2 = TRAILS.color("color2", "End color", 0xA37BFF);
    public static final Setting TR_LEN = TRAILS.num("len", "Length", 5, 80, 5, 30);
    public static final Setting TR_WIDTH = TRAILS.num("width", "Width", 0.05f, 0.6f, 0.05f, 0.18f);
    public static final Setting TR_SELF = TRAILS.bool("self", "Yourself", true);
    public static final Setting TR_OTHERS = TRAILS.bool("others", "Other players", false);
    public static final Setting TR_PEARLS = TRAILS.bool("pearls", "Ender pearls", true);

    public static final Module HALO = m("halo", "Halo", WORLD, "Glowing ring above the head", false);
    public static final Setting HA_COLOR = HALO.color("color", "Color", 0x9FE8FF);
    public static final Setting HA_SIZE = HALO.num("size", "Size", 0.2f, 0.7f, 0.05f, 0.34f);
    public static final Setting HA_HEIGHT = HALO.num("height", "Height", 0, 1, 0.05f, 0.25f);
    public static final Setting HA_THICK = HALO.num("thick", "Thickness", 0.02f, 0.2f, 0.01f, 0.07f);
    public static final Setting HA_OTHERS = HALO.bool("others", "Other players", false);

    public static final Module CHINA_HAT = m("china_hat", "China Hat", WORLD, "Translucent cone above the head", false);
    public static final Setting CHAT_COLOR = CHINA_HAT.color("color", "Color", 0x4DB8FF);
    public static final Setting CHAT_COLOR2 = CHINA_HAT.color("color2", "Rim color", 0xFFFFFF);
    public static final Setting CHAT_SIZE = CHINA_HAT.num("size", "Size", 0.3f, 1.2f, 0.05f, 0.62f);
    public static final Setting CHAT_HEIGHT = CHINA_HAT.num("height", "Height", 0.1f, 0.7f, 0.05f, 0.3f);
    public static final Setting CHAT_ALPHA = CHINA_HAT.num("alpha", "Opacity", 0.2f, 1f, 0.05f, 0.55f);
    public static final Setting CHAT_OTHERS = CHINA_HAT.bool("others", "Other players", false);

    public static final Module ATMOSPHERE = m("atmosphere", "Atmosphere", WORLD, "Client-side time of day, clear weather, tint and vignette", false);
    public static final Setting AT_TIME_ON = ATMOSPHERE.bool("timeon", "Override time", true);
    public static final Setting AT_TIME = ATMOSPHERE.num("time", "Time of day", 0, 24000, 500, 6000);
    public static final Setting AT_WEATHER = ATMOSPHERE.bool("weather", "Clear weather", true);
    public static final Setting AT_TINT = ATMOSPHERE.color("tint", "Tint color", 0x3DA5FF);
    public static final Setting AT_TINT_AMT = ATMOSPHERE.num("tintamt", "Tint amount", 0, 0.5f, 0.02f, 0);
    public static final Setting AT_VIG = ATMOSPHERE.num("vig", "Vignette", 0, 1, 0.05f, 0);

    public static final Module MOTION_BLUR = m("motion_blur", "Motion Blur", WORLD, "Blends the previous frame into the world view", false);
    public static final Setting MB_STRENGTH = MOTION_BLUR.num("strength", "Strength", 0.1f, 0.9f, 0.05f, 0.5f);

    public static final Module SATURATION = m("saturation", "Color Saturation", WORLD, "Saturation and contrast of the world view", false);
    public static final Setting CS_SAT = SATURATION.num("sat", "Saturation", 0f, 3f, 0.05f, 1.4f);
    public static final Setting CS_CONTRAST = SATURATION.num("contrast", "Contrast", 0.6f, 1.6f, 0.05f, 1f);

    public static final Module ASPECT = m("aspect", "Aspect Ratio", WORLD, "Stretches the 3D view to a custom aspect ratio", false);
    public static final Setting AR_RATIO = ASPECT.num("ratio", "Ratio", 0.5f, 3f, 0.01f, 1.33f);

    public static final Module FULLBRIGHT = m("fullbright", "Fullbright", WORLD, "Maximum brightness everywhere (client-side night vision)", false);

    // ============================================================ CLIENT
    public static final Module CLICK_GUI = m("click_gui", "Click GUI", CLIENT, "This menu. Open with the bound key (default X) or /life", true);
    public static final Setting CG_THEME = CLICK_GUI.mode("theme", "Theme", "Ocean", "Ocean", "Ice", "Aqua", "Indigo");
    public static final Setting CG_SCALE = CLICK_GUI.num("scale", "Scale", 0.6f, 1.5f, 0.05f, 1f);
    public static final Setting CG_BG = CLICK_GUI.mode("bg", "Background", "Blur", "Blur", "Dim", "None");
    public static final Setting CG_ANIM = CLICK_GUI.bool("anim", "Animations", true);
    public static final Setting CG_FONT = CLICK_GUI.bool("font", "Modern font", true);
    public static final Setting CG_MENU = CLICK_GUI.bool("mainmenu", "Custom main menu", true);

    public static final Module PERFORMANCE = m("performance", "Performance", CLIENT, "Limits for effects so the game stays smooth", true);
    public static final Setting PF_MAXFX = PERFORMANCE.num("maxfx", "Max effects", 50, 1000, 50, 400);
    public static final Setting PF_DIST = PERFORMANCE.num("dist", "Effect distance", 16, 128, 8, 64);
    public static final Setting PF_QUALITY = PERFORMANCE.mode("quality", "Quality", "High", "Low", "Medium", "High");
    public static final Setting PF_ADAPT = PERFORMANCE.bool("adapt", "Adaptive (lower FPS = fewer effects)", true);

    public static final Module DISCORD = m("discord", "Discord Presence", CLIENT, "Shows LifeVisuals in your Discord status (needs /life discord <app id>)", false);
    public static final Setting DC_SERVER = DISCORD.bool("server", "Show server address", false);

    public static final Module NAMETAG = m("nametag", "Aura Nametag", CLIENT, "Glowing nametag with health above other players", false);
    public static final Setting NT_COLOR = NAMETAG.color("color", "Aura color", 0x4DB8FF);
    public static final Setting NT_SCALE = NAMETAG.num("scale", "Scale", 0.5f, 2.5f, 0.1f, 1f);
    public static final Setting NT_HP = NAMETAG.bool("hp", "Health bar", true);

    static {
        CLICK_GUI.locked = true;
        CLICK_GUI.enabled = true;
    }

    public static Module byId(String id) {
        for (Module mod : ALL) if (mod.id.equalsIgnoreCase(id)) return mod;
        return null;
    }

    public static Module byName(String s) {
        String k = s.toLowerCase().replace("_", "").replace(" ", "");
        for (Module mod : ALL) {
            if (mod.id.replace("_", "").equals(k) || mod.name.toLowerCase().replace(" ", "").equals(k)) return mod;
        }
        return null;
    }
}
