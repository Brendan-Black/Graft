package black.graft.gradle;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.nio.file.Path;

import org.gradle.api.DefaultTask;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.TaskAction;
import org.gradle.work.DisableCachingByDefault;

/**
 * Fetches the client's assets into Gradle's cache, shared by every project. Untracked: the cache checks itself, file by
 * file, and a run with nothing missing takes a second.
 */
@DisableCachingByDefault(because = "it fills a shared cache outside the build, which checks itself")
public abstract class DownloadAssets extends DefaultTask {
	public DownloadAssets() {
		doNotTrackState("the shared asset cache checks itself");
	}

	/** Where the asset index comes from. */
	@Input
	public abstract Property<String> getIndexUri();

	/** The asset index's SHA-1. */
	@Input
	public abstract Property<String> getIndexSha1();

	/** Where the index goes, under the assets folder. */
	@Input
	public abstract Property<String> getIndexPath();

	/** The shared assets folder. */
	@Internal
	public abstract Property<String> getAssetsDir();

	@TaskAction
	public void fetch() {
		try {
			int fetched = Assets.fetch(new Downloads(Http.jdk()),
				new VersionFile.Download(getIndexPath().get(), URI.create(getIndexUri().get()), getIndexSha1().get()),
				Path.of(getAssetsDir().get()));
			getLogger().lifecycle(fetched == 0 ? "Minecraft's assets are all cached." : "Fetched " + fetched + " of Minecraft's assets.");
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}
}
