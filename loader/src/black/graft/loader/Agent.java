package black.graft.loader;

import black.graft.internal.ErrorCode;
import black.graft.internal.LoadingException;

import java.io.IOException;
import java.lang.constant.ClassDesc;
import java.lang.instrument.Instrumentation;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.jar.JarFile;

import black.graft.Dist;
import black.graft.Nullable;
import black.graft.Version;
import black.graft.internal.GraftRuntime;
import black.graft.loader.transform.Fatal;
import black.graft.loader.transform.GraftTransformer;
import black.graft.loader.transform.MinecraftPatches;
import black.graft.loader.transform.Patch;

/**
 * Graft's start. On a client it's a Java agent ({@code -javaagent:graft.jar} in the launcher's JVM arguments), which the
 * JVM starts before Minecraft's {@code main}. For a server, {@link Launch} unpacks the server first and then starts it.
 */
public final class Agent {
	private static final String HOOKS = "black.graft.loader.Hooks";
	private static final String CLIENT_HOOKS = "black.graft.loader.ClientHooks";
	private static final System.Logger LOG = System.getLogger(black.graft.ModId.GRAFT.value());
	private static volatile @Nullable Instrumentation launcherInstrumentation;
	private static volatile List<ModCandidate> mods = List.of();

	private Agent() {}

	/** {@code -javaagent:graft.jar}: Minecraft is already on the class path. */
	public static void premain(@Nullable String args, Instrumentation instrumentation) {
		try {
			start(instrumentation, Environment.read(systemProperties(), name -> ClassLoader.getSystemResource(name) != null));
		} catch (LoadingException e) {
			throw Fatal.halt(e);
		}
	}

	/**
	 * {@code java -jar graft.jar}: the JVM calls this before {@link Launch#main}, whose job it is to put Minecraft on the
	 * class path first. So this only keeps the instrumentation for it.
	 */
	public static void agentmain(@Nullable String args, Instrumentation instrumentation) {
		launcherInstrumentation = instrumentation;
	}

	/** The mods Graft started with, and where their files are. */
	static List<ModCandidate> mods() {
		return mods;
	}

	static Instrumentation launcherInstrumentation() {
		Instrumentation i = launcherInstrumentation;
		if (i == null) {
			throw new IllegalStateException("graft.jar was started without its Launcher-Agent-Class; run it with java -jar");
		}
		return i;
	}

	/**
	 * Finds and checks the mods, puts their jars on the class path, and patches Minecraft's classes as they load.
	 *
	 * @throws LoadingException if the mods can't run together, or this isn't a Minecraft Graft knows
	 */
	static void start(Instrumentation instrumentation, Environment environment) {
		ClassLoader system = ClassLoader.getSystemClassLoader();
		Version minecraft = Versions.minecraft(system.getResourceAsStream(Versions.MINECRAFT_RESOURCE));
		Version graft = Versions.graft(Agent.class.getClassLoader().getResourceAsStream(Versions.GRAFT_RESOURCE));
		List<ModCandidate> resolved = Resolution.resolve(Discovery.discover(environment.modsDir(), environment.devRoots()), minecraft, graft);

		for (ModCandidate m : resolved) {
			if (m.kind() == ModCandidate.Kind.JAR) {
				try {
					instrumentation.appendToSystemClassLoaderSearch(new JarFile(m.source().toFile()));
				} catch (IOException e) {
					throw new LoadingException(ErrorCode.BAD_JAR, m.source() + " can't be opened: " + e, e);
				}
			}
		}

		List<Patch> patches = new ArrayList<>(MinecraftPatches.common(ClassDesc.of(HOOKS)));
		if (environment.dist() == Dist.CLIENT) {
			patches.addAll(MinecraftPatches.client(ClassDesc.of(CLIENT_HOOKS)));
		}
		GraftRuntime runtime = new GraftRuntime(environment.dist(), environment.configDir(), resolved.stream().map(ModCandidate::info).toList(),
				ClassLoader.getSystemClassLoader());
		GraftRuntime.install(runtime);
		Agent.mods = List.copyOf(resolved);
		instrumentation.addTransformer(new GraftTransformer(patches));
		LOG.log(System.Logger.Level.INFO, runtime.summary("Graft " + graft + " on Minecraft " + minecraft));
	}

	static Map<String, String> systemProperties() {
		Map<String, String> properties = new HashMap<>();
		System.getProperties().stringPropertyNames().forEach(k -> properties.put(k, System.getProperty(k)));
		return properties;
	}
}
