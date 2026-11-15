package black.truezoom.client;

/**
 * How far in the view is zoomed, eased toward the target each tick and blended between ticks each frame, so zooming is
 * smooth at any frame rate. No game types: the logic is tested on its own.
 */
final class Zoom {
	private static final double SETTLED = 1e-3;

	private final double factor;
	private final double smoothing;
	private double previous = 1;
	private double current = 1;

	/**
	 * @param factor    how many times closer the view gets at full zoom; at least 1
	 * @param smoothing the share of the remaining distance covered each tick, from just above 0 (slow) to 1 (instant)
	 */
	Zoom(double factor, double smoothing) {
		if (!(factor >= 1)) {
			throw new IllegalArgumentException("zoom factor " + factor + " must be at least 1");
		}
		if (!(smoothing > 0 && smoothing <= 1)) {
			throw new IllegalArgumentException("smoothing " + smoothing + " must be above 0 and at most 1");
		}
		this.factor = factor;
		this.smoothing = smoothing;
	}

	/** One game tick: move toward full zoom while {@code held}, and back to none when not. */
	void tick(boolean held) {
		previous = current;
		double target = held ? factor : 1;
		current += (target - current) * smoothing;
		if (Math.abs(target - current) < SETTLED) {
			current = target;
		}
	}

	/** The zoom between the last two ticks, {@code partialTick} of the way from the earlier one. */
	double level(float partialTick) {
		return previous + (current - previous) * partialTick;
	}

	/** Narrows the field of view by the zoom. */
	float apply(float fov, float partialTick) {
		return (float) (fov / level(partialTick));
	}
}
