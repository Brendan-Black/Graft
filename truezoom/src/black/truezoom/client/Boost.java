package black.truezoom.client;

/**
 * The boost: tap the zoom key, then press it again quickly and hold, and the speed climbs fast, tick by tick, up to a
 * maximum; let go and it stops. Jumping during a boost climbs the same way. No game types: the logic is tested on its own.
 */
final class Boost {
	/** The longest a press can be held and still count as a tap, in ticks. */
	static final int TAP_TICKS = 6;
	/** The longest gap between a tap and the next press for the two to be a double tap, in ticks. */
	static final int GAP_TICKS = 6;
	/** The speed a boost starts at, in blocks per tick: a little above sprinting. */
	static final double START = 0.4;
	/** How much faster each tick of a boost is than the one before. */
	static final double GROWTH = 1.3;
	/** The speed a boosted jump starts at, in blocks per tick: a normal jump's. */
	static final double JUMP = 0.42;
	private static final int NEVER = Integer.MAX_VALUE;

	private final double maximum;
	private boolean wasDown;
	private int heldTicks;
	private int sinceTap = NEVER;
	private boolean boosting;
	private double speed;
	private double rising;

	/** @param maximum the top speed, in blocks per tick; at least {@link #START} */
	Boost(double maximum) {
		if (!(maximum >= START)) {
			throw new IllegalArgumentException("top speed " + maximum + " must be at least " + START + " blocks per tick");
		}
		this.maximum = maximum;
	}

	/** One game tick with the key {@code down} or not; returns the speed to move at, or 0 when not boosting. */
	double tick(boolean down) {
		if (down) {
			if (!wasDown) {
				boosting = sinceTap <= GAP_TICKS;
				sinceTap = NEVER;
				heldTicks = 0;
				speed = 0;
			}
			heldTicks++;
		} else {
			if (wasDown) {
				// A short press is a tap, the first half of a double tap; the end of a long hold or of a boost isn't.
				sinceTap = !boosting && heldTicks <= TAP_TICKS ? 0 : NEVER;
			} else if (sinceTap != NEVER) {
				sinceTap++;
			}
			boosting = false;
		}
		wasDown = down;
		if (!boosting) {
			return 0;
		}
		speed = speed == 0 ? START : Math.min(maximum, speed * GROWTH);
		return speed;
	}

	/**
	 * One game tick of the boosted jump: during a boost, a jump from the ground rises faster every tick, as the boost
	 * does, for as long as jump is held. Returns the speed to rise at, or 0 when not rising. Call after {@link #tick}.
	 *
	 * @param jumping  whether jump is held
	 * @param onGround whether the player stands on something: a boosted jump starts there, not in midair
	 */
	double rise(boolean jumping, boolean onGround) {
		if (!boosting || !jumping) {
			rising = 0;
			return 0;
		}
		if (rising == 0 && !onGround) {
			return 0;
		}
		rising = rising == 0 ? JUMP : Math.min(maximum, rising * GROWTH);
		return rising;
	}

	/** Whether a boost is on: the key was double-tapped and is still held. */
	boolean boosting() {
		return boosting;
	}
}
