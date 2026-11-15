package black.graft.loader.transform;

import java.lang.classfile.ClassHierarchyResolver;
import java.lang.instrument.ClassFileTransformer;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import black.graft.Nullable;
import black.graft.internal.ErrorCode;
import black.graft.internal.LoadingException;

/**
 * Patches Minecraft's classes as the JVM loads them.
 *
 * <p>The JVM ignores an exception thrown from here and loads the class unpatched, which would leave mods half working
 * with no word why. So a failure prints its report and halts the JVM instead.
 */
public final class GraftTransformer implements ClassFileTransformer {
	private final Map<String, List<Patch>> byClass;

	public GraftTransformer(List<Patch> patches) {
		Map<String, List<Patch>> map = new HashMap<>();
		for (Patch p : patches) {
			map.computeIfAbsent(p.owner(), _ -> new ArrayList<>()).add(p);
		}
		map.replaceAll((_, v) -> List.copyOf(v));
		this.byClass = Map.copyOf(map);
	}

	@Override
	public byte @Nullable [] transform(@Nullable ClassLoader loader, @Nullable String className, @Nullable Class<?> redefined,
		@Nullable ProtectionDomain domain, byte[] bytes) {
		if (className == null) {
			return null;
		}
		List<Patch> patches = byClass.get(className);
		if (patches == null) {
			return null;
		}
		ClassLoader parsing = loader != null ? loader : ClassLoader.getSystemClassLoader();
		ClassHierarchyResolver hierarchy = ClassHierarchyResolver.ofResourceParsing(parsing).orElse(ClassHierarchyResolver.defaultResolver()).cached();
		try {
			return ClassPatcher.patch(bytes, patches, hierarchy);
		} catch (LoadingException e) {
			throw Fatal.halt(e);
		} catch (RuntimeException | LinkageError e) {
			throw Fatal.halt(new LoadingException(ErrorCode.PATCH_FAILED, "patching " + className.replace('/', '.') + " failed: " + e, e));
		}
	}
}
