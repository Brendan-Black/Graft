package black.graft;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A release version of a mod, of Graft, or of Minecraft: one to three dot-separated numbers, such as {@code 26.3},
 * {@code 1.0.0} or {@code 26.3.1}. Missing parts are zero, so {@code 26.3} and {@code 26.3.0} are the same version.
 * Pre-release suffixes ({@code -beta}, {@code -snapshot-8}) aren't versions Graft can order, and are refused.
 *
 * @param major the first number
 * @param minor the second number, 0 if not written
 * @param patch the third number, 0 if not written
 */
public record Version(int major, int minor, int patch) implements Comparable<Version> {
	private static final Pattern SHAPE = Pattern.compile("(0|[1-9]\\d{0,8})(?:\\.(0|[1-9]\\d{0,8}))?(?:\\.(0|[1-9]\\d{0,8}))?");

	/**
	 * @throws IllegalArgumentException if any part is negative
	 */
	public Version {
		if (major < 0 || minor < 0 || patch < 0) {
			throw new IllegalArgumentException("a version can't have a negative part: " + major + "." + minor + "." + patch);
		}
	}

	/**
	 * Reads a version as written in a descriptor or by Minecraft.
	 *
	 * @throws IllegalArgumentException if {@code text} isn't one to three dot-separated numbers
	 */
	public static Version parse(String text) {
		Matcher m = SHAPE.matcher(text);
		if (!m.matches()) {
			throw new IllegalArgumentException("not a version: \"" + text + "\"; expected one to three numbers such as 1.0.0 or 26.3");
		}
		return new Version(part(m.group(1)), part(m.group(2)), part(m.group(3)));
	}

	private static int part(@Nullable String group) {
		return group == null ? 0 : Integer.parseInt(group);
	}

	@Override
	public int compareTo(Version other) {
		int c = Integer.compare(major, other.major);
		if (c == 0) {
			c = Integer.compare(minor, other.minor);
		}
		if (c == 0) {
			c = Integer.compare(patch, other.patch);
		}
		return c;
	}

	/** The version in full, {@code 26.3.0}. */
	@Override
	public String toString() {
		return major + "." + minor + "." + patch;
	}
}
