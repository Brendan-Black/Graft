package black.graft.fabric;

import black.graft.client.ClientTick;
import black.graft.internal.GraftRuntime;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;

/** The client half of Graft for Fabric: client entrypoints, key bindings, the client tick, and (through a mixin) the FOV. */
public final class GraftFabricClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		GraftRuntime runtime = GraftRuntime.current();
		runtime.initializeClient();
		for (KeyMapping mapping : runtime.client().mappings()) {
			KeyMappingHelper.registerKeyMapping(mapping);
		}
		ClientTickEvents.END_CLIENT_TICK.register(client -> runtime.events().post(new ClientTick(client)));
		runtime.finishStartup();
	}

	/** Called by {@link black.graft.fabric.mixin.CameraMixin} with vanilla's field of view, every frame. */
	public static float modifyFov(float fov, float partialTick) {
		return GraftRuntime.current().client().modifyFov(fov, partialTick);
	}
}
