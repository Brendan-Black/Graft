package black.graft.gradle;

import java.io.File;
import java.util.List;

import org.gradle.api.GradleException;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.tasks.Classpath;
import org.gradle.api.tasks.JavaExec;
import org.gradle.process.CommandLineArgumentProvider;
import org.gradle.work.DisableCachingByDefault;

/** {@code runClient} and {@code runServer}: the game with Graft's agent, in a run folder of the project's. */
@DisableCachingByDefault(because = "it runs the game, which produces nothing to cache")
public abstract class RunGame extends JavaExec {
	/** The Graft jar the game runs with, from the project's {@code graft} dependency. */
	@Classpath
	public abstract ConfigurableFileCollection getAgent();

	public RunGame() {
		getJvmArgumentProviders().add(new AgentArgument(getAgent()));
	}

	@Override
	public void exec() {
		File dir = getWorkingDir();
		if (!dir.isDirectory() && !dir.mkdirs()) {
			throw new GradleException("can't create the run folder " + dir);
		}
		super.exec();
	}

	/** {@code -javaagent:<graft jar>}, worked out when the task runs. */
	private static final class AgentArgument implements CommandLineArgumentProvider {
		private final ConfigurableFileCollection agent;

		AgentArgument(ConfigurableFileCollection agent) {
			this.agent = agent;
		}

		@Override
		public Iterable<String> asArguments() {
			if (agent.isEmpty()) {
				throw new GradleException("""
					No Graft jar to run with. Add the loader to the graft configuration:

					    dependencies {
					        graft("com.github.Brendan-Black.Graft:loader:<version>")
					    }""");
			}
			return List.of("-javaagent:" + agent.getSingleFile().getAbsolutePath());
		}
	}
}
