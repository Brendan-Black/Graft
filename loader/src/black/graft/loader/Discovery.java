package black.graft.loader;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

/** Finds mods: every jar in the mods folder, plus development folders named by {@code runClient}/{@code runServer}. */
public final class Discovery {
	private static final String JAR_SUFFIX = ".jar";

	private Discovery() {}

	/**
	 * Reads every mod's descriptor. Files that aren't jars are left alone; a jar that isn't a Graft mod is a problem, since
	 * it's probably a mod for another loader that would otherwise silently do nothing.
	 *
	 * @param modsDir  the mods folder; it needn't exist
	 * @param devRoots development folders, each holding a descriptor
	 * @throws LoadingException listing every jar or descriptor that couldn't be read, all with the first problem's code
	 */
	public static List<ModCandidate> discover(Path modsDir, List<Path> devRoots) {
		List<ModCandidate> found = new ArrayList<>();
		List<LoadingException> failures = new ArrayList<>();

		for (Path root : devRoots) {
			Path descriptor = root.resolve(Descriptors.FILE_NAME);
			try (InputStream in = Files.newInputStream(descriptor)) {
				found.add(new ModCandidate(Descriptors.read(in, root.toString()), root, ModCandidate.Kind.DIRECTORY));
			} catch (IOException e) {
				failures.add(new LoadingException(ErrorCode.BAD_DESCRIPTOR, root + ": no readable " + Descriptors.FILE_NAME + ": " + e, e));
			} catch (LoadingException e) {
				failures.add(e);
			}
		}

		for (Path jar : jars(modsDir)) {
			try (ZipFile zip = new ZipFile(jar.toFile())) {
				ZipEntry entry = zip.getEntry(Descriptors.FILE_NAME);
				if (entry == null) {
					failures.add(new LoadingException(ErrorCode.BAD_DESCRIPTOR,
						jar + " isn't a Graft mod: it has no " + Descriptors.FILE_NAME + ". Is it a mod for another loader?"));
					continue;
				}
				try (InputStream in = zip.getInputStream(entry)) {
					found.add(new ModCandidate(Descriptors.read(in, jar.toString()), jar, ModCandidate.Kind.JAR));
				}
			} catch (ZipException e) {
				failures.add(new LoadingException(ErrorCode.BAD_JAR, jar + " isn't a readable jar: " + e.getMessage(), e));
			} catch (IOException e) {
				failures.add(new LoadingException(ErrorCode.BAD_JAR, jar + " can't be read: " + e, e));
			} catch (LoadingException e) {
				failures.add(e);
			}
		}

		if (!failures.isEmpty()) {
			List<String> problems = new ArrayList<>();
			for (LoadingException f : failures) {
				problems.addAll(f.problems());
			}
			LoadingException combined = new LoadingException(failures.getFirst().code(), problems);
			failures.forEach(combined::addSuppressed);
			throw combined;
		}
		return found;
	}

	private static List<Path> jars(Path modsDir) {
		if (!Files.isDirectory(modsDir)) {
			return List.of();
		}
		List<Path> jars = new ArrayList<>();
		try (DirectoryStream<Path> entries = Files.newDirectoryStream(modsDir)) {
			for (Path p : entries) {
				if (Files.isRegularFile(p) && p.getFileName().toString().endsWith(JAR_SUFFIX)) {
					jars.add(p);
				}
			}
		} catch (IOException e) {
			throw new LoadingException(ErrorCode.BAD_JAR, "the mods folder " + modsDir + " can't be listed: " + e, e);
		}
		jars.sort(null);
		return jars;
	}
}
