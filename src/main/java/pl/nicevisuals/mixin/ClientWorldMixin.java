package pl.nicevisuals.mixin;

import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import pl.nicevisuals.NiceVisualsClient;
import pl.nicevisuals.module.Sky;

/** Lets the Sky module replace the sky colour (return type differs between versions, so it is checked at runtime). */
@Mixin(ClientWorld.class)
public class ClientWorldMixin {
    @Inject(method = "getSkyColor", at = @At("RETURN"), cancellable = true, require = 0)
    private void nicevisuals$sky(Vec3d cameraPos, float tickDelta, CallbackInfoReturnable<Object> cir) {
        try {
            Sky sky = NiceVisualsClient.modules == null ? null : NiceVisualsClient.modules.sky;
            if (sky == null || !sky.isEnabled()) return;
            int rgb = sky.rgb();
            Object orig = cir.getReturnValue();
            if (orig instanceof Integer) cir.setReturnValue(0xFF000000 | rgb);
            else if (orig instanceof Vec3d) {
                cir.setReturnValue(new Vec3d((rgb >> 16 & 255) / 255.0, (rgb >> 8 & 255) / 255.0, (rgb & 255) / 255.0));
            }
        } catch (Throwable ignored) {}
    }
}
