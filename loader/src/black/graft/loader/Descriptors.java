package black.graft.loader;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;

import black.graft.Dependency;
import black.graft.ModId;
import black.graft.ModInfo;
import black.graft.Nullable;
import black.graft.Version;
import black.graft.VersionRange;

/** Reads {@code graft.mod.properties}, the descriptor at the root of every mod. */
public final class Descriptors {
	/** Where the descriptor is, relative to the mod's root. */
	public static final String FILE_NAME = "graft.mod.properties";

	private static final String DEPENDS = "depends.";
	private static final String ID = "id";
	private static final String NAME = "name";
	private static final String VERSION = "version";
	private static final String DESCRIPTION = "description";
	private static final String AUTHORS = "authors";
	private static final String LICENSE = "license";
	private static final String ENTRYPOINT = "entrypoint";
	private static final String CLIENT_ENTRYPOINT = "client-entrypoint";
	private static final Set<String> KEYS = Set.of(ID, NAME, VERSION, DESCRIPTION, AUTHORS, LICENSE, ENTRYPOINT, CLIENT_ENTRYPOINT);
	private static final Pattern CLASS_NAME = Pattern.compile("[\\p{L}_$][\\p{L}\\p{N}_$]*(\\.[\\p{L}_$][\\p{L}\\p{N}_$]*)*");

	private Descriptors() {}

	/**
	 * Reads a descriptor.
	 *
	 * @param in     the file's bytes, UTF-8
	 * @param origin where it came from, for messages: a jar or folder
	 * @throws LoadingException with {@link ErrorCode#BAD_DESCRIPTOR}, listing every problem in it
	 */
	public static ModInfo read(InputStream in, String origin) {
		Properties p = new Properties();
		try {
			p.load(new InputStreamReader(in, StandardCharsets.UTF_8));
		} catch (IOException | IllegalArgumentException e) {
			throw new LoadingException(ErrorCode.BAD_DESCRIPTOR, origin + ": " + FILE_NAME + " can't be read: " + e.getMessage(), e);
		}
		return parse(p, origin);
	}

	static ModInfo parse(Properties p, String origin) {
		List<String> problems = new ArrayList<>();
		for (String key : new TreeSet<>(p.stringPropertyNames())) {
			if (!KEYS.contains(key) && !key.startsWith(DEPENDS)) {
				problems.add(origin + ": unknown key \"" + key + "\"; the keys are " + new TreeSet<>(KEYS) + " and depends.<mod id>");
			}
		}

		ModId id = required(p, ID, origin, problems, ModId::new);
		String name = required(p, NAME, origin, problems, s -> {
			if (s.isBlank()) {
				throw new IllegalArgumentException("is blank");
			}
			return s.strip();
		});
		Version version = required(p, VERSION, origin, problems, Version::parse);
		if (id != null && (id.equals(ModId.MINECRAFT) || id.equals(ModId.GRAFT))) {
			problems.add(origin + ": the id \"" + id + "\" is reserved for " + (id.equals(ModId.GRAFT) ? "Graft" : "the game"));
		}

		String entrypoint = className(p, ENTRYPOINT, origin, problems);
		String clientEntrypoint = className(p, CLIENT_ENTRYPOINT, origin, problems);

		List<Dependency> dependencies = new ArrayList<>();
		for (String key : new TreeSet<>(p.stringPropertyNames())) {
			if (!key.startsWith(DEPENDS)) {
				continue;
			}
			String target = key.substring(DEPENDS.length());
			String raw = p.getProperty(key);
			try {
				ModId dependsOn = new ModId(target);
				if (dependsOn.equals(id)) {
					problems.add(origin + ": " + key + ": a mod can't depend on itself");
					continue;
				}
				dependencies.add(new Dependency(dependsOn, VersionRange.parse(raw)));
			} catch (IllegalArgumentException e) {
				problems.add(origin + ": " + key + ": " + e.getMessage());
			}
		}

		if (!problems.isEmpty() || id == null || name == null || version == null) {
			throw new LoadingException(ErrorCode.BAD_DESCRIPTOR, problems);
		}
		List<String> authors = Arrays.stream(p.getProperty(AUTHORS, "").split(",")).map(String::strip).filter(s -> !s.isEmpty()).toList();
		return new ModInfo(
			id,
			name,
			version,
			p.getProperty(DESCRIPTION, "").strip(),
			authors,
			p.getProperty(LICENSE, "").strip(),
			entrypoint,
			clientEntrypoint,
			dependencies);
	}

	/** Reads a required key, adding a problem (and returning null) if it's missing or invalid. */
	private static <T> @Nullable T required(Properties p, String key, String origin, List<String> problems, Parser<T> parser) {
		String raw = p.getProperty(key);
		if (raw == null) {
			problems.add(origin + ": \"" + key + "\" is required");
			return null;
		}
		try {
			return parser.parse(raw.strip());
		} catch (IllegalArgumentException e) {
			problems.add(origin + ": " + key + ": " + e.getMessage());
			return null;
		}
	}

	private static @Nullable String className(Properties p, String key, String origin, List<String> problems) {
		String raw = p.getProperty(key);
		if (raw == null) {
			return null;
		}
		String name = raw.strip();
		if (!CLASS_NAME.matcher(name).matches()) {
			problems.add(origin + ": " + key + ": \"" + raw + "\" isn't a class name such as com.example.MyMod");
			return null;
		}
		return name;
	}

	@FunctionalInterface
	private interface Parser<T> {
		T parse(String raw);
	}
}
