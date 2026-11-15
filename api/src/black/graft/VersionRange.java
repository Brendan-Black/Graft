package black.graft;

import java.util.Objects;

/**
 * The versions a dependency accepts, written the way Maven and Forge write them:
 *
 * <table>
 * <caption>Range syntax</caption>
 * <tr><th>Written</th><th>Accepts</th></tr>
 * <tr><td>{@code [26.3,26.4)}</td><td>26.3 up to, not including, 26.4</td></tr>
 * <tr><td>{@code [0.1,)}</td><td>0.1 and anything newer</td></tr>
 * <tr><td>{@code (,2.0]}</td><td>anything up to and including 2.0</td></tr>
 * <tr><td>{@code [1.2.0]}</td><td>exactly 1.2.0</td></tr>
 * <tr><td>{@code *}</td><td>any version</td></tr>
 * </table>
 *
 * A bare version such as {@code 1.0} is refused: Maven reads it as "1.0 or anything else", which is never what's meant.
 */
public final class VersionRange {
	private static final String ANY = "*";

	private final @Nullable Version lower;
	private final boolean lowerInclusive;
	private final @Nullable Version upper;
	private final boolean upperInclusive;

	private VersionRange(@Nullable Version lower, boolean lowerInclusive, @Nullable Version upper, boolean upperInclusive) {
		this.lower = lower;
		this.lowerInclusive = lowerInclusive;
		this.upper = upper;
		this.upperInclusive = upperInclusive;
	}

	/** Accepts every version. */
	public static VersionRange any() {
		return new VersionRange(null, false, null, false);
	}

	/**
	 * Reads a range written as in the table above.
	 *
	 * @throws IllegalArgumentException if {@code text} isn't a range, or is one no version can satisfy
	 */
	public static VersionRange parse(String text) {
		String t = text.strip();
		if (t.equals(ANY)) {
			return any();
		}
		if (t.length() < 3) {
			throw invalid(text, "too short");
		}
		char open = t.charAt(0);
		char close = t.charAt(t.length() - 1);
		if ((open != '[' && open != '(') || (close != ']' && close != ')')) {
			throw invalid(text, "a range starts with [ or ( and ends with ] or )");
		}
		String body = t.substring(1, t.length() - 1);
		int comma = body.indexOf(',');
		if (comma < 0) {
			if (open != '[' || close != ']') {
				throw invalid(text, "an exact version is written [1.0.0]");
			}
			Version exact = parseBound(text, body);
			return new VersionRange(exact, true, exact, true);
		}
		if (body.indexOf(',', comma + 1) >= 0) {
			throw invalid(text, "a range has one comma");
		}
		String lowerText = body.substring(0, comma).strip();
		String upperText = body.substring(comma + 1).strip();
		Version lower = lowerText.isEmpty() ? null : parseBound(text, lowerText);
		Version upper = upperText.isEmpty() ? null : parseBound(text, upperText);
		if (lower == null && open == '[' || upper == null && close == ']') {
			throw invalid(text, "an open end is written with ( or ), not [ or ]");
		}
		if (lower != null && upper != null) {
			int c = lower.compareTo(upper);
			if (c > 0 || c == 0 && !(open == '[' && close == ']')) {
				throw invalid(text, "no version is inside it");
			}
		}
		return new VersionRange(lower, open == '[', upper, close == ']');
	}

	private static Version parseBound(String range, String bound) {
		try {
			return Version.parse(bound.strip());
		} catch (IllegalArgumentException e) {
			throw invalid(range, String.valueOf(e.getMessage()), e);
		}
	}

	private static IllegalArgumentException invalid(String text, String why) {
		return invalid(text, why, null);
	}

	private static IllegalArgumentException invalid(String text, String why, @Nullable Throwable cause) {
		return new IllegalArgumentException("not a version range: \"" + text + "\"; " + why, cause);
	}

	/** Whether {@code version} is inside this range. */
	public boolean contains(Version version) {
		if (lower != null) {
			int c = version.compareTo(lower);
			if (c < 0 || c == 0 && !lowerInclusive) {
				return false;
			}
		}
		if (upper != null) {
			int c = version.compareTo(upper);
			if (c > 0 || c == 0 && !upperInclusive) {
				return false;
			}
		}
		return true;
	}

	@Override
	public boolean equals(Object o) {
		return o instanceof VersionRange r && Objects.equals(lower, r.lower) && lowerInclusive == r.lowerInclusive
			&& Objects.equals(upper, r.upper) && upperInclusive == r.upperInclusive;
	}

	@Override
	public int hashCode() {
		return Objects.hash(lower, lowerInclusive, upper, upperInclusive);
	}

	/** The range in the syntax it's read in, with versions written in full: {@code [26.3.0,26.4.0)}. */
	@Override
	public String toString() {
		if (lower == null && upper == null) {
			return ANY;
		}
		if (lower != null && lower.equals(upper)) {
			return "[" + lower + "]";
		}
		return (lowerInclusive ? "[" : "(") + (lower == null ? "" : lower) + "," + (upper == null ? "" : upper)
			+ (upperInclusive ? "]" : ")");
	}
}
