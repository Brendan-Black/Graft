package black.graft.internal;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import black.graft.ModInfo;

/** Test helpers: descriptors, made up but shaped like real ones. */
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
}
