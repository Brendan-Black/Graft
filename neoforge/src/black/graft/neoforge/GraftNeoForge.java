package black.graft.neoforge;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import black.graft.Dist;
import black.graft.ModId;
import black.graft.ModInfo;
import black.graft.event.ServerStarting;
import black.graft.event.ServerStopping;
import black.graft.event.ServerTick;
import black.graft.internal.Descriptors;
import black.graft.internal.ErrorCode;
import black.graft.internal.GraftRuntime;
import black.graft.internal.LoadingException;
import net.minecraft.core.registries.Registries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.jarcontents.JarContents;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.server.ServerAboutToStartEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.registries.RegisterEvent;
import net.neoforged.neoforgespi.language.IModFileInfo;

/**
 * Graft for NeoForge: runs Graft mods as part of a NeoForge game. A Graft mod's jar carries a {@code neoforge.mods.toml}
 * next to its {@code graft.mod.properties} (Graft's Gradle plugin writes it), so NeoForge loads it like any other mod,
 * checks its dependencies, and serves its assets and data. This finds those mods, runs their entrypoints, and connects
 * Graft's API to NeoForge's events.
 */
@Mod(GraftNeoForge.MOD_ID)
public final class GraftNeoForge {
	/** Graft for NeoForge's own NeoForge mod id: {@link ModId#GRAFT}, which every Graft mod depends on. */
	static final String MOD_ID = "graft";
	private static final System.Logger LOG = System.getLogger(MOD_ID);

	/**
	 * NeoForge constructs this as it constructs the mods, before any registry event.
	 *
	 * @param modBus    Graft for NeoForge's own mod event bus, where registration and creative tab events arrive
	 * @param container Graft for NeoForge's own mod
	 * @param neoDist   client or dedicated server
	 */
	public GraftNeoForge(IEventBus modBus, ModContainer container, net.neoforged.api.distmarker.Dist neoDist) {
		Dist dist = neoDist.isClient() ? Dist.CLIENT : Dist.SERVER;
		List<ModInfo> mods = graftMods(ModList.get().getModFiles()).stream().filter(m -> m.side().runsOn(dist)).toList();
		GraftRuntime runtime = new GraftRuntime(dist, FMLPaths.CONFIGDIR.get(), mods, GraftNeoForge.class.getClassLoader());
		GraftRuntime.install(runtime);

		runtime.initializeCommon();
		// Content.register writes every registry it needs at once. NeoForge unfreezes them all for the registry events, and
		// blocks come first: their event is the earliest point blocks, items and test functions can all go in.
		modBus.addListener(RegisterEvent.class, event -> {
			if (event.getRegistryKey().equals(Registries.BLOCK)) {
				runtime.content().register(false); // NeoForge gives block states their ids itself
			}
		});
		modBus.addListener(BuildCreativeModeTabContentsEvent.class, event -> runtime.content().entriesFor(event.getTabKey()).forEach(event::accept));
		NeoForge.EVENT_BUS.addListener(ServerAboutToStartEvent.class, event -> {
			runtime.content().checkTestInstances(event.getServer().registryAccess());
			runtime.events().post(new ServerStarting(event.getServer()));
		});
		NeoForge.EVENT_BUS.addListener(ServerStoppingEvent.class, event -> runtime.events().post(new ServerStopping(event.getServer())));
		NeoForge.EVENT_BUS.addListener(ServerTickEvent.Post.class, event -> runtime.events().post(new ServerTick(event.getServer())));
		if (dist == Dist.CLIENT) {
			GraftNeoForgeClient.initialize(runtime, modBus); // a separate class, so a dedicated server never loads client classes
		}
		runtime.finishStartup();
		LOG.log(System.Logger.Level.INFO, runtime.summary("Graft for NeoForge " + container.getModInfo().getVersion()));
	}

	/** Every NeoForge mod file that holds a Graft mod, read from its descriptor, in Graft's load order (by id). */
	static List<ModInfo> graftMods(List<IModFileInfo> files) {
		List<ModInfo> mods = new ArrayList<>();
		List<String> problems = new ArrayList<>();
		for (IModFileInfo file : files) {
			JarContents contents = file.getFile().getContents();
			if (!contents.containsFile(Descriptors.FILE_NAME)) {
				continue;
			}
			String origin = file.getFile().getFileName();
			try (InputStream in = contents.openFile(Descriptors.FILE_NAME)) {
				mods.add(Descriptors.read(in, origin));
			} catch (IOException e) {
				problems.add(origin + ": " + Descriptors.FILE_NAME + " can't be read: " + e);
			} catch (LoadingException e) {
				problems.addAll(e.problems());
			}
		}
		if (!problems.isEmpty()) {
			throw new LoadingException(ErrorCode.BAD_DESCRIPTOR, problems);
		}
		mods.sort(Comparator.comparing(ModInfo::id));
		return List.copyOf(mods);
	}
}
