package black.graft.client;

/**
 * A mod's client-only entrypoint: the class its descriptor names as {@code client-entrypoint}, with a public no-argument
 * constructor. A dedicated server never loads it, so it can use anything under {@code net.minecraft.client}.
 *
 * <p>Graft calls {@link #initializeClient} once, right after the mod's {@link black.graft.Mod#initialize} if it has one,
 * before the game window opens.
 */
@FunctionalInterface
public interface ClientMod {
	/**
	 * Declares this mod's key bindings, view changes and client event listeners.
	 *
	 * @param setup what the mod can declare on the client
	 */
	void initializeClient(ClientSetup setup);
}
