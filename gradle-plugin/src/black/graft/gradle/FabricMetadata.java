package black.graft.gradle;

import java.util.ArrayList;
import java.util.List;

import black.graft.internal.Releases;
import black.graft.Dependency;
import black.graft.ModId;
import black.graft.ModInfo;
import black.graft.Side;
import black.version.Version;
import black.graft.VersionRange;

/**
 * Writes the {@code fabric.mod.json} that lets Fabric load a Graft mod, from the mod's {@code graft.mod.properties}. The
 * descriptor stays the one source of truth: this is derived from it at build time and never written by hand.
 *
 * <p>The file declares no entrypoints. Cambium (Fabric mod id {@code graft}) finds Graft mods by their descriptor
 * and runs their Graft entrypoints itself, so Fabric only needs the mod's identity and dependencies.
 */
final class FabricMetadata {
	private FabricMetadata() {}

	static String json(ModInfo mod) {
		List<String> depends = new ArrayList<>();
		boolean graft = false;
		for (Dependency d : mod.dependencies()) {
			depends.add(string(d.id().value()) + ": " + string(predicate(d.versions())));
			graft |= d.id().equals(ModId.GRAFT);
		}
		if (!graft) {
			depends.add(string(ModId.GRAFT.value()) + ": " + string("*")); // every Graft mod needs Cambium to run on Fabric
		}
		StringBuilder out = new StringBuilder();
		out.append("{\n");
		out.append("  \"schemaVersion\": 1,\n");
		out.append("  \"id\": ").append(string(mod.id().value())).append(",\n");
		out.append("  \"version\": ").append(string(Releases.full(mod.version()))).append(",\n");
		out.append("  \"name\": ").append(string(mod.name())).append(",\n");
		if (!mod.description().isEmpty()) {
			out.append("  \"description\": ").append(string(mod.description())).append(",\n");
		}
		out.append("  \"authors\": [").append(String.join(", ", mod.authors().stream().map(FabricMetadata::string).toList())).append("],\n");
		if (!mod.license().isEmpty()) {
			out.append("  \"license\": ").append(string(mod.license())).append(",\n");
		}
		// Fabric doesn't load a client mod on a dedicated server. A server mod stays "*": single player runs a server too.
		out.append("  \"environment\": ").append(string(mod.side() == Side.CLIENT ? "client" : "*")).append(",\n");
		out.append("  \"depends\": {\n    ").append(String.join(",\n    ", depends)).append("\n  },\n");
		out.append("  \"custom\": { \"graft\": { \"descriptor\": \"graft.mod.properties\" } }\n");
		out.append("}\n");
		return out.toString();
	}

	/**
	 * A Graft range as a Fabric version predicate: {@code [26.3,26.4)} is {@code >=26.3.0 <26.4.0}, {@code [1.2.0]} is
	 * {@code =1.2.0}, {@code *} is {@code *}.
	 */
	static String predicate(VersionRange range) {
		Version min = range.minimum().orElse(null);
		Version max = range.maximum().orElse(null);
		if (min == null && max == null) {
			return "*";
		}
		if (min != null && min.equals(max)) {
			return "=" + Releases.full(min);
		}
		List<String> parts = new ArrayList<>();
		if (min != null) {
			parts.add((range.includesMinimum() ? ">=" : ">") + Releases.full(min));
		}
		if (max != null) {
			parts.add((range.includesMaximum() ? "<=" : "<") + Releases.full(max));
		}
		return String.join(" ", parts);
	}

	/** A JSON string literal. */
	static String string(String value) {
		StringBuilder out = new StringBuilder("\"");
		for (char c : value.toCharArray()) {
			switch (c) {
				case '"' -> out.append("\\\"");
				case '\\' -> out.append("\\\\");
				case '\n' -> out.append("\\n");
				case '\r' -> out.append("\\r");
				case '\t' -> out.append("\\t");
				default -> {
					if (c < 0x20) {
						out.append(String.format("\\u%04x", (int) c));
					} else {
						out.append(c);
					}
				}
			}
		}
		return out.append('"').toString();
	}
}
