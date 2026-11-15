package black.graft.loader;

import black.graft.internal.ErrorCode;
import black.graft.internal.LoadingException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Unpacks Minecraft's server jar, which is a bundle: the real server jar and its libraries are inside it, listed with their
 * SHA-256 checksums. This does what the bundle's own launcher does (the same folders, the same checks) so that Graft can
 * put the result on the class path it patches.
 */
public final class Bundler {
	private static final String META_INF = "META-INF/";
	private static final String MAIN_CLASS = META_INF + "main-class";
	private static final String VERSIONS = "versions";
	private static final String LIBRARIES = "libraries";

	private Bundler() {}

	/**
	 * What unpacking gave: the class to start, and the jars it needs.
	 *
	 * @param mainClass the server's main class, {@code net.minecraft.server.Main}
	 * @param classPath the server jar, then its libraries
	 */
	public record Unpacked(String mainClass, List<Path> classPath) {
		public Unpacked {
			classPath = List.copyOf(classPath);
		}
	}

	/**
	 * Unpacks {@code bundle} into {@code versions/} and {@code libraries/} under {@code into}, skipping files already there
	 * with the right checksum. Each file is written to a temporary name and checked before it takes its real one, so an
	 * interrupted run leaves nothing half-written.
	 *
	 * @throws LoadingException {@link ErrorCode#BAD_SERVER_JAR} if it isn't a bundle, {@link ErrorCode#CHECKSUM_MISMATCH} if a file inside doesn't match
	 */
	public static Unpacked unpack(Path bundle, Path into) {
		if (!Files.isRegularFile(bundle)) {
			throw new LoadingException(ErrorCode.BAD_SERVER_JAR, "there's no server jar at " + bundle
				+ "; download it from minecraft.net, or name it with --server-jar");
		}
		try (ZipFile zip = new ZipFile(bundle.toFile())) {
			String mainClass = text(zip, bundle, MAIN_CLASS).strip();
			List<Path> classPath = new ArrayList<>();
			classPath.addAll(unpackList(zip, bundle, VERSIONS, into));
			classPath.addAll(unpackList(zip, bundle, LIBRARIES, into));
			return new Unpacked(mainClass, classPath);
		} catch (IOException e) {
			throw new LoadingException(ErrorCode.BAD_SERVER_JAR, bundle + " can't be read as a Minecraft server jar: " + e, e);
		}
	}

	private static List<Path> unpackList(ZipFile zip, Path bundle, String kind, Path into) throws IOException {
		List<Path> out = new ArrayList<>();
		String list = text(zip, bundle, META_INF + kind + ".list");
		int lineNumber = 0;
		for (String line : list.split("\n")) {
			lineNumber++;
			if (line.isBlank()) {
				continue;
			}
			String[] fields = line.strip().split("\t");
			if (fields.length != 3 || !fields[0].matches("[0-9a-f]{64}") || fields[2].contains("..")) {
				throw new LoadingException(ErrorCode.BAD_SERVER_JAR,
					bundle + ": line " + lineNumber + " of META-INF/" + kind + ".list isn't \"<sha256>\\t<id>\\t<path>\": " + line);
			}
			String sha256 = fields[0];
			String path = fields[2];
			Path target = into.resolve(kind).resolve(path).normalize();
			if (!target.startsWith(into.resolve(kind).normalize())) {
				throw new LoadingException(ErrorCode.BAD_SERVER_JAR, bundle + ": " + path + " points outside " + kind + "/");
			}
			if (!Files.isRegularFile(target) || !sha256(target).equals(sha256)) {
				extract(zip, bundle, META_INF + kind + "/" + path, target, sha256);
			}
			out.add(target);
		}
		return out;
	}

	private static void extract(ZipFile zip, Path bundle, String entryName, Path target, String sha256) throws IOException {
		ZipEntry entry = zip.getEntry(entryName);
		if (entry == null) {
			throw new LoadingException(ErrorCode.BAD_SERVER_JAR, bundle + " lists " + entryName + " but doesn't contain it");
		}
		Files.createDirectories(target.getParent());
		Path temp = Files.createTempFile(target.getParent(), target.getFileName().toString(), ".part");
		try {
			try (InputStream in = zip.getInputStream(entry)) {
				Files.copy(in, temp, StandardCopyOption.REPLACE_EXISTING);
			}
			String actual = sha256(temp);
			if (!actual.equals(sha256)) {
				throw new LoadingException(ErrorCode.CHECKSUM_MISMATCH,
					bundle + ": " + entryName + " has SHA-256 " + actual + ", but the bundle lists " + sha256 + ". Download the server jar again.");
			}
			try {
				Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
			} catch (AtomicMoveNotSupportedException e) {
				// Falling back on purpose: the file is complete and checked; only the rename can't be atomic here.
				Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING);
			}
		} finally {
			Files.deleteIfExists(temp);
		}
	}

	private static String text(ZipFile zip, Path bundle, String name) throws IOException {
		ZipEntry entry = zip.getEntry(name);
		if (entry == null) {
			throw new LoadingException(ErrorCode.BAD_SERVER_JAR, bundle + " isn't a Minecraft server bundle: it has no " + name);
		}
		try (BufferedReader in = new BufferedReader(new InputStreamReader(zip.getInputStream(entry), StandardCharsets.UTF_8))) {
			StringBuilder out = new StringBuilder();
			in.lines().forEach(l -> out.append(l).append('\n'));
			return out.toString();
		}
	}

	static String sha256(Path file) throws IOException {
		MessageDigest digest;
		try {
			digest = MessageDigest.getInstance("SHA-256");
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException("every JDK has SHA-256", e);
		}
		try (InputStream in = Files.newInputStream(file)) {
			byte[] buffer = new byte[1 << 16];
			for (int n; (n = in.read(buffer)) > 0;) {
				digest.update(buffer, 0, n);
			}
		}
		return HexFormat.of().formatHex(digest.digest());
	}
}
