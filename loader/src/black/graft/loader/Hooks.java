package black.graft.loader;

import java.util.Collection;

import black.graft.event.ServerStarting;
import black.graft.event.ServerStopping;
import black.graft.event.ServerTick;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.packs.repository.RepositorySource;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;

/**
 * What patched Minecraft code calls, on client and server alike. Internal: mods never call these. Each method's name and
 * descriptor is fixed by {@link black.graft.loader.transform.MinecraftPatches}.
 */
public final class Hooks {
	private Hooks() {}

	/** Start of {@code Bootstrap.bootStrap()}: run the mods' entrypoints. */
	public static void initialize() {
		ModRuntime.current().initializeMods();
	}

	/** Start of {@code BuiltInRegistries.freeze()}: create the mods' blocks and items. */
	public static void registerContent() {
		ModRuntime.current().content().register();
	}

	/** {@code CreativeModeTab.buildContents}, after vanilla filled the tab: the mods' entries, which the patch adds to it. */
	public static Collection<ItemStack> tabEntries(CreativeModeTab tab) {
		return ModRuntime.current().content().entriesFor(tab);
	}

	/** Start of {@code new PackRepository(sources)}: offer each mod's assets or data as a pack. */
	public static RepositorySource[] extendPackSources(RepositorySource[] sources) {
		return ModPacks.extend(ModRuntime.current().mods(), sources);
	}

	/** Start of {@code MinecraftServer.runServer()}. */
	public static void serverStarting(MinecraftServer server) {
		ModRuntime.current().events().post(new ServerStarting(server));
	}

	/** Start of {@code MinecraftServer.stopServer()}. */
	public static void serverStopping(MinecraftServer server) {
		ModRuntime.current().events().post(new ServerStopping(server));
	}

	/** End of {@code MinecraftServer.tickServer(...)}. */
	public static void serverTick(MinecraftServer server) {
		ModRuntime.current().events().post(new ServerTick(server));
	}
}
