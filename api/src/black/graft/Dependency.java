package black.graft;

/**
 * One mod's need for another, at a version: a {@code depends.<id>} line in a descriptor. Graft refuses to start unless every
 * dependency is installed at a version inside its range.
 *
 * @param id       the mod depended on; {@link ModId#MINECRAFT} and {@link ModId#GRAFT} are always installed
 * @param versions the versions that will do
 */
public record Dependency(ModId id, VersionRange versions) {
	@Override
	public String toString() {
		return id + " " + versions;
	}
}
