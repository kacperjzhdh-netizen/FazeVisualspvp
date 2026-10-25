package pl.life.visuals;

import net.minecraft.client.MinecraftClient;
import pl.life.visuals.module.Modules;

/** Limity wydajnosci stosowane przez wszystkie efekty. */
public final class Perf {
    private Perf() {}

    private static float factor() {
        if (!Modules.PF_ADAPT.bool()) return 1f;
        int fps = MinecraftClient.getInstance().getCurrentFps();
        if (fps <= 0) return 1f;
        if (fps < 30) return 0.35f;
        if (fps < 50) return 0.6f;
        return 1f;
    }

    public static int maxFx() {
        return Math.max(20, (int) (Modules.PF_MAXFX.num() * factor()));
    }

    public static double maxDist() {
        return Modules.PF_DIST.num();
    }

    /** Liczba segmentow okregu zalezna od jakosci. */
    public static int circleSegs() {
        switch (Modules.PF_QUALITY.mode()) {
            case 0: return 16;
            case 1: return 28;
            default: return 40;
        }
    }

    public static int scaled(int amount) {
        return Math.max(1, Math.round(amount * factor()));
    }
}
