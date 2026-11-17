package black.graft.loader;

import java.nio.charset.StandardCharsets;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import black.graft.ModInfo;
import black.graft.internal.ErrorCode;

/**
 * Whether a joining client has the server's mods. Blocks and items travel between client and server as numbers, given
 * out in registration order, so a client with other mods reads every modded block wrong instead of failing. The client
 * sends a fingerprint of its mods after its handshake's host name, behind a NUL as Forge's marker is (servers and
 * proxies already split the host name there), and the server compares it with its own before the login goes on.
 *
 * <p>Only {@link black.graft.Side#BOTH} mods count. A {@code side=client} or {@code side=server} mod adds no blocks or items
 * (Graft refuses them), so either end can have it alone.
 */
final class JoinCheck {
	/** What goes between the host name and the fingerprint. The 1 is this check's version. */
	static final String MARK = "\0graft1\0";
	/** The longest host name the handshake carries. Longer ones are sent without a fingerprint rather than fail to send. */
	static final int MAX_HOST_LENGTH = 255;
	private static final int FINGERPRINT_BYTES = 16;

	private JoinCheck() {}

	/** The host name a client sends: {@code host}, then the mark and the fingerprint of its mods, if they fit. */
	static String mark(String host, List<ModInfo> mods) {
		String marked = host + MARK + fingerprint(shared(mods));
		return marked.length() <= MAX_HOST_LENGTH ? marked : host;
	}

	/** Why a client that sent {@code host} can't join a server with {@code serverMods}, worded for that player; empty if it can. */
	static Optional<String> refusal(String host, List<ModInfo> allServerMods) {
		List<ModInfo> serverMods = shared(allServerMods);
		int at = host.lastIndexOf(MARK);
		Optional<String> theirs = at < 0 ? Optional.empty() : Optional.of(host.substring(at + MARK.length()));
		if (theirs.isEmpty()) {
			return serverMods.isEmpty() ? Optional.empty()
				: Optional.of("This server has mods, and your game doesn't have Graft (" + ErrorCode.MODS_DIFFER + "). Install Graft and "
					+ "exactly these mods, at these versions: " + list(serverMods) + ".");
		}
		if (theirs.get().equals(fingerprint(serverMods))) {
			return Optional.empty();
		}
		return Optional.of(serverMods.isEmpty()
			? "This server has no mods that players need, and your game has some (" + ErrorCode.MODS_DIFFER + "). Remove them to join."
			: "Your mods aren't this server's (" + ErrorCode.MODS_DIFFER
				+ "). Install exactly these, at these versions, and no other mods except client-only ones: "
				+ list(serverMods) + ".");
	}

	/** The mods both ends must have: not the ones a descriptor marks {@code side=client} or {@code side=server}. */
	static List<ModInfo> shared(List<ModInfo> mods) {
		return mods.stream().filter(m -> m.side().needsBothEnds()).toList();
	}

	/** A short hash of each mod's id and version, whatever order the mods are in. */
	static String fingerprint(List<ModInfo> mods) {
		String lines = mods.stream().sorted(Comparator.comparing(m -> m.id().value())).map(m -> m.id() + " " + m.version() + "\n")
			.collect(Collectors.joining());
		byte[] hash = Bundler.newSha256().digest(lines.getBytes(StandardCharsets.UTF_8));
		return HexFormat.of().formatHex(hash, 0, FINGERPRINT_BYTES);
	}

	private static String list(List<ModInfo> mods) {
		return mods.stream().sorted(Comparator.comparing(m -> m.id().value())).map(ModInfo::toString).collect(Collectors.joining(", "));
	}
}
