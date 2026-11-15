package black.graft.gradle;

import static black.graft.gradle.Check.eq;
import static black.graft.gradle.Check.fails;
import static black.graft.gradle.Check.ok;
import static black.graft.gradle.Check.tempDir;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import black.graft.json.Json;

final class PluginTest {
	private static final Platform LINUX = new Platform("linux", "amd64");
	private static final Platform MAC = new Platform("osx", "aarch64");

	private PluginTest() {}

	static void platform() {
		ok(LINUX.allows(Optional.empty()), "no rules: allowed");
		Optional<Json> macOnly = Optional.of(Json.parse("[{\"action\":\"allow\",\"os\":{\"name\":\"osx\"}}]"));
		ok(MAC.allows(macOnly) && !LINUX.allows(macOnly), "an allow rule for one OS");
		Optional<Json> notMac = Optional.of(Json.parse("[{\"action\":\"allow\"},{\"action\":\"disallow\",\"os\":{\"name\":\"osx\"}}]"));
		ok(LINUX.allows(notMac) && !MAC.allows(notMac), "the last matching rule decides");
		Optional<Json> x86 = Optional.of(Json.parse("[{\"action\":\"allow\",\"os\":{\"arch\":\"x86\"}}]"));
		ok(!LINUX.allows(x86) && new Platform("linux", "i386").allows(x86), "x86 means a 32-bit JVM");
		Optional<Json> feature = Optional.of(Json.parse("[{\"action\":\"allow\",\"features\":{\"is_demo_user\":true}}]"));
		ok(!LINUX.allows(feature), "launcher features are never on");
		fails(Json.JsonException.class, () -> LINUX.allows(Optional.of(Json.parse("{}"))), "rules must be an array");
	}

	static void versionFile() {
		VersionFile v = VersionFile.parse(VERSION_JSON, LINUX);
		eq("26.3/26.3-client.jar", v.client().path(), "the client jar's cache path");
		eq(List.of("org/lwjgl/lwjgl/3.3.3/lwjgl-3.3.3.jar", "org/lwjgl/lwjgl/3.3.3/lwjgl-3.3.3-natives-linux.jar"),
			v.libraries().stream().map(VersionFile.Download::path).toList(), "common libraries and this OS's natives, not the Mac's");
		eq("34", v.assetIndexId(), "the asset index id");
		eq(List.of("--enable-native-access=ALL-UNNAMED", "-Djava.library.path=${natives_directory}"), v.jvmArguments(),
			"JVM arguments for this OS, without the Mac's or the class path");
		eq(List.of("-XstartOnFirstThread", "a", "b", "--enable-native-access=ALL-UNNAMED", "-Djava.library.path=${natives_directory}"),
			VersionFile.parse(VERSION_JSON, MAC).jvmArguments(), "a Mac gets its own, arrays flattened");
		fails(Json.JsonException.class, () -> VersionFile.parse(VERSION_JSON.replace("lwjgl-3.3.3.jar\"", "../../etc/passwd\""), LINUX),
			"a library path that climbs out of the cache is refused");
	}

	static void downloads() throws IOException {
		Path cache = tempDir("downloads");
		PseudoHttp http = new PseudoHttp().serve("https://example.test/a.jar", "jar bytes").serve("https://example.test/bad.jar", "tampered");
		Downloads downloads = new Downloads(http);
		String sha1 = Downloads.sha1("jar bytes".getBytes(StandardCharsets.UTF_8));

		Path a = downloads.fetch(URI.create("https://example.test/a.jar"), sha1, cache.resolve("libs/a.jar"));
		eq("jar bytes", Files.readString(a), "a checked file lands at its path");
		downloads.fetch(URI.create("https://example.test/a.jar"), sha1, cache.resolve("libs/a.jar"));
		eq(1, http.received().size(), "a cached file isn't fetched again");

		IOException e = fails(IOException.class,
			() -> downloads.fetch(URI.create("https://example.test/bad.jar"), sha1, cache.resolve("libs/bad.jar")), "a hash mismatch");
		ok(e.getMessage().contains("not keeping it"), "the mismatch is reported");
		ok(!Files.exists(cache.resolve("libs/bad.jar")), "a file that failed its check never gets its name");
		try (var files = Files.list(cache.resolve("libs"))) {
			eq(List.of(a), files.toList(), "and no partial file is left behind");
		}
		fails(IOException.class, () -> downloads.fetch(URI.create("https://example.test/a.jar"), "not-a-sha1", cache.resolve("x")), "a bad hash");
		fails(IllegalStateException.class, () -> downloads.fetch(URI.create("https://example.test/unknown.jar"), sha1, cache.resolve("u")),
			"the pseudo fails loudly on a URL it wasn't given");
	}

	static void assets() throws IOException {
		Path assetsDir = tempDir("assets");
		String sound = "a sound";
		String lang = "a language";
		String soundHash = Downloads.sha1(sound.getBytes(StandardCharsets.UTF_8));
		String langHash = Downloads.sha1(lang.getBytes(StandardCharsets.UTF_8));
		String index = "{\"objects\": {\"minecraft/sounds/a.ogg\": {\"hash\": \"" + soundHash + "\", \"size\": 7},"
			+ " \"minecraft/lang/x.json\": {\"hash\": \"" + langHash + "\", \"size\": 10}}}";
		String indexHash = Downloads.sha1(index.getBytes(StandardCharsets.UTF_8));
		PseudoHttp http = new PseudoHttp()
			.serve("https://example.test/34.json", index)
			.serve("https://resources.download.minecraft.net/" + soundHash.substring(0, 2) + "/" + soundHash, sound)
			.serve("https://resources.download.minecraft.net/" + langHash.substring(0, 2) + "/" + langHash, lang);
		VersionFile.Download indexDownload = new VersionFile.Download("indexes/34.json", URI.create("https://example.test/34.json"), indexHash);

		eq(2, Assets.fetch(new Downloads(http), indexDownload, assetsDir), "both objects are fetched");
		eq(sound, Files.readString(assetsDir.resolve("objects").resolve(soundHash.substring(0, 2)).resolve(soundHash)), "objects go under objects/xx/");
		eq(0, Assets.fetch(new Downloads(http), indexDownload, assetsDir), "a second run finds everything cached");
		eq(3, http.received().size(), "and asks for nothing more");
	}

	private static final String VERSION_JSON = """
		{
		  "id": "26.3",
		  "downloads": {"client": {"url": "https://example.test/client.jar", "sha1": "0000000000000000000000000000000000000001"}},
		  "assetIndex": {"id": "34", "url": "https://example.test/34.json", "sha1": "0000000000000000000000000000000000000002"},
		  "libraries": [
		    {"name": "org.lwjgl:lwjgl:3.3.3", "downloads": {"artifact": {"path": "org/lwjgl/lwjgl/3.3.3/lwjgl-3.3.3.jar",
		      "url": "https://example.test/lwjgl.jar", "sha1": "0000000000000000000000000000000000000003"}}},
		    {"name": "org.lwjgl:lwjgl:3.3.3:natives-linux", "rules": [{"action": "allow", "os": {"name": "linux"}}],
		      "downloads": {"artifact": {"path": "org/lwjgl/lwjgl/3.3.3/lwjgl-3.3.3-natives-linux.jar",
		      "url": "https://example.test/natives-linux.jar", "sha1": "0000000000000000000000000000000000000004"}}},
		    {"name": "org.lwjgl:lwjgl:3.3.3:natives-macos", "rules": [{"action": "allow", "os": {"name": "osx"}}],
		      "downloads": {"artifact": {"path": "org/lwjgl/lwjgl/3.3.3/lwjgl-3.3.3-natives-macos.jar",
		      "url": "https://example.test/natives-macos.jar", "sha1": "0000000000000000000000000000000000000005"}}}
		  ],
		  "arguments": {"jvm": [
		    {"rules": [{"action": "allow", "os": {"name": "osx"}}], "value": ["-XstartOnFirstThread", "a", "b"]},
		    "--enable-native-access=ALL-UNNAMED",
		    "-Djava.library.path=${natives_directory}",
		    "-cp", "${classpath}"
		  ]}
		}""";
}
