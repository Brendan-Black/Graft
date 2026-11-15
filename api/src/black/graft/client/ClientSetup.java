package black.graft.client;

import black.graft.ContentName;
import black.graft.ModInfo;
import black.graft.config.Config;
import black.graft.config.ConfigSpec;
import black.graft.event.Events;

/**
 * What a mod can declare from {@link ClientMod#initializeClient}. Its methods fail once {@code initializeClient} has
 * returned.
 *
 * <p>Implemented by Graft: mods receive it, they don't implement it.
 */
public interface ClientSetup {
	/** This mod, as its descriptor describes it. */
	ModInfo mod();

	/**
	 * Declares a key binding, listed in Controls under this mod's own category and saved with the player's other keys.
	 * Its translation key is {@code key.<mod id>.<name>}; the category's is {@code key.category.<mod id>.keys}.
	 *
	 * @param name       its name within this mod
	 * @param defaultKey the GLFW key code it starts on, such as {@code InputConstants.KEY_C}
	 * @return a handle to read the key's state from client tick listeners
	 * @throws IllegalArgumentException if this mod already declared a key binding by that name
	 */
	KeyBinding keyBinding(ContentName name, int defaultKey);

	/**
	 * Adds a step to how the game works out its field of view each frame. Steps run in mod order, each given the field of
	 * view the one before returned, starting from vanilla's (which already includes the player's FOV setting, sprinting
	 * and so on).
	 *
	 * @param modifier the step
	 */
	void modifyFov(FovModifier modifier);

	/** Where to listen for game events, client ones included. */
	Events events();

	/**
	 * Reads this mod's client config file, {@code config/<mod id>-client.properties}, writing it with defaults first if
	 * needed.
	 *
	 * @param spec every setting the file holds
	 * @throws black.graft.config.ConfigException if a player's edits left it unusable; the game stops and says why
	 * @throws IllegalStateException              if this mod already loaded its client config
	 */
	Config config(ConfigSpec spec);

	/** A logger named after this mod, for its own messages. */
	System.Logger logger();
}
