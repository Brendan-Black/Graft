package black.graft.cambium.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import black.graft.cambium.CambiumClient;
import net.minecraft.client.Camera;

/**
 * Fabric API has no field-of-view event, so Cambium adds the one call Graft's own loader adds: at every return
 * from {@code Camera.calculateFov}, the mods' {@link black.graft.client.FovModifier}s run over vanilla's value.
 */
@Mixin(Camera.class)
abstract class CameraMixin {
	@Inject(method = "calculateFov", at = @At("RETURN"), cancellable = true)
	private void graft$modifyFov(float partialTick, CallbackInfoReturnable<Float> fov) {
		fov.setReturnValue(CambiumClient.modifyFov(fov.getReturnValueF(), partialTick));
	}
}
