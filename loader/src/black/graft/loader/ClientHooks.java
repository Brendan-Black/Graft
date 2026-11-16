package black.graft.loader;

import black.graft.client.ClientTick;
import black.graft.internal.GraftRuntime;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.protocol.handshake.ClientIntent;

/**
 * What patched client code calls. Separate from {@link Hooks} so a dedicated server, which has no client classes, never
 * loads it. Internal: mods never call these.
 */
public final class ClientHooks {
	private ClientHooks() {}

	/** {@code new Options(...)}, right after the key mapping array is made: add the mods' key mappings to it. */
	public static KeyMapping[] extendKeyMappings(KeyMapping[] vanilla) {
		return GraftRuntime.current().client().extend(vanilla);
	}

	/** Every return from {@code Camera.calculateFov(partialTick)}: run the mods' field-of-view steps. */
	public static float modifyFov(float fov, float partialTick) {
		return GraftRuntime.current().client().modifyFov(fov, partialTick);
	}

	/**
	 * Start of {@code Connection.initiateServerboundConnection(...)}: a client joining a server sends the fingerprint of its mods
	 * after the host name, for the server's {@link Hooks#loginStarted} to check. Status pings go unmarked.
	 */
	public static String markHostName(String host, ClientIntent intent) {
		return intent == ClientIntent.STATUS ? host : JoinCheck.mark(host, GraftRuntime.current().mods());
	}

	/** End of {@code Minecraft.tick()}. */
	public static void clientTick(Minecraft client) {
		GraftRuntime.current().events().post(new ClientTick(client));
	}
}
