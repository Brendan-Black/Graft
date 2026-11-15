package black.graft.loader;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import black.graft.ModId;
import black.graft.ModInfo;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.PackLocationInfo;
import net.minecraft.server.packs.PackSelectionConfig;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.PathPackResources;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackCompatibility;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraft.server.packs.repository.RepositorySource;
import net.minecraft.server.packs.repository.ServerPacksSource;
import net.minecraft.world.flag.FeatureFlagSet;

/**
 * Offers each mod's {@code assets/} as a resource pack and its {@code data/} as a data pack. They're required packs at the
 * top of the list, like vanilla's own: always on, and able to override vanilla's files. Mods don't need a
 * {@code pack.mcmeta}; Graft supplies the metadata, so a mod's pack can't be marked for the wrong game version.
 */
final class ModPacks {
	private static final String CLIENT_PACK_SOURCE = "net.minecraft.client.resources.ClientPackSource";
	private static final Map<ModId, Path> ROOTS = new ConcurrentHashMap<>();

	private ModPacks() {}

	/** Adds Graft's source when {@code sources} is the client's resource packs or a server's data packs, and not otherwise. */
	static RepositorySource[] extend(List<ModCandidate> mods, RepositorySource[] sources) {
		PackType type = null;
		for (RepositorySource s : sources) {
			if (s instanceof ServerPacksSource) {
				type = PackType.SERVER_DATA;
			} else if (s.getClass().getName().equals(CLIENT_PACK_SOURCE)) {
				type = PackType.CLIENT_RESOURCES;
			}
		}
		if (type == null || mods.isEmpty()) {
			return sources;
		}
		PackType packType = type;
		RepositorySource[] extended = new RepositorySource[sources.length + 1];
		System.arraycopy(sources, 0, extended, 0, sources.length);
		extended[sources.length] = add -> {
			for (ModCandidate m : mods) {
				Path root = root(m);
				if (Files.isDirectory(root.resolve(packType.getDirectory()))) {
					add.accept(pack(m.info(), root));
				}
			}
		};
		return extended;
	}

	private static Pack pack(ModInfo mod, Path root) {
		PackLocationInfo location = new PackLocationInfo("mod/" + mod.id(), Component.literal(mod.name()), PackSource.BUILT_IN, Optional.empty());
		Pack.Metadata metadata = new Pack.Metadata(
			Component.literal(mod.description().isEmpty() ? mod.name() : mod.description()),
			PackCompatibility.COMPATIBLE,
			FeatureFlagSet.of(),
			List.of());
		return new Pack(location, new PathPackResources.PathResourcesSupplier(root), metadata, new PackSelectionConfig(true, Pack.Position.TOP, false));
	}

	/** The folder a mod's files are in: its development folder, or the root of its jar, opened once and kept open. */
	private static Path root(ModCandidate mod) {
		if (mod.kind() == ModCandidate.Kind.DIRECTORY) {
			return mod.source();
		}
		return ROOTS.computeIfAbsent(mod.info().id(), _ -> {
			try {
				FileSystem jar = FileSystems.newFileSystem(mod.source());
				return jar.getPath("/");
			} catch (IOException e) {
				throw new LoadingException(ErrorCode.BAD_JAR, mod.source() + " can't be opened for its assets and data: " + e, e);
			}
		});
	}
}
