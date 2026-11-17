package black.graft.loader;

import java.util.Collection;
import java.util.List;

import black.graft.internal.GraftRuntime;

import black.graft.event.ServerStarting;
import black.graft.event.ServerStopping;
import black.graft.event.ServerTick;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.handshake.ClientIntentionPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerLoginPacketListenerImpl;
import net.minecraft.server.packs.repository.Pack;
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
		GraftRuntime.current().initializeMods();
	}

	/** Start of {@code BuiltInRegistries.freeze()}: create the mods' blocks and items. */
	public static void registerContent() {
		GraftRuntime.current().content().register(true);
	}

	/** {@code CreativeModeTab.buildContents}, after vanilla filled the tab: the mods' entries, which the patch adds to it. */
	public static Collection<ItemStack> tabEntries(CreativeModeTab tab) {
		return GraftRuntime.current().content().entriesFor(tab);
	}

	/** Start of {@code new PackRepository(sources)}: offer each mod's assets or data as a pack. */
	public static RepositorySource[] extendPackSources(RepositorySource[] sources) {
		return ModPacks.extend(Agent.mods(), sources);
	}

	/** Every return from {@code PackRepository.rebuildSelected(ids)}: put mods' newly added packs just above vanilla's. */
	public static List<Pack> placeModPacks(List<Pack> selected, Collection<String> requested) {
		return ModPacks.place(Agent.mods(), selected, requested);
	}

	/** Start of {@code MinecraftServer.runServer()}. */
	public static void serverStarting(MinecraftServer server) {
		GraftRuntime.current().content().checkTestInstances(server.registryAccess());
		GraftRuntime.current().events().post(new ServerStarting(server));
	}

	/** Start of {@code MinecraftServer.stopServer()}. */
	public static void serverStopping(MinecraftServer server) {
		GraftRuntime.current().events().post(new ServerStopping(server));
	}

	/**
	 * End of {@code ServerHandshakePacketListenerImpl.beginLogin(...)}, once the login listener is set up: refuse a client whose
	 * mods aren't this server's, the way vanilla refuses a login.
	 */
	public static void loginStarted(Connection connection, ClientIntentionPacket handshake) {
		if (connection.isConnected() && connection.getPacketListener() instanceof ServerLoginPacketListenerImpl login) {
			JoinCheck.refusal(handshake.hostName(), GraftRuntime.current().mods()).ifPresent(why -> login.disconnect(Component.literal(why)));
		}
	}

	/** End of {@code MinecraftServer.tickServer(...)}. */
	public static void serverTick(MinecraftServer server) {
		GraftRuntime.current().events().post(new ServerTick(server));
	}
}
