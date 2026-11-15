package black.graft.internal;

import java.util.HashSet;
import java.util.Set;

import black.graft.ContentName;
import black.graft.ModInfo;
import black.graft.client.ClientSetup;
import black.graft.client.FovModifier;
import black.graft.client.KeyBinding;
import black.graft.config.Config;
import black.graft.config.ConfigSpec;
import black.graft.event.Events;

/** The {@link ClientSetup} one mod gets while its client entrypoint runs. */
final class ClientSetupImpl implements ClientSetup {
	private final ModInfo mod;
	private final GraftRuntime runtime;
	private final ClientContent client;
	private final GraftRuntime.Window window;
	private final Events events;
	private final Set<ContentName> keyNames = new HashSet<>();
	private boolean configLoaded;

	ClientSetupImpl(ModInfo mod, GraftRuntime runtime, ClientContent client) {
		this.mod = mod;
		this.runtime = runtime;
		this.client = client;
		this.window = new GraftRuntime.Window(mod, "initializeClient()");
		this.events = new SetupImpl.ModEvents(mod, runtime.events(), window);
	}

	void close() {
		window.close();
	}

	@Override
	public ModInfo mod() {
		return mod;
	}

	@Override
	public KeyBinding keyBinding(ContentName name, int defaultKey) {
		window.check("declare key binding " + name);
		if (!keyNames.add(name)) {
			throw new IllegalArgumentException(mod.id() + " declared key binding " + name + " twice");
		}
		return client.keyBinding(mod.id(), name.value(), defaultKey);
	}

	@Override
	public void modifyFov(FovModifier modifier) {
		window.check("add a field-of-view modifier");
		client.modifyFov(modifier);
	}

	@Override
	public Events events() {
		return events;
	}

	@Override
	public Config config(ConfigSpec spec) {
		window.check("load its client config");
		if (configLoaded) {
			throw new IllegalStateException(mod.id() + " loaded its client config twice; load it once and keep the Config");
		}
		configLoaded = true;
		return runtime.loadConfig(mod, spec, mod.id() + "-client.properties", mod.name() + " client settings");
	}

	@Override
	public System.Logger logger() {
		return System.getLogger(mod.id().value());
	}
}
