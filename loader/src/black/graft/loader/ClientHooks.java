package black.graft.loader;

import black.graft.client.ClientTick;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;

/**
 * What patched client code calls. Separate from {@link Hooks} so a dedicated server, which has no client classes, never
 * loads it. Internal: mods never call these.
 */
public final class ClientHooks {
	private ClientHooks() {}

	/** {@code new Options(...)}, right after the key mapping array is made: add the mods' key mappings to it. */
	public static KeyMapping[] extendKeyMappings(KeyMapping[] vanilla) {
		return ModRuntime.current().client().extend(vanilla);
	}

	/** Every return from {@code Camera.calculateFov(partialTick)}: run the mods' field-of-view steps. */
	public static float modifyFov(float fov, float partialTick) {
		return ModRuntime.current().client().modifyFov(fov, partialTick);
	}

	/** End of {@code Minecraft.tick()}. */
	public static void clientTick(Minecraft client) {
		ModRuntime.current().events().post(new ClientTick(client));
	}
}
