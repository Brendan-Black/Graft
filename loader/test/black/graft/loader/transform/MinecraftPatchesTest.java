package black.graft.loader.transform;

import static black.graft.Check.eq;
import static black.graft.Check.ok;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.classfile.ClassFile;
import java.lang.classfile.ClassHierarchyResolver;
import java.lang.constant.ClassDesc;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Applies every one of Graft's patches to the real Minecraft 26.3 classes, then runs the JVM's own verifier over each
 * patched class. The jars are the ones the build already fetched (by SHA-1) to compile against; nothing runs from them.
 * This is the test that fails, in Graft's build, when a Minecraft update moves a hook.
 */
public final class MinecraftPatchesTest {
	private static final ClassDesc HOOKS = ClassDesc.of("black.graft.loader.Hooks");
	private static final ClassDesc CLIENT_HOOKS = ClassDesc.of("black.graft.loader.ClientHooks");

	private MinecraftPatchesTest() {}

	public static void run() throws IOException {
		String classpath = System.getProperty("graft.test.minecraft", "");
		ok(!classpath.isBlank(), "the build passes the Minecraft class path in graft.test.minecraft");
		List<URL> urls = new ArrayList<>();
		for (String path : classpath.split(File.pathSeparator)) {
			urls.add(new File(path).toURI().toURL());
		}
		// Parent is the platform loader: the game's classes are only ever parsed here, never loaded.
		try (URLClassLoader minecraft = new URLClassLoader(urls.toArray(URL[]::new), ClassLoader.getPlatformClassLoader())) {
			ClassHierarchyResolver hierarchy = ClassHierarchyResolver.ofResourceParsing(minecraft)
				.orElse(ClassHierarchyResolver.ofResourceParsing(MinecraftPatchesTest.class.getClassLoader()))
				.orElse(ClassHierarchyResolver.defaultResolver())
				.cached();
			ClassFile verifier = ClassFile.of(ClassFile.ClassHierarchyResolverOption.of(hierarchy));

			List<Patch> all = new ArrayList<>(MinecraftPatches.common(HOOKS));
			all.addAll(MinecraftPatches.client(CLIENT_HOOKS));
			eq(10, all.size(), "Graft makes ten patches (the README's table lists them)");

			Map<String, List<Patch>> byClass = new LinkedHashMap<>();
			for (Patch p : all) {
				byClass.computeIfAbsent(p.owner(), _ -> new ArrayList<>()).add(p);
			}
			for (Map.Entry<String, List<Patch>> e : byClass.entrySet()) {
				byte[] original = read(minecraft, e.getKey());
				ok(verifier.verify(original).isEmpty(), e.getKey() + " verifies before patching (so the verifier sees its whole hierarchy)");
				byte[] patched = ClassPatcher.patch(original, e.getValue(), hierarchy);
				ok(!Arrays.equals(original, patched), e.getKey() + " changes when patched");
				List<VerifyError> errors = verifier.verify(patched);
				ok(errors.isEmpty(), e.getKey() + " verifies after patching: " + errors);
			}
		}
	}

	private static byte[] read(ClassLoader loader, String internalName) throws IOException {
		try (InputStream in = loader.getResourceAsStream(internalName + ".class")) {
			if (in == null) {
				throw new AssertionError(internalName + " isn't in the Minecraft jars");
			}
			return in.readAllBytes();
		}
	}
}
