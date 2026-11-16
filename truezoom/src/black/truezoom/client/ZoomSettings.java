package black.truezoom.client;

import black.graft.config.ConfigSpec;
import black.graft.config.Setting;

/** True Zoom's settings, in {@code config/truezoom-client.properties}. */
final class ZoomSettings {
	static final Setting.Decimal FACTOR = new Setting.Decimal("zoom_factor", 4.0, 1.0, 50.0,
		"How many times closer the view gets while the zoom key is held.");
	static final Setting.Decimal SMOOTHING = new Setting.Decimal("smoothing", 0.5, 0.05, 1.0,
		"How quickly the zoom moves: the share of the way it goes each tick. 1.0 is instant.");
	static final Setting.Decimal BOOST_SPEED = new Setting.Decimal("boost_top_speed", 60.0, 10.0, 100.0,
		"The top speed of the boost (double-tap the zoom key and hold), forward and up, in blocks per second. Sprinting is about 5.6.");
	static final ConfigSpec SPEC = ConfigSpec.of(FACTOR, SMOOTHING, BOOST_SPEED);

	private ZoomSettings() {}
}
