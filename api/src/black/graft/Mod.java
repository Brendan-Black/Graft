package black.graft;

/**
 * A mod's entrypoint on both client and dedicated server: the class its descriptor names as {@code entrypoint}. It needs
 * a public no-argument constructor.
 *
 * <p>Graft calls {@link #initialize} once, when Minecraft starts bootstrapping and before any registry is frozen. That's
 * the moment to declare content, listen for events and read config; the content itself is created a little later,
 * inside Minecraft's bootstrap, and is ready once {@link Registered#get()} stops throwing.
 *
 * <p>Client-only code (rendering, input, anything under {@code net.minecraft.client}) belongs in a
 * {@link black.graft.client.ClientMod}, which a dedicated server never loads.
 */
@FunctionalInterface
public interface Mod {
	/**
	 * Declares everything this mod adds. Called once, on the thread that starts Minecraft.
	 *
	 * @param setup what the mod can declare, all of it under its own namespace
	 */
	void initialize(Setup setup);
}
