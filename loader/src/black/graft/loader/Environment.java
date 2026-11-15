package black.graft.loader;

import java.io.File;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Predicate;

import black.graft.Dist;

/**
 * Where the game is, and how Graft was started. Read from system properties, which {@code runClient}, {@code runServer}
 * and {@code java -jar graft.jar server} set; a player's launcher sets none, and the defaults fit it.
 *
 * @param gameDir   the game folder: the working directory, as the Minecraft launcher sets it
 * @param modsDir   {@code graft.mods}, or {@code mods} in the game folder
 * @param configDir {@code config} in the game folder
 * @param devRoots  {@code graft.dev.mods}: development folders, separated like a class path
 * @param dist      {@code graft.dist} ({@code client} or {@code server}), or {@code client} when the client's classes are there
 */
public record Environment(Path gameDir, Path modsDir, Path configDir, List<Path> devRoots, Dist dist) {
	public static final String MODS_PROPERTY = "graft.mods";
	public static final String DEV_MODS_PROPERTY = "graft.dev.mods";
	public static final String DIST_PROPERTY = "graft.dist";
	private static final String CLIENT_CLASS = "net/minecraft/client/Minecraft.class";

	public Environment {
		devRoots = List.copyOf(devRoots);
	}

	/**
	 * Reads the environment.
	 *
	 * @param properties the system properties, or test values
	 * @param hasClass   whether a class file resource is on the class path, such as {@code net/minecraft/client/Minecraft.class}
	 */
	public static Environment read(Map<String, String> properties, Predicate<String> hasClass) {
		Path gameDir = Path.of(properties.getOrDefault("user.dir", ".")).toAbsolutePath();
		String mods = properties.get(MODS_PROPERTY);
		Path modsDir = mods != null ? Path.of(mods).toAbsolutePath() : gameDir.resolve("mods");
		String dev = properties.getOrDefault(DEV_MODS_PROPERTY, "");
		List<Path> devRoots = Arrays.stream(dev.split(File.pathSeparator)).filter(s -> !s.isBlank()).map(s -> Path.of(s).toAbsolutePath()).toList();
		String distName = properties.get(DIST_PROPERTY);
		Dist dist;
		if (distName == null) {
			dist = hasClass.test(CLIENT_CLASS) ? Dist.CLIENT : Dist.SERVER;
		} else {
			try {
				dist = Dist.valueOf(distName.strip().toUpperCase(Locale.ROOT));
			} catch (IllegalArgumentException e) {
				throw new IllegalArgumentException(DIST_PROPERTY + " is \"" + distName + "\"; expected client or server", e);
			}
		}
		return new Environment(gameDir, modsDir, gameDir.resolve("config"), devRoots, dist);
	}
}
