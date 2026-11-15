package black.graft.config;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * One setting in a mod's config file: its key, its type, the values it accepts and its default. Declare each as a
 * constant, list them in a {@link ConfigSpec}, and read them from the loaded {@link Config}:
 *
 * <pre>{@code
 * static final Setting.Decimal FACTOR = new Setting.Decimal("zoom_factor", 4.0, 1.0, 50.0,
 *         "How many times closer the view gets while zooming.");
 * static final ConfigSpec SPEC = ConfigSpec.of(FACTOR);
 * ...
 * double factor = config.get(FACTOR);
 * }</pre>
 *
 * @param <T> the value's type
 */
public sealed interface Setting<T> {
	/** The key it has in the file: letters, digits, {@code _ . -}, starting with a letter. */
	String key();

	/** The value it has until a player changes it. */
	T defaultValue();

	/** A sentence for the player, written above it in the file. */
	String comment();

	/**
	 * Reads a value as a player wrote it.
	 *
	 * @throws IllegalArgumentException if it isn't one this setting accepts, saying what is
	 */
	T parse(String raw);

	/** Writes a value the way {@link #parse} reads it. */
	String format(T value);

	/** What values it accepts, and its default, for the comment line in the file. */
	String describe();

	/**
	 * {@code true} or {@code false}.
	 *
	 * @param key          the key in the file
	 * @param defaultValue the value until a player changes it
	 * @param comment      a sentence for the player
	 */
	record Flag(String key, Boolean defaultValue, String comment) implements Setting<Boolean> {
		public Flag {
			Keys.check(key);
		}

		@Override
		public Boolean parse(String raw) {
			return switch (raw.strip().toLowerCase(Locale.ROOT)) {
				case "true" -> true;
				case "false" -> false;
				default -> throw new IllegalArgumentException("\"" + raw + "\" isn't true or false");
			};
		}

		@Override
		public String format(Boolean value) {
			return value.toString();
		}

		@Override
		public String describe() {
			return "true or false, default " + defaultValue + ".";
		}
	}

	/**
	 * A whole number from {@code min} to {@code max}, both included.
	 *
	 * @param key          the key in the file
	 * @param defaultValue the value until a player changes it; inside the range
	 * @param min          the smallest value accepted
	 * @param max          the largest value accepted
	 * @param comment      a sentence for the player
	 */
	record WholeNumber(String key, Integer defaultValue, int min, int max, String comment) implements Setting<Integer> {
		public WholeNumber {
			Keys.check(key);
			if (min > max || defaultValue < min || defaultValue > max) {
				throw new IllegalArgumentException(
					"setting " + key + ": default " + defaultValue + " must be inside " + min + " to " + max);
			}
		}

		@Override
		public Integer parse(String raw) {
			int value;
			try {
				value = Integer.parseInt(raw.strip());
			} catch (NumberFormatException e) {
				throw new IllegalArgumentException("\"" + raw + "\" isn't a whole number", e);
			}
			if (value < min || value > max) {
				throw new IllegalArgumentException(value + " isn't from " + min + " to " + max);
			}
			return value;
		}

		@Override
		public String format(Integer value) {
			return value.toString();
		}

		@Override
		public String describe() {
			return min + " to " + max + ", default " + defaultValue + ".";
		}
	}

	/**
	 * A number from {@code min} to {@code max}, both included.
	 *
	 * @param key          the key in the file
	 * @param defaultValue the value until a player changes it; inside the range
	 * @param min          the smallest value accepted
	 * @param max          the largest value accepted
	 * @param comment      a sentence for the player
	 */
	record Decimal(String key, Double defaultValue, double min, double max, String comment) implements Setting<Double> {
		public Decimal {
			Keys.check(key);
			if (!Double.isFinite(min) || !Double.isFinite(max) || min > max || !(defaultValue >= min && defaultValue <= max)) {
				throw new IllegalArgumentException(
					"setting " + key + ": default " + defaultValue + " must be inside " + min + " to " + max);
			}
		}

		@Override
		public Double parse(String raw) {
			double value;
			try {
				value = Double.parseDouble(raw.strip());
			} catch (NumberFormatException e) {
				throw new IllegalArgumentException("\"" + raw + "\" isn't a number", e);
			}
			if (!(value >= min && value <= max)) {
				throw new IllegalArgumentException(raw.strip() + " isn't from " + min + " to " + max);
			}
			return value;
		}

		@Override
		public String format(Double value) {
			return value.toString();
		}

		@Override
		public String describe() {
			return min + " to " + max + ", default " + defaultValue + ".";
		}
	}
}

/** The shape every setting key has. */
final class Keys {
	private static final Pattern SHAPE = Pattern.compile("[A-Za-z][A-Za-z0-9_.\\-]{0,63}");

	private Keys() {}

	static void check(String key) {
		if (!SHAPE.matcher(key).matches()) {
			throw new IllegalArgumentException(
				"not a setting key: \"" + key + "\"; expected letters, digits, _ . and -, starting with a letter");
		}
	}
}
