package black.graft.loader.transform;

import static black.graft.Check.eq;
import static black.graft.Check.fails;
import static black.graft.Check.ok;
import static java.lang.constant.ConstantDescs.CD_Object;
import static java.lang.constant.ConstantDescs.CD_float;
import static java.lang.constant.ConstantDescs.CD_int;
import static java.lang.constant.ConstantDescs.CD_void;
import static java.lang.constant.ConstantDescs.INIT_NAME;
import static java.lang.constant.ConstantDescs.MTD_void;

import java.lang.classfile.ClassFile;
import java.lang.classfile.ClassHierarchyResolver;
import java.lang.classfile.Opcode;
import java.lang.constant.ClassDesc;
import java.lang.constant.MethodTypeDesc;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import black.graft.internal.ErrorCode;
import black.graft.internal.LoadingException;
import black.graft.loader.transform.Patch.Injection;

/**
 * Builds a small class with the shapes Graft patches in Minecraft (a constructor that sets a field, a method that calls
 * another, one that returns a float from two places), patches it, loads it, and runs it: the injected calls really
 * happen, in the right places, with the right values.
 */
public final class PatcherTest {
	private static final ClassDesc TARGET = ClassDesc.of("graft.test.Target");
	private static final ClassDesc RECORDER = ClassDesc.of(Recorder.class.getName());
	private static final ClassDesc INT_ARRAY = CD_int.arrayType();
	private static final ClassHierarchyResolver HIERARCHY = ClassHierarchyResolver.ofResourceParsing(PatcherTest.class.getClassLoader())
		.orElse(ClassHierarchyResolver.defaultResolver());

	private PatcherTest() {}

	public static void run() throws Exception {
		byte[] original = target();

		List<Patch> patches = List.of(
			new Patch("graft/test/Target", "start", "()V", "a head hook", new Injection.AtHead(),
				code -> code.ldc("head").invokestatic(RECORDER, "note", MethodTypeDesc.of(CD_void, ClassDesc.of("java.lang.String")))),
			new Patch("graft/test/Target", INIT_NAME, "()V", "replacing an array right after the constructor sets it",
				new Injection.AfterFieldWrite(Opcode.PUTFIELD, "graft/test/Target", "values"),
				code -> code.aload(0).aload(0).getfield(TARGET, "values", INT_ARRAY)
					.invokestatic(RECORDER, "extend", MethodTypeDesc.of(INT_ARRAY, INT_ARRAY))
					.putfield(TARGET, "values", INT_ARRAY)),
			new Patch("graft/test/Target", "start", "()V", "after a call", new Injection.AfterInvoke("graft/test/Target", "step", "()V"),
				code -> code.ldc("after step").invokestatic(RECORDER, "note", MethodTypeDesc.of(CD_void, ClassDesc.of("java.lang.String")))),
			new Patch("graft/test/Target", "fov", "(ZF)F", "wrapping every returned float", new Injection.BeforeReturns(),
				code -> code.fload(2).invokestatic(RECORDER, "scale", MethodTypeDesc.of(CD_float, CD_float, CD_float))));

		byte[] patched = ClassPatcher.patch(original, patches, HIERARCHY);
		ok(ClassFile.of(ClassFile.ClassHierarchyResolverOption.of(HIERARCHY)).verify(patched).isEmpty(), "the patched class verifies");

		Recorder.reset();
		Class<?> c = new ClassLoader(PatcherTest.class.getClassLoader()) {
			Class<?> define() {
				return defineClass("graft.test.Target", patched, 0, patched.length);
			}
		}.define();
		Object target = c.getDeclaredConstructor().newInstance();
		int[] values = (int[]) c.getField("values").get(target);
		eq(List.of(1, 2, 99), box(values), "the constructor's array went through the hook");

		c.getMethod("start").invoke(target);
		eq(List.of("head", "step ran", "after step"), Recorder.notes(), "head hook first, then vanilla, then the after-call hook");

		Method fov = c.getMethod("fov", boolean.class, float.class);
		eq(35f, fov.invoke(target, true, 0.5f), "the first return's value is wrapped (70 * 0.5)");
		eq(45f, fov.invoke(target, false, 0.5f), "and the second's (90 * 0.5)");

		// Failures: nothing is patched unless everything can be.
		LoadingException missingMethod = fails(LoadingException.class, () -> ClassPatcher.patch(original, List.of(patches.get(0),
			new Patch("graft/test/Target", "gone", "()V", "a hook on a method that was renamed", new Injection.AtHead(), _ -> {})), HIERARCHY),
			"a missing method");
		eq(ErrorCode.PATCH_TARGET_MISSING, missingMethod.code(), "a missing target has its own code");
		ok(missingMethod.getMessage().contains("no method graft/test/Target.gone()V, needed for a hook on a method that was renamed"),
			"the message names the method and what it was for");
		LoadingException missingPoint = fails(LoadingException.class, () -> ClassPatcher.patch(original, List.of(
			new Patch("graft/test/Target", "start", "()V", "after a call that's gone", new Injection.AfterInvoke("graft/test/Target", "gone", "()V"),
				_ -> {})),
			HIERARCHY), "a missing injection point");
		ok(missingPoint.getMessage().contains("found graft/test/Target.start()V but not where"), "a method without the injection point is told apart");
		fails(IllegalArgumentException.class, () -> ClassPatcher.patch(original, List.of(
			new Patch("graft/test/Other", "start", "()V", "wrong class", new Injection.AtHead(), _ -> {})), HIERARCHY),
			"a patch for another class is a programming error");
	}

	/** The class to patch, written with the ClassFile API: the shapes, not the names, of Minecraft's. */
	private static byte[] target() {
		return ClassFile.of().build(TARGET, cls -> cls
			.withFlags(ClassFile.ACC_PUBLIC)
			.withField("values", INT_ARRAY, ClassFile.ACC_PUBLIC)
			.withMethodBody(INIT_NAME, MTD_void, ClassFile.ACC_PUBLIC, code -> code
				.aload(0).invokespecial(CD_Object, INIT_NAME, MTD_void)
				.aload(0).iconst_2().newarray(java.lang.classfile.TypeKind.INT)
				.dup().iconst_0().iconst_1().iastore()
				.dup().iconst_1().iconst_2().iastore()
				.putfield(TARGET, "values", INT_ARRAY)
				.return_())
			.withMethodBody("step", MTD_void, ClassFile.ACC_PUBLIC, code -> code
				.ldc("step ran").invokestatic(RECORDER, "note", MethodTypeDesc.of(CD_void, ClassDesc.of("java.lang.String")))
				.return_())
			.withMethodBody("start", MTD_void, ClassFile.ACC_PUBLIC, code -> code
				.aload(0).invokevirtual(TARGET, "step", MTD_void)
				.return_())
			.withMethodBody("fov", MethodTypeDesc.of(CD_float, java.lang.constant.ConstantDescs.CD_boolean, CD_float), ClassFile.ACC_PUBLIC, code -> {
				var otherwise = code.newLabel();
				code.iload(1).ifeq(otherwise)
					.ldc(70f).freturn()
					.labelBinding(otherwise)
					.ldc(90f).freturn();
			}));
	}

	private static List<Integer> box(int[] values) {
		List<Integer> out = new ArrayList<>();
		for (int v : values) {
			out.add(v);
		}
		return out;
	}
}
