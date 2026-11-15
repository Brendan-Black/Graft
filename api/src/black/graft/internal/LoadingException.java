package black.graft.internal;

import java.util.List;

/**
 * Why Graft won't start the game: one or more problems, each with the same code, worded for the player who has to fix
 * their mods folder.
 */
public final class LoadingException extends RuntimeException {
	private static final long serialVersionUID = 1L;

	private final ErrorCode code;
	private final transient List<String> problems;

	public LoadingException(ErrorCode code, List<String> problems) {
		if (problems.isEmpty()) {
			throw new IllegalArgumentException("a LoadingException needs at least one problem");
		}
		super(format(code, problems));
		this.code = code;
		this.problems = List.copyOf(problems);
	}

	public LoadingException(ErrorCode code, String problem) {
		this(code, List.of(problem));
	}

	public LoadingException(ErrorCode code, String problem, Throwable cause) {
		super(format(code, List.of(problem)), cause);
		this.code = code;
		this.problems = List.of(problem);
	}

	private static String format(ErrorCode code, List<String> problems) {
		if (problems.size() == 1) {
			return "[" + code + "] " + problems.getFirst();
		}
		return "[" + code + "] " + problems.size() + " problems:\n  " + String.join("\n  ", problems);
	}

	public ErrorCode code() {
		return code;
	}

	public List<String> problems() {
		return problems;
	}
}
