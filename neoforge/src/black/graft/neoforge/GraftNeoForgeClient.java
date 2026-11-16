package black.graft.neoforge;

import black.graft.client.ClientTick;
import black.graft.internal.GraftRuntime;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import net.neoforged.neoforge.common.NeoForge;

/** The client half of Graft for NeoForge: client entrypoints, key bindings, the client tick, and the field of view. */
final class GraftNeoForgeClient {
	private GraftNeoForgeClient() {}

	static void initialize(GraftRuntime runtime, IEventBus modBus) {
		runtime.initializeClient();
		modBus.addListener(RegisterKeyMappingsEvent.class, event -> {
			for (KeyMapping mapping : runtime.client().mappings()) {
				event.register(mapping);
			}
		});
		NeoForge.EVENT_BUS.addListener(ClientTickEvent.Post.class, _ -> runtime.events().post(new ClientTick(Minecraft.getInstance())));
		// NeoForge fires this with the camera's field of view, every frame, as Graft's own loader hooks Camera.calculateFov.
		NeoForge.EVENT_BUS.addListener(ViewportEvent.ComputeFov.class,
			event -> event.setFOV(runtime.client().modifyFov(event.getFOV(), event.getPartialTick())));
	}
}
