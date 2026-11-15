package black.graft.gradle;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.SourceSetContainer;
import org.gradle.api.tasks.TaskProvider;

/**
 * {@code black.graft.mod}: everything a Graft mod's build needs.
 *
 * <ul>
 * <li>Minecraft on the compile class path ({@code black.graft.minecraft}).
 * <li>A {@code graft} configuration for the loader: compiled against, and run as the game's agent.
 * <li>{@code runClient} and {@code runServer}, which start the game from {@code run/client} and {@code run/server} with
 * this mod loaded straight from the build, no jar needed.
 * </ul>
 */
public final class ModPlugin implements Plugin<Project> {
	private static final String GROUP = "graft";
	private static final String CLIENT_MAIN = "net.minecraft.client.main.Main";
	private static final String SERVER_MAIN = "net.minecraft.server.Main";

	@Override
	public void apply(Project project) {
		project.getPluginManager().apply(MinecraftPlugin.class);
		GraftMinecraftExtension minecraft = project.getExtensions().getByType(GraftMinecraftExtension.class);
		MinecraftCache.Prepared prepared = minecraft.prepared();

		Configuration graft = project.getConfigurations().create("graft", c -> {
			c.setDescription("The Graft loader this mod compiles against and runs with.");
			c.setCanBeConsumed(false);
			c.setTransitive(false);
		});
		project.getConfigurations().getByName(JavaPlugin.COMPILE_ONLY_CONFIGURATION_NAME).extendsFrom(graft);

		SourceSet main = project.getExtensions().getByType(SourceSetContainer.class).getByName(SourceSet.MAIN_SOURCE_SET_NAME);
		File resources = main.getOutput().getResourcesDir();
		if (resources == null) {
			throw new IllegalStateException("the main source set has no resources folder");
		}
		// The same jar runs on Fabric: fabric.mod.json, written from the descriptor, lets Graft for Fabric load it.
		TaskProvider<GenerateFabricMetadata> fabric = project.getTasks().register("graftFabricMetadata", GenerateFabricMetadata.class, t -> {
			t.setGroup(GROUP);
			t.setDescription("Writes fabric.mod.json from graft.mod.properties, so the mod also loads on Fabric.");
			t.getResources().from(main.getResources().getSourceDirectories());
			t.getOutputDir().set(project.getLayout().getBuildDirectory().dir("generated/graft/fabric"));
		});
		// Into processResources directly, not as a source folder: a build script that sets its own resource folders keeps it.
		project.getTasks().named(main.getProcessResourcesTaskName(), org.gradle.language.jvm.tasks.ProcessResources.class, t -> t.from(fabric));

		File clientDir = project.file("run/client");
		File serverDir = project.file("run/server");

		TaskProvider<DownloadAssets> assets = project.getTasks().register("downloadAssets", DownloadAssets.class, t -> {
			t.setGroup(GROUP);
			t.setDescription("Fetches Minecraft's sounds and other assets into Gradle's cache, for runClient.");
			t.getIndexUri().set(prepared.version().assetIndex().uri().toString());
			t.getIndexSha1().set(prepared.version().assetIndex().sha1());
			t.getIndexPath().set(prepared.version().assetIndex().path());
			t.getAssetsDir().set(prepared.assetsDir().toString());
		});

		project.getTasks().register("runClient", RunGame.class, t -> {
			t.setGroup(GROUP);
			t.setDescription("Starts the Minecraft client with this mod, from run/client.");
			t.dependsOn(assets, main.getClassesTaskName());
			t.getAgent().from(graft);
			t.classpath(minecraft.getClasspath(), main.getRuntimeClasspath());
			t.getMainClass().set(CLIENT_MAIN);
			t.setWorkingDir(clientDir);
			t.jvmArgs(launcherJvmArguments(prepared, clientDir));
			t.jvmArgs("-Dgraft.dev.mods=" + resources.getAbsolutePath());
			t.args("--username", "Dev", "--version", MinecraftCache.MINECRAFT_VERSION, "--gameDir", clientDir.getAbsolutePath(),
				"--assetsDir", prepared.assetsDir().toString(), "--assetIndex", prepared.version().assetIndexId(), "--accessToken", "0");
		});

		project.getTasks().register("runServer", RunGame.class, t -> {
			t.setGroup(GROUP);
			t.setDescription("Starts a Minecraft dedicated server with this mod, from run/server.");
			t.dependsOn(main.getClassesTaskName());
			t.getAgent().from(graft);
			t.classpath(minecraft.getClasspath(), main.getRuntimeClasspath());
			t.getMainClass().set(SERVER_MAIN);
			t.setWorkingDir(serverDir);
			t.jvmArgs("-Dgraft.dist=server", "-Dgraft.dev.mods=" + resources.getAbsolutePath());
			t.args("--nogui");
			t.setStandardInput(System.in);
		});
	}

	/** The JVM arguments Mojang's launcher passes on this machine, with its placeholders filled in for a development run. */
	private static List<String> launcherJvmArguments(MinecraftCache.Prepared prepared, File runDir) {
		String natives = new File(runDir, "natives").getAbsolutePath();
		List<String> out = new ArrayList<>();
		for (String a : prepared.version().jvmArguments()) {
			out.add(a.replace("${natives_directory}", natives).replace("${launcher_name}", "graft").replace("${launcher_version}", "dev"));
		}
		return out;
	}
}
