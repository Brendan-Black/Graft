package black.graft;

import java.util.List;
import java.util.Optional;

/**
 * What a mod says about itself in its descriptor, {@code graft.mod.properties} at the root of its jar:
 *
 * <pre>
 * id=truezoom
 * name=True Zoom
 * version=1.0.0
 * description=Hold C to zoom in.
 * authors=Syagrius
 * license=Apache-2.0
 * entrypoint=black.truezoom.TrueZoom
 * client-entrypoint=black.truezoom.client.TrueZoomClient
 * depends.minecraft=[26.3,26.4)
 * depends.graft=[0.1,0.2)
 * </pre>
 *
 * Only {@code id}, {@code name} and {@code version} are required. A mod with only assets and data needs no entrypoint.
 *
 * @param id               the mod's id, and the namespace of its content
 * @param name             the name players see
 * @param version          the mod's own version
 * @param description      a sentence or two for players; empty if not given
 * @param authors          who made it, in the order given; empty if not given
 * @param license          the license's SPDX id or name; empty if not given
 * @param entrypoint       the {@link Mod} class Graft starts on both client and server, if any
 * @param clientEntrypoint the {@link black.graft.client.ClientMod} class Graft starts on the client only, if any
 * @param dependencies     the mods, and the Minecraft and Graft versions, this mod needs
 */
public record ModInfo(
	ModId id,
	String name,
	Version version,
	String description,
	List<String> authors,
	String license,
	@Nullable String entrypoint,
	@Nullable String clientEntrypoint,
	List<Dependency> dependencies) {

	public ModInfo {
		if (name.isBlank()) {
			throw new IllegalArgumentException("mod " + id + " has a blank name");
		}
		authors = List.copyOf(authors);
		dependencies = List.copyOf(dependencies);
	}

	/** The {@link Mod} class Graft starts on both client and server, if the mod has one. */
	public Optional<String> optEntrypoint() {
		return Optional.ofNullable(entrypoint);
	}

	/** The {@link black.graft.client.ClientMod} class Graft starts on the client only, if the mod has one. */
	public Optional<String> optClientEntrypoint() {
		return Optional.ofNullable(clientEntrypoint);
	}

	@Override
	public String toString() {
		return id + " " + version;
	}
}
