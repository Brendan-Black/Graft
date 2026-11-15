package black.graft;

/** Which program the game is running as. A client also runs a server for single player, but it's still {@link #CLIENT}. */
public enum Dist {
	/** The game with a window: the Minecraft launcher's install, or {@code runClient}. */
	CLIENT,
	/** A dedicated server, with no window and no client classes. */
	SERVER
}
