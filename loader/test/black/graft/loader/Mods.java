package black.graft.loader;

import black.graft.internal.Descriptors;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import black.graft.ModInfo;

/** Test helpers: descriptors, mod jars and candidates, made up but shaped like real ones. */
final class Mods {
	private Mods() {}

	static String descriptor(String id, String version, String... extraLines) {
		StringBuilder s = new StringBuilder("id=" + id + "\nname=Mod " + id + "\nversion=" + version + "\n");
		for (String line : extraLines) {
			s.append(line).append('\n');
		}
		return s.toString();
	}

	static ModInfo info(String descriptor) {
		return Descriptors.read(new ByteArrayInputStream(descriptor.getBytes(StandardCharsets.UTF_8)), "test");
	}

	static ModCandidate candidate(String id, String version, String... extraLines) {
		return new ModCandidate(info(descriptor(id, version, extraLines)), Path.of(id + ".jar"), ModCandidate.Kind.JAR);
	}

	/** Writes a zip with these entries (name to content). */
	static Path jar(Path file, Map<String, String> entries) throws IOException {
		try (OutputStream out = Files.newOutputStream(file); ZipOutputStream zip = new ZipOutputStream(out)) {
			for (Map.Entry<String, String> e : entries.entrySet()) {
				zip.putNextEntry(new ZipEntry(e.getKey()));
				zip.write(e.getValue().getBytes(StandardCharsets.UTF_8));
				zip.closeEntry();
			}
		}
		return file;
	}

	static List<String> ids(List<ModCandidate> mods) {
		return mods.stream().map(m -> m.info().id().value()).toList();
	}
}
