package pl.faze.visuals;

import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.thrown.EnderPearlEntity;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Logika celu (target), wykrywanie obrazen zadanych celowi oraz efekty czasteczkowe.
 * Wszystko dziala wylacznie po stronie klienta na podstawie danych, ktore serwer i tak wysyla klientowi.
 */
public final class TargetTracker {
    private TargetTracker() {}

    /** Plywajacy napis w swiecie (Damage Indicator / Totem Pop). */
    public static final class FloatingText {
        public final double x, y, z;
        public final String text;
        public final int color;
        public final long created;
        FloatingText(double x, double y, double z, String text, int color) {
            this.x = x; this.y = y; this.z = z; this.text = text; this.color = color;
            this.created = System.currentTimeMillis();
        }
    }

    private record AttackRecord(long time, float hpBefore, boolean crit) {}

    private static PlayerEntity target;
    private static long lastSeen;
    private static final Map<Integer, AttackRecord> attacks = new HashMap<>();
    private static final Map<Integer, Vec3d> pearlLast = new HashMap<>();
    public static final List<FloatingText> texts = new ArrayList<>();

    private static final long ATTACK_WINDOW_MS = 700;

    public static void init() {
        AttackEntityCallback.EVENT.register((player, world, hand, entity, hit) -> {
            if (world.isClient) onAttack(player, entity);
            return ActionResult.PASS;
        });
    }

    public static PlayerEntity getTarget() {
        return target;
    }

    public static boolean isTarget(Entity e) {
        return e != null && e == target;
    }

    private static float hp(LivingEntity e) {
        return e.getHealth() + e.getAbsorptionAmount();
    }

    private static void onAttack(PlayerEntity me, Entity victim) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || me != mc.player || !(victim instanceof LivingEntity living)) return;
        long now = System.currentTimeMillis();

        // Przewidywanie krytyka po stronie klienta (te same warunki co w grze).
        boolean crit = me.fallDistance > 0.0f && !me.isOnGround() && !me.isClimbing()
                && !me.isTouchingWater() && !me.hasStatusEffect(StatusEffects.BLINDNESS)
                && !me.hasVehicle() && !me.isSprinting() && me.getAttackCooldownProgress(0.5f) > 0.9f;

        attacks.put(victim.getId(), new AttackRecord(now, hp(living), crit));

        if (Config.get().targetMode != 0 && victim instanceof PlayerEntity p && p != me) {
            target = p;
            lastSeen = now;
        }
    }

    public static void tick(MinecraftClient mc) {
        ClientWorld world = mc.world;
        if (world == null || mc.player == null) {
            target = null;
            attacks.clear();
            pearlLast.clear();
            texts.clear();
            return;
        }
        long now = System.currentTimeMillis();
        Config c = Config.get();

        if (c.targetMode != 1 && mc.targetedEntity instanceof PlayerEntity p && p != mc.player) {
            target = p;
            lastSeen = now;
        }
        if (target != null && (target.isRemoved() || !target.isAlive()
                || now - lastSeen > (long) (c.targetTimeout * 1000f))) {
            target = null;
        }

        // Obrazenia zadane po ataku: spadek HP ofiary w krotkim oknie czasu po moim ataku.
        Iterator<Map.Entry<Integer, AttackRecord>> it = attacks.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Integer, AttackRecord> en = it.next();
            AttackRecord rec = en.getValue();
            Entity e = world.getEntityById(en.getKey());
            if (!(e instanceof LivingEntity le) || e.isRemoved() || now - rec.time() > ATTACK_WINDOW_MS) {
                it.remove();
                continue;
            }
            float drop = rec.hpBefore() - hp(le);
            if (drop > 0.01f) {
                onDamageDealt(le, drop, rec.crit());
                it.remove();
            }
        }

        pearls(world, c);

        long ttl = 1000;
        texts.removeIf(t -> now - t.created > ttl);
    }

    private static void onDamageDealt(LivingEntity e, float dmg, boolean crit) {
        Config c = Config.get();
        ThreadLocalRandom r = ThreadLocalRandom.current();
        ClientWorld w = MinecraftClient.getInstance().world;
        if (w == null) return;
        double cx = e.getX(), cy = e.getY() + e.getHeight() * 0.55, cz = e.getZ();

        if (c.hitParticles) {
            DustParticleEffect fx = new DustParticleEffect(c.hitColor & 0xFFFFFF, 1.2f);
            int n = (int) c.hitAmount;
            for (int i = 0; i < n; i++) {
                w.addParticle(fx, cx + rnd(r, 0.3), cy + rnd(r, 0.4), cz + rnd(r, 0.3),
                        rnd(r, 0.12), r.nextDouble() * 0.12, rnd(r, 0.12));
            }
        }
        if (c.critParticles && crit) {
            DustParticleEffect fx = new DustParticleEffect(c.critColor & 0xFFFFFF, 1.0f);
            int n = (int) c.critAmount;
            for (int i = 0; i < n; i++) {
                double vx = rnd(r, 0.35), vy = r.nextDouble() * 0.3, vz = rnd(r, 0.35);
                w.addParticle(fx, cx, cy + 0.2, cz, vx, vy, vz);
                if (i % 2 == 0) w.addParticle(ParticleTypes.CRIT, cx, cy + 0.2, cz, vx, vy, vz);
            }
        }
        if (c.damageIndicator) {
            if (texts.size() > 32) texts.remove(0);
            texts.add(new FloatingText(cx + rnd(r, 0.3), e.getY() + e.getHeight() + 0.3, cz + rnd(r, 0.3),
                    String.format("-%.1f", dmg), crit ? 0xFFFF55 : c.damageColor));
        }
    }

    public static void onTotemPop(LivingEntity e) {
        Config c = Config.get();
        if (!c.totemVisuals) return;
        ClientWorld w = MinecraftClient.getInstance().world;
        if (w == null) return;
        ThreadLocalRandom r = ThreadLocalRandom.current();
        DustParticleEffect fx = new DustParticleEffect(c.totemColor & 0xFFFFFF, 1.4f);
        double cx = e.getX(), cy = e.getY() + e.getHeight() * 0.5, cz = e.getZ();
        // pierscien + wybuch
        for (int i = 0; i < 36; i++) {
            double a = i * (Math.PI * 2 / 36);
            w.addParticle(fx, cx, cy, cz, Math.cos(a) * 0.35, 0.02, Math.sin(a) * 0.35);
        }
        for (int i = 0; i < 30; i++) {
            w.addParticle(fx, cx, cy, cz, rnd(r, 0.3), r.nextDouble() * 0.4, rnd(r, 0.3));
        }
        if (texts.size() > 32) texts.remove(0);
        texts.add(new FloatingText(cx, e.getY() + e.getHeight() + 0.6, cz, "TOTEM POP", c.totemColor));
    }

    private static void pearls(ClientWorld world, Config c) {
        if (!c.pearlTrail) {
            pearlLast.clear();
            return;
        }
        DustParticleEffect fx = new DustParticleEffect(c.pearlColor & 0xFFFFFF, 1.0f);
        Set<Integer> alive = new HashSet<>();
        for (Entity e : world.getEntities()) {
            if (!(e instanceof EnderPearlEntity)) continue;
            alive.add(e.getId());
            Vec3d now = new Vec3d(e.getX(), e.getY(), e.getZ());
            Vec3d last = pearlLast.get(e.getId());
            if (last == null) last = now;
            double dist = now.distanceTo(last);
            int steps = Math.max(1, Math.min(24, (int) Math.ceil(dist * c.pearlDensity)));
            for (int i = 1; i <= steps; i++) {
                double t = i / (double) steps;
                world.addParticle(fx, last.x + (now.x - last.x) * t, last.y + (now.y - last.y) * t + 0.1,
                        last.z + (now.z - last.z) * t, 0, 0, 0);
            }
            pearlLast.put(e.getId(), now);
        }
        pearlLast.keySet().retainAll(alive);
    }

    private static double rnd(ThreadLocalRandom r, double range) {
        return (r.nextDouble() * 2 - 1) * range;
    }
}
