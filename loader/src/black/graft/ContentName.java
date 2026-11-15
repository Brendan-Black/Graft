package black.graft;

import java.util.regex.Pattern;

import black.TypedString;

/**
 * The name of one thing a mod adds, unique within the mod: {@code zoom_block} in {@code truezoom:zoom_block}. It's the path
 * half of a Minecraft identifier, so it takes what those take: lower-case ASCII letters, digits, and {@code _ - . /}.
 *
 * @param value the name, such as {@code zoom_block}
 */
public record ContentName(String value) implements TypedString {
	private static final Pattern SHAPE = Pattern.compile("[a-z0-9_.\\-/]{1,128}");

	/**
	 * @throws IllegalArgumentException if {@code value} isn't a valid content name
	 */
	public ContentName {
		if (!SHAPE.matcher(value).matches()) {
			throw new IllegalArgumentException(
				"not a content name: \"" + value + "\"; expected 1 to 128 of a-z, 0-9, _ - . and /");
		}
	}

	@Override
	public String toString() {
		return value;
	}
}
