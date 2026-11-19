package pl.nicevisuals.module;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.entity.LivingEntity;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.Vec3d;
import pl.nicevisuals.setting.BoolSetting;
import pl.nicevisuals.setting.ModeSetting;
import pl.nicevisuals.setting.NumSetting;

/** Particle based effects: hit effect, trails and kill effect. */
public final class Effects {
    private Effects() {}

    static ParticleEffect type(int i) {
        return switch (i) {
            case 1 -> ParticleTypes.CRIT;
            case 2 -> ParticleTypes.FLAME;
            case 3 -> ParticleTypes.HEART;
            case 4 -> ParticleTypes.TOTEM_OF_UNDYING;
            case 5 -> ParticleTypes.SOUL_FIRE_FLAME;
            case 6 -> ParticleTypes.ELECTRIC_SPARK;
            case 7 -> ParticleTypes.CLOUD;
            default -> ParticleTypes.END_ROD;
        };
    }
    static final String[] STYLES = {"Glow", "Crit", "Flame", "Hearts", "Totem", "Soul fire", "Sparks", "Cloud"};

    public static class HitEffect extends Module {
        private final ModeSetting style = add(new ModeSetting("Style", 1, STYLES));
        private final NumSetting amount = add(new NumSetting("Amount", 20, 4, 80));

        public HitEffect() { super("Hit Effect", Category.COMBAT); }

        public void hit(MinecraftClient mc, LivingEntity e) {
            if (mc.world == null) return;
            ParticleEffect t = type(style.index);
            for (int i = 0; i < amount.asInt(); i++) {
                double a = Math.random() * Math.PI * 2, up = Math.random() * 0.25;
                mc.world.addParticle(t, e.getX(), e.getY() + e.getHeight() * 0.6, e.getZ(),
                        Math.cos(a) * 0.12, up, Math.sin(a) * 0.12);
            }
        }
    }

    public static class Trails extends Module {
        private final ModeSetting style = add(new ModeSetting("Style", 0, STYLES));
        private final NumSetting amount = add(new NumSetting("Amount", 2, 1, 8));

        public Trails() { super("Trails", Category.WORLD); }

        @Override public void onTick(MinecraftClient mc) {
            if (mc.world == null || mc.player == null) return;
            Vec3d v = mc.player.getVelocity();
            if (v.x * v.x + v.z * v.z < 0.0004 && Math.abs(v.y) < 0.05) return;
            ParticleEffect t = type(style.index);
            for (int i = 0; i < amount.asInt(); i++)
                mc.world.addParticle(t, mc.player.getX() + (Math.random() - 0.5) * 0.4, mc.player.getY() + 0.1 + Math.random() * 0.3,
                        mc.player.getZ() + (Math.random() - 0.5) * 0.4, 0, 0.01, 0);
        }
    }

    public static class KillEffect extends Module {
        private final ModeSetting style = add(new ModeSetting("Style", 4, STYLES));
        private final BoolSetting sound = add(new BoolSetting("Sound", true));
        private LivingEntity target;
        private long lastHit;
        private Vec3d pos;
        private int anim;

        public KillEffect() { super("Kill Effect", Category.COMBAT); }

        public void registerHit(LivingEntity e) { target = e; lastHit = System.currentTimeMillis(); }

        @Override public void onTick(MinecraftClient mc) {
            if (mc.world == null) return;
            if (target != null) {
                long age = System.currentTimeMillis() - lastHit;
                if (!target.isAlive() || target.getHealth() <= 0) {
                    if (age < 4000) {
                        pos = new Vec3d(target.getX(), target.getY(), target.getZ());
                        anim = 24;
                        if (sound.value) mc.getSoundManager().play(PositionedSoundInstance.master(SoundEvents.ENTITY_PLAYER_LEVELUP, 1.2f));
                    }
                    target = null;
                } else if (age > 5000) target = null;
            }
            if (anim > 0 && pos != null) {
                ParticleEffect t = type(style.index);
                double prog = (24 - anim) / 24.0;
                for (int k = 0; k < 3; k++) {
                    double a = prog * 12 + k * Math.PI * 2 / 3;
                    mc.world.addParticle(t, pos.x + Math.cos(a) * 0.8, pos.y + prog * 2.2, pos.z + Math.sin(a) * 0.8, 0, 0.03, 0);
                }
                anim--;
            }
        }
    }
}
