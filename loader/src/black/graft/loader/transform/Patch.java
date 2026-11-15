package black.graft.loader.transform;

import java.lang.classfile.CodeBuilder;
import java.lang.classfile.Opcode;

/**
 * One change to one method of a Minecraft class: where in the method, and what code goes there. Patches only ever add
 * calls into Graft; they never remove or reorder vanilla code.
 *
 * @param owner      the class, in internal form: {@code net/minecraft/server/Bootstrap}
 * @param method     the method's name
 * @param descriptor the method's descriptor: {@code ()V}
 * @param why        what the patch is for, for error messages and the README's table
 * @param at         where the code goes
 * @param code       the code
 */
public record Patch(String owner, String method, String descriptor, String why, Injection at, Emitter code) {
	/** Writes the injected instructions. */
	@FunctionalInterface
	public interface Emitter {
		void emit(CodeBuilder code);
	}

	/** Where in a method a patch's code goes. */
	public sealed interface Injection {
		/** Before the method's first instruction. */
		record AtHead() implements Injection {}

		/** Before every return instruction. For a method that returns a value, the value is on the stack, and the code must leave a value of the same type. */
		record BeforeReturns() implements Injection {}

		/**
		 * Straight after an instruction that writes a field.
		 *
		 * @param op         {@link Opcode#PUTFIELD} or {@link Opcode#PUTSTATIC}
		 * @param fieldOwner the field's class, in internal form
		 * @param name       the field's name
		 */
		record AfterFieldWrite(Opcode op, String fieldOwner, String name) implements Injection {}

		/**
		 * Straight after a call to a method.
		 *
		 * @param methodOwner the called method's class or interface, in internal form
		 * @param name        the called method's name
		 * @param descriptor  the called method's descriptor
		 */
		record AfterInvoke(String methodOwner, String name, String descriptor) implements Injection {}
	}

	@Override
	public String toString() {
		return owner.replace('/', '.') + "." + method + descriptor + " (" + why + ")";
	}
}
