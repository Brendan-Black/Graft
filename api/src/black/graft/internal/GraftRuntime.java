package black.graft.internal;

import java.lang.reflect.InvocationTargetException;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

import black.graft.Dist;
import black.graft.Mod;
import black.graft.ModInfo;
import black.graft.Nullable;
import black.graft.client.ClientMod;
import black.graft.config.Config;
import black.graft.config.ConfigException;
import black.graft.config.ConfigSpec;

/**
 * The running Graft, whatever it runs on: the mods, what they declared, and the event bus. Internal: not part of the API.
 *
 * <p>A platform creates one and drives it. Graft's own agent calls {@link #initializeMods()} from inside Minecraft's
 * bootstrap. Graft for Fabric calls {@link #initializeCommon()} from its mod initializer and {@link #initializeClient()}
 * from its client initializer. Either way, {@link #finishStartup()} ends the window for declarations, and the platform
 * then puts the declared content into the game its own way.
 */
public final class GraftRuntime {
	private static volatile @Nullable GraftRuntime current;

	private final Dist dist;
	private final Path configDir;
	private final List<ModInfo> mods;
	private final ClassLoader classes;
	private final EventBus events = new EventBus();
	private final Content content = new Content();
	private final @Nullable ClientContent client;
	private boolean commonDone;
	private boolean clientDone;

	/**
	 * @param dist client or dedicated server
	 * @param configDir where mods' config files go
	 * @param mods the mods, in load order
	 * @param classes the class loader the mods' entrypoints are loaded from
	 */
	public GraftRuntime(Dist dist, Path configDir, List<ModInfo> mods, ClassLoader classes) {
		this.dist = dist;
		this.configDir = configDir;
		this.mods = List.copyOf(mods);
		this.classes = classes;
		this.client = dist == Dist.CLIENT ? new ClientContent() : null;
	}

	/** Makes this the game's runtime. */
	public static void install(GraftRuntime runtime) {
		if (current != null) {
			throw new IllegalStateException("Graft was started twice");
		}
		current = runtime;
	}

	/**
	 * The game's runtime.
	 *
	 * @throws IllegalStateException if no platform started Graft
	 */
	public static GraftRuntime current() {
		GraftRuntime r = current;
		if (r == null) {
			throw new IllegalStateException("Graft's hooks ran but Graft never started");
		}
		return r;
	}

	public EventBus events() {
		return events;
	}

	public Content content() {
		return content;
	}

	public Dist dist() {
		return dist;
	}

	public List<ModInfo> mods() {
		return mods;
	}

	/**
	 * What client mods declared.
	 *
	 * @throws IllegalStateException on a dedicated server
	 */
	public ClientContent client() {
		ClientContent c = client;
		if (c == null) {
			throw new IllegalStateException("client code ran on a dedicated server");
		}
		return c;
	}

	/** One line for the log: what's running, on what. */
	public String summary(String platform) {
		List<String> names = mods.stream().map(m -> m.id() + " " + m.version()).toList();
		return platform + " (" + dist.name().toLowerCase(Locale.ROOT) + "), " + mods.size() + (mods.size() == 1 ? " mod" : " mods")
				+ (names.isEmpty() ? "" : ": " + String.join(", ", names));
	}

	/** Everything at once, for a platform that has one moment for it: common entrypoints, client ones, and the end of startup. */
	public void initializeMods() {
		initializeCommon();
		if (client != null) {
			initializeClient();
		}
		finishStartup();
	}

	/** Runs every mod's {@link Mod} entrypoint, in load order. Runs once. */
	public void initializeCommon() {
		if (commonDone) {
			return;
		}
		commonDone = true;
		for (ModInfo info : mods) {
			info.optEntrypoint().ifPresent(name -> {
				Mod mod = instantiate(info, name, Mod.class);
				SetupImpl setup = new SetupImpl(info, this);
				run(info, name, () -> mod.initialize(setup));
				setup.close();
			});
		}
	}

	/** Runs every mod's {@link ClientMod} entrypoint, in load order, after the common ones. Runs once. */
	public void initializeClient() {
		ClientContent c = client();
		if (clientDone) {
			return;
		}
		clientDone = true;
		initializeCommon();
		for (ModInfo info : mods) {
			info.optClientEntrypoint().ifPresent(name -> {
				ClientMod mod = instantiate(info, name, ClientMod.class);
				ClientSetupImpl setup = new ClientSetupImpl(info, this, c);
				run(info, name, () -> mod.initializeClient(setup));
				setup.close();
			});
		}
	}

	/** No more listeners: events start being delivered. */
	public void finishStartup() {
		events.freeze();
	}

	/** Reads one of a mod's config files, turning a player's mistake into a loading problem. */
	Config loadConfig(ModInfo mod, ConfigSpec spec, String fileName, String title) {
		try {
			return Config.load(spec, configDir.resolve(fileName), title);
		} catch (ConfigException e) {
			throw new LoadingException(ErrorCode.BAD_CONFIG, e.problems().stream().map(p -> e.file() + ": " + p).toList());
		}
	}

	private <T> T instantiate(ModInfo mod, String className, Class<T> type) {
		String who = mod.name() + " (" + mod.id() + ")";
		Class<?> c;
		try {
			c = Class.forName(className, true, classes);
		} catch (ClassNotFoundException e) {
			throw new LoadingException(ErrorCode.BAD_ENTRYPOINT, who + " names entrypoint " + className + ", which isn't in its jar", e);
		}
		if (!type.isAssignableFrom(c)) {
			throw new LoadingException(ErrorCode.BAD_ENTRYPOINT, who + "'s entrypoint " + className + " doesn't implement " + type.getName());
		}
		try {
			return type.cast(c.getDeclaredConstructor().newInstance());
		} catch (NoSuchMethodException | IllegalAccessException | InstantiationException e) {
			throw new LoadingException(ErrorCode.BAD_ENTRYPOINT, who + "'s entrypoint " + className + " needs a public no-argument constructor", e);
		} catch (InvocationTargetException e) {
			Throwable cause = e.getCause() != null ? e.getCause() : e;
			throw new LoadingException(ErrorCode.ENTRYPOINT_FAILED, who + "'s entrypoint " + className + " threw while being constructed: " + cause, cause);
		}
	}

	private static void run(ModInfo mod, String className, Runnable body) {
		try {
			body.run();
		} catch (LoadingException e) {
			throw e;
		} catch (RuntimeException e) {
			throw new LoadingException(ErrorCode.ENTRYPOINT_FAILED, mod.name() + " (" + mod.id() + ")'s " + className + " threw while setting up: " + e, e);
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
				throw new IllegalStateException(mod.id() + " tried to " + what + " after its " + entrypoint + " returned; declare everything from there");
			}
		}

		void close() {
			open = false;
		}
	}
}
