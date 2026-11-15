package black.graft.loader;

import black.graft.internal.ErrorCode;
import black.graft.internal.LoadingException;

import static black.graft.Check.eq;
import static black.graft.Check.fails;
import static black.graft.Check.ok;
import static black.graft.Check.tempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class BundlerTest {
	private static final String SERVER = "server jar bytes";
	private static final String LIBRARY = "library jar bytes";

	private BundlerTest() {}

	public static void run() throws IOException {
		Path dir = tempDir("bundler");
		Path bundle = bundle(dir.resolve("server.jar"), sha256(SERVER), sha256(LIBRARY), "com/example/lib/1.0/lib-1.0.jar");
		Path into = Files.createDirectory(dir.resolve("game"));

		Bundler.Unpacked unpacked = Bundler.unpack(bundle, into);
		eq("net.minecraft.server.Main", unpacked.mainClass(), "the main class, from META-INF/main-class");
		Path server = into.resolve("versions/26.3/server-26.3.jar");
		Path library = into.resolve("libraries/com/example/lib/1.0/lib-1.0.jar");
		eq(List.of(server, library), unpacked.classPath(), "the server jar, then its libraries");
		eq(SERVER, Files.readString(server), "the server jar is unpacked");
		eq(LIBRARY, Files.readString(library), "a library is unpacked");

		Files.writeString(library, "a stale or damaged copy");
		Bundler.unpack(bundle, into);
		eq(LIBRARY, Files.readString(library), "a file that doesn't match its checksum is replaced");

		// A tampered bundle: the library's bytes don't match the list. Nothing of it is kept.
		Path tampered = bundle(dir.resolve("tampered.jar"), sha256(SERVER), sha256("something else"), "com/example/other/1.0/other-1.0.jar");
		LoadingException mismatch = fails(LoadingException.class, () -> Bundler.unpack(tampered, into), "tampered");
		eq(ErrorCode.CHECKSUM_MISMATCH, mismatch.code(), "a checksum mismatch");
		ok(!Files.exists(into.resolve("libraries/com/example/other")) || isEmptyTree(into.resolve("libraries/com/example/other")),
			"nothing is left of the file that failed its check");

		Path escaping = bundle(dir.resolve("escaping.jar"), sha256(SERVER), sha256(LIBRARY), "../../outside.jar");
		eq(ErrorCode.BAD_SERVER_JAR, fails(LoadingException.class, () -> Bundler.unpack(escaping, into), "escape").code(),
			"a path out of libraries/ is refused");
		ok(!Files.exists(dir.resolve("outside.jar")), "and nothing is written outside");

		Path notBundle = Mods.jar(dir.resolve("plain.jar"), Map.of("a.txt", "a"));
		ok(fails(LoadingException.class, () -> Bundler.unpack(notBundle, into), "plain jar").getMessage().contains("isn't a Minecraft server bundle"),
			"a jar that isn't a bundle says so");
		ok(fails(LoadingException.class, () -> Bundler.unpack(dir.resolve("missing.jar"), into), "missing").getMessage().contains("--server-jar"),
			"a missing server jar says how to name one");
	}

	private static Path bundle(Path file, String serverSha, String librarySha, String libraryPath) throws IOException {
		Map<String, String> entries = new LinkedHashMap<>();
		entries.put("META-INF/main-class", "net.minecraft.server.Main");
		entries.put("META-INF/versions.list", serverSha + "\t26.3\t26.3/server-26.3.jar\n");
		entries.put("META-INF/libraries.list", librarySha + "\tcom.example:lib:1.0\t" + libraryPath + "\n");
		entries.put("META-INF/versions/26.3/server-26.3.jar", SERVER);
		entries.put("META-INF/libraries/" + libraryPath, LIBRARY);
		return Mods.jar(file, entries);
	}

	private static boolean isEmptyTree(Path dir) throws IOException {
		try (var files = Files.walk(dir)) {
			return files.noneMatch(Files::isRegularFile);
		}
	}

	private static String sha256(String text) {
		try {
			return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(text.getBytes(StandardCharsets.UTF_8)));
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException(e);
		}
	}
}
