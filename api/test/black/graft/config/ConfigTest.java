package black.graft.config;

import static black.graft.Check.eq;
import static black.graft.Check.fails;
import static black.graft.Check.ok;
import static black.graft.Check.tempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public final class ConfigTest {
	private static final Setting.Decimal FACTOR = new Setting.Decimal("zoom_factor", 4.0, 1.0, 50.0, "How many times closer.");
	private static final Setting.WholeNumber STEPS = new Setting.WholeNumber("steps", 3, 1, 10, "Scroll steps.\nOne per notch.");
	private static final Setting.Flag SMOOTH = new Setting.Flag("smooth", true, "Ease in and out.");
	private static final ConfigSpec SPEC = ConfigSpec.of(FACTOR, STEPS, SMOOTH);

	private ConfigTest() {}

	public static void run() throws IOException {
		Path dir = tempDir("config");
		Path file = dir.resolve("config").resolve("truezoom.properties");

		// First run: defaults, and a file a player can read and edit.
		Config fresh = Config.load(SPEC, file, "True Zoom settings");
		eq(4.0, fresh.get(FACTOR), "a decimal default");
		eq(3, fresh.get(STEPS), "a whole number default");
		eq(true, fresh.get(SMOOTH), "a flag default");
		String written = Files.readString(file);
		ok(written.startsWith("# True Zoom settings\n"), "the file starts with its title");
		ok(written.contains("# How many times closer.\n# 1.0 to 50.0, default 4.0.\nzoom_factor=4.0\n"), "each setting has its comment and range");
		ok(written.contains("# Scroll steps.\n# One per notch.\n"), "a multi-line comment stays comments");
		try (var files = Files.list(file.getParent())) {
			eq(List.of(file), files.toList(), "no temporary file is left behind");
		}

		// A player's edits are read, and kept when a newer mod adds a setting.
		Files.writeString(file, "zoom_factor=8.5\nsteps = 10\n");
		Config edited = Config.load(SPEC, file, "True Zoom settings");
		eq(8.5, edited.get(FACTOR), "an edited decimal");
		eq(10, edited.get(STEPS), "an edited whole number, at the edge of its range");
		eq(true, edited.get(SMOOTH), "a setting missing from the file gets its default");
		String rewritten = Files.readString(file);
		ok(rewritten.contains("zoom_factor=8.5") && rewritten.contains("steps=10") && rewritten.contains("smooth=true"),
			"the file is rewritten with the new setting, keeping the player's values");

		Files.writeString(file, "zoom_factor=4.0\nsteps=3\nsmooth=TRUE\n");
		Config.load(SPEC, file, "title");
		eq("zoom_factor=4.0\nsteps=3\nsmooth=TRUE\n", Files.readString(file), "a complete file isn't touched");

		// A setting the mod no longer has doesn't stop the game: it leaves the file, and stays as a comment.
		Files.writeString(file, "zoom_factor=8.5\nsteps=3\nsmooth=false\nold_mode=fast\n");
		Config retired = Config.load(SPEC, file, "True Zoom settings");
		eq(8.5, retired.get(FACTOR), "the other settings are read as the player set them");
		String withoutOld = Files.readString(file);
		ok(!withoutOld.contains("\nold_mode="), "the old setting is taken out of the file: " + withoutOld);
		ok(withoutOld.endsWith("# Settings this mod no longer has, as they were:\n# old_mode=fast\n"), "and kept at its end, as a comment");
		ok(withoutOld.contains("zoom_factor=8.5") && withoutOld.contains("smooth=false"), "the player's values are kept");
		eq(8.5, Config.load(SPEC, file, "title").get(FACTOR), "the rewritten file reads back");
		eq(withoutOld, Files.readString(file), "and isn't rewritten again");

		// Values that don't fit stop the game and say exactly what's wrong, all at once.
		Files.writeString(file, "zoom_factor=0.5\nsteps=lots\nsmooth=yes\nzoomfactor=2\n");
		ConfigException e = fails(ConfigException.class, () -> Config.load(SPEC, file, "title"), "a file with mistakes");
		eq(3, e.problems().size(), "every problem: " + e.problems());
		ok(e.problems().stream().anyMatch(p -> p.startsWith("zoom_factor: 0.5 isn't from 1.0 to 50.0")), "out of range");
		ok(e.problems().stream().anyMatch(p -> p.startsWith("steps: \"lots\" isn't a whole number")), "not a number");
		ok(e.problems().stream().anyMatch(p -> p.startsWith("smooth: \"yes\" isn't true or false")), "not a flag");
		eq(file, e.file(), "the file is named");
		eq("zoom_factor=0.5\nsteps=lots\nsmooth=yes\nzoomfactor=2\n", Files.readString(file), "a file with mistakes is left as the player wrote it");

		Files.writeString(file, "zoom_factor=NaN\n");
		fails(ConfigException.class, () -> Config.load(SPEC, file, "title"), "NaN isn't a number in range");

		// Programming mistakes fail early, in the mod author's own run.
		fails(IllegalArgumentException.class, () -> fresh.get(new Setting.Flag("other", false, "")), "a setting from another spec");
		fails(IllegalArgumentException.class, () -> ConfigSpec.of(SMOOTH, new Setting.Flag("smooth", false, "")), "two settings with one key");
		fails(IllegalArgumentException.class, () -> new Setting.WholeNumber("n", 11, 1, 10, ""), "a default outside its range");
		fails(IllegalArgumentException.class, () -> new Setting.Decimal("d", 1.0, 2.0, 1.0, ""), "an empty range");
		fails(IllegalArgumentException.class, () -> new Setting.Flag("has space", true, ""), "a key that isn't a key");
	}
}
