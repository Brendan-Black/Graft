package black.graft;

import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;

/**
 * A handle to something a mod declared, which Minecraft creates during its bootstrap. Keep the handle in a field and call
 * {@link #get()} when the thing is needed, from event listeners and game code; at {@link Mod#initialize} time it doesn't
 * exist yet.
 *
 * <p>Implemented by Graft: mods receive handles, they don't implement them.
 *
 * @param <T> what was declared: a {@code Block}, an {@code Item} or a subclass
 */
public interface Registered<T> {
	/** The full id, {@code truezoom:zoom_block}. */
	Identifier id();

	/** The registry key, for vanilla methods that take one. */
	ResourceKey<? super T> key();

	/** Whether Minecraft has created it yet. */
	boolean isPresent();

	/**
	 * The thing itself.
	 *
	 * @throws IllegalStateException if it's asked for before Minecraft's registries are populated
	 */
	T get();
}
