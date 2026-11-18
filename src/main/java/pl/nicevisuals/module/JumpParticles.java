package pl.nicevisuals.module;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;
import pl.nicevisuals.setting.BoolSetting;
import pl.nicevisuals.setting.ModeSetting;
import pl.nicevisuals.setting.NumSetting;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Spawns a ring of particles under a player's feet when they jump. */
public class JumpParticles extends Module {
    private final ModeSetting style = add(new ModeSetting("Style", 0,
            "Glow", "Cloud", "Flame", "Hearts", "Crit", "Totem", "Soul fire", "Sparks"));
    private final NumSetting amount = add(new NumSetting("Amount", 24, 6, 80));
    private final NumSetting radius = add(new NumSetting("Radius", 10, 3, 25));
    private final BoolSetting landing = add(new BoolSetting("Landing too", false));
    private final BoolSetting others = add(new BoolSetting("Other players", false));
    private final Map<UUID, Boolean> ground = new HashMap<>();

    public JumpParticles() { super("Jump Particles", Category.WORLD); }

    private ParticleEffect type() {
        return switch (style.index) {
            case 1 -> ParticleTypes.CLOUD;
            case 2 -> ParticleTypes.FLAME;
            case 3 -> ParticleTypes.HEART;
            case 4 -> ParticleTypes.CRIT;
            case 5 -> ParticleTypes.TOTEM_OF_UNDYING;
            case 6 -> ParticleTypes.SOUL_FIRE_FLAME;
            case 7 -> ParticleTypes.ELECTRIC_SPARK;
            default -> ParticleTypes.END_ROD;
        };
    }

    private void burst(MinecraftClient mc, PlayerEntity p) {
        int n = amount.asInt();
        double r = radius.value / 10.0;
        ParticleEffect t = type();
        for (int i = 0; i < n; i++) {
            double a = i * Math.PI * 2 / n;
            double c = Math.cos(a), s = Math.sin(a);
            mc.world.addParticle(t, p.getX() + c * 0.3, p.getY() + 0.05, p.getZ() + s * 0.3,
                    c * 0.06 * r, 0.02, s * 0.06 * r);
        }
    }

    @Override
    public void onTick(MinecraftClient mc) {
        if (mc.world == null || mc.player == null) { ground.clear(); return; }
        for (PlayerEntity p : mc.world.getPlayers()) {
            if (p != mc.player && !others.value) continue;
            if (p != mc.player && p.squaredDistanceTo(mc.player) > 48 * 48) continue;
            boolean now = p.isOnGround();
            Boolean was = ground.put(p.getUuid(), now);
            if (was == null) continue;
            if (was && !now && p.getVelocity().y > 0.05) burst(mc, p);
            else if (!was && now && landing.value) burst(mc, p);
        }
    }

    @Override public void onDisable() { ground.clear(); }
}
