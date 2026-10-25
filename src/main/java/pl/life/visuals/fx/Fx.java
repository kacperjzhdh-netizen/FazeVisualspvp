package pl.life.visuals.fx;

import pl.life.visuals.Perf;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/** Lekki system wlasnych efektow w swiecie (aktualizowany co klatke, rysowany w WorldVisuals). */
public final class Fx {
    private Fx() {}

    public enum Kind { DOT, BUBBLE, RING_GROUND, RING_VERT, BOLT }

    public static final class P {
        public Kind kind;
        public double x, y, z, vx, vy, vz;
        public float size, size1, life, delay, gravity, drag;
        public int color;
        public long born;
        public double[] bx, by, bz;
        public boolean second;
    }

    public static final class FloatText {
        public double x, y, z;
        public String text;
        public int color;
        public float scale, life;
        public long born;
    }

    public static final List<P> LIST = new ArrayList<>();
    public static final List<FloatText> TEXTS = new ArrayList<>();
    private static long lastNs = System.nanoTime();

    public static P add(P p) {
        p.born = System.nanoTime();
        if (LIST.size() >= Perf.maxFx()) LIST.remove(0);
        LIST.add(p);
        return p;
    }

    public static P dot(double x, double y, double z, double vx, double vy, double vz, float size, float life, int color, float gravity) {
        P p = new P();
        p.kind = Kind.DOT;
        p.x = x;
        p.y = y;
        p.z = z;
        p.vx = vx;
        p.vy = vy;
        p.vz = vz;
        p.size = size;
        p.size1 = size * 0.3f;
        p.life = life;
        p.color = color;
        p.gravity = gravity;
        p.drag = 1.2f;
        return add(p);
    }

    public static P bubble(double x, double y, double z, double vx, double vy, double vz, float size, float life, int color) {
        P p = dot(x, y, z, vx, vy, vz, size, life, color, -0.15f);
        p.kind = Kind.BUBBLE;
        p.size1 = size * 1.15f;
        p.drag = 0.8f;
        return p;
    }

    /** Pierscien na ziemi, rosnie od size do size1. */
    public static P ring(double x, double y, double z, float size, float size1, float life, int color, float delay) {
        P p = new P();
        p.kind = Kind.RING_GROUND;
        p.x = x;
        p.y = y;
        p.z = z;
        p.size = size;
        p.size1 = size1;
        p.life = life;
        p.color = color;
        p.delay = delay;
        return add(p);
    }

    public static P bolt(double x, double y, double z, double height, float life, int color) {
        P p = new P();
        p.kind = Kind.BOLT;
        p.x = x;
        p.y = y;
        p.z = z;
        p.life = life;
        p.color = color;
        p.size = 3f;
        ThreadLocalRandom r = ThreadLocalRandom.current();
        int n = 14;
        p.bx = new double[n + 1];
        p.by = new double[n + 1];
        p.bz = new double[n + 1];
        double jx = 0, jz = 0;
        for (int i = 0; i <= n; i++) {
            double t = i / (double) n;
            jx += (r.nextDouble() - 0.5) * 0.9 * (i == 0 ? 0 : 1);
            jz += (r.nextDouble() - 0.5) * 0.9 * (i == 0 ? 0 : 1);
            double fade = 1.0 - t * 0.0;
            p.bx[i] = x + jx * (1 - t) * fade;
            p.by[i] = y + height * (1 - t);
            p.bz[i] = z + jz * (1 - t) * fade;
        }
        return add(p);
    }

    public static void text(double x, double y, double z, String text, int color, float scale, float life) {
        FloatText t = new FloatText();
        t.x = x;
        t.y = y;
        t.z = z;
        t.text = text;
        t.color = color;
        t.scale = scale;
        t.life = life;
        t.born = System.nanoTime();
        if (TEXTS.size() > 40) TEXTS.remove(0);
        TEXTS.add(t);
    }

    public static float age(P p) {
        return (System.nanoTime() - p.born) / 1_000_000_000f - p.delay;
    }

    public static float age(FloatText t) {
        return (System.nanoTime() - t.born) / 1_000_000_000f;
    }

    public static void clear() {
        LIST.clear();
        TEXTS.clear();
    }

    /** Aktualizacja fizyki i usuwanie przeterminowanych efektow (raz na klatke). */
    public static void update() {
        long now = System.nanoTime();
        float dt = Math.min(0.1f, (now - lastNs) / 1_000_000_000f);
        lastNs = now;
        for (int i = LIST.size() - 1; i >= 0; i--) {
            P p = LIST.get(i);
            float a = age(p);
            if (a > p.life) {
                LIST.remove(i);
                continue;
            }
            if (a < 0) continue;
            if (p.kind == Kind.DOT || p.kind == Kind.BUBBLE) {
                p.x += p.vx * dt;
                p.y += p.vy * dt;
                p.z += p.vz * dt;
                p.vy -= p.gravity * dt;
                float k = Math.max(0f, 1f - p.drag * dt);
                p.vx *= k;
                p.vy *= k;
                p.vz *= k;
            }
        }
        for (int i = TEXTS.size() - 1; i >= 0; i--) {
            if (age(TEXTS.get(i)) > TEXTS.get(i).life) TEXTS.remove(i);
        }
    }
}
