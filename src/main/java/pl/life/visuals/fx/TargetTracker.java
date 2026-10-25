package pl.life.visuals.fx;

import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.util.ActionResult;
import pl.life.visuals.Perf;
import pl.life.visuals.module.Modules;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Wybor celu (patrze / atakuje), wykrywanie POTWIERDZONYCH obrazen (spadek HP ofiary w krotkim oknie po moim ataku),
 * licznik combo oraz wywolywanie efektow trafienia i zabicia.
 */
public final class TargetTracker {
    private TargetTracker() {}

    private record Attack(long time, float hpBefore, boolean crit) {}

    private static PlayerEntity target;
    private static long lastSeen;
    private static final Map<Integer, Attack> attacks = new HashMap<>();
    private static final Map<Integer, Long> victims = new HashMap<>();
    private static final long ATTACK_WINDOW_MS = 700;
    private static final long TARGET_TIMEOUT_MS = 5000;
    private static int prevHurtTime;

    // dane dla HUD
    public static long lastHitMs;
    public static boolean lastHitCrit;
    public static int combo, bestCombo;
    public static long comboMs;

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
        boolean crit = me.fallDistance > 0.0f && !me.isOnGround() && !me.isClimbing()
                && !me.isTouchingWater() && !me.hasStatusEffect(StatusEffects.BLINDNESS)
                && !me.hasVehicle() && !me.isSprinting() && me.getAttackCooldownProgress(0.5f) > 0.9f;
        attacks.put(victim.getId(), new Attack(now, hp(living), crit));
        if (victim instanceof PlayerEntity p && p != me) {
            target = p;
            lastSeen = now;
        }
    }

    public static void reset() {
        target = null;
        attacks.clear();
        victims.clear();
        combo = 0;
        Fx.clear();
    }

    public static void tick(MinecraftClient mc) {
        ClientWorld world = mc.world;
        ClientPlayerEntity me = mc.player;
        if (world == null || me == null) {
            reset();
            return;
        }
        long now = System.currentTimeMillis();

        if (mc.targetedEntity instanceof PlayerEntity p && p != me) {
            target = p;
            lastSeen = now;
        }
        if (target != null && (target.isRemoved() || !target.isAlive() || now - lastSeen > TARGET_TIMEOUT_MS)) {
            target = null;
        }

        // combo: reset po otrzymaniu obrazen lub po uplywie czasu
        if (me.hurtTime > 0 && prevHurtTime == 0) combo = 0;
        prevHurtTime = me.hurtTime;
        if (combo > 0 && now - comboMs > Modules.CO_RESET.num() * 1000f) combo = 0;

        // potwierdzone obrazenia
        Iterator<Map.Entry<Integer, Attack>> it = attacks.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Integer, Attack> en = it.next();
            Attack rec = en.getValue();
            Entity e = world.getEntityById(en.getKey());
            if (!(e instanceof LivingEntity le) || e.isRemoved() || now - rec.time() > ATTACK_WINDOW_MS) {
                it.remove();
                continue;
            }
            float drop = rec.hpBefore() - hp(le);
            if (drop > 0.01f) {
                onHit(le, drop, rec.crit());
                it.remove();
            }
        }

        // zabicia
        Iterator<Map.Entry<Integer, Long>> vit = victims.entrySet().iterator();
        while (vit.hasNext()) {
            Map.Entry<Integer, Long> en = vit.next();
            Entity e = world.getEntityById(en.getKey());
            if (!(e instanceof LivingEntity le) || e.isRemoved() || now - en.getValue() > 4000) {
                vit.remove();
                continue;
            }
            if (le.getHealth() <= 0f || le.isDead()) {
                Effects.kill(le);
                vit.remove();
            }
        }
    }

    private static void onHit(LivingEntity e, float dmg, boolean crit) {
        MinecraftClient mc = MinecraftClient.getInstance();
        ClientWorld w = mc.world;
        if (w == null) return;
        long now = System.currentTimeMillis();
        lastHitMs = now;
        lastHitCrit = crit;
        if (combo > 0 && now - comboMs > Modules.CO_RESET.num() * 1000f) combo = 0;
        combo++;
        bestCombo = Math.max(bestCombo, combo);
        comboMs = now;
        victims.put(e.getId(), now);

        ThreadLocalRandom r = ThreadLocalRandom.current();
        double cx = e.getX(), cy = e.getY() + e.getHeight() * 0.55, cz = e.getZ();

        if (Modules.HIT_PARTICLES.enabled) {
            DustParticleEffect fx = new DustParticleEffect(Modules.HP_COLOR.rgb(), Modules.HP_SIZE.num());
            int n = Perf.scaled(Modules.HP_AMOUNT.intv());
            for (int i = 0; i < n; i++) {
                w.addParticle(fx, cx + rnd(r, 0.3), cy + rnd(r, 0.4), cz + rnd(r, 0.3), rnd(r, 0.12), r.nextDouble() * 0.12, rnd(r, 0.12));
            }
            if (crit && Modules.HP_CRIT.bool()) {
                DustParticleEffect cf = new DustParticleEffect(Modules.HP_CRIT_COLOR.rgb(), 1.0f);
                for (int i = 0; i < Perf.scaled(18); i++) {
                    double vx = rnd(r, 0.35), vy = r.nextDouble() * 0.3, vz = rnd(r, 0.35);
                    w.addParticle(cf, cx, cy + 0.2, cz, vx, vy, vz);
                    if (i % 2 == 0) w.addParticle(net.minecraft.particle.ParticleTypes.CRIT, cx, cy + 0.2, cz, vx, vy, vz);
                }
            }
        }
        if (Modules.HIT_BUBBLES.enabled) {
            int n = Perf.scaled(Modules.HB_AMOUNT.intv());
            for (int i = 0; i < n; i++) {
                float size = Modules.HB_SIZE.num() * (0.6f + r.nextFloat() * 0.8f);
                Fx.bubble(cx + rnd(r, 0.35), cy + rnd(r, 0.45), cz + rnd(r, 0.35),
                        rnd(r, 0.5), 0.35 + r.nextDouble() * 0.5, rnd(r, 0.5), size, Modules.HB_LIFE.num() * (0.7f + r.nextFloat() * 0.6f), Modules.HB_COLOR.rgb());
            }
        }
        if (Modules.DAMAGE_NUMBERS.enabled) {
            Fx.text(cx + rnd(r, 0.3), e.getY() + e.getHeight() + 0.3, cz + rnd(r, 0.3), String.format("-%.1f", dmg),
                    crit ? Modules.DN_CRIT.rgb() : Modules.DN_COLOR.rgb(), Modules.DN_SCALE.num(), Modules.DN_TIME.num());
        }
        Effects.hitSound(crit);
    }

    private static double rnd(ThreadLocalRandom r, double range) {
        return (r.nextDouble() * 2 - 1) * range;
    }
}
