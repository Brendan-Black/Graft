package black.graft.fabric;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import black.graft.Dist;
import black.graft.ModId;
import black.graft.ModInfo;
import black.graft.VanillaTab;
import black.graft.event.ServerStarting;
import black.graft.event.ServerStopping;
import black.graft.event.ServerTick;
import black.graft.internal.Descriptors;
import black.graft.internal.ErrorCode;
import black.graft.internal.GraftRuntime;
import black.graft.internal.LoadingException;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;

/**
 * Graft for Fabric: runs Graft mods as part of a Fabric game. A Graft mod's jar carries a {@code fabric.mod.json} next to
 * its {@code graft.mod.properties} (Graft's Gradle plugin writes it), so Fabric loads it like any other mod, checks its
 * dependencies, and serves its assets and data. This finds those mods, runs their entrypoints, and connects Graft's API to
 * Fabric API's events.
 */
public final class GraftFabric implements ModInitializer {
	private static final System.Logger LOG = System.getLogger(ModId.GRAFT.value());

	@Override
	public void onInitialize() {
		FabricLoader fabric = FabricLoader.getInstance();
		Dist dist = fabric.getEnvironmentType() == EnvType.CLIENT ? Dist.CLIENT : Dist.SERVER;
		GraftRuntime runtime = new GraftRuntime(dist, fabric.getConfigDir(), graftMods(fabric.getAllMods()), GraftFabric.class.getClassLoader());
		GraftRuntime.install(runtime);

		runtime.initializeCommon();
		runtime.content().register(false);
		for (VanillaTab tab : runtime.content().tabsWithEntries()) {
			ResourceKey<CreativeModeTab> key = ResourceKey.create(Registries.CREATIVE_MODE_TAB, Identifier.withDefaultNamespace(tab.path()));
			CreativeModeTabEvents.modifyOutputEvent(key).register(output -> runtime.content().entriesFor(key).forEach(output::accept));
		}
		ServerLifecycleEvents.SERVER_STARTING.register(server -> {
			runtime.content().checkTestInstances(server.registryAccess());
			runtime.events().post(new ServerStarting(server));
		});
		ServerLifecycleEvents.SERVER_STOPPING.register(server -> runtime.events().post(new ServerStopping(server)));
		ServerTickEvents.END_SERVER_TICK.register(server -> runtime.events().post(new ServerTick(server)));

		if (dist == Dist.SERVER) {
			runtime.finishStartup(); // on a client, GraftFabricClient finishes once client mods have declared theirs
		}
		String version = fabric.getModContainer(ModId.GRAFT.value()).map(c -> c.getMetadata().getVersion().getFriendlyString()).orElse("?");
		LOG.log(System.Logger.Level.INFO, runtime.summary("Graft for Fabric " + version));
	}

	/** Every Fabric mod that is a Graft mod, read from its descriptor, in Graft's load order (by id). */
	static List<ModInfo> graftMods(Iterable<ModContainer> containers) {
		List<ModInfo> mods = new ArrayList<>();
		List<String> problems = new ArrayList<>();
		for (ModContainer container : containers) {
			Optional<Path> descriptor = container.findPath(Descriptors.FILE_NAME);
			if (descriptor.isEmpty() || !Files.isRegularFile(descriptor.get())) {
				continue;
			}
			String origin = container.getMetadata().getId() + " (" + container.getOrigin() + ")";
			try (InputStream in = Files.newInputStream(descriptor.get())) {
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
