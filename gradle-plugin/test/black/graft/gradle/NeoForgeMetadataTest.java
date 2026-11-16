package black.graft.gradle;

import static black.graft.gradle.Check.eq;
import static black.graft.gradle.Check.ok;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import black.graft.ModInfo;
import black.graft.VersionRange;
import black.graft.internal.Descriptors;

final class NeoForgeMetadataTest {
	private NeoForgeMetadataTest() {}

	static void run() {
		eq("[26.3.0,26.4.0)", NeoForgeMetadata.range(VersionRange.parse("[26.3,26.4)")), "a range is already Maven's syntax");
		eq("[1.2.0]", NeoForgeMetadata.range(VersionRange.parse("[1.2]")), "exact");
		eq("[0,)", NeoForgeMetadata.range(VersionRange.any()), "any version, which Maven spells [0,)");

		String toml = NeoForgeMetadata.toml(read("""
			id=quoted
			name=The "Quoted" Mod
			version=2.1
			description=Line one\\nline two
			authors=A, B
			license=Apache-2.0
			depends.minecraft=[26.3,26.4)
			"""));
		ok(toml.startsWith("license=\"Apache-2.0\"\n"), "the license comes first, before any table: " + toml);
		ok(toml.contains("[[mods]]\nmodId=\"quoted\"\nversion=\"2.1.0\"\ndisplayName=\"The \\\"Quoted\\\" Mod\"\n"), "the mod, quotes escaped");
		ok(toml.contains("description=\"Line one\\nline two\"\n"), "a newline is escaped");
		ok(toml.contains("authors=\"A, B\"\n"), "authors in one string");
		ok(!toml.contains("modLoader") && !toml.contains("entryClass"), "no language loader or entry class: Graft for NeoForge runs the mod");
		ok(toml.contains("[[dependencies.quoted]]\nmodId=\"minecraft\"\ntype=\"required\"\nversionRange=\"[26.3.0,26.4.0)\"\nside=\"BOTH\"\n"),
			"Minecraft's range");
		ok(toml.contains("[[dependencies.quoted]]\nmodId=\"graft\"\ntype=\"required\"\nversionRange=\"[0,)\"\n"),
			"Graft for NeoForge is always required");

		String pinned = NeoForgeMetadata.toml(read("id=pinned\nname=P\nversion=1\ndepends.graft=[0.1,0.2)\n"));
		ok(pinned.contains("versionRange=\"[0.1.0,0.2.0)\""), "a mod's own Graft range is kept");
		eq(1, pinned.split("modId=\"graft\"", -1).length - 1, "and Graft isn't required twice");
		ok(pinned.startsWith("license=\"unspecified\""), "NeoForge needs a license, so one without says so");
		ok(!pinned.contains("description=") && !pinned.contains("authors="), "empty fields are left out");
	}

	private static ModInfo read(String descriptor) {
		return Descriptors.read(new ByteArrayInputStream(descriptor.getBytes(StandardCharsets.UTF_8)), "test");
	}
}
