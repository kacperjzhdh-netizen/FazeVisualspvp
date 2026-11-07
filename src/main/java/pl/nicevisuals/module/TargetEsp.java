package pl.nicevisuals.module;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.LivingEntity;
import pl.nicevisuals.setting.NumSetting;

/** Highlights the last entity you hit with a see-through glow outline. */
public class TargetEsp extends Module {
    private final NumSetting seconds = add(new NumSetting("Hide after s", 5, 1, 20));
    private LivingEntity target;
    private long lastHit;

    public TargetEsp() { super("Target ESP", Category.COMBAT); }

    public void setTarget(LivingEntity e) {
        if (target != null && target != e) target.setGlowing(false);
        target = e;
        lastHit = System.currentTimeMillis();
    }

    @Override
    public void onTick(MinecraftClient mc) {
        if (target == null) return;
        boolean expired = System.currentTimeMillis() - lastHit > seconds.value * 1000 || !target.isAlive();
        if (expired) { target.setGlowing(false); target = null; }
        else target.setGlowing(true);
    }

    @Override
    public void onDisable() {
        if (target != null) { target.setGlowing(false); target = null; }
    }
}
