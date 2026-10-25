package pl.life.visuals.fx;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.registry.Registries;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;
import pl.life.visuals.Perf;
import pl.life.visuals.module.Modules;
import pl.life.visuals.module.Setting;

import java.util.concurrent.ThreadLocalRandom;

/** Efekty zdarzen: zabicie, smierc, totem, skok, dzwieki trafien. */
public final class Effects {
    private Effects() {}

    private static final String[] SOUND_IDS = {
            "block.note_block.bell", "block.note_block.pling", "entity.experience_orb.pickup",
            "block.amethyst_block.chime", "ui.button.click", "entity.player.attack.crit", "block.note_block.bit"
    };

    public static void play(String id, float pitch, float volume) {
        try {
            SoundEvent ev = Registries.SOUND_EVENT.get(Identifier.of(id));
            if (ev != null) MinecraftClient.getInstance().getSoundManager().play(PositionedSoundInstance.master(ev, pitch, volume));
        } catch (Exception ignored) {
        }
    }

    public static void hitSound(boolean crit) {
        if (!Modules.HIT_SOUNDS.enabled) return;
        Setting s = (crit && Modules.HSD_CRIT.bool()) ? Modules.HSD_CRIT_SOUND : Modules.HSD_SOUND;
        play(SOUND_IDS[Math.max(0, Math.min(SOUND_IDS.length - 1, s.mode()))], Modules.HSD_PITCH.num(), Modules.HSD_VOL.num());
    }

    private static double rnd(ThreadLocalRandom r, double range) {
        return (r.nextDouble() * 2 - 1) * range;
    }

    public static void kill(LivingEntity e) {
        if (!Modules.KILL_EFFECT.enabled) return;
        ThreadLocalRandom r = ThreadLocalRandom.current();
        double x = e.getX(), y = e.getY(), z = e.getZ(), h = e.getHeight();
        int color = Modules.KE_COLOR.rgb();
        float size = Modules.KE_SIZE.num();
        switch (Modules.KE_STYLE.mode()) {
            case 0: { // Burst
                for (int i = 0; i < Perf.scaled(34); i++) {
                    double a = r.nextDouble() * Math.PI * 2, sp = (1.2 + r.nextDouble() * 1.8) * size;
                    Fx.dot(x, y + h * 0.5, z, Math.cos(a) * sp, (r.nextDouble() - 0.2) * 2.2 * size, Math.sin(a) * sp, 0.09f * size, 0.7f + r.nextFloat() * 0.5f, color, 4f);
                }
                Fx.ring(x, y + 0.05, z, 0.3f, 2.4f * size, 0.7f, color, 0f);
                break;
            }
            case 1: { // Lightning
                Fx.bolt(x, y, z, 16.0, 0.55f, 0xFFFFFF);
                Fx.bolt(x, y, z, 16.0, 0.55f, color);
                Fx.ring(x, y + 0.05, z, 0.2f, 2.8f * size, 0.6f, color, 0f);
                for (int i = 0; i < Perf.scaled(14); i++) {
                    double a = r.nextDouble() * Math.PI * 2, sp = 1.0 + r.nextDouble() * 1.5;
                    Fx.dot(x, y + 0.2, z, Math.cos(a) * sp, 1.5 + r.nextDouble() * 1.5, Math.sin(a) * sp, 0.06f * size, 0.5f, color, 6f);
                }
                if (Modules.KE_SOUND.bool()) play("entity.lightning_bolt.thunder", 1.0f, 0.5f);
                break;
            }
            case 2: { // Souls
                for (int i = 0; i < Perf.scaled(18); i++) {
                    Fx.dot(x + rnd(r, 0.4), y + r.nextDouble() * h, z + rnd(r, 0.4), rnd(r, 0.25), 1.0 + r.nextDouble() * 1.2, rnd(r, 0.25),
                            0.1f * size, 1.2f + r.nextFloat() * 0.8f, color, -0.2f).drag = 0.4f;
                }
                break;
            }
            default: { // Rings
                for (int i = 0; i < 4; i++) Fx.ring(x, y + 0.05 + i * 0.45, z, 0.25f, 1.8f * size, 0.7f, color, i * 0.12f);
            }
        }
    }

    public static void death(LivingEntity e) {
        if (!Modules.DEATH_EFFECT.enabled) return;
        ThreadLocalRandom r = ThreadLocalRandom.current();
        int color = Modules.DE_COLOR.rgb();
        float size = Modules.DE_SIZE.num();
        double x = e.getX(), y = e.getY(), z = e.getZ();
        for (int i = 0; i < Perf.scaled(16); i++) {
            Fx.dot(x + rnd(r, 0.35), y + r.nextDouble() * e.getHeight(), z + rnd(r, 0.35), rnd(r, 0.3), 0.8 + r.nextDouble() * 1.0, rnd(r, 0.3),
                    0.09f * size, 1.0f + r.nextFloat() * 0.8f, color, -0.15f).drag = 0.5f;
        }
        Fx.ring(x, y + 0.05, z, 0.3f, 1.6f * size, 0.9f, color, 0f);
    }

    public static void totemPop(LivingEntity e) {
        if (!Modules.TOTEM_POP.enabled) return;
        MinecraftClient mc = MinecraftClient.getInstance();
        ClientWorld w = mc.world;
        if (w == null) return;
        if (e == mc.player && !Modules.TP_SELF.bool()) return;
        ThreadLocalRandom r = ThreadLocalRandom.current();
        int color = Modules.TP_COLOR.rgb();
        double cx = e.getX(), cy = e.getY() + e.getHeight() * 0.5, cz = e.getZ();
        DustParticleEffect fx = new DustParticleEffect(color, 1.3f);
        int n = Perf.scaled(Modules.TP_AMOUNT.intv());
        for (int i = 0; i < n; i++) {
            double a = i * (Math.PI * 2 / n);
            w.addParticle(fx, cx, cy, cz, Math.cos(a) * 0.35, 0.02 + r.nextDouble() * 0.1, Math.sin(a) * 0.35);
        }
        for (int i = 0; i < n; i++) {
            Fx.dot(cx, cy, cz, rnd(r, 1.6), 1.0 + r.nextDouble() * 1.8, rnd(r, 1.6), 0.08f, 0.9f, color, 3f);
        }
        Fx.ring(e.getX(), e.getY() + 0.05, e.getZ(), 0.3f, 2.2f, 0.8f, color, 0f);
        Fx.ring(e.getX(), e.getY() + 0.9, e.getZ(), 0.3f, 1.6f, 0.8f, color, 0.1f);
        if (Modules.TP_TEXT.bool()) Fx.text(cx, e.getY() + e.getHeight() + 0.6, cz, "TOTEM POP", color, 1.0f, 1.4f);
    }

    public static void jump(PlayerEntity p) {
        MinecraftClient mc = MinecraftClient.getInstance();
        boolean self = p == mc.player;
        ThreadLocalRandom r = ThreadLocalRandom.current();
        double x = p.getX(), y = p.getY(), z = p.getZ();
        if (Modules.JUMP_CIRCLES.enabled && (self || Modules.JC_OTHERS.bool())) {
            int c = Modules.JC_COLOR.rgb();
            float s = Modules.JC_SIZE.num(), t = Modules.JC_TIME.num();
            int st = Modules.JC_STYLE.mode();
            Fx.P ring = Fx.ring(x, y + 0.03, z, 0.15f, s, t, c, 0f);
            ring.second = st == 1; // wypelniony
            if (st == 2) Fx.ring(x, y + 0.03, z, 0.1f, s * 0.65f, t, c, 0.08f);
        }
        if (Modules.JUMP_PARTICLES.enabled && (self || Modules.JP_OTHERS.bool())) {
            int c = Modules.JP_COLOR.rgb();
            for (int i = 0; i < Perf.scaled(Modules.JP_AMOUNT.intv()); i++) {
                double a = r.nextDouble() * Math.PI * 2, sp = 0.5 + r.nextDouble() * 1.2;
                Fx.dot(x, y + 0.1, z, Math.cos(a) * sp, 0.6 + r.nextDouble() * 1.2, Math.sin(a) * sp, 0.07f * Modules.JP_SIZE.num(), 0.6f + r.nextFloat() * 0.4f, c, 5f);
            }
        }
    }
}
