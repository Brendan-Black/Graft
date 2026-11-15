package black.graft.loader;

import java.lang.reflect.InvocationTargetException;
import java.util.List;

import black.graft.Dist;
import black.graft.ModInfo;
import black.graft.Nullable;
import black.graft.Version;
import black.graft.config.Config;
import black.graft.config.ConfigException;
import black.graft.config.ConfigSpec;

/**
 * The running loader: the mods that resolved, what they declared, and the event bus. One per game, created by the agent
 * before Minecraft's first class loads; patched Minecraft code reaches it through {@link Hooks}.
 */
public final class ModRuntime {
	private static volatile @Nullable ModRuntime current;

	private final Environment environment;
	private final List<ModCandidate> mods;
	private final Version minecraft;
	private final Version graft;
	private final EventBus events = new EventBus();
	private final Content content = new Content();
	private final @Nullable ClientRuntime client;
	private boolean initialized;

	ModRuntime(Environment environment, List<ModCandidate> mods, Version minecraft, Version graft) {
		this.environment = environment;
		this.mods = List.copyOf(mods);
		this.minecraft = minecraft;
		this.graft = graft;
		this.client = environment.dist() == Dist.CLIENT ? new ClientRuntime() : null;
	}

	/** Makes this the game's runtime. */
	static void install(ModRuntime runtime) {
		if (current != null) {
			throw new IllegalStateException("Graft was started twice");
		}
		current = runtime;
	}

	/**
	 * The game's runtime.
	 *
	 * @throws IllegalStateException if Graft's agent didn't start, so patched code is running without it
	 */
	static ModRuntime current() {
		ModRuntime r = current;
		if (r == null) {
			throw new IllegalStateException("Graft's hooks ran but Graft never started: was the game launched with -javaagent:graft.jar?");
		}
		return r;
	}

	EventBus events() {
		return events;
	}

	Content content() {
		return content;
	}

	ClientRuntime client() {
		ClientRuntime c = client;
		if (c == null) {
			throw new IllegalStateException("client code ran on a dedicated server");
		}
		return c;
	}

	List<ModCandidate> mods() {
		return mods;
	}

	Environment environment() {
		return environment;
	}

	/** One line for the log: what's running. */
	String summary() {
		List<String> names = mods.stream().map(m -> m.info().id() + " " + m.info().version()).toList();
		return "Graft " + graft + " on Minecraft " + minecraft + " (" + environment.dist().name().toLowerCase(java.util.Locale.ROOT) + "), "
			+ mods.size() + (mods.size() == 1 ? " mod" : " mods") + (names.isEmpty() ? "" : ": " + String.join(", ", names));
	}

	/**
	 * Runs every mod's entrypoint, then on a client every client entrypoint, in load order. Called from the start of
	 * Minecraft's bootstrap; runs once even if the bootstrap is asked for again.
	 */
	void initializeMods() {
		if (initialized) {
			return;
		}
		initialized = true;
		for (ModCandidate m : mods) {
			ModInfo info = m.info();
			info.optEntrypoint().ifPresent(name -> {
				black.graft.Mod mod = instantiate(info, name, black.graft.Mod.class);
				SetupImpl setup = new SetupImpl(info, this);
				run(info, name, () -> mod.initialize(setup));
				setup.close();
			});
		}
		ClientRuntime clientRuntime = client;
		if (clientRuntime != null) {
			for (ModCandidate m : mods) {
				ModInfo info = m.info();
				info.optClientEntrypoint().ifPresent(name -> {
					black.graft.client.ClientMod mod = instantiate(info, name, black.graft.client.ClientMod.class);
					ClientSetupImpl setup = new ClientSetupImpl(info, this, clientRuntime);
					run(info, name, () -> mod.initializeClient(setup));
					setup.close();
				});
			}
		}
		events.freeze();
	}

	/** Reads one of a mod's config files, turning a player's mistake into a loading problem. */
	Config loadConfig(ModInfo mod, ConfigSpec spec, String fileName, String title) {
		try {
			return Config.load(spec, environment.configDir().resolve(fileName), title);
		} catch (ConfigException e) {
			throw new LoadingException(ErrorCode.BAD_CONFIG, e.problems().stream().map(p -> e.file() + ": " + p).toList());
		}
	}

	private static <T> T instantiate(ModInfo mod, String className, Class<T> type) {
		Class<?> c;
		try {
			c = Class.forName(className, true, ClassLoader.getSystemClassLoader());
		} catch (ClassNotFoundException e) {
			throw new LoadingException(ErrorCode.BAD_ENTRYPOINT, mod.name() + " (" + mod.id() + ") names entrypoint " + className
				+ ", which isn't in its jar", e);
		}
		if (!type.isAssignableFrom(c)) {
			throw new LoadingException(ErrorCode.BAD_ENTRYPOINT, mod.name() + " (" + mod.id() + ")'s entrypoint " + className
				+ " doesn't implement " + type.getName());
		}
		try {
			return type.cast(c.getDeclaredConstructor().newInstance());
		} catch (NoSuchMethodException | IllegalAccessException | InstantiationException e) {
			throw new LoadingException(ErrorCode.BAD_ENTRYPOINT, mod.name() + " (" + mod.id() + ")'s entrypoint " + className
				+ " needs a public no-argument constructor", e);
		} catch (InvocationTargetException e) {
			Throwable cause = e.getCause() != null ? e.getCause() : e;
			throw new LoadingException(ErrorCode.ENTRYPOINT_FAILED, mod.name() + " (" + mod.id() + ")'s entrypoint " + className
				+ " threw while being constructed: " + cause, cause);
		}
	}

	private static void run(ModInfo mod, String className, Runnable body) {
		try {
			body.run();
		} catch (LoadingException e) {
			throw e;
		} catch (RuntimeException e) {
			throw new LoadingException(ErrorCode.ENTRYPOINT_FAILED, mod.name() + " (" + mod.id() + ")'s " + className + " threw while setting up: "
				+ e, e);
		}
	}

	/** A mod's declarations are only accepted while its entrypoint runs. */
	static final class Window {
		private final ModInfo mod;
		private final String entrypoint;
		private boolean open = true;

		Window(ModInfo mod, String entrypoint) {
			this.mod = mod;
			this.entrypoint = entrypoint;
		}

		void check(String what) {
			if (!open) {
				throw new IllegalStateException(mod.id() + " tried to " + what + " after its " + entrypoint
					+ " returned; declare everything from there");
			}
		}

		void close() {
			open = false;
		}
	}
}
