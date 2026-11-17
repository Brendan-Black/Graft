package black.graft;

import black.graft.loader.BundlerTest;
import black.graft.loader.DiscoveryTest;
import black.graft.loader.JoinCheckTest;
import black.graft.loader.PackOrderTest;
import black.graft.loader.ResolutionTest;
import black.graft.loader.StartupTest;
import black.graft.loader.transform.MinecraftPatchesTest;
import black.graft.loader.transform.PatcherTest;

/** Runs every test group; exits non-zero if any check failed. Run from the loader's directory. */
public final class AllTests {
	private AllTests() {}

	public static void main(String[] args) {
		Check.group("discovery (mods folder, development folders)", DiscoveryTest::run);
		Check.group("resolution (duplicates, dependencies, versions)", ResolutionTest::run);
		Check.group("startup (environment, versions)", StartupTest::run);
		Check.group("server bundle unpacking", BundlerTest::run);
		Check.group("join check (same mods on both ends)", JoinCheckTest::run);
		Check.group("pack order (mod packs above vanilla, under the player's)", PackOrderTest::run);
		Check.group("patcher (synthetic classes, run for real)", PatcherTest::run);
		Check.group("Minecraft 26.3 patches (real jars, JVM verifier)", MinecraftPatchesTest::run);
		System.exit(Check.summary());
	}
}
