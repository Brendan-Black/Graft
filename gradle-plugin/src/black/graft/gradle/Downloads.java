package black.graft.gradle;

import java.io.IOException;
import java.net.URI;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.regex.Pattern;

/**
 * Downloads files into the cache, each checked against the SHA-1 Mojang publishes for it. A file only gets its real name
 * once it has been checked, so a file in the cache is a checked file, and is never fetched again.
 */
final class Downloads {
	static final Pattern SHA1 = Pattern.compile("[0-9a-f]{40}");
	private final Http http;

	Downloads(Http http) {
		this.http = http;
	}

	/**
	 * Makes sure {@code target} holds the file at {@code uri}.
	 *
	 * @return {@code target}
	 * @throws IOException if it can't be fetched, or what arrived doesn't match {@code sha1}
	 */
	Path fetch(URI uri, String sha1, Path target) throws IOException {
		if (!SHA1.matcher(sha1).matches()) {
			throw new IOException("not a SHA-1 for " + uri + ": " + sha1);
		}
		if (Files.isRegularFile(target)) {
			return target;
		}
		byte[] body = http.get(uri);
		String actual = sha1(body);
		if (!actual.equals(sha1)) {
			throw new IOException(uri + " has SHA-1 " + actual + ", expected " + sha1 + "; not keeping it");
		}
		Files.createDirectories(target.getParent());
		Path temp = Files.createTempFile(target.getParent(), target.getFileName().toString(), ".part");
		try {
			Files.write(temp, body);
			try {
				Files.move(temp, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
			} catch (AtomicMoveNotSupportedException e) {
				// Falling back on purpose: the bytes are checked; only the rename can't be atomic here.
				Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING);
			}
		} finally {
			Files.deleteIfExists(temp);
		}
		return target;
	}

	static String sha1(byte[] bytes) {
		try {
			return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-1").digest(bytes));
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException("every JDK has SHA-1", e);
		}
	}
}
