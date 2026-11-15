package black.truezoom.client;

import static black.truezoom.Check.eq;
import static black.truezoom.Check.fails;
import static black.truezoom.Check.ok;
import static black.truezoom.Check.tempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import black.graft.config.Config;

public final class ZoomTest {
	private ZoomTest() {}

	public static void run() throws IOException {
		Zoom zoom = new Zoom(4.0, 0.5);
		eq(70f, zoom.apply(70f, 0.5f), "no zoom until the key is held");

		zoom.tick(true);
		eq(2.5, zoom.level(1f), "half the way to 4x after one tick at 0.5 smoothing");
		eq(1.75, zoom.level(0.5f), "frames between ticks blend the two");
		eq(28f, zoom.apply(70f, 1f), "the field of view narrows by the zoom (70 / 2.5)");

		for (int i = 0; i < 20; i++) {
			zoom.tick(true);
		}
		eq(4.0, zoom.level(1f), "held long enough, it settles exactly on the factor");
		eq(17.5f, zoom.apply(70f, 0.3f), "and stays there between ticks");

		for (int i = 0; i < 20; i++) {
			zoom.tick(false);
		}
		eq(1.0, zoom.level(0f), "released, it eases back to exactly none");

		Zoom instant = new Zoom(10.0, 1.0);
		instant.tick(true);
		eq(10.0, instant.level(1f), "smoothing 1.0 is instant");

		fails(IllegalArgumentException.class, () -> new Zoom(0.5, 0.5), "a factor below 1 would zoom out");
		fails(IllegalArgumentException.class, () -> new Zoom(4.0, 0.0), "smoothing 0 would never move");
		fails(IllegalArgumentException.class, () -> new Zoom(Double.NaN, 0.5), "NaN isn't a factor");

		Path file = tempDir("settings").resolve("truezoom-client.properties");
		Config defaults = Config.load(ZoomSettings.SPEC, file, "True Zoom client settings");
		eq(4.0, defaults.get(ZoomSettings.FACTOR), "4x by default");
		eq(0.5, defaults.get(ZoomSettings.SMOOTHING), "half-way per tick by default");
		ok(Files.readString(file).contains("zoom_factor=4.0"), "the settings file is written for players to edit");
	}
}
