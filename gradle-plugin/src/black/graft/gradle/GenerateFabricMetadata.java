package black.graft.gradle;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.tasks.CacheableTask;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;
import org.gradle.api.tasks.TaskAction;

import black.graft.ModInfo;
import black.graft.internal.Descriptors;
import black.graft.internal.LoadingException;

/**
 * Reads the mod's {@code graft.mod.properties} with Graft's own parser, so a descriptor the loader would refuse fails the
 * build instead, and writes the {@code fabric.mod.json} that lets the same jar load on Fabric.
 */
@CacheableTask
public abstract class GenerateFabricMetadata extends DefaultTask {
	/** The mod's resource folders; exactly one holds {@code graft.mod.properties}. */
	@InputFiles
	@PathSensitive(PathSensitivity.RELATIVE)
	public abstract ConfigurableFileCollection getResources();

	/** Where {@code fabric.mod.json} is written. */
	@OutputDirectory
	public abstract DirectoryProperty getOutputDir();

	@TaskAction
	public void generate() {
		List<Path> descriptors = getResources().getFiles().stream()
			.map(dir -> dir.toPath().resolve(Descriptors.FILE_NAME))
			.filter(Files::isRegularFile)
			.toList();
		if (descriptors.size() != 1) {
			throw new GradleException("expected one " + Descriptors.FILE_NAME + " in the main resources, found " + descriptors.size()
				+ (descriptors.isEmpty() ? "; every Graft mod needs one at the root of its resources" : ": " + descriptors));
		}
		Path descriptor = descriptors.getFirst();
		if (Files.exists(descriptor.resolveSibling("fabric.mod.json"))) {
			throw new GradleException(descriptor.resolveSibling("fabric.mod.json") + " exists, but Graft writes that file from "
				+ Descriptors.FILE_NAME + "; delete it and put what it says in the descriptor");
		}
		ModInfo mod;
		try (InputStream in = Files.newInputStream(descriptor)) {
			mod = Descriptors.read(in, descriptor.toString());
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		} catch (LoadingException e) {
			throw new GradleException(String.join("\n", e.problems()), e);
		}
		Path out = getOutputDir().get().getAsFile().toPath().resolve("fabric.mod.json");
		try {
			Files.createDirectories(out.getParent());
			Files.writeString(out, FabricMetadata.json(mod), StandardCharsets.UTF_8);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}
}
