package black.graft.loader;

import black.graft.internal.LoadingException;
import black.graft.internal.Descriptors;

import static black.graft.Check.eq;
import static black.graft.Check.fails;
import static black.graft.Check.ok;
import static black.graft.Check.tempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

public final class DiscoveryTest {
	private DiscoveryTest() {}

	public static void run() throws IOException {
		Path game = tempDir("discovery");
		Path mods = Files.createDirectory(game.resolve("mods"));
		Mods.jar(mods.resolve("b-mod.jar"), Map.of(Descriptors.FILE_NAME, Mods.descriptor("bmod", "1.0"), "assets/bmod/lang/en_us.json", "{}"));
		Mods.jar(mods.resolve("a-mod.jar"), Map.of(Descriptors.FILE_NAME, Mods.descriptor("amod", "2.0")));
		Files.writeString(mods.resolve("notes.txt"), "not a jar, left alone");
		Files.createDirectory(mods.resolve("folder.jar"));
		Path dev = Files.createDirectory(game.resolve("dev"));
		Files.writeString(dev.resolve(Descriptors.FILE_NAME), Mods.descriptor("devmod", "0.1"));

		List<ModCandidate> found = Discovery.discover(mods, List.of(dev));
		eq(List.of("devmod", "amod", "bmod"), Mods.ids(found), "development folders first, then jars by file name");
		eq(ModCandidate.Kind.DIRECTORY, found.get(0).kind(), "a development folder");
		eq(ModCandidate.Kind.JAR, found.get(1).kind(), "a jar");
		eq(mods.resolve("a-mod.jar"), found.get(1).source(), "a jar's source is its path");

		eq(List.of(), Discovery.discover(game.resolve("no-such-folder"), List.of()), "no mods folder means no mods");

		// Every bad file is reported together, so a player fixes the folder in one go.
		Mods.jar(mods.resolve("fabric-mod.jar"), Map.of("fabric.mod.json", "{}"));
		Files.writeString(mods.resolve("broken.jar"), "this isn't a zip");
		Mods.jar(mods.resolve("typo.jar"), Map.of(Descriptors.FILE_NAME, "id=typo\nname=Typo\nversion=one\n"));
		LoadingException e = fails(LoadingException.class, () -> Discovery.discover(mods, List.of(dev)), "bad jars fail discovery");
		eq(3, e.problems().size(), "all three problems: " + e.problems());
		ok(e.problems().stream().anyMatch(p -> p.contains("fabric-mod.jar isn't a Graft mod") && p.contains("another loader")),
			"a jar without a descriptor is called out, with a hint");
		ok(e.problems().stream().anyMatch(p -> p.contains("broken.jar isn't a readable jar")), "a corrupt jar is named");
		ok(e.problems().stream().anyMatch(p -> p.contains("typo.jar") && p.contains("version")), "a bad descriptor names its jar");
		eq(3, e.getSuppressed().length, "each underlying failure is kept");

		Path emptyDev = Files.createDirectory(game.resolve("empty-dev"));
		ok(fails(LoadingException.class, () -> Discovery.discover(game.resolve("none"), List.of(emptyDev)), "missing descriptor")
			.getMessage().contains("no readable " + Descriptors.FILE_NAME), "a development folder without a descriptor is named");
	}
}
