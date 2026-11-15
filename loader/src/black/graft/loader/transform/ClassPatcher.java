package black.graft.loader.transform;

import java.lang.classfile.ClassFile;
import java.lang.classfile.ClassHierarchyResolver;
import java.lang.classfile.ClassModel;
import java.lang.classfile.ClassTransform;
import java.lang.classfile.CodeBuilder;
import java.lang.classfile.CodeElement;
import java.lang.classfile.CodeTransform;
import java.lang.classfile.MethodModel;
import java.lang.classfile.instruction.FieldInstruction;
import java.lang.classfile.instruction.InvokeInstruction;
import java.lang.classfile.instruction.ReturnInstruction;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import black.graft.internal.ErrorCode;
import black.graft.internal.LoadingException;
import black.graft.loader.transform.Patch.Injection;

/** Applies patches to a class's bytes. Pure: bytes in, bytes out, so it runs the same in tests as in the game. */
public final class ClassPatcher {
	private ClassPatcher() {}

	/**
	 * Applies every patch to the class. Each patch must find its method, and at least one place in it to inject, or nothing
	 * is changed: half a set of patches is worse than none.
	 *
	 * @param classBytes the class as the JVM would load it
	 * @param patches    patches whose {@link Patch#owner()} is this class
	 * @param hierarchy  finds the classes the patched code mentions, to compute stack maps; it parses class files and never
	 *                   loads them
	 * @return the patched class
	 * @throws LoadingException {@link ErrorCode#PATCH_TARGET_MISSING} if a patch's method or injection point isn't there
	 */
	public static byte[] patch(byte[] classBytes, List<Patch> patches, ClassHierarchyResolver hierarchy) {
		ClassFile files = ClassFile.of(ClassFile.ClassHierarchyResolverOption.of(hierarchy));
		ClassModel model = files.parse(classBytes);
		String owner = model.thisClass().asInternalName();
		for (Patch p : patches) {
			if (!p.owner().equals(owner)) {
				throw new IllegalArgumentException("patch " + p + " is for " + p.owner() + ", not " + owner);
			}
		}

		Map<Patch, Integer> hits = new HashMap<>();
		ClassTransform transform = ClassTransform.ACCEPT_ALL;
		for (Patch p : patches) {
			hits.put(p, 0);
			ClassTransform one = ClassTransform.transformingMethodBodies(
				(MethodModel m) -> m.methodName().equalsString(p.method()) && m.methodType().equalsString(p.descriptor()),
				CodeTransform.ofStateful(() -> new Injector(p, hits)));
			transform = transform.andThen(one);
		}
		byte[] patched = files.transformClass(model, transform);

		List<String> missing = new ArrayList<>();
		for (Patch p : patches) {
			if (hits.getOrDefault(p, 0) == 0) {
				missing.add(model.methods().stream().anyMatch(m -> m.methodName().equalsString(p.method()) && m.methodType().equalsString(p.descriptor()))
					? "found " + p.owner() + "." + p.method() + p.descriptor() + " but not where " + p.at() + " goes in it, for " + p.why()
					: "no method " + p.owner() + "." + p.method() + p.descriptor() + ", needed for " + p.why());
			}
		}
		if (!missing.isEmpty()) {
			throw new LoadingException(ErrorCode.PATCH_TARGET_MISSING, missing);
		}
		return patched;
	}

	/** Puts one patch's code in one method, counting how many times. */
	private static final class Injector implements CodeTransform {
		private final Patch patch;
		private final Map<Patch, Integer> hits;

		Injector(Patch patch, Map<Patch, Integer> hits) {
			this.patch = patch;
			this.hits = hits;
		}

		@Override
		public void atStart(CodeBuilder code) {
			if (patch.at() instanceof Injection.AtHead) {
				inject(code);
			}
		}

		@Override
		public void accept(CodeBuilder code, CodeElement element) {
			switch (patch.at()) {
				case Injection.AtHead _ -> code.with(element);
				case Injection.BeforeReturns _ -> {
					if (element instanceof ReturnInstruction) {
						inject(code);
					}
					code.with(element);
				}
				case Injection.AfterFieldWrite f -> {
					code.with(element);
					if (element instanceof FieldInstruction fi && fi.opcode() == f.op() && fi.owner().asInternalName().equals(f.fieldOwner())
						&& fi.name().equalsString(f.name())) {
						inject(code);
					}
				}
				case Injection.AfterInvoke i -> {
					code.with(element);
					if (element instanceof InvokeInstruction ii && ii.owner().asInternalName().equals(i.methodOwner())
						&& ii.name().equalsString(i.name()) && ii.type().equalsString(i.descriptor())) {
						inject(code);
					}
				}
			}
		}

		private void inject(CodeBuilder code) {
			patch.code().emit(code);
			hits.merge(patch, 1, Integer::sum);
		}
	}
}
