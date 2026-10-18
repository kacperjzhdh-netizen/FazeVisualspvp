package pl.faze.visuals.mixin;

import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import pl.faze.visuals.Config;
import pl.faze.visuals.TargetTracker;

/** Kolor obrysu Glow dla celu. */
@Mixin(Entity.class)
public class EntityMixin {
    @Inject(method = "getTeamColorValue", at = @At("HEAD"), cancellable = true)
    private void faze$glowColor(CallbackInfoReturnable<Integer> cir) {
        Config c = Config.get();
        if (c.glow && TargetTracker.isTarget((Entity) (Object) this)) {
            cir.setReturnValue(c.glowColor & 0xFFFFFF);
        }
    }
}
