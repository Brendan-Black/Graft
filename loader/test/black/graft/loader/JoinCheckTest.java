package black.graft.loader;

import static black.graft.Check.eq;
import static black.graft.Check.ok;

import java.util.List;
import java.util.Optional;

import black.graft.ModInfo;

public final class JoinCheckTest {
	private static final String HOST = "play.example.net";

	private JoinCheckTest() {}

	public static void run() {
		ModInfo zoom = Mods.info(Mods.descriptor("truezoom", "1.0.0"));
		ModInfo zoomNewer = Mods.info(Mods.descriptor("truezoom", "1.1.0"));
		ModInfo other = Mods.info(Mods.descriptor("other", "2.0"));
		List<ModInfo> server = List.of(zoom, other);

		String marked = JoinCheck.mark(HOST, server);
		ok(marked.startsWith(HOST + "\0"), "the host name comes first, ended by a NUL as proxies expect: " + marked.replace('\0', '|'));
		eq(Optional.empty(), JoinCheck.refusal(marked, server), "the same mods join");
		eq(Optional.empty(), JoinCheck.refusal(JoinCheck.mark(HOST, List.of(other, zoom)), server), "whatever order they loaded in");
		eq(Optional.empty(), JoinCheck.refusal(JoinCheck.mark(HOST, List.of()), List.of()), "no mods on either end join");
		eq(Optional.empty(), JoinCheck.refusal(HOST, List.of()), "a vanilla client joins a server with no mods");

		String missing = JoinCheck.refusal(JoinCheck.mark(HOST, List.of(zoom)), server).orElse("");
		ok(missing.contains("GRAFT-106"), "a client missing a mod is refused, with the code: " + missing);
		ok(missing.contains("other 2.0") && missing.contains("truezoom 1.0.0"), "and told every mod and version the server has");
		ok(JoinCheck.refusal(JoinCheck.mark(HOST, List.of(zoomNewer, other)), server).isPresent(), "another version of a mod is refused");
		ok(JoinCheck.refusal(JoinCheck.mark(HOST, List.of(zoom, other, Mods.info(Mods.descriptor("extra", "1.0")))), server).isPresent(),
			"an extra mod is refused");

		String vanilla = JoinCheck.refusal(HOST, server).orElse("");
		ok(vanilla.contains("doesn't have Graft") && vanilla.contains("truezoom 1.0.0"), "a vanilla client is told to install Graft: " + vanilla);
		String modsOnVanilla = JoinCheck.refusal(JoinCheck.mark(HOST, List.of(zoom)), List.of()).orElse("");
		ok(modsOnVanilla.contains("no mods"), "a modded client on a server without mods is told to remove them: " + modsOnVanilla);

		// A mod for one end only isn't asked of the other: a zoom mod doesn't keep its player off servers without it.
		ModInfo clientOnly = Mods.info(Mods.descriptor("minimap", "1.0", "side=client"));
		ModInfo serverOnly = Mods.info(Mods.descriptor("admintools", "1.0", "side=server"));
		eq(Optional.empty(), JoinCheck.refusal(JoinCheck.mark(HOST, List.of(zoom, other, clientOnly)), server),
			"a client with an extra client-only mod joins");
		eq(Optional.empty(), JoinCheck.refusal(JoinCheck.mark(HOST, List.of(clientOnly)), List.of()), "even a server with no mods");
		eq(Optional.empty(), JoinCheck.refusal(JoinCheck.mark(HOST, server), List.of(zoom, other, serverOnly)),
			"a server with an extra server-only mod lets clients without it in");
		eq(Optional.empty(), JoinCheck.refusal(HOST, List.of(serverOnly)), "a vanilla client joins a server whose only mods are server-only");
		String listed = JoinCheck.refusal(JoinCheck.mark(HOST, List.of()), List.of(zoom, serverOnly)).orElse("");
		ok(listed.contains("truezoom 1.0.0") && !listed.contains("admintools"), "a refused player is only told the mods they need: " + listed);

		String longHost = "h".repeat(JoinCheck.MAX_HOST_LENGTH - 10);
		eq(longHost, JoinCheck.mark(longHost, server), "a host name too long for the fingerprint is sent as it is, not cut");
		ok(JoinCheck.mark("h".repeat(200), server).length() <= JoinCheck.MAX_HOST_LENGTH, "a marked host name fits the handshake");
		eq(Optional.empty(), JoinCheck.refusal("a\0graft1\0x" + JoinCheck.mark("", server), server), "the last mark is the one read");
	}
}
