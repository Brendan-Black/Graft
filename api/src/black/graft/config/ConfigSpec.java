package black.graft.config;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Every setting in one config file, in the order the file lists them.
 *
 * @param settings the settings; no two share a key
 */
public record ConfigSpec(List<Setting<?>> settings) {
	/**
	 * @throws IllegalArgumentException if two settings share a key
	 */
	public ConfigSpec {
		settings = List.copyOf(settings);
		Set<String> keys = new HashSet<>();
		for (Setting<?> s : settings) {
			if (!keys.add(s.key())) {
				throw new IllegalArgumentException("two settings have the key " + s.key());
			}
		}
	}

	/** A spec of these settings, in this order. */
	public static ConfigSpec of(Setting<?>... settings) {
		return new ConfigSpec(List.of(settings));
	}
}
