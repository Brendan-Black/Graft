package black.graft;

/**
 * Where a mod is needed: the {@code side} line in its descriptor. It decides whether a server asks joining players for the
 * mod, so a zoom or minimap mod on a player's game doesn't keep them off servers that don't have it.
 *
 * <p>Only {@link #BOTH} mods may add blocks or items. Those travel between client and server as numbers, so both ends need
 * them; a one-sided mod that added them would show the other end the wrong blocks.
 */
public enum Side {
	/** Needed on the client and the server alike, and checked when a player joins. The default. */
	BOTH,
	/**
	 * Only for the player's own game: views, keys, the screen. Not loaded on a dedicated server, and not asked of players
	 * joining one.
	 */
	CLIENT,
	/**
	 * Only for the server: commands, rules, admin tools. Loaded wherever a server runs, single player included, and not
	 * asked of players joining one.
	 */
	SERVER;

	/** Whether a mod for this side is loaded on {@code dist}. */
	public boolean runsOn(Dist dist) {
		return this != CLIENT || dist == Dist.CLIENT;
	}

	/** Whether a server and a joining client must both have a mod for this side, at the same version. */
	public boolean needsBothEnds() {
		return this == BOTH;
	}
}
