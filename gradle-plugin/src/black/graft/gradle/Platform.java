package black.graft.gradle;

import java.util.Locale;
import java.util.Optional;

import black.graft.json.Json;
import black.graft.json.Json.JsonObject;

/**
 * The machine the build runs on, as Mojang's version files name machines: {@code windows}, {@code osx} or {@code linux},
 * and {@code x86} for a 32-bit JVM.
 *
 * @param os   {@code windows}, {@code osx} or {@code linux}
 * @param arch the JVM's {@code os.arch}
 */
record Platform(String os, String arch) {
	private static final String X86 = "x86";

	static Platform current() {
		String name = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
		String os = name.contains("win") ? "windows" : name.contains("mac") ? "osx" : "linux";
		return new Platform(os, System.getProperty("os.arch", ""));
	}

	/**
	 * Applies a library's or argument's {@code rules}, the way the launcher does: without rules, it's allowed; with them,
	 * the last rule that matches this machine decides, and none matching means it's not.
	 */
	boolean allows(Optional<Json> rules) {
		if (rules.isEmpty()) {
			return true;
		}
		if (!(rules.get() instanceof Json.JsonArray array)) {
			throw new Json.JsonException("\"rules\" is " + rules.get().kind() + ", expected an array");
		}
		boolean allowed = false;
		for (JsonObject rule : array.objects()) {
			if (rule.member("features").isPresent()) {
				continue; // launcher features (demo mode, custom resolution) are never on in a development run
			}
			if (matches(rule.member("os"))) {
				allowed = rule.string("action").equals("allow");
			}
		}
		return allowed;
	}

	private boolean matches(Optional<Json> osRule) {
		if (osRule.isEmpty()) {
			return true;
		}
		if (!(osRule.get() instanceof JsonObject o)) {
			throw new Json.JsonException("\"os\" is " + osRule.get().kind() + ", expected an object");
		}
		if (o.member(MojangJson.NAME).isPresent() && !o.string(MojangJson.NAME).equals(os)) {
			return false;
		}
		if (o.member(MojangJson.ARCH).isPresent() && o.string(MojangJson.ARCH).equals(X86)) {
			return arch.equals(X86) || arch.equals("i386") || arch.equals("i686");
		}
		return true;
	}
}
