package black.graft.internal;

import java.util.regex.Pattern;

import black.version.Version;

/**
 * The versions Graft reads, in descriptors, ranges and from Minecraft: release versions of one to three dot-separated
 * numbers, such as {@code 26.3}, {@code 1.0.0} or {@code 26.3.1}. Pre-release suffixes ({@code -beta},
 * {@code -snapshot-8}) aren't releases, and are refused, as are a leading {@code v} and leading zeros.
 *
 * <p>What a version is, and how two compare, is {@link Version}'s: {@code 26.3.0} is greater than {@code 26.3}, the more
 * precise of two otherwise equal versions.
 */
public final class Releases {
	private static final Pattern SHAPE = Pattern.compile("(0|[1-9]\\d{0,8})(?:\\.(0|[1-9]\\d{0,8})){0,2}");

	private Releases() {}

	/**
	 * Reads a release version as written in a descriptor or by Minecraft.
	 *
	 * @throws IllegalArgumentException if {@code text} isn't one to three dot-separated numbers
	 */
	public static Version parse(String text) {
		if (!SHAPE.matcher(text).matches()) {
			throw new IllegalArgumentException("not a version: \"" + text + "\"; expected one to three numbers such as 1.0.0 or 26.3");
		}
		return new Version(text);
	}

	/**
	 * A release padded to three parts, {@code 26.3} as {@code 26.3.0}, for loaders that read the two as the same version
	 * (Fabric, NeoForge) and whose metadata Graft has always written in full.
	 */
	public static String full(Version version) {
		StringBuilder out = new StringBuilder(version.toString());
		for (int parts = out.toString().split("\\.").length; parts < 3; parts++) {
			out.append(".0");
		}
		return out.toString();
	}
}
