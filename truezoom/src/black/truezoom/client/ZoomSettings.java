package black.truezoom.client;

import black.graft.config.ConfigSpec;
import black.graft.config.Setting;

/** True Zoom's settings, in {@code config/truezoom-client.properties}. */
final class ZoomSettings {
	static final Setting.Decimal FACTOR = new Setting.Decimal("zoom_factor", 4.0, 1.0, 50.0,
		"How many times closer the view gets while the zoom key is held.");
	static final Setting.Decimal SMOOTHING = new Setting.Decimal("smoothing", 0.5, 0.05, 1.0,
		"How quickly the zoom moves: the share of the way it goes each tick. 1.0 is instant.");
	static final ConfigSpec SPEC = ConfigSpec.of(FACTOR, SMOOTHING);

	private ZoomSettings() {}
}
