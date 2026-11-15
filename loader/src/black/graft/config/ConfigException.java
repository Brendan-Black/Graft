package black.graft.config;

import java.nio.file.Path;
import java.util.List;

/** A config file a player edited into something its mod can't use. Each problem names the key, the value, and what fits. */
public final class ConfigException extends RuntimeException {
	private static final long serialVersionUID = 1L;

	private final transient Path file;
	private final transient List<String> problems;

	ConfigException(Path file, List<String> problems) {
		super(file + " has " + problems.size() + (problems.size() == 1 ? " problem" : " problems") + ":\n  "
			+ String.join("\n  ", problems));
		this.file = file;
		this.problems = List.copyOf(problems);
	}

	ConfigException(Path file, String problem, Throwable cause) {
		super(file + ": " + problem, cause);
		this.file = file;
		this.problems = List.of(problem);
	}

	/** The file. */
	public Path file() {
		return file;
	}

	/** One line per problem, in file order. */
	public List<String> problems() {
		return problems;
	}
}
