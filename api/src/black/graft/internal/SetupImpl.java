package black.graft.internal;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;

import black.graft.ContentName;
import black.graft.Dist;
import black.graft.ModInfo;
import black.graft.Registered;
import black.graft.Setup;
import black.graft.VanillaTab;
import black.graft.config.Config;
import black.graft.config.ConfigSpec;
import black.graft.event.Event;
import black.graft.event.Events;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;

/** The {@link Setup} one mod gets while its entrypoint runs. */
final class SetupImpl implements Setup {
	private final ModInfo mod;
	private final GraftRuntime runtime;
	private final GraftRuntime.Window window;
	private final Set<ContentName> blocks = new HashSet<>();
	private final Set<ContentName> items = new HashSet<>();
	private final Set<Registered<?>> blocksWithItems = new HashSet<>();
	private final Events events;
	private boolean configLoaded;

	SetupImpl(ModInfo mod, GraftRuntime runtime) {
		this.mod = mod;
		this.runtime = runtime;
		this.window = new GraftRuntime.Window(mod, "initialize()");
		this.events = new ModEvents(mod, runtime.events(), window);
	}

	void close() {
		window.close();
	}

	@Override
	public ModInfo mod() {
		return mod;
	}

	@Override
	public Dist dist() {
		return runtime.dist();
	}

	@Override
	public <B extends Block> Registered<B> block(ContentName name, Function<BlockBehaviour.Properties, B> factory) {
		window.check("declare block " + name);
		if (!blocks.add(name)) {
			throw new IllegalArgumentException(mod.id() + " declared block " + name + " twice");
		}
		return runtime.content().block(ResourceKey.create(Registries.BLOCK, id(name)), factory);
	}

	@Override
	public <I extends Item> Registered<I> item(ContentName name, Function<Item.Properties, I> factory) {
		window.check("declare item " + name);
		if (!items.add(name)) {
			throw new IllegalArgumentException(mod.id() + " declared item " + name + " twice");
		}
		return runtime.content().item(ResourceKey.create(Registries.ITEM, id(name)), factory);
	}

	@Override
	public Registered<BlockItem> blockItem(Registered<? extends Block> block) {
		window.check("declare the item for " + block.id());
		if (!block.id().getNamespace().equals(mod.id().value()) || !blocks.contains(new ContentName(block.id().getPath()))) {
			throw new IllegalArgumentException(mod.id() + " can't declare an item for " + block.id() + ", which it didn't declare");
		}
		if (!blocksWithItems.add(block)) {
			throw new IllegalArgumentException(mod.id() + " declared the item for " + block.id() + " twice");
		}
		return item(new ContentName(block.id().getPath()), properties -> new BlockItem(block.get(), properties.useBlockDescriptionPrefix()));
	}

	@Override
	public void addToTab(VanillaTab tab, Registered<? extends ItemLike> item) {
		window.check("add " + item.id() + " to a creative tab");
		runtime.content().addToTab(tab, item);
	}

	@Override
	public Events events() {
		return events;
	}

	@Override
	public Config config(ConfigSpec spec) {
		window.check("load its config");
		if (configLoaded) {
			throw new IllegalStateException(mod.id() + " loaded its config twice; load it once and keep the Config");
		}
		configLoaded = true;
		return runtime.loadConfig(mod, spec, mod.id() + ".properties", mod.name() + " settings");
	}

	@Override
	public System.Logger logger() {
		return System.getLogger(mod.id().value());
	}

	private Identifier id(ContentName name) {
		return Identifier.fromNamespaceAndPath(mod.id().value(), name.value());
	}

	/** A mod's view of the event bus, open only while its entrypoint runs. */
	static final class ModEvents implements Events {
		private final ModInfo mod;
		private final EventBus bus;
		private final GraftRuntime.Window window;

		ModEvents(ModInfo mod, EventBus bus, GraftRuntime.Window window) {
			this.mod = mod;
			this.bus = bus;
			this.window = window;
		}

		@Override
		public <E extends Event> void listen(Class<E> type, Consumer<? super E> listener) {
			window.check("listen for " + type.getSimpleName());
			bus.listen(mod.id(), type, listener);
		}
	}
}
