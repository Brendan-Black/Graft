package black.graft.config;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.TreeSet;

/**
 * A mod's settings, read from its config file. Mods get one from their setup; {@link #load} is public so tools and tests
 * can read a file the same way.
 *
 * <p>The file is a {@code .properties} file with a comment above each setting. Graft writes it with defaults the first
 * time, and adds any setting a newer version of the mod brings, keeping the values a player set. A key it doesn't know or a
 * value out of range stops the game with a message saying which, rather than being quietly replaced: the player meant
 * something by it.
 */
public final class Config {
	private final ConfigSpec spec;
	private final Map<String, Object> values;

	private Config(ConfigSpec spec, Map<String, Object> values) {
		this.spec = spec;
		this.values = Map.copyOf(values);
	}

	/**
	 * Reads {@code file}, writing it first if it's missing or lacks some of the settings.
	 *
	 * @param spec  the settings the file holds
	 * @param file  the file, such as {@code config/truezoom.properties}
	 * @param title a line for the top of the file, such as "True Zoom settings"
	 * @throws ConfigException if the file has unknown keys or values that don't fit, or can't be read or written
	 */
	public static Config load(ConfigSpec spec, Path file, String title) {
		Properties raw = new Properties();
		boolean exists = Files.exists(file);
		if (exists) {
			try (Reader in = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
				raw.load(in);
			} catch (IOException | IllegalArgumentException e) {
				throw new ConfigException(file, "can't be read: " + e.getMessage(), e);
			}
		}

		List<String> problems = new ArrayList<>();
		Map<String, Object> values = new HashMap<>();
		Map<String, Setting<?>> byKey = new HashMap<>();
		for (Setting<?> s : spec.settings()) {
			byKey.put(s.key(), s);
		}
		for (String key : new TreeSet<>(raw.stringPropertyNames())) {
			if (!byKey.containsKey(key)) {
				problems.add("unknown setting \"" + key + "\"; the settings are " + byKey.keySet().stream().sorted().toList());
			}
		}
		boolean missing = false;
		for (Setting<?> s : spec.settings()) {
			String given = raw.getProperty(s.key());
			if (given == null) {
				values.put(s.key(), s.defaultValue());
				missing = true;
				continue;
			}
			try {
				values.put(s.key(), s.parse(given));
			} catch (IllegalArgumentException e) {
				problems.add(s.key() + ": " + e.getMessage() + "; expected " + s.describe());
			}
		}
		if (!problems.isEmpty()) {
			throw new ConfigException(file, problems);
		}

		Config config = new Config(spec, values);
		if (!exists || missing) {
			config.write(file, title);
		}
		return config;
	}

	/**
	 * The value a setting has.
	 *
	 * @throws IllegalArgumentException if {@code setting} isn't in this config's spec
	 */
	public <T> T get(Setting<T> setting) {
		if (!spec.settings().contains(setting)) {
			throw new IllegalArgumentException("setting " + setting.key() + " isn't in this config; its settings are "
				+ spec.settings().stream().map(Setting::key).toList());
		}
		Object value = values.get(setting.key());
		if (value == null) {
			throw new IllegalStateException("setting " + setting.key() + " has no value, which load() never allows");
		}
		@SuppressWarnings("unchecked") // load() stored what this setting's own parse() returned
		T typed = (T) value;
		return typed;
	}

	/** Writes the whole file to a temporary sibling and moves it into place, so a crash never leaves half a file. */
	private void write(Path file, String title) {
		StringBuilder out = new StringBuilder();
		out.append("# ").append(title.replace('\n', ' ')).append('\n');
		out.append("# Graft adds settings this file is missing and keeps the values you set.\n");
		for (Setting<?> s : spec.settings()) {
			out.append('\n');
			for (String line : s.comment().split("\n", -1)) {
				out.append("# ").append(line).append('\n');
			}
			out.append("# ").append(s.describe()).append('\n');
			out.append(s.key()).append('=').append(formatted(s)).append('\n');
		}
		try {
			Path dir = file.toAbsolutePath().getParent();
			Files.createDirectories(dir);
			Path temp = Files.createTempFile(dir, file.getFileName().toString(), ".tmp");
			try {
				Files.writeString(temp, out, StandardCharsets.UTF_8);
				try {
					Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
				} catch (AtomicMoveNotSupportedException e) {
					// Falling back on purpose: some file systems can't rename atomically; a plain replace is the best left.
					Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING);
				}
			} finally {
				Files.deleteIfExists(temp);
			}
		} catch (IOException e) {
			throw new ConfigException(file, "can't be written: " + e.getMessage(), e);
		}
	}

	private <T> String formatted(Setting<T> setting) {
		return setting.format(get(setting));
	}
}
