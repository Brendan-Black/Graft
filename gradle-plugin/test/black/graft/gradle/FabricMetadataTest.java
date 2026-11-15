package black.graft.gradle;

import static black.graft.gradle.Check.eq;
import static black.graft.gradle.Check.ok;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import black.graft.ModInfo;
import black.graft.VersionRange;
import black.graft.internal.Descriptors;
import black.graft.json.Json;
import black.graft.json.Json.JsonObject;

final class FabricMetadataTest {
	private FabricMetadataTest() {}

	static void run() {
		eq(">=26.3.0 <26.4.0", FabricMetadata.predicate(VersionRange.parse("[26.3,26.4)")), "a half-open range");
		eq(">=0.1.0", FabricMetadata.predicate(VersionRange.parse("[0.1,)")), "open upward");
		eq("<=2.0.0", FabricMetadata.predicate(VersionRange.parse("(,2.0]")), "open downward");
		eq(">1.0.0 <2.0.0", FabricMetadata.predicate(VersionRange.parse("(1.0,2.0)")), "exclusive bounds");
		eq("=1.2.0", FabricMetadata.predicate(VersionRange.parse("[1.2]")), "exact");
		eq("*", FabricMetadata.predicate(VersionRange.any()), "any");

		ModInfo mod = Descriptors.read(new ByteArrayInputStream("""
			id=quoted
			name=The "Quoted" Mod
			version=2.1
			description=Line one\\nline two \\\\ done
			authors=A, B
			depends.minecraft=[26.3,26.4)
			depends.othermod=[1.0,)
			""".getBytes(StandardCharsets.UTF_8)), "test");
		String text = FabricMetadata.json(mod);
		JsonObject json = Json.parseObject(text); // what Graft writes, Graft's strict reader accepts
		eq(1, json.number("schemaVersion").intValue(), "schema version 1");
		eq("quoted", json.string("id"), "the Graft id is the Fabric id");
		eq("2.1.0", json.string("version"), "the version, in full");
		eq("The \"Quoted\" Mod", json.string("name"), "quotes survive");
		eq("Line one\nline two \\ done", json.string("description"), "newlines and backslashes survive");
		eq(">=26.3.0 <26.4.0", json.object("depends").string("minecraft"), "Minecraft's range");
		eq(">=1.0.0", json.object("depends").string("othermod"), "another mod's range");
		eq("*", json.object("depends").string("graft"), "Graft for Fabric is always required");
		ok(json.member("entrypoints").isEmpty(), "no Fabric entrypoints: Graft for Fabric runs the Graft ones");
		eq("graft.mod.properties", json.object("custom").object("graft").string("descriptor"), "it points back at the descriptor");

		ModInfo pinned = Descriptors.read(new ByteArrayInputStream("id=pinned\nname=P\nversion=1\ndepends.graft=[0.1,0.2)\n"
			.getBytes(StandardCharsets.UTF_8)), "test");
		eq(">=0.1.0 <0.2.0", Json.parseObject(FabricMetadata.json(pinned)).object("depends").string("graft"),
			"a mod's own Graft range is kept, not replaced");
		eq("\"\\u0001\"", FabricMetadata.string("\u0001"), "control characters are escaped");
	}
}
