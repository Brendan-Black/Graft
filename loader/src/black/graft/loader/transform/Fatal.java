package black.graft.loader.transform;

import java.io.PrintStream;

import black.graft.internal.LoadingException;

/** Stops the game over a problem that can't be thrown to anyone who would act on it. */
public final class Fatal {
	private Fatal() {}

	/**
	 * Prints the problem to standard error, where the launcher's log and a server console both show it, and halts the JVM
	 * with exit code 1.
	 *
	 * @return never; declared so callers can write {@code throw Fatal.halt(e)} and the compiler knows the path ends
	 */
	public static Error halt(LoadingException e) {
		PrintStream err = System.err;
		err.println();
		err.println("Graft can't start the game.");
		err.println(e.getMessage());
		e.printStackTrace(err);
		err.flush();
		Runtime.getRuntime().halt(1);
		return new AssertionError("unreachable: the JVM halted", e);
	}
}
