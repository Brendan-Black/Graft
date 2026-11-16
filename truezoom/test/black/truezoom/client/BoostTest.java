package black.truezoom.client;

import static black.truezoom.Check.eq;
import static black.truezoom.Check.fails;
import static black.truezoom.Check.ok;

public final class BoostTest {
	private BoostTest() {}

	public static void run() {
		Boost held = new Boost(3.0);
		for (int i = 0; i < 40; i++) {
			eq(0.0, held.tick(true), "holding the key is just the zoom");
		}
		held.tick(false);
		eq(0.0, held.tick(true), "a press right after a long hold isn't a double tap");

		Boost boost = new Boost(3.0);
		tap(boost);
		eq(0.0, boost.tick(false), "a gap");
		eq(Boost.START, boost.tick(true), "the second press starts the boost");
		ok(boost.boosting(), "and the boost is on");
		eq(Boost.START * Boost.GROWTH, boost.tick(true), "each tick held, it's faster");
		double speed = 0;
		for (int i = 0; i < 20; i++) {
			speed = boost.tick(true);
		}
		eq(3.0, speed, "until it reaches the top speed");
		eq(0.0, boost.tick(false), "letting go stops it");
		ok(!boost.boosting(), "and the boost is off");
		eq(0.0, boost.tick(true), "the end of a boost isn't a tap");

		Boost slow = new Boost(3.0);
		tap(slow);
		for (int i = 0; i < Boost.GAP_TICKS + 1; i++) {
			slow.tick(false);
		}
		eq(0.0, slow.tick(true), "a second press too long after the tap is a new press, not a double tap");

		Boost justInTime = new Boost(3.0);
		tap(justInTime);
		for (int i = 0; i < Boost.GAP_TICKS; i++) {
			justInTime.tick(false);
		}
		eq(Boost.START, justInTime.tick(true), "the longest gap still counts");

		Boost jump = new Boost(3.0);
		eq(0.0, jump.rise(true, true), "no boost, no boosted jump");
		tap(jump);
		jump.tick(true);
		eq(Boost.JUMP, jump.rise(true, true), "jumping from the ground during a boost starts at a normal jump's speed");
		jump.tick(true);
		eq(Boost.JUMP * Boost.GROWTH, jump.rise(true, false), "and climbs faster every tick, off the ground now");
		double up = 0;
		for (int i = 0; i < 20; i++) {
			jump.tick(true);
			up = jump.rise(true, false);
		}
		eq(3.0, up, "up to the top speed");
		jump.tick(true);
		eq(0.0, jump.rise(false, false), "letting go of jump stops the climb");
		jump.tick(true);
		eq(0.0, jump.rise(true, false), "and jump in midair doesn't start it again");
		jump.tick(false);
		eq(0.0, jump.rise(true, true), "nor does jumping once the boost is over");

		fails(IllegalArgumentException.class, () -> new Boost(0.1), "a top speed below the start");
		fails(IllegalArgumentException.class, () -> new Boost(Double.NaN), "NaN isn't a speed");
	}

	/** One short press. */
	private static void tap(Boost boost) {
		boost.tick(true);
		boost.tick(false);
	}
}
