package black.graft.client;

import net.minecraft.client.KeyMapping;

/**
 * A key binding a mod declared. Graft creates the vanilla {@link KeyMapping} as the game's options load, before the window
 * opens, so it's ready by the first {@link ClientTick}.
 *
 * <p>Implemented by Graft: mods receive it, they don't implement it.
 */
public interface KeyBinding {
	/** Whether the key is held down right now. */
	boolean isDown();

	/** Whether the key was pressed since the last call, counting each press once. */
	boolean consumeClick();

	/**
	 * The vanilla key mapping, for anything this interface doesn't cover.
	 *
	 * @throws IllegalStateException before the game's options have loaded
	 */
	KeyMapping mapping();
}
