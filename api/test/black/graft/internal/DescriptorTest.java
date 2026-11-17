package black.graft.internal;

import static black.graft.Check.eq;
import static black.graft.Check.fails;
import static black.graft.Check.ok;

import java.util.List;

import black.graft.Dependency;
import black.graft.ModId;
import black.graft.ModInfo;
import black.graft.Side;
import black.graft.Version;
import black.graft.VersionRange;

public final class DescriptorTest {
	private DescriptorTest() {}

	public static void run() {
		ModInfo full = Mods.info("""
			# True Zoom
			id=truezoom
			name=True Zoom
			version=1.0.0
			description=Hold C to zoom in.
			authors=Syagrius, Someone Else ,
			license=Apache-2.0
			entrypoint=black.truezoom.TrueZoom
			client-entrypoint=black.truezoom.client.TrueZoomClient
			depends.minecraft=[26.3,26.4)
			depends.graft=[0.1,0.2)
			""");
		eq(new ModId("truezoom"), full.id(), "id");
		eq("True Zoom", full.name(), "name");
		eq(Version.parse("1.0.0"), full.version(), "version");
		eq(List.of("Syagrius", "Someone Else"), full.authors(), "authors, trimmed, empty ones dropped");
		eq("black.truezoom.TrueZoom", full.optEntrypoint().orElseThrow(), "entrypoint");
		eq("black.truezoom.client.TrueZoomClient", full.optClientEntrypoint().orElseThrow(), "client entrypoint");
		eq(List.of(new Dependency(ModId.GRAFT, VersionRange.parse("[0.1,0.2)")), new Dependency(ModId.MINECRAFT, VersionRange.parse("[26.3,26.4)"))),
			full.dependencies(), "dependencies, by key");

		ModInfo minimal = Mods.info("id=assetsonly\nname=Assets Only\nversion=2\n");
		ok(minimal.optEntrypoint().isEmpty() && minimal.optClientEntrypoint().isEmpty(), "entrypoints are optional");
		eq("", minimal.description(), "description defaults to empty");
		ok(minimal.authors().isEmpty() && minimal.dependencies().isEmpty(), "no authors, no dependencies");
		eq(Side.BOTH, minimal.side(), "side defaults to both");
		eq(Side.CLIENT, Mods.info(Mods.descriptor("zoom", "1", "side=client")).side(), "side=client");
		eq(Side.SERVER, Mods.info(Mods.descriptor("admin", "1", "side= server ")).side(), "side=server, trimmed");
		ok(fails(LoadingException.class, () -> Mods.info(Mods.descriptor("odd", "1", "side=Client")), "a side that isn't one").getMessage()
			.contains("isn't both, client or server"), "a side is written in lower case, as one of the three");

		// Every problem in one descriptor is reported at once, so a mod author fixes them in one go.
		LoadingException e = fails(LoadingException.class, () -> Mods.info("""
			id=Bad-Id
			version=1.0-beta
			entrypoint=not a class
			depends.graft=1.0
			depends.Bad=[1,2)
			nmae=typo
			"""), "a descriptor with many problems");
		eq(ErrorCode.BAD_DESCRIPTOR, e.code(), "code");
		eq(7, e.problems().size(), "one problem each: unknown key, id, missing name, version, entrypoint, range, dependency id: " + e.problems());
		ok(e.getMessage().startsWith("[GRAFT-101]"), "the message starts with the code");
		ok(e.getMessage().contains("unknown key \"nmae\""), "a typo'd key is named");
		ok(e.getMessage().contains("\"name\" is required"), "a missing key is named");

		ok(fails(LoadingException.class, () -> Mods.info(Mods.descriptor("graft", "1.0")), "reserved").getMessage().contains("reserved"),
			"graft is reserved");
		ok(fails(LoadingException.class, () -> Mods.info(Mods.descriptor("minecraft", "1.0")), "reserved").getMessage().contains("reserved"),
			"minecraft is reserved");
		ok(fails(LoadingException.class, () -> Mods.info(Mods.descriptor("selfish", "1.0", "depends.selfish=*")), "self").getMessage()
			.contains("can't depend on itself"), "a mod can't depend on itself");
		fails(LoadingException.class, () -> Mods.info("id=blank\nname=  \nversion=1\n"), "a blank name is refused");
	}
}
