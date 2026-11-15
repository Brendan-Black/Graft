package black.graft.loader;

import static black.graft.Check.eq;
import static black.graft.Check.fails;
import static black.graft.Check.ok;

import java.util.List;

import black.graft.Version;

public final class ResolutionTest {
	private static final Version MINECRAFT = Version.parse("26.3");
	private static final Version GRAFT = Version.parse("0.1.0");

	private ResolutionTest() {}

	public static void run() {
		List<ModCandidate> fine = Resolution.resolve(List.of(
			Mods.candidate("zeta", "1.0", "depends.alpha=[1.0,2.0)", "depends.minecraft=[26.3,26.4)", "depends.graft=[0.1,0.2)"),
			Mods.candidate("alpha", "1.5")), MINECRAFT, GRAFT);
		eq(List.of("alpha", "zeta"), Mods.ids(fine), "load order is by id, whatever order they were found in");
		eq(List.of(), Resolution.resolve(List.of(), MINECRAFT, GRAFT), "no mods is fine");

		LoadingException duplicate = fails(LoadingException.class,
			() -> Resolution.resolve(List.of(Mods.candidate("twice", "1.0"), Mods.candidate("twice", "1.1")), MINECRAFT, GRAFT), "duplicate");
		eq(ErrorCode.DUPLICATE_MOD, duplicate.code(), "a mod installed twice");
		ok(duplicate.getMessage().contains("1.0") && duplicate.getMessage().contains("1.1"), "both versions are named");

		LoadingException missing = fails(LoadingException.class,
			() -> Resolution.resolve(List.of(Mods.candidate("needy", "1.0", "depends.absent=[1,)", "depends.minecraft=[27,)")), MINECRAFT, GRAFT),
			"missing");
		eq(ErrorCode.MISSING_DEPENDENCY, missing.code(), "a missing mod outranks a wrong version");
		eq(2, missing.problems().size(), "and both problems are listed: " + missing.problems());
		ok(missing.getMessage().contains("needs mod absent [1.0.0,), which isn't installed"),
			"the missing mod and its range are named");

		LoadingException wrongMinecraft = fails(LoadingException.class,
			() -> Resolution.resolve(List.of(Mods.candidate("old", "1.0", "depends.minecraft=[26.2,26.3)")), MINECRAFT, GRAFT), "minecraft");
		eq(ErrorCode.WRONG_VERSION, wrongMinecraft.code(), "Minecraft at the wrong version");
		ok(wrongMinecraft.getMessage().contains("but 26.3.0 is installed"), "the installed version is named");

		fails(LoadingException.class,
			() -> Resolution.resolve(List.of(Mods.candidate("newer", "1.0", "depends.graft=[0.2,)")), MINECRAFT, GRAFT), "Graft too old");
		fails(LoadingException.class,
			() -> Resolution.resolve(List.of(Mods.candidate("a", "1.0", "depends.bb=[2.0]"), Mods.candidate("bb", "2.1")), MINECRAFT, GRAFT),
			"another mod at the wrong version");
	}
}
