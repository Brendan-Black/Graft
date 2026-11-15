package black.truezoom;

import black.truezoom.client.ZoomTest;

/** Runs every test group; exits non-zero if any check failed. */
public final class AllTests {
	private AllTests() {}

	public static void main(String[] args) {
		Check.group("zoom (easing, frame blending, settings)", ZoomTest::run);
		System.exit(Check.summary());
	}
}
