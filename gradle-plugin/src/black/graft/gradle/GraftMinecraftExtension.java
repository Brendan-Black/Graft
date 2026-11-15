package black.graft.gradle;

import java.io.File;

import org.gradle.api.file.FileCollection;

/**
 * {@code graftMinecraft} in a build script: the Minecraft jars the project compiles against, for build logic that needs
 * them too, such as tests that read the game's classes.
 */
public final class GraftMinecraftExtension {
	private final FileCollection classpath;
	private final MinecraftCache.Prepared prepared;

	GraftMinecraftExtension(FileCollection classpath, MinecraftCache.Prepared prepared) {
		this.classpath = classpath;
		this.prepared = prepared;
	}

	/** The client jar, then its libraries for this machine. */
	public FileCollection getClasspath() {
		return classpath;
	}

	/** The client jar alone, which a Fabric run names as the game jar. */
	public File getClientJar() {
		return prepared.classPath().getFirst().toFile();
	}

	/** The Minecraft version, {@code 26.3}. */
	public String getVersion() {
		return MinecraftCache.MINECRAFT_VERSION;
	}

	MinecraftCache.Prepared prepared() {
		return prepared;
	}
}
