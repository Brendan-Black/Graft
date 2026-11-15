package black.graft;

import java.util.regex.Pattern;

import black.TypedString;

/**
 * A mod's id: the namespace of everything it adds ({@code truezoom:zoom_block}) and the name other mods depend on it by.
 * Lower-case ASCII letters, digits and {@code _}, starting with a letter, 2 to 64 characters long. {@code minecraft} and
 * {@code graft} are taken by the game and the loader.
 *
 * @param value the id, such as {@code truezoom}
 */
public record ModId(String value) implements TypedString, Comparable<ModId> {
	private static final Pattern SHAPE = Pattern.compile("[a-z][a-z0-9_]{1,63}");

	/** The game itself, which every mod can depend on by version. */
	public static final ModId MINECRAFT = new ModId("minecraft");

	/** This loader, which every mod can depend on by version. */
	public static final ModId GRAFT = new ModId("graft");

	/**
	 * @throws IllegalArgumentException if {@code value} isn't a valid mod id
	 */
	public ModId {
		if (!SHAPE.matcher(value).matches()) {
			throw new IllegalArgumentException(
				"not a mod id: \"" + value + "\"; expected 2 to 64 of a-z, 0-9 and _, starting with a letter");
		}
	}

	@Override
	public int compareTo(ModId other) {
		return value.compareTo(other.value);
	}

	@Override
	public String toString() {
		return value;
	}
}
