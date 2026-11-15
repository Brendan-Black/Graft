package black.graft.gradle;

/** Runs every test group; exits non-zero if any check failed. */
public final class AllTests {
	private AllTests() {}

	public static void main(String[] args) {
		Check.group("platform rules", PluginTest::platform);
		Check.group("version file", PluginTest::versionFile);
		Check.group("downloads (PseudoHttp: hashes, cache, failures)", PluginTest::downloads);
		Check.group("assets (PseudoHttp: index, objects, resuming)", PluginTest::assets);
		Check.group("Fabric metadata from descriptors", FabricMetadataTest::run);
		System.exit(Check.summary());
	}
}
