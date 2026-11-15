package black.graft.loader;

import black.graft.internal.ErrorCode;
import black.graft.internal.LoadingException;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

import black.graft.Version;
import black.graft.json.Json;

/** Works out which Minecraft and which Graft are running, before any of Minecraft's classes load. */
public final class Versions {
	static final String MINECRAFT_RESOURCE = "version.json";
	static final String GRAFT_RESOURCE = "black/graft/loader/graft.properties";

	private Versions() {}

	/**
	 * Minecraft's version, from the {@code version.json} at the root of its jar.
	 *
	 * @param in that file, or null if it isn't on the class path
	 * @throws LoadingException {@link ErrorCode#UNKNOWN_MINECRAFT} if it's missing, unreadable, or a snapshot
	 */
	public static Version minecraft(@black.graft.Nullable InputStream in) {
		if (in == null) {
			throw new LoadingException(ErrorCode.UNKNOWN_MINECRAFT,
				"Minecraft's version.json isn't on the class path: is Graft running alongside the game?");
		}
		String id;
		try (in) {
			id = Json.parseObject(new String(in.readAllBytes(), StandardCharsets.UTF_8)).string("id");
		} catch (IOException | Json.JsonException e) {
			throw new LoadingException(ErrorCode.UNKNOWN_MINECRAFT, "Minecraft's version.json can't be read: " + e.getMessage(), e);
		}
		try {
			return Version.parse(id);
		} catch (IllegalArgumentException e) {
			throw new LoadingException(ErrorCode.UNKNOWN_MINECRAFT,
				"Minecraft " + id + " isn't a release version, and Graft only runs on releases", e);
		}
	}

	/**
	 * Graft's own version, which its build writes into the jar.
	 *
	 * @param in {@code black/graft/loader/graft.properties}, or null if it isn't on the class path
	 */
	public static Version graft(@black.graft.Nullable InputStream in) {
		if (in == null) {
			throw new IllegalStateException(GRAFT_RESOURCE + " is missing: Graft's jar wasn't built by its own build");
		}
		Properties p = new Properties();
		try (in) {
			p.load(new InputStreamReader(in, StandardCharsets.UTF_8));
		} catch (IOException e) {
			throw new IllegalStateException(GRAFT_RESOURCE + " can't be read", e);
		}
		return Version.parse(p.getProperty("version", "missing"));
	}
}
