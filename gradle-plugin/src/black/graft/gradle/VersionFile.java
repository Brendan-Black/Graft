package black.graft.gradle;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import black.graft.json.Json;
import black.graft.json.Json.JsonObject;

/**
 * What the plugin needs from a Minecraft version file ({@code 26.3.json}): the client jar, the libraries for this
 * machine, the asset index, and the JVM arguments the launcher would pass.
 *
 * @param client       the client jar
 * @param libraries    the libraries this machine needs, in the file's order
 * @param assetIndexId the asset index's id, for {@code --assetIndex}
 * @param assetIndex   the asset index file
 * @param jvmArguments the launcher's JVM arguments for this machine, with {@code ${...}} placeholders left in, and without
 *                     the class path, which Gradle sets
 */
record VersionFile(Download client, List<Download> libraries, String assetIndexId, Download assetIndex, List<String> jvmArguments) {
	private static final Pattern SAFE_PATH = Pattern.compile("[A-Za-z0-9_.\\-/+]+");

	VersionFile {
		libraries = List.copyOf(libraries);
		jvmArguments = List.copyOf(jvmArguments);
	}

	/**
	 * A file to download.
	 *
	 * @param path where it goes in the cache, relative to its folder; never absolute, never with {@code ..}
	 * @param uri  where it comes from
	 * @param sha1 its SHA-1, hex
	 */
	record Download(String path, URI uri, String sha1) {
		Download {
			if (!SAFE_PATH.matcher(path).matches() || path.startsWith("/") || path.contains("..")) {
				throw new Json.JsonException("unsafe download path in the version file: \"" + path + "\"");
			}
		}
	}

	static VersionFile parse(String json, Platform platform) {
		JsonObject v = Json.parseObject(json);
		String id = v.string("id");
		JsonObject clientDownload = v.object("downloads").object("client");
		Download client = new Download(id + "/" + id + "-client.jar", URI.create(clientDownload.string("url")), clientDownload.string("sha1"));

		List<Download> libraries = new ArrayList<>();
		for (JsonObject library : v.array("libraries").objects()) {
			if (!platform.allows(library.member("rules"))) {
				continue;
			}
			JsonObject artifact = library.object("downloads").object("artifact");
			libraries.add(new Download(artifact.string("path"), URI.create(artifact.string("url")), artifact.string("sha1")));
		}

		JsonObject index = v.object("assetIndex");
		String indexId = index.string("id");
		Download assetIndex = new Download("indexes/" + indexId + ".json", URI.create(index.string("url")), index.string("sha1"));

		List<String> jvm = new ArrayList<>();
		for (Json argument : v.object("arguments").array("jvm").elements()) {
			switch (argument) {
				case Json.JsonString s -> jvm.add(s.value());
				case JsonObject o -> {
					if (platform.allows(o.member("rules"))) {
						switch (o.member("value").orElseThrow(() -> new Json.JsonException("a JVM argument has no \"value\""))) {
							case Json.JsonString s -> jvm.add(s.value());
							case Json.JsonArray a -> a.elements().forEach(e -> jvm.add(((Json.JsonString) e).value()));
							default -> throw new Json.JsonException("a JVM argument's value isn't a string or array");
						}
					}
				}
				default -> throw new Json.JsonException("a JVM argument is " + argument.kind());
			}
		}
		int cp = jvm.indexOf("-cp");
		if (cp >= 0 && cp + 1 < jvm.size()) {
			jvm.subList(cp, cp + 2).clear();
		}
		return new VersionFile(client, libraries, indexId, assetIndex, jvm);
	}
}
