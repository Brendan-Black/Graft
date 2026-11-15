package black.graft.gradle;

import java.nio.file.Path;

import black.graft.ModId;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.file.FileCollection;
import org.gradle.api.plugins.JavaPlugin;

/**
 * {@code black.graft.minecraft}: puts Minecraft's client jar and libraries on the compile class path, fetched from Mojang
 * and checked against their published SHA-1s. Graft's own build uses it; mods use {@code black.graft.mod}, which applies it.
 *
 * <p>The files are fetched while the build is configured, the first time only, because the compile class path has to be
 * known then. After that they come from Gradle's cache, offline.
 */
public final class MinecraftPlugin implements Plugin<Project> {
	static final String EXTENSION = "graftMinecraft";

	@Override
	public void apply(Project project) {
		project.getPluginManager().apply(JavaPlugin.class);
		Path cache = project.getGradle().getGradleUserHomeDir().toPath().resolve("caches").resolve(ModId.GRAFT.value());
		MinecraftCache.Prepared prepared = new MinecraftCache(cache, Http.jdk()).prepare(Platform.current());
		FileCollection classpath = project.files(prepared.classPath().stream().map(Path::toFile).toArray());
		project.getExtensions().add(GraftMinecraftExtension.class, EXTENSION, new GraftMinecraftExtension(classpath, prepared));
		project.getDependencies().add(JavaPlugin.COMPILE_ONLY_CONFIGURATION_NAME, classpath);
	}
}
