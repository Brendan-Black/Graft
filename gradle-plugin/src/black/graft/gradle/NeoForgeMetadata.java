package black.graft.gradle;

import black.graft.Dependency;
import black.graft.ModId;
import black.graft.ModInfo;
import black.graft.VersionRange;

/**
 * Writes the {@code META-INF/neoforge.mods.toml} that lets NeoForge load a Graft mod, from the mod's
 * {@code graft.mod.properties}. Like {@link FabricMetadata}, it's derived at build time and never written by hand.
 *
 * <p>The file names no language loader and no entry class: NeoForge loads such a mod as resources only. Graft for NeoForge
 * (NeoForge mod id {@code graft}) finds Graft mods by their descriptor and runs their Graft entrypoints itself.
 */
final class NeoForgeMetadata {
	/** The path inside the jar. */
	static final String PATH = "META-INF/neoforge.mods.toml";
	/** NeoForge requires a license; a descriptor may leave it out. */
	private static final String NO_LICENSE = "unspecified";
	private static final String MOD_ID = "modId=";

	private NeoForgeMetadata() {}

	static String toml(ModInfo mod) {
		StringBuilder out = new StringBuilder();
		out.append("license=").append(string(mod.license().isEmpty() ? NO_LICENSE : mod.license())).append('\n');
		out.append('\n');
		out.append("[[mods]]\n");
		out.append(MOD_ID).append(string(mod.id().value())).append('\n');
		out.append("version=").append(string(mod.version().toString())).append('\n');
		out.append("displayName=").append(string(mod.name())).append('\n');
		if (!mod.description().isEmpty()) {
			out.append("description=").append(string(mod.description())).append('\n');
		}
		if (!mod.authors().isEmpty()) {
			out.append("authors=").append(string(String.join(", ", mod.authors()))).append('\n');
		}
		out.append('\n');
		out.append("[modproperties.").append(mod.id().value()).append("]\n");
		out.append("graftDescriptor=\"graft.mod.properties\"\n");

		boolean graft = false;
		for (Dependency d : mod.dependencies()) {
			dependency(out, mod.id(), d.id(), d.versions());
			graft |= d.id().equals(ModId.GRAFT);
		}
		if (!graft) {
			dependency(out, mod.id(), ModId.GRAFT, VersionRange.any()); // every Graft mod needs Graft for NeoForge to run on NeoForge
		}
		return out.toString();
	}

	/** A Graft range as a NeoForge (Maven) range. They're the same syntax, except that any version is {@code [0,)}. */
	static String range(VersionRange range) {
		return range.minimum().isEmpty() && range.maximum().isEmpty() ? "[0,)" : range.toString();
	}

	private static void dependency(StringBuilder out, ModId mod, ModId on, VersionRange versions) {
		out.append('\n');
		out.append("[[dependencies.").append(mod.value()).append("]]\n");
		out.append(MOD_ID).append(string(on.value())).append('\n');
		out.append("type=\"required\"\n");
		out.append("versionRange=").append(string(range(versions))).append('\n');
		out.append("side=\"BOTH\"\n");
	}

	/** A TOML basic string. Its escapes are JSON's, so this is {@link FabricMetadata#string}. */
	private static String string(String value) {
		return FabricMetadata.string(value);
	}
}
