package pl.faze.visuals.mixin;

import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pl.faze.visuals.TargetTracker;

/** Wykrywa aktywacje totemu (status 35 wysylany przez serwer). */
@Mixin(LivingEntity.class)
public class LivingEntityMixin {
    @Inject(method = "handleStatus", at = @At("HEAD"))
    private void faze$status(byte status, CallbackInfo ci) {
        if (status == 35) {
            LivingEntity self = (LivingEntity) (Object) this;
            if (self.getWorld().isClient) {
                TargetTracker.onTotemPop(self);
            }
        }
    }
}
