package black.graft;

import black.graft.config.ConfigTest;
import black.graft.internal.DescriptorTest;
import black.graft.internal.EventBusTest;
import black.graft.json.JsonTest;

/** Runs every test group; exits non-zero if any check failed. Run from the api module's directory. */
public final class AllTests {
	private AllTests() {}

	public static void main(String[] args) {
		Check.group("values (ids, names, versions, ranges)", ValuesTest::run);
		Check.group("json", JsonTest::run);
		Check.group("descriptors", DescriptorTest::run);
		Check.group("config files", ConfigTest::run);
		Check.group("event bus", EventBusTest::run);
		System.exit(Check.summary());
	}
}
