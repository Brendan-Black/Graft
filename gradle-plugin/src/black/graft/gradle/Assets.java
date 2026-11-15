package black.graft.gradle;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.Semaphore;

import black.graft.json.Json;
import black.graft.json.Json.JsonObject;

/** Downloads the sounds, languages and other files the client loads from its assets folder rather than its jar. */
final class Assets {
	private static final String OBJECTS = "https://resources.download.minecraft.net/";
	private static final int PARALLEL = 16;

	private Assets() {}

	/**
	 * Fetches the index, then every object it lists that isn't cached yet, 16 at a time.
	 *
	 * @return how many objects were downloaded
	 * @throws IOException naming the first object that failed; objects already fetched stay, so a rerun resumes
	 */
	static int fetch(Downloads downloads, VersionFile.Download index, Path assetsDir) throws IOException {
		Path indexFile = downloads.fetch(index.uri(), index.sha1(), assetsDir.resolve(index.path()));
		JsonObject objects = Json.parseObject(Files.readString(indexFile, StandardCharsets.UTF_8)).object("objects");

		List<String> hashes = new ArrayList<>();
		for (Map.Entry<String, Json> e : objects.members().entrySet()) {
			if (!(e.getValue() instanceof JsonObject object)) {
				throw new IOException("asset " + e.getKey() + " isn't an object in " + indexFile);
			}
			String hash = object.string("hash");
			if (!Files.isRegularFile(objectPath(assetsDir, hash))) {
				hashes.add(hash);
			}
		}

		Semaphore permits = new Semaphore(PARALLEL);
		try (ExecutorService pool = Executors.newVirtualThreadPerTaskExecutor()) {
			List<Future<Path>> pending = new ArrayList<>();
			for (String hash : hashes) {
				pending.add(pool.submit(() -> {
					permits.acquire();
					try {
						return downloads.fetch(URI.create(OBJECTS + hash.substring(0, 2) + "/" + hash), hash, objectPath(assetsDir, hash));
					} finally {
						permits.release();
					}
				}));
			}
			for (Future<Path> f : pending) {
				try {
					f.get();
				} catch (ExecutionException e) {
					Throwable cause = e.getCause() != null ? e.getCause() : e;
					throw new IOException("downloading Minecraft's assets failed: " + cause.getMessage(), cause);
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
					throw new IOException("interrupted while downloading Minecraft's assets", e);
				}
			}
		}
		return hashes.size();
	}

	private static Path objectPath(Path assetsDir, String hash) throws IOException {
		if (!hash.matches("[0-9a-f]{40}")) {
			throw new IOException("not an asset hash: " + hash);
		}
		return assetsDir.resolve("objects").resolve(hash.substring(0, 2)).resolve(hash);
	}
}
