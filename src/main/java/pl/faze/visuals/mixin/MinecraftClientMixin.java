package pl.faze.visuals.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import pl.faze.visuals.Config;
import pl.faze.visuals.TargetTracker;

/** Glow: wymusza obrys (outline) wokol aktualnego celu. */
@Mixin(MinecraftClient.class)
public class MinecraftClientMixin {
    @Inject(method = "hasOutline", at = @At("HEAD"), cancellable = true)
    private void faze$outline(Entity entity, CallbackInfoReturnable<Boolean> cir) {
        if (Config.get().glow && TargetTracker.isTarget(entity)) {
            cir.setReturnValue(true);
        }
    }
}
