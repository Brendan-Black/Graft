package black.graft;

import static black.graft.Check.eq;
import static black.graft.Check.fails;
import static black.graft.Check.ok;

import java.util.ArrayList;
import java.util.List;

final class ValuesTest {
	private ValuesTest() {}

	static void run() {
		// Mod ids
		eq("truezoom", new ModId("truezoom").value(), "a plain mod id");
		eq("a_1", new ModId("a_1").toString(), "digits and _ after the first letter");
		for (String bad : List.of("", "a", "TrueZoom", "1abc", "true-zoom", "true zoom", "x".repeat(65), "trüezoom")) {
			fails(IllegalArgumentException.class, () -> new ModId(bad), "mod id \"" + bad + "\" is refused");
		}
		ok(fails(IllegalArgumentException.class, () -> new ModId("Bad"), "message").getMessage().contains("\"Bad\""),
			"the message names the bad id");

		// Content names
		eq("zoom_block", new ContentName("zoom_block").value(), "a plain name");
		eq("blocks/zoom.v2-b", new ContentName("blocks/zoom.v2-b").value(), "paths, dots and dashes");
		for (String bad : List.of("", "Zoom", "zoom block", "zoom:block", "x".repeat(129))) {
			fails(IllegalArgumentException.class, () -> new ContentName(bad), "content name \"" + bad + "\" is refused");
		}

		// Versions
		eq(new Version(26, 3, 0), Version.parse("26.3"), "missing parts are zero");
		eq(new Version(1, 0, 0), Version.parse("1"), "one part");
		eq("26.3.1", Version.parse("26.3.1").toString(), "written in full");
		ok(Version.parse("26.3").compareTo(Version.parse("26.3.0")) == 0, "26.3 and 26.3.0 are the same");
		ok(Version.parse("1.10").compareTo(Version.parse("1.9")) > 0, "numeric, not text, order");
		List<Version> sorted = new ArrayList<>(List.of(Version.parse("2.0"), Version.parse("1.0.1"), Version.parse("1.0")));
		sorted.sort(null);
		eq(List.of(Version.parse("1.0"), Version.parse("1.0.1"), Version.parse("2.0")), sorted, "sorts");
		for (String bad : List.of("", "1.0-beta", "26.4-snapshot-8", "01.2", "1.2.3.4", "v1.0", "1..2", "-1")) {
			fails(IllegalArgumentException.class, () -> Version.parse(bad), "version \"" + bad + "\" is refused");
		}
		fails(IllegalArgumentException.class, () -> new Version(1, -1, 0), "no negative parts");

		// Ranges
		VersionRange mc = VersionRange.parse("[26.3,26.4)");
		ok(mc.contains(Version.parse("26.3")), "lower bound included");
		ok(mc.contains(Version.parse("26.3.9")), "inside");
		ok(!mc.contains(Version.parse("26.4")), "upper bound excluded");
		ok(!mc.contains(Version.parse("26.2.9")), "below");
		VersionRange from = VersionRange.parse("[0.1,)");
		ok(from.contains(Version.parse("0.1")) && from.contains(Version.parse("99")), "open-ended upward");
		VersionRange upTo = VersionRange.parse("(,2.0]");
		ok(upTo.contains(Version.parse("0.0.1")) && upTo.contains(Version.parse("2.0")) && !upTo.contains(Version.parse("2.0.1")),
			"open-ended downward, upper included");
		VersionRange exact = VersionRange.parse("[1.2]");
		ok(exact.contains(Version.parse("1.2.0")) && !exact.contains(Version.parse("1.2.1")), "exact");
		ok(VersionRange.parse("*").contains(Version.parse("12345")), "any");
		ok(VersionRange.parse("(1.0,2.0)").contains(Version.parse("1.5")) && !VersionRange.parse("(1.0,2.0)").contains(Version.parse("1.0")),
			"exclusive lower bound");
		eq("[26.3.0,26.4.0)", mc.toString(), "written back in full");
		eq("[1.2.0]", exact.toString(), "exact written back");
		eq(mc, VersionRange.parse(" [ 26.3 , 26.4 ) "), "spaces don't matter");
		for (String bad : List.of("1.0", "[1.0", "1.0]", "[2.0,1.0)", "[1.0,1.0)", "(1.0]", "[,1.0)", "(1.0,]", "[1,2,3]", "[a,b)", "")) {
			fails(IllegalArgumentException.class, () -> VersionRange.parse(bad), "range \"" + bad + "\" is refused");
		}
		ok(fails(IllegalArgumentException.class, () -> VersionRange.parse("1.0"), "bare").getMessage().contains("\"1.0\""),
			"the message quotes the range");
	}
}
