package black.graft;

import black.graft.internal.Releases;
import black.version.Version;
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

		// Versions: Graft reads releases only; black.version.Version orders them.
		eq(new Version("26.3"), Releases.parse("26.3"), "a release");
		eq("26.3", Releases.parse("26.3").toString(), "written as given");
		ok(Releases.parse("26.3.0").compareTo(Releases.parse("26.3")) > 0, "26.3.0 is greater than 26.3: the more precise wins");
		ok(Releases.parse("26.3.1").compareTo(Releases.parse("26.3.0")) > 0, "a patch is greater");
		ok(Releases.parse("1.10").compareTo(Releases.parse("1.9")) > 0, "numeric, not text, order");
		List<Version> sorted = new ArrayList<>(List.of(Releases.parse("2.0"), Releases.parse("1.0.1"), Releases.parse("1.0.0"), Releases.parse("1.0")));
		sorted.sort(null);
		eq(List.of(Releases.parse("1.0"), Releases.parse("1.0.0"), Releases.parse("1.0.1"), Releases.parse("2.0")), sorted, "sorts");
		for (String bad : List.of("", "1.0-beta", "26.4-snapshot-8", "01.2", "1.2.3.4", "v1.0", "1..2", "-1", "1.0+build", "1234567890")) {
			fails(IllegalArgumentException.class, () -> Releases.parse(bad), "version \"" + bad + "\" is refused");
		}
		ok(Releases.parse("1.0") instanceof black.TypedString, "a version is a TypedString");
		eq("26.3.0", Releases.full(Releases.parse("26.3")), "in full: missing parts written as zero");
		eq("1.0.0", Releases.full(Releases.parse("1")), "in full, from one part");
		eq("26.3.1", Releases.full(Releases.parse("26.3.1")), "already in full");

		// Ranges
		VersionRange mc = VersionRange.parse("[26.3,26.4)");
		ok(mc.contains(Releases.parse("26.3")), "lower bound included");
		ok(mc.contains(Releases.parse("26.3.9")), "inside");
		ok(mc.contains(Releases.parse("26.3.0")), "26.3.0 is above 26.3, so inside");
		ok(!mc.contains(Releases.parse("26.4")) && !mc.contains(Releases.parse("26.4.0")), "upper bound excluded, and 26.4.0 is above it");
		ok(!mc.contains(Releases.parse("26.2.9")), "below");
		VersionRange from = VersionRange.parse("[0.1,)");
		ok(from.contains(Releases.parse("0.1")) && from.contains(Releases.parse("99")), "open-ended upward");
		VersionRange upTo = VersionRange.parse("(,2.0]");
		ok(upTo.contains(Releases.parse("0.0.1")) && upTo.contains(Releases.parse("2.0")) && !upTo.contains(Releases.parse("2.0.1")),
			"open-ended downward, upper included");
		ok(!upTo.contains(Releases.parse("2.0.0")), "2.0.0 is above 2.0, so outside (,2.0]");
		VersionRange exact = VersionRange.parse("[1.2]");
		ok(exact.contains(Releases.parse("1.2")) && !exact.contains(Releases.parse("1.2.0")) && !exact.contains(Releases.parse("1.2.1")),
			"exact: [1.2] is 1.2 alone, not 1.2.0");
		ok(VersionRange.parse("*").contains(Releases.parse("12345")), "any");
		ok(VersionRange.parse("(1.0,2.0)").contains(Releases.parse("1.5")) && !VersionRange.parse("(1.0,2.0)").contains(Releases.parse("1.0")),
			"exclusive lower bound");
		eq("[26.3,26.4)", mc.toString(), "written back as given");
		eq("[1.2]", exact.toString(), "exact written back");
		eq(mc, VersionRange.parse(" [ 26.3 , 26.4 ) "), "spaces don't matter");
		for (String bad : List.of("1.0", "[1.0", "1.0]", "[2.0,1.0)", "[1.0,1.0)", "(1.0]", "[,1.0)", "(1.0,]", "[1,2,3]", "[a,b)", "")) {
			fails(IllegalArgumentException.class, () -> VersionRange.parse(bad), "range \"" + bad + "\" is refused");
		}
		ok(fails(IllegalArgumentException.class, () -> VersionRange.parse("1.0"), "bare").getMessage().contains("\"1.0\""),
			"the message quotes the range");
	}
}
