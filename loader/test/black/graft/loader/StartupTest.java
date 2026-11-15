package black.graft.loader;

import black.graft.internal.ErrorCode;
import black.graft.internal.LoadingException;

import static black.graft.Check.eq;
import static black.graft.Check.fails;
import static black.graft.Check.ok;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import black.graft.Dist;
import black.graft.Version;

public final class StartupTest {
	private StartupTest() {}

	public static void run() {
		// Environment
		Path game = Path.of("/games/mc").toAbsolutePath();
		Environment launcher = Environment.read(Map.of("user.dir", game.toString()), _ -> true);
		eq(game.resolve("mods"), launcher.modsDir(), "mods default to the game folder's");
		eq(game.resolve("config"), launcher.configDir(), "config is in the game folder");
		eq(Dist.CLIENT, launcher.dist(), "the client's classes mean a client");
		eq(List.of(), launcher.devRoots(), "no development folders by default");
		eq(Dist.SERVER, Environment.read(Map.of("user.dir", game.toString()), _ -> false).dist(), "no client classes means a server");

		Environment dev = Environment.read(Map.of(
			"user.dir", game.toString(),
			Environment.MODS_PROPERTY, "/elsewhere/mods",
			Environment.DIST_PROPERTY, " Server ",
			Environment.DEV_MODS_PROPERTY, "/a" + File.pathSeparator + File.pathSeparator + "/b"), _ -> true);
		eq(Path.of("/elsewhere/mods").toAbsolutePath(), dev.modsDir(), "graft.mods overrides");
		eq(Dist.SERVER, dev.dist(), "graft.dist overrides, any case");
		eq(List.of(Path.of("/a").toAbsolutePath(), Path.of("/b").toAbsolutePath()), dev.devRoots(), "development folders, blanks skipped");
		ok(fails(IllegalArgumentException.class, () -> Environment.read(Map.of(Environment.DIST_PROPERTY, "both"), _ -> true), "bad dist")
			.getMessage().contains("expected client or server"), "a bad graft.dist says what fits");

		// Versions
		eq(Version.parse("26.3"), Versions.minecraft(stream("{\"id\": \"26.3\", \"name\": \"26.3\", \"world_version\": 5023}")),
			"Minecraft's version from version.json");
		LoadingException snapshot = fails(LoadingException.class, () -> Versions.minecraft(stream("{\"id\": \"26.4-snapshot-8\"}")), "snapshot");
		eq(ErrorCode.UNKNOWN_MINECRAFT, snapshot.code(), "a snapshot isn't supported");
		ok(snapshot.getMessage().contains("26.4-snapshot-8"), "and is named");
		eq(ErrorCode.UNKNOWN_MINECRAFT, fails(LoadingException.class, () -> Versions.minecraft(null), "absent").code(),
			"no version.json means no Minecraft");
		eq(ErrorCode.UNKNOWN_MINECRAFT, fails(LoadingException.class, () -> Versions.minecraft(stream("{\"name\": 1")), "garbled").code(),
			"a garbled version.json");
		eq(Version.parse("0.1.0"), Versions.graft(stream("version=0.1.0\n")), "Graft's version from its jar");
		fails(IllegalStateException.class, () -> Versions.graft(null), "Graft's version file is required");
	}

	private static InputStream stream(String text) {
		return new ByteArrayInputStream(text.getBytes(StandardCharsets.UTF_8));
	}
}
