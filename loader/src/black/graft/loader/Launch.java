package black.graft.loader;

import black.graft.internal.ErrorCode;
import black.graft.internal.LoadingException;

import java.io.IOException;
import java.lang.instrument.Instrumentation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.jar.JarFile;

import black.graft.loader.transform.Fatal;

/**
 * {@code java -jar graft.jar server}: runs a dedicated server with mods. Minecraft's server jar is a bundle that starts
 * the real server in a class loader of its own, out of an agent's reach, so this unpacks it the same way and starts the
 * server on the class path Graft patches.
 */
public final class Launch {
	private static final String USAGE = """
		usage: java -jar graft.jar server [--server-jar <file>] [Minecraft server arguments...]

		Runs a Minecraft server with the mods in ./mods. --server-jar defaults to ./server.jar.
		On a client, Graft isn't run like this: add -javaagent:<path to graft.jar> to the launcher profile's JVM arguments.""";
	private static final String SERVER_JAR = "--server-jar";
	private static final String SERVER_COMMAND = "server";

	private Launch() {}

	public static void main(String[] args) throws Throwable {
		if (args.length == 0 || !args[0].equals(SERVER_COMMAND)) {
			System.err.println(USAGE);
			System.exit(2);
			return;
		}
		List<String> rest = new ArrayList<>(Arrays.asList(args).subList(1, args.length));
		Path serverJar = Path.of("server.jar");
		int flag = rest.indexOf(SERVER_JAR);
		if (flag >= 0) {
			if (flag + 1 >= rest.size()) {
				System.err.println(SERVER_JAR + " needs a file\n\n" + USAGE);
				System.exit(2);
				return;
			}
			serverJar = Path.of(rest.get(flag + 1));
			rest.subList(flag, flag + 2).clear();
		}

		Instrumentation instrumentation = Agent.launcherInstrumentation();
		Bundler.Unpacked server;
		try {
			server = Bundler.unpack(serverJar.toAbsolutePath(), Path.of("").toAbsolutePath());
			for (Path jar : server.classPath()) {
				try {
					instrumentation.appendToSystemClassLoaderSearch(new JarFile(jar.toFile()));
				} catch (IOException e) {
					throw new LoadingException(ErrorCode.BAD_SERVER_JAR, jar + " can't be opened: " + e, e);
				}
			}
			System.setProperty(Environment.DIST_PROPERTY, SERVER_COMMAND);
			Agent.start(instrumentation, Environment.read(Agent.systemProperties(), name -> ClassLoader.getSystemResource(name) != null));
		} catch (LoadingException e) {
			throw Fatal.halt(e);
		}

		Method main = Class.forName(server.mainClass(), true, ClassLoader.getSystemClassLoader()).getMethod("main", String[].class);
		try {
			main.invoke(null, (Object) rest.toArray(String[]::new));
		} catch (InvocationTargetException e) {
			throw e.getCause();
		}
	}
}
