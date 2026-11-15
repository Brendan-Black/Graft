package black.graft.loader;

import java.nio.file.Path;

import black.graft.ModInfo;

/**
 * A mod found on disk, before its dependencies are checked.
 *
 * @param info   what its descriptor says
 * @param source the jar in the mods folder, or a development folder holding its resources
 * @param kind   which of the two {@code source} is
 */
public record ModCandidate(ModInfo info, Path source, Kind kind) {
	/** Where a mod's files are. */
	public enum Kind {
		/** A jar in the mods folder. Graft adds it to the class path. */
		JAR,
		/** A folder of resources from {@code runClient} or {@code runServer}; its classes are already on the class path. */
		DIRECTORY
	}
}
