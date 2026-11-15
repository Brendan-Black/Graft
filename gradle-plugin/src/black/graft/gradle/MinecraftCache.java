package black.graft.gradle;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Minecraft's files in Gradle's cache, under {@code caches/graft}: the version file, the client jar and libraries, and
 * (only when {@code runClient} needs them) the assets.
 */
final class MinecraftCache {
	/** The Minecraft this Graft is for. */
	static final String MINECRAFT_VERSION = "26.3";
	/** Pinned by hash, so the plugin never has to trust a manifest lookup. */
	static final URI VERSION_FILE = URI.create("https://piston-meta.mojang.com/v1/packages/9f67e10ed013deeaad20ac3c800652765d493408/26.3.json");
	static final String VERSION_FILE_SHA1 = "9f67e10ed013deeaad20ac3c800652765d493408";

	private final Path root;
	private final Downloads downloads;

	MinecraftCache(Path root, Http http) {
		this.root = root;
		this.downloads = new Downloads(http);
	}

	/**
	 * The files a mod compiles against, fetched if they aren't cached yet.
	 *
	 * @throws UncheckedIOException if a file can't be fetched or doesn't match its hash
	 */
	Prepared prepare(Platform platform) {
		try {
			Path versionFile = downloads.fetch(VERSION_FILE, VERSION_FILE_SHA1, root.resolve("versions").resolve(MINECRAFT_VERSION + ".json"));
			VersionFile version = VersionFile.parse(Files.readString(versionFile, StandardCharsets.UTF_8), platform);
			List<Path> classPath = new ArrayList<>();
			classPath.add(downloads.fetch(version.client().uri(), version.client().sha1(), root.resolve("versions").resolve(version.client().path())));
			for (VersionFile.Download library : version.libraries()) {
				classPath.add(downloads.fetch(library.uri(), library.sha1(), root.resolve("libraries").resolve(library.path())));
			}
			return new Prepared(version, classPath, root.resolve("assets"));
		} catch (IOException e) {
			throw new UncheckedIOException("Graft couldn't get Minecraft " + MINECRAFT_VERSION + "'s files: " + e.getMessage(), e);
		}
	}

	/**
	 * Minecraft, ready to compile against.
	 *
	 * @param version   what the version file says
	 * @param classPath the client jar, then its libraries
	 * @param assetsDir where {@code runClient}'s assets go
	 */
	record Prepared(VersionFile version, List<Path> classPath, Path assetsDir) {
		Prepared {
			classPath = List.copyOf(classPath);
		}
	}
}
